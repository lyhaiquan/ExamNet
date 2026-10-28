package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import labcast.client.net.LcpClient;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;

/**
 * Quản trị quản lý lớp: mọi lớp trên server kèm giáo viên phụ trách và số học viên; tạo lớp và giao cho một giáo
 * viên, đổi tên, đổi giáo viên, xoá lớp chưa có học viên. Học viên vào lớp bằng nhập Excel hoặc màn Tài khoản.
 */
public final class ClassesScreen extends BorderPane {
    /** Một dòng trong bảng. Getter cho PropertyValueFactory. */
    public record Row(
            long id, String name, long teacherId, String teacher, int students, String joinCode, String created) {
        public String getName() {
            return name;
        }

        public String getTeacher() {
            return teacher;
        }

        public String getStudentCount() {
            return Integer.toString(students);
        }

        public String getJoinCode() {
            return joinCode.isBlank() ? "—" : joinCode;
        }

        public String getCreated() {
            return created;
        }
    }

    /** Một giáo viên để chọn khi tạo / sửa lớp. */
    record TeacherItem(long id, String fullName, String username, boolean locked) {
        @Override
        public String toString() {
            return fullName + " (" + username + ")" + (locked ? " · đang bị khoá" : "");
        }
    }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LcpClient client;
    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final FilteredList<Row> visible = new FilteredList<>(rows);
    private final TableView<Row> table = new TableView<>(visible);
    private final TextField search = new TextField();
    private final Label feedback = new Label("Đang tải…");
    private final Label classCount = new Label("0");
    private final Label studentCount = new Label("0");
    private final Label emptyCount = new Label("0");
    private List<TeacherItem> teachers = List.of();
    private Runnable onChanged = () -> {};
    private boolean loading;

