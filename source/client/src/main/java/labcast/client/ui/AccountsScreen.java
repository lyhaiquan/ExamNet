package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import labcast.client.net.LcpClient;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;

/**
 * Quản trị quản lý tài khoản: mọi giáo viên, học viên, quản trị trong hệ thống; thêm giáo viên, học viên (mật khẩu
 * tạm, lần đầu đăng nhập họ tự đặt mật khẩu riêng); sửa họ tên, chuyển lớp; đặt lại mật khẩu; khoá, mở khoá.
 */
public final class AccountsScreen extends BorderPane {
    /** Một dòng trong bảng. Getter cho PropertyValueFactory. */
    public record Row(
            long id,
            String fullName,
            String username,
            String role,
            String roleLabel,
            String classes,
            long classId,
            String status,
            boolean locked,
            boolean mustChange,
            String tempPassword) {
        public String getFullName() {
            return fullName;
        }

        public String getUsername() {
            return username;
        }

        public String getRoleLabel() {
            return roleLabel;
        }

        public String getClasses() {
            return classes;
        }

        public String getStatus() {
            return status;
        }
    }

    /** Một lớp để chọn khi thêm / chuyển học viên. */
    record ClassItem(long id, String name, String teacher) {
        @Override
        public String toString() {
            return name + (teacher.isBlank() ? "" : "  ·  " + teacher);
        }
    }

    private final LcpClient client;
    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final FilteredList<Row> visible = new FilteredList<>(rows);
    private final TableView<Row> table = new TableView<>(visible);
    private final TextField search = new TextField();
    private final ComboBox<String> roleFilter = new ComboBox<>();
    private final ComboBox<String> classFilter = new ComboBox<>();
    private final Label feedback = new Label("Đang tải…");
    private final Label teachers = new Label("0");
    private final Label students = new Label("0");
    private final Label locked = new Label("0");
    private List<ClassItem> classes = List.of();
    private boolean loading;

