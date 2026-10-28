package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import labcast.client.net.LcpClient;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;

/** Bảng quản lý sinh viên: dữ liệu thực từ enrollments, progress, Session và activity. */
public final class StudentManagementScreen extends BorderPane {
    public record StudentRow(
            String name,
            String username,
            String connection,
            String activity,
            String lesson,
            String practice,
            String completed,
            String updated) {
        public String getName() {
            return name;
        }

        public String getUsername() {
            return username;
        }

        public String getConnection() {
            return connection;
        }

        public String getActivity() {
            return activity;
        }

        public String getLesson() {
            return lesson;
        }

        public String getPractice() {
            return practice;
        }

        public String getCompleted() {
            return completed;
        }

        public String getUpdated() {
            return updated;
        }
    }

    private final LcpClient client;
    private final TableView<StudentRow> table = new TableView<>();
    private final ObservableList<StudentRow> records = FXCollections.observableArrayList();
    private final FilteredList<StudentRow> visible = new FilteredList<>(records);
    private final TextField search = new TextField();
    private final ComboBox<String> filter = new ComboBox<>();
    private final Label count = new Label("0");
    private final Label online = new Label("0");
    private final Label working = new Label("0");
    private final Label feedback = new Label("Đang đồng bộ...");
    private final Label details = new Label("Chọn một sinh viên để xem chi tiết hoạt động.");
    private final PasswordRequestsBanner pwdRequests;
    private final Timeline timer;
    private boolean loading;
    private volatile boolean closed;
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault());

    public StudentManagementScreen(LcpClient client) {
        this.client = client;
        getStyleClass().add("students-dashboard");
        setPadding(new Insets(25, 30, 25, 30));
        Label heading = new Label("Quản lý sinh viên");
        heading.getStyleClass().add("workspace-heading");
        Label description = new Label("Danh sách, trạng thái trực tuyến và tiến độ học tập trong lớp hiện tại.");
        description.getStyleClass().add("muted");
        VBox headingBox = new VBox(6, heading, description);
        VBox totalCard = statCard("TỔNG SINH VIÊN", count, "students-stat-total");
        VBox onlineCard = statCard("ĐANG TRỰC TUYẾN", online, "students-stat-online");
        VBox workingCard = statCard("ĐANG LÀM BÀI", working, "students-stat-working");
        HBox metrics = new HBox(12, totalCard, onlineCard, workingCard);
        for (VBox card : new VBox[] {totalCard, onlineCard, workingCard}) HBox.setHgrow(card, Priority.ALWAYS);
        metrics.getStyleClass().add("students-metrics");
        search.setPromptText("Tìm theo họ tên, tài khoản, bài đang làm...");
        search.getStyleClass().add("students-search");
        search.setMinHeight(46);
        search.setPrefHeight(46);
        search.setMaxHeight(46);
        search.textProperty().addListener((obs, old, value) -> applyFilter());
        filter.getItems().addAll("Tất cả", "Trực tuyến", "Ngoại tuyến", "Đang làm bài");
        filter.getSelectionModel().selectFirst();
        filter.setMinWidth(175);
        filter.setMinHeight(46);
        filter.setPrefHeight(46);
        filter.setMaxHeight(46);
        filter.getStyleClass().add("students-filter");
        filter.setOnAction(e -> applyFilter());
        Button refresh = new Button("↻ Làm mới");
        refresh.getStyleClass().add("students-refresh");
        refresh.setMinHeight(46);
        refresh.setPrefHeight(46);
        refresh.setMaxHeight(46);
        refresh.setOnAction(e -> reload());
        HBox controls = new HBox(10, search, filter, refresh);
        controls.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);
        feedback.getStyleClass().add("muted");
        pwdRequests = new PasswordRequestsBanner(client, this::reload);
        VBox header = new VBox(17, headingBox, pwdRequests, metrics, controls, feedback);
        header.setPadding(new Insets(0, 0, 18, 0));
        setTop(header);

        table.getColumns().add(AccountsScreen.orderColumn());
        addColumn("HỌ VÀ TÊN", "name", 180);
        addColumn("TÀI KHOẢN", "username", 120);
        TableColumn<StudentRow, String> connection = addColumn("KẾT NỐI", "connection", 125);
        connection.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value);
                getStyleClass().removeAll("students-online-cell", "students-offline-cell");
                if (!empty)
                    getStyleClass().add("Trực tuyến".equals(value) ? "students-online-cell" : "students-offline-cell");
            }
        });
        addColumn("ĐANG LÀM GÌ", "activity", 190);
        addColumn("MÃ BÀI", "lesson", 150);
        addColumn("BUỔI THỰC HÀNH", "practice", 150);
        addColumn("ĐÃ XONG", "completed", 85);
        addColumn("CẬP NHẬT", "updated", 110);
        // đặt lại mật khẩu cho học viên quên mật khẩu (server chỉ cho học viên lớp mình dạy)
        TableColumn<StudentRow, String> pwd = new TableColumn<>("MẬT KHẨU");
        pwd.setPrefWidth(110);
        pwd.setSortable(false);
        pwd.setCellFactory(col -> new TableCell<>() {
            private final Button reset = new Button("Đặt lại");

            {
                reset.getStyleClass().add("students-reset");
                reset.setOnAction(e -> {
                    StudentRow row =
                            getTableRow() == null ? null : getTableRow().getItem();
                    if (row != null) {
                        PasswordDialogs.reset(getScene().getWindow(), client, row.username(), row.name());
                    }
                });
            }

            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setGraphic(empty ? null : reset);
            }
        });
        table.getColumns().add(pwd);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(visible);
        table.setPlaceholder(new Label("Chưa có sinh viên trong lớp đang chọn."));
        table.getStyleClass().add("students-table");
        table.setFixedCellSize(50);
        table.getSelectionModel().selectedItemProperty().addListener((o, old, row) -> {
            if (row == null) details.setText("Chọn một sinh viên để xem chi tiết hoạt động.");
            else
                details.setText(row.name() + " (" + row.username() + ")   •   " + row.connection()
                        + "   •   " + row.activity() + "   •   Bài: " + row.lesson()
                        + "   •   " + row.updated());
        });
        details.getStyleClass().add("students-detail-label");
        details.setWrapText(true);
        Label infoTip = new Label("Chi tiết sinh viên được chọn");
        infoTip.getStyleClass().add("students-detail-caption");
        VBox info = new VBox(8, section("Thông tin hoạt động"), infoTip, details);
        info.getStyleClass().add("students-detail-card");
        VBox container = new VBox(14, table, info);
        VBox.setVgrow(table, Priority.ALWAYS);
        setCenter(container);
        timer = new Timeline(new KeyFrame(Duration.seconds(5), e -> reload()));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
        reload();
    }

    private Label section(String title) {
        Label label = new Label(title);
        label.getStyleClass().add("workspace-section-title");
        return label;
    }

    private VBox statCard(String label, Label number, String style) {
        Label title = new Label(label);
        title.getStyleClass().add("students-stat-caption");
        number.getStyleClass().add("students-stat-number");
        VBox card = new VBox(8, title, number);
        card.getStyleClass().addAll("students-stat-card", style);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinHeight(105);
        return card;
    }

    private TableColumn<StudentRow, String> addColumn(String title, String property, int width) {
        TableColumn<StudentRow, String> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setPrefWidth(width);
        table.getColumns().add(col);
        return col;
    }

    private void applyFilter() {
        String q = search.getText() == null ? "" : search.getText().strip().toLowerCase();
        String option = filter.getValue();
        visible.setPredicate(row -> {
            if (!(row.name() + " " + row.username() + " " + row.activity() + " " + row.lesson())
                    .toLowerCase()
                    .contains(q)) return false;
            if ("Trực tuyến".equals(option) && !"Trực tuyến".equals(row.connection())) return false;
            if ("Ngoại tuyến".equals(option) && !"Ngoại tuyến".equals(row.connection())) return false;
            return !"Đang làm bài".equals(option)
                    || ("Trực tuyến".equals(row.connection())
                            && row.lesson() != null
                            && !row.lesson().isBlank()
                            && !"—".equals(row.lesson()));
        });
    }

    public void stop() {
        closed = true;
        timer.stop();
    }

    public void reload() {
        if (closed || loading) return;
        loading = true;
        pwdRequests.reload();
        var pending = client.expect(MessageType.STUDENT_OVERVIEW_DATA, MessageType.ERROR);
        client.send(MessageType.STUDENT_OVERVIEW_REQ, new byte[0]);
        pending.orTimeout(12, TimeUnit.SECONDS)
                .whenComplete((frame, error) -> Platform.runLater(() -> {
                    loading = false;
                    if (closed) return;
                    if (error != null || frame.type() == MessageType.ERROR) {
                        try {
                            feedback.setText(
                                    error != null
                                            ? error.getMessage()
                                            : ErrorMessage.from(frame).message());
                        } catch (Exception ignored) {
                            feedback.setText("Server không trả dữ liệu sinh viên.");
                        }
                        return;
                    }
                    try {
                        JsonNode items = JsonPayload.tree(frame).path("students");
                        records.clear();
                        int onlineCount = 0, activeCount = 0;
                        for (JsonNode r : items) {
                            boolean connected = r.path("online").asBoolean();
                            if (connected) onlineCount++;
                            String lesson = r.path("lessonId").asText("");
                            if (connected && !lesson.isBlank()) activeCount++;
                            long ts = r.path("updatedAt").asLong();
                            records.add(new StudentRow(
                                    r.path("fullName").asText(),
                                    r.path("username").asText(),
                                    connected ? "Trực tuyến" : "Ngoại tuyến",
                                    r.path("activity").asText("—"),
                                    lesson.isBlank() ? "—" : lesson,
                                    r.path("practice").asText("—"),
                                    Integer.toString(r.path("completed").asInt()),
                                    ts <= 0 ? "—" : DATE.format(Instant.ofEpochMilli(ts))));
                        }
                        count.setText(Integer.toString(records.size()));
                        online.setText(Integer.toString(onlineCount));
                        working.setText(Integer.toString(activeCount));
                        feedback.setText("Đồng bộ dữ liệu mỗi 5 giây · " + records.size() + " sinh viên");
                        applyFilter();
                    } catch (Exception ex) {
                        feedback.setText("Dữ liệu không hợp lệ: " + ex.getMessage());
                    }
                }));
    }
}