    public ClassesScreen(LcpClient client) {
        this.client = client;
        getStyleClass().add("students-dashboard");
        setPadding(new Insets(25, 30, 25, 30));

        Label heading = new Label("Quản lý lớp");
        heading.getStyleClass().add("workspace-heading");
        Label description = new Label("Tạo lớp và giao cho giáo viên phụ trách. Học viên vào lớp bằng \"Nhập danh sách"
                + " lớp (Excel)\" ở trang chủ hoặc ở màn Tài khoản.");
        description.getStyleClass().add("muted");
        description.setWrapText(true);
        Button add = new Button("+  Tạo lớp");
        add.setId("class-add");
        add.getStyleClass().add("primary");
        add.setOnAction(e -> editDialog(null));
        VBox headingText = new VBox(6, heading, description);
        HBox.setHgrow(headingText, Priority.ALWAYS);
        HBox headingBox = new HBox(12, headingText, add);
        headingBox.setAlignment(Pos.CENTER_LEFT);

        HBox metrics = new HBox(
                12,
                stat("SỐ LỚP", classCount, "students-stat-total"),
                stat("HỌC VIÊN ĐÃ VÀO LỚP", studentCount, "students-stat-online"),
                stat("LỚP CHƯA CÓ HỌC VIÊN", emptyCount, "students-stat-working"));
        metrics.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        search.setPromptText("Tìm theo tên lớp, giáo viên…");
        search.getStyleClass().add("students-search");
        search.textProperty().addListener((o, a, b) -> applyFilter());
        HBox.setHgrow(search, Priority.ALWAYS);
        Button refresh = new Button("↻ Làm mới");
        refresh.getStyleClass().add("students-refresh");
        refresh.setOnAction(e -> reload());
        HBox controls = new HBox(10, search, refresh);
        controls.setAlignment(Pos.CENTER_LEFT);
        feedback.getStyleClass().add("muted");
        VBox header = new VBox(16, headingBox, metrics, controls, feedback);
        header.setPadding(new Insets(0, 0, 16, 0));
        setTop(header);

        table.getColumns().add(AccountsScreen.orderColumn());
        column("TÊN LỚP", "name", 260);
        column("GIÁO VIÊN", "teacher", 180);
        column("HỌC VIÊN", "studentCount", 90).setMinWidth(80);
        column("MÃ MỜI", "joinCode", 90).setMinWidth(80);
        column("NGÀY TẠO", "created", 100).setMinWidth(90);
        TableColumn<Row, String> actions = new TableColumn<>("THAO TÁC");
        actions.setPrefWidth(150);
        actions.setMinWidth(140);
        actions.setSortable(false);
        actions.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                Row r = empty || getTableRow() == null ? null : getTableRow().getItem();
                setGraphic(r == null ? null : actionsFor(r));
            }
        });
        table.getColumns().add(actions);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Chưa có lớp nào. Bấm \"+ Tạo lớp\" để bắt đầu."));
        table.getStyleClass().add("students-table");
        table.setFixedCellSize(50);
        setCenter(table);
        reload();
    }

    /** Gọi sau mỗi lần tạo, sửa, xoá lớp thành công (trang chủ tải lại ô chọn lớp). */
    public void onChanged(Runnable r) {
        this.onChanged = r == null ? () -> {} : r;
    }

    private static VBox stat(String caption, Label number, String style) {
        Label title = new Label(caption);
        title.getStyleClass().add("students-stat-caption");
        number.getStyleClass().add("students-stat-number");
        VBox card = new VBox(8, title, number);
        card.getStyleClass().addAll("students-stat-card", style);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private TableColumn<Row, String> column(String title, String property, int width) {
        TableColumn<Row, String> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        c.setPrefWidth(width);
        table.getColumns().add(c);
        return c;
    }

    private Node actionsFor(Row r) {
        Button edit = small("Sửa");
        edit.setOnAction(e -> editDialog(r));
        Button delete = small("Xoá");
        delete.getStyleClass().add("account-lock");
        delete.setOnAction(e -> confirmDelete(r));
        HBox box = new HBox(6, edit, delete);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static Button small(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("students-reset");
        b.setMinWidth(Region.USE_PREF_SIZE);
        return b;
    }

    private Window window() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private void applyFilter() {
        String q = search.getText() == null ? "" : search.getText().strip().toLowerCase();
        visible.setPredicate(r -> (r.name() + " " + r.teacher()).toLowerCase().contains(q));
    }

    /** Tải lại danh sách lớp và giáo viên. */
    public void reload() {
        if (loading) return;
        loading = true;
        var reply = client.expect(MessageType.ADMIN_CLASS_DATA, MessageType.ERROR);
        client.send(MessageType.ADMIN_CLASS_LIST, new byte[0]);
        reply.orTimeout(12, TimeUnit.SECONDS)
                .whenComplete((f, err) -> Platform.runLater(() -> {
                    loading = false;
                    try {
                        if (err != null) {
                            feedback.setText("Server không trả lời");
                        } else if (f.type() == MessageType.ERROR) {
                            feedback.setText(ErrorMessage.from(f).message());
                        } else {
                            show(JsonPayload.tree(f));
                        }
                    } catch (Exception e) {
                        feedback.setText(e.getMessage());
                    }
                }));
    }

    private void show(JsonNode data) {
        List<TeacherItem> ts = new ArrayList<>();
        for (JsonNode t : data.path("teachers")) {
            ts.add(new TeacherItem(
                    t.path("id").asLong(),
                    t.path("fullName").asText(),
                    t.path("username").asText(),
                    t.path("locked").asBoolean()));
        }
        teachers = ts;
        List<Row> list = new ArrayList<>();
        int students = 0;
        int empty = 0;
        for (JsonNode c : data.path("classes")) {
            int n = c.path("students").asInt();
            students += n;
            if (n == 0) empty++;
            long at = c.path("createdAt").asLong();
            list.add(new Row(
                    c.path("id").asLong(),
                    c.path("name").asText(),
                    c.path("teacherId").asLong(),
                    c.path("teacher").asText(),
                    n,
                    c.path("joinCode").asText(""),
                    at <= 0 ? "—" : DAY.format(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()))));
        }
        rows.setAll(list);
        classCount.setText(Integer.toString(list.size()));
        studentCount.setText(Integer.toString(students));
        emptyCount.setText(Integer.toString(empty));
        feedback.setText(list.size() + " lớp · " + ts.size() + " giáo viên");
        applyFilter();
    }

    private void confirmDelete(Row r) {
        if (r.students() > 0) {
            info("Lớp " + r.name() + " còn " + r.students() + " học viên. Chuyển học viên sang lớp khác ở màn Tài khoản"
                    + " (nút Sửa từng học viên) rồi mới xoá được lớp.");
            return;
        }
        Alert a = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Xoá lớp " + r.name()
                        + "? Lớp chưa có học viên nào; mã mời và phòng thực hành trống của lớp cũng bị xoá.",
                ButtonType.OK,
                ButtonType.CANCEL);
        a.setHeaderText(null);
        if (window() != null) a.initOwner(window());
        Theme.apply(a.getDialogPane());
        a.showAndWait()
                .filter(b -> b == ButtonType.OK)
                .ifPresent(b -> send(
                        Frame.of(
                                MessageType.ADMIN_CLASS_DELETE,
                                new PayloadWriter().i64(r.id()).toBytes()),
                        msg -> {
                            feedback.setText("✓ " + msg);
                            reload();
                            onChanged.run();
                        },
                        this::info));
    }

    /** Tạo ({@code r} {@code null}) hoặc sửa một lớp: tên và giáo viên phụ trách. */
    Dialog<ButtonType> editDialog(Row r) {
        boolean creating = r == null;
        TextField name = new TextField(creating ? "" : r.name());
        name.setPromptText("vd. Lớp D22CQCN04 – Cấu trúc dữ liệu và giải thuật");
        name.setPrefColumnCount(30);
        ComboBox<TeacherItem> teacher = new ComboBox<>(FXCollections.observableArrayList(teachers));
        teacher.setMaxWidth(Double.MAX_VALUE);
        teacher.setPromptText(teachers.isEmpty() ? "Chưa có giáo viên: tạo ở màn Tài khoản trước" : "Chọn giáo viên");
        if (!creating) {
            teachers.stream().filter(t -> t.id() == r.teacherId()).findFirst().ifPresent(teacher::setValue);
        }
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.addRow(0, new Label("Tên lớp"), name);
        form.addRow(1, new Label("Giáo viên"), teacher);
        GridPane.setHgrow(name, Priority.ALWAYS);
        GridPane.setHgrow(teacher, Priority.ALWAYS);
        Label hint = new Label(
                creating
                        ? "Lớp thuộc giáo viên được chọn: giáo viên thấy lớp này ở trang chủ và Lớp thực hành."
                        : "Đổi giáo viên thì giáo viên cũ không còn quản lý lớp này; học viên và bài làm giữ nguyên.");
        hint.setWrapText(true);
        hint.setMaxWidth(440);
        hint.getStyleClass().add("muted");
        Label error = new Label();
        error.setWrapText(true);
        error.setStyle("-fx-text-fill: #EF4444;");

        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(creating ? "Tạo lớp" : "Sửa lớp");
        d.setHeaderText(creating ? "Tạo lớp mới" : "Sửa " + r.name());
        if (window() != null) d.initOwner(window());
        Theme.apply(d.getDialogPane());
        d.getDialogPane().setContent(new VBox(12, form, hint, error));
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        ok.setText(creating ? "Tạo" : "Lưu");
        ((Button) d.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Huỷ");
        Platform.runLater(name::requestFocus);
        ok.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume();
            String n = name.getText() == null ? "" : name.getText().strip();
            if (n.isEmpty()) {
                error.setText("Nhập tên lớp");
                return;
            }
            if (n.length() > 100) {
                error.setText("Tên lớp dài quá 100 ký tự");
                return;
            }
            if (teacher.getValue() == null) {
                error.setText("Chọn giáo viên phụ trách");
                return;
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", creating ? 0 : r.id());
            body.put("name", n);
            body.put("teacherId", teacher.getValue().id());
            ok.setDisable(true);
            error.setText("");
            send(
                    JsonPayload.frame(MessageType.ADMIN_CLASS_SAVE, body),
                    msg -> {
                        d.close();
                        feedback.setText("✓ " + msg);
                        reload();
                        onChanged.run();
                    },
                    err -> {
                        ok.setDisable(false);
                        error.setText(err);
                    });
        });
        d.show();
        return d;
    }

    /** Gửi một lệnh quản trị, chờ {@code ADMIN_OK} hoặc {@code ERROR}, gọi lại trên luồng giao diện. */
    private void send(Frame f, Consumer<String> onOk, Consumer<String> onError) {
        var reply = client.expect(MessageType.ADMIN_OK, MessageType.ERROR);
        client.send(f);
        reply.orTimeout(15, TimeUnit.SECONDS)
                .whenComplete((r, err) -> Platform.runLater(() -> {
                    try {
                        if (err != null) onError.accept("Server không trả lời, thử lại sau");
                        else if (r.type() == MessageType.ADMIN_OK)
                            onOk.accept(r.reader().str());
                        else onError.accept(ErrorMessage.from(r).message());
                    } catch (Exception e) {
                        onError.accept(e.getMessage());
                    }
                }));
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setHeaderText(null);
        if (window() != null) a.initOwner(window());
        Theme.apply(a.getDialogPane());
        a.show();
    }
}