    public AccountsScreen(LcpClient client) {
        this.client = client;
        getStyleClass().add("students-dashboard");
        setPadding(new Insets(25, 30, 25, 30));

        Label heading = new Label("Tài khoản");
        heading.getStyleClass().add("workspace-heading");
        Label description = new Label(
                "Giáo viên và học viên do quản trị tạo. Tài khoản quản trị tạo trong file cấu" + " hình server.");
        description.getStyleClass().add("muted");
        description.setWrapText(true);
        Button add = new Button("+  Thêm tài khoản");
        add.setId("account-add");
        add.getStyleClass().add("primary");
        add.setOnAction(e -> editDialog(null));
        HBox.setHgrow(description, Priority.ALWAYS);
        VBox headingText = new VBox(6, heading, description);
        HBox.setHgrow(headingText, Priority.ALWAYS);
        HBox headingBox = new HBox(12, headingText, add);
        headingBox.setAlignment(Pos.CENTER_LEFT);

        HBox metrics = new HBox(
                12,
                stat("GIÁO VIÊN", teachers, "students-stat-total"),
                stat("HỌC VIÊN", students, "students-stat-online"),
                stat("ĐANG BỊ KHOÁ", locked, "students-stat-working"));
        metrics.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        search.setPromptText("Tìm theo họ tên, tài khoản, lớp…");
        search.getStyleClass().add("students-search");
        search.textProperty().addListener((o, a, b) -> applyFilter());
        HBox.setHgrow(search, Priority.ALWAYS);
        roleFilter.getItems().addAll("Mọi vai trò", "Giáo viên", "Học viên", "Quản trị", "Đang bị khoá");
        roleFilter.getSelectionModel().selectFirst();
        roleFilter.getStyleClass().add("students-filter");
        roleFilter.setOnAction(e -> applyFilter());
        classFilter.getItems().add(ALL_CLASSES);
        classFilter.getSelectionModel().selectFirst();
        classFilter.getStyleClass().add("students-filter");
        classFilter.valueProperty().addListener((o, a, b) -> applyFilter());
        Button refresh = new Button("↻ Làm mới");
        refresh.getStyleClass().add("students-refresh");
        refresh.setOnAction(e -> reload());
        // lúc nào cũng xuất được: server giữ mật khẩu tạm tới khi người dùng tự đặt mật khẩu riêng
        Button export = new Button("⬇ Xuất Excel");
        export.setId("account-export");
        export.getStyleClass().add("students-refresh");
        export.setTooltip(new Tooltip("Xuất các tài khoản đang hiện (lọc theo lớp trước) kèm mật khẩu tạm, gửi giáo"
                + " viên phát cho học viên"));
        export.setOnAction(e -> exportVisible());
        HBox controls = new HBox(10, search, classFilter, roleFilter, refresh, export);
        controls.setAlignment(Pos.CENTER_LEFT);
        feedback.getStyleClass().add("muted");
        VBox header = new VBox(16, headingBox, metrics, controls, feedback);
        header.setPadding(new Insets(0, 0, 16, 0));
        setTop(header);

        table.getColumns().add(orderColumn());
        column("HỌ VÀ TÊN", "fullName", 180);
        column("TÀI KHOẢN", "username", 120);
        column("VAI TRÒ", "roleLabel", 90).setMinWidth(80);
        column("LỚP", "classes", 190);
        TableColumn<Row, String> status = column("TRẠNG THÁI", "status", 150);
        status.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : v);
                getStyleClass().removeAll("students-online-cell", "students-offline-cell");
                if (!empty && v != null && v.startsWith("Đã khoá"))
                    getStyleClass().add("students-offline-cell");
            }
        });
        TableColumn<Row, String> actions = new TableColumn<>("THAO TÁC");
        actions.setPrefWidth(230);
        actions.setMinWidth(220);
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
        table.setPlaceholder(new Label("Chưa có tài khoản nào."));
        table.getStyleClass().add("students-table");
        table.setFixedCellSize(50);
        setCenter(table);
        reload();
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

    /** Số thứ tự theo thứ tự đang hiện (sau lọc, sắp xếp), không gắn với tài khoản. */
    static <T> TableColumn<T, Void> orderColumn() {
        TableColumn<T, Void> c = new TableColumn<>("STT");
        c.setSortable(false);
        c.setReorderable(false);
        c.setPrefWidth(52);
        c.setMinWidth(46);
        c.setMaxWidth(64);
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setText(
                        empty || getTableRow() == null || getTableRow().getItem() == null
                                ? null
                                : String.valueOf(getIndex() + 1));
            }
        });
        c.getStyleClass().add("order-column");
        return c;
    }

    private TableColumn<Row, String> column(String title, String property, int width) {
        TableColumn<Row, String> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        c.setPrefWidth(width);
        table.getColumns().add(c);
        return c;
    }

    private Node actionsFor(Row r) {
        if (r.role().equals("ADMIN")) {
            Label l = new Label("Sửa trong file cấu hình");
            l.getStyleClass().add("muted");
            return l;
        }
        Button edit = small("Sửa");
        edit.setOnAction(e -> editDialog(r));
        Button reset = small("Mật khẩu");
        reset.setOnAction(e -> PasswordDialogs.reset(window(), client, r.username(), r.fullName(), this::reload));
        Button lock = small(r.locked() ? "Mở khoá" : "Khoá");
        lock.getStyleClass().add(r.locked() ? "account-unlock" : "account-lock");
        lock.setOnAction(e -> confirmLock(r));
        HBox box = new HBox(6, edit, reset, lock);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static Button small(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("students-reset");
        b.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE); // không cắt chữ khi bảng hẹp
        return b;
    }

    private Window window() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private void applyFilter() {
        String q = search.getText() == null ? "" : search.getText().strip().toLowerCase();
        String f = roleFilter.getValue();
        String cls = classFilter.getValue();
        visible.setPredicate(r -> {
            if (!(r.fullName() + " " + r.username() + " " + r.classes())
                    .toLowerCase()
                    .contains(q)) return false;
            if (cls != null
                    && !cls.equals(ALL_CLASSES)
                    && !List.of(r.classes().split(", ")).contains(cls)) return false;
            return switch (f == null ? "" : f) {
                case "Giáo viên" -> r.role().equals("TEACHER");
                case "Học viên" -> r.role().equals("STUDENT");
                case "Quản trị" -> r.role().equals("ADMIN");
                case "Đang bị khoá" -> r.locked();
                default -> true;
            };
        });
    }

    /** Tải lại danh sách tài khoản và lớp. */
    public void reload() {
        if (loading) return;
        loading = true;
        var reply = client.expect(MessageType.ACCOUNT_LIST_DATA, MessageType.ERROR);
        client.send(MessageType.ACCOUNT_LIST_REQ, new byte[0]);
        reply.orTimeout(12, TimeUnit.SECONDS)
                .whenComplete((f, err) -> Platform.runLater(() -> {
                    loading = false;
                    try {
                        if (err != null) {
                            feedback.setText("Server không trả lời");
                            return;
                        }
                        if (f.type() == MessageType.ERROR) {
                            feedback.setText(ErrorMessage.from(f).message());
                            return;
                        }
                        show(JsonPayload.tree(f));
                    } catch (Exception e) {
                        feedback.setText(e.getMessage());
                    }
                }));
    }

    private void show(JsonNode data) {
        List<ClassItem> cls = new ArrayList<>();
        for (JsonNode c : data.path("classes")) {
            cls.add(new ClassItem(
                    c.path("id").asLong(),
                    c.path("name").asText(),
                    c.path("teacher").asText("")));
        }
        classes = cls;
        List<Row> list = new ArrayList<>();
        int t = 0, s = 0, l = 0;
        for (JsonNode a : data.path("accounts")) {
            String role = a.path("role").asText();
            boolean isLocked = a.path("locked").asBoolean();
            if (role.equals("TEACHER")) t++;
            if (role.equals("STUDENT")) s++;
            if (isLocked) l++;
            List<String> names = new ArrayList<>();
            a.path("classes").forEach(n -> names.add(n.asText()));
            String status = isLocked
                    ? "Đã khoá"
                    : a.path("mustChange").asBoolean()
                            ? "Chờ tự đặt mật khẩu"
                            : a.path("online").asBoolean() ? "Đang trực tuyến" : "Hoạt động";
            list.add(new Row(
                    a.path("id").asLong(),
                    a.path("fullName").asText(),
                    a.path("username").asText(),
                    role,
                    switch (role) {
                        case "ADMIN" -> "Quản trị";
                        case "TEACHER" -> "Giáo viên";
                        default -> "Học viên";
                    },
                    names.isEmpty() ? "—" : String.join(", ", names),
                    a.path("classId").asLong(),
                    status,
                    isLocked,
                    a.path("mustChange").asBoolean(),
                    a.path("tempPassword").isTextual() ? a.path("tempPassword").asText() : null));
        }
        rows.setAll(list);
        teachers.setText(Integer.toString(t));
        students.setText(Integer.toString(s));
        locked.setText(Integer.toString(l));
        feedback.setText(list.size() + " tài khoản · " + classes.size() + " lớp");
        String keep = classFilter.getValue();
        classFilter.getItems().setAll(ALL_CLASSES);
        cls.stream().map(ClassItem::name).distinct().sorted().forEach(classFilter.getItems()::add);
        classFilter.setValue(classFilter.getItems().contains(keep) ? keep : ALL_CLASSES);
        applyFilter();
    }

    static final String ALL_CLASSES = "Mọi lớp";

    /** Xuất các tài khoản đang hiện trong bảng (trừ quản trị) ra Excel. */
    private void exportVisible() {
        List<Row> list = visible.stream().filter(r -> !r.role().equals("ADMIN")).toList();
        if (list.isEmpty()) {
            feedback.setText("Không có tài khoản giáo viên, học viên nào đang hiện để xuất.");
            return;
        }
        String cls = classFilter.getValue();
        String title = cls == null || cls.equals(ALL_CLASSES) ? "" : cls;
        FileChooser fc = new FileChooser();
        fc.setTitle("Lưu file tài khoản" + (title.isEmpty() ? "" : " lớp " + title));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        fc.setInitialFileName(RosterImportDialog.fileName(title.isEmpty() ? "tat ca" : title));
        File file = fc.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) return;
        Path out = file.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")
                ? file.toPath()
                : file.toPath().resolveSibling(file.getName() + ".xlsx");
        try {
            XlsxWriter.write(out, "Tài khoản", EXPORT_WIDTHS, exportSheet(title, list, LocalDateTime.now()));
            long withPwd = list.stream().filter(r -> r.tempPassword() != null).count();
            feedback.setText("✓ Đã xuất " + list.size() + " tài khoản (" + withPwd + " có mật khẩu tạm) vào " + out);
        } catch (Exception ex) {
            feedback.setText("Không lưu được file: " + ex.getMessage());
        }
    }

    static final double[] EXPORT_WIDTHS = {6, 18, 28, 11, 26, 15, 46};

    /**
     * File tài khoản gửi giáo viên: mật khẩu tạm của ai chưa tự đặt mật khẩu; người đã tự đặt thì ghi chú, không lộ gì.
     * {@code className} rỗng: nhiều lớp.
     */
    static List<XlsxWriter.Line> exportSheet(String className, List<Row> list, LocalDateTime now) {
        List<XlsxWriter.Line> out = new ArrayList<>();
        String where = className.isEmpty() ? "" : " - lớp " + className;
        out.add(new XlsxWriter.Line(XlsxWriter.TITLE, List.of("Tài khoản LabCast" + where)));
        out.add(new XlsxWriter.Line(
                XlsxWriter.NOTE,
                List.of("Xuất lúc " + now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                        + ". Giữ kín file này: có mật khẩu tạm.")));
        out.add(new XlsxWriter.Line(
                XlsxWriter.NOTE,
                List.of("Cách vào lớp: mở LabCast, chọn lớp ở màn đăng nhập, gõ Tên đăng nhập và Mật khẩu tạm. Lần đầu"
                        + " đăng nhập app bắt đặt mật khẩu mới của riêng mình.")));
        out.add(new XlsxWriter.Line(
                XlsxWriter.NOTE,
                List.of("Quên mật khẩu: bấm \"Quên mật khẩu?\" ở màn đăng nhập, giáo viên sẽ đặt lại mật khẩu tạm.")));
        out.add(new XlsxWriter.Line(XlsxWriter.PLAIN, List.of()));
        out.add(new XlsxWriter.Line(
                XlsxWriter.HEADER,
                List.of("STT", "Tên đăng nhập", "Họ và tên", "Vai trò", "Lớp", "Mật khẩu tạm", "Ghi chú")));
        int stt = 0;
        for (Row r : list) {
            String note;
            if (r.locked()) note = "Tài khoản đang bị khoá";
            else if (r.tempPassword() != null) note = "Đổi mật khẩu ở lần đăng nhập đầu";
            else if (r.mustChange()) note = "Không còn lưu mật khẩu tạm: bấm \"Mật khẩu\" để cấp mật khẩu tạm mới";
            else note = "Đã tự đặt mật khẩu riêng";
            out.add(new XlsxWriter.Line(
                    XlsxWriter.CELL,
                    List.of(
                            String.valueOf(++stt),
                            r.username(),
                            r.fullName(),
                            r.roleLabel(),
                            r.classes(),
                            r.tempPassword() == null || r.locked() ? "" : r.tempPassword(),
                            note)));
        }
        return out;
    }

    private void confirmLock(Row r) {
        boolean lock = !r.locked();
        Alert a = new Alert(
                Alert.AlertType.CONFIRMATION,
                lock
                        ? "Khoá " + r.fullName() + " (" + r.username() + ")? Người này bị đăng xuất ngay và không đăng"
                                + " nhập được cho tới khi mở khoá. Bài làm, thảo luận vẫn giữ nguyên."
                        : "Mở khoá " + r.fullName() + " (" + r.username() + ")?",
                ButtonType.OK,
                ButtonType.CANCEL);
        a.setHeaderText(null);
        if (window() != null) a.initOwner(window());
        a.showAndWait()
                .filter(b -> b == ButtonType.OK)
                .ifPresent(b -> send(
                        Frame.of(
                                MessageType.ACCOUNT_LOCK,
                                new PayloadWriter().i64(r.id()).bool(lock).toBytes()),
                        msg -> {
                            info(msg);
                            reload();
                        },
                        err -> info(err)));
    }

    /** Thêm ({@code r} {@code null}) hoặc sửa một tài khoản giáo viên / học viên. */
    Dialog<ButtonType> editDialog(Row r) {
        boolean creating = r == null;
        ComboBox<String> role = new ComboBox<>(FXCollections.observableArrayList("Học viên", "Giáo viên"));
        role.getSelectionModel().select(creating || r.role().equals("STUDENT") ? 0 : 1);
        role.setDisable(!creating);
        TextField username = new TextField(creating ? "" : r.username());
        username.setPromptText("vd. sv01, nguyen.an");
        username.setEditable(creating);
        TextField fullName = new TextField(creating ? "" : r.fullName());
        fullName.setPromptText("Họ và tên");
        ComboBox<ClassItem> classBox = new ComboBox<>(FXCollections.observableArrayList(classes));
        classBox.setMaxWidth(Double.MAX_VALUE);
        classBox.setPromptText(classes.isEmpty() ? "Chưa có lớp: tạo giáo viên kèm lớp trước" : "Chọn lớp");
        if (!creating) {
            classes.stream().filter(c -> c.id() == r.classId()).findFirst().ifPresent(classBox::setValue);
        }
        TextField newClass = new TextField();
        newClass.setPromptText(creating ? "vd. DSA Nâng cao - K24" : "Để trống nếu không thêm lớp");
        TextField password = new TextField(PasswordRules.suggest());
        Button another = new Button("Số khác");
        another.setOnAction(e -> password.setText(PasswordRules.suggest()));
        HBox pwdRow = new HBox(8, password, another);
        HBox.setHgrow(password, Priority.ALWAYS);
        Label classLabel = new Label();
        Label pwdLabel = new Label("Mật khẩu tạm");
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        Runnable layout = () -> {
            boolean student = role.getSelectionModel().getSelectedIndex() == 0;
            form.getChildren().clear();
            int row = 0;
            form.addRow(row++, new Label("Vai trò"), role);
            form.addRow(row++, new Label("Tên đăng nhập"), username);
            form.addRow(row++, new Label("Họ và tên"), fullName);
            classLabel.setText(student ? "Lớp" : creating ? "Lớp dạy" : "Thêm lớp dạy");
            form.addRow(row++, classLabel, student ? classBox : newClass);
            if (creating) form.addRow(row, pwdLabel, pwdRow);
        };
        role.setOnAction(e -> layout.run());
        layout.run();
        for (Node n : new Node[] {username, fullName, classBox, newClass}) GridPane.setHgrow(n, Priority.ALWAYS);
        username.setPrefColumnCount(22);
        Label hint = new Label(
                creating
                        ? "Đọc tên đăng nhập và mật khẩu tạm cho người dùng. Lần đầu đăng nhập app bắt họ đặt mật khẩu của"
                                + " riêng mình."
                        : r.role().equals("STUDENT")
                                ? "Đổi lớp thì học viên chuyển hẳn sang lớp mới."
                                : "Giáo viên dạy thêm lớp mới thì gõ tên lớp.");
        hint.setWrapText(true);
        hint.setMaxWidth(420);
        hint.getStyleClass().add("muted");
        Label error = new Label();
        error.setWrapText(true);
        error.setStyle("-fx-text-fill: #EF4444;");

        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(creating ? "Thêm tài khoản" : "Sửa tài khoản");
        d.setHeaderText(creating ? "Thêm giáo viên hoặc học viên" : "Sửa " + r.fullName());
        if (window() != null) {
            d.initOwner(window());
            d.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        }
        d.getDialogPane().setContent(new VBox(12, form, hint, error));
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        ok.setText(creating ? "Tạo" : "Lưu");
        ((Button) d.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Huỷ");
        Platform.runLater(creating ? username::requestFocus : fullName::requestFocus);
        ok.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume();
            boolean student = role.getSelectionModel().getSelectedIndex() == 0;
            String bad = AccountForm.check(
                    creating,
                    student,
                    username.getText(),
                    fullName.getText(),
                    classBox.getValue() == null ? 0 : classBox.getValue().id(),
                    newClass.getText(),
                    password.getText());
            if (bad != null) {
                error.setText(bad);
                return;
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", creating ? 0 : r.id());
            body.put("username", username.getText().strip());
            body.put("fullName", fullName.getText().strip());
            body.put("role", student ? "STUDENT" : "TEACHER");
            body.put(
                    "classId",
                    student && classBox.getValue() != null ? classBox.getValue().id() : 0);
            body.put("newClassName", student ? "" : newClass.getText().strip());
            if (creating) body.put("password", password.getText());
            ok.setDisable(true);
            error.setText("");
            send(
                    JsonPayload.frame(MessageType.ACCOUNT_SAVE, body),
                    msg -> {
                        d.close();
                        info(
                                creating
                                        ? msg + ".\nTên đăng nhập: "
                                                + username.getText().strip() + "\nMật khẩu tạm: " + password.getText()
                                        : msg + ".");
                        reload();
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
        a.show();
    }
}
