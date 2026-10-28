package labcast.client.ui;

import java.time.Year;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import labcast.client.net.DiscoveryClient.ServerInfo;
import labcast.common.protocol.Role;

/**
 * Màn đăng nhập ba cột: lời chào bên trái, lớp tìm thấy trong LAN (không phải gõ IP) và ô nhập tay ở giữa, form
 * đăng nhập bên phải. Thẻ vai trò Sinh viên / Giáo viên / Quản trị đổi màu và lời giới thiệu; tài khoản khác vai trò
 * đang chọn thì không vào, chuyển sang đúng thẻ để bấm lại (icon LabCast chạy thử mở mỗi cửa sổ một vai trò).
 */
public final class LoginView extends StackPane {
    private final ListView<ServerInfo> servers = new ListView<>();
    private final TextField manual = new TextField();
    private final TextField user = new TextField();
    private final PasswordField pass = new PasswordField();
    /** Hiện mật khẩu: ô chữ thường dùng chung nội dung với ô mật khẩu. */
    private final TextField passShown = new TextField();

    private final Button loginBtn = new Button("Đăng nhập");
    private final Label status = new Label();
    private final Hyperlink forgotLink = new Hyperlink("Quên mật khẩu?");
    private final CheckBox remember = new CheckBox("Ghi nhớ tên đăng nhập");
    private final ToggleGroup roles = new ToggleGroup();
    private final Label formTitle = new Label();
    private final Label formSub = new Label();
    private final StackPane roleBadge = new StackPane();
    /** Quản trị không chọn lớp: một dòng báo máy chủ sẽ nối tới (tự tìm thấy), bấm "Đổi" mới hiện cột chọn lớp. */
    private final Label serverInfo = new Label();

    private final Hyperlink serverToggle = new Hyperlink("Đổi máy chủ");
    private final HBox serverLine = new HBox(10);
    private VBox classColumn;
    private HBox shell;
    private boolean pickServer;
    private Role role;

    /** Máy server sẽ nối tới: lớp chọn trong danh sách hoặc địa chỉ gõ tay. */
    private record Target(String host, int port) {}

    public LoginView(Consumer<LoginController.LoggedIn> onLoggedIn) {
        this(onLoggedIn, Role.STUDENT);
    }

    /** {@code initialRole}: thẻ vai trò chọn sẵn (tham số {@code --vai} khi mở app). */
    public LoginView(Consumer<LoginController.LoggedIn> onLoggedIn, Role initialRole) {
        Theme.apply(this);
        getStyleClass().add("login-bg");

        BorderPane page = new BorderPane();
        page.getStyleClass().add("login-page");
        page.setTop(topBar());
        VBox hero = hero();
        page.setLeft(hero);
        classColumn = classPane(onLoggedIn);
        shell = new HBox(classColumn, formPane(onLoggedIn));
        shell.getStyleClass().add("login-shell");
        shell.setMaxWidth(1080);
        shell.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane center = new StackPane(shell);
        center.setAlignment(Pos.CENTER);
        BorderPane.setMargin(center, new Insets(0, 36, 0, 0));
        page.setCenter(center);
        Label copy = new Label("© " + Year.now().getValue() + " LabCast · Phòng thực hành DSA và SQL");
        copy.getStyleClass().add("login-footer");
        BorderPane.setMargin(copy, new Insets(10, 36, 22, 36));
        page.setBottom(copy);
        // cửa sổ hẹp: bỏ cột lời chào, giữ khung chọn lớp + đăng nhập
        widthProperty().addListener((o, a, w) -> {
            boolean wide = w.doubleValue() >= 1240;
            hero.setVisible(wide);
            hero.setManaged(wide);
            BorderPane.setMargin(center, new Insets(0, 36, 0, wide ? 0 : 36));
        });

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("login-scroll");
        getChildren().add(scroll);

        selectRole(initialRole == null ? Role.STUDENT : initialRole);
        discover();
    }

    /** Tham số dòng lệnh {@code --vai=...} → vai trò; không nhận ra thì {@code null}. */
    public static Role roleFromArg(String arg) {
        if (arg == null) return null;
        return switch (arg.strip().toLowerCase(Locale.ROOT)) {
            case "quan-tri", "quantri", "admin" -> Role.ADMIN;
            case "giao-vien", "giaovien", "teacher" -> Role.TEACHER;
            case "hoc-vien", "sinh-vien", "hocvien", "sinhvien", "student" -> Role.STUDENT;
            default -> null;
        };
    }

    /** "Sinh viên", "Giáo viên", "Quản trị". */
    public static String roleName(Role r) {
        return switch (r) {
            case ADMIN -> "Quản trị";
            case TEACHER -> "Giáo viên";
            default -> "Sinh viên";
        };
    }

    // ---------- Khung trên, cột lời chào ----------

    private HBox topBar() {
        Label mark = new Label("LC");
        mark.getStyleClass().addAll("brand-mark", "large");
        Label title = new Label("LabCast");
        title.getStyleClass().add("login-brand");
        Label sub = new Label("DSA & SQL PRACTICE PLATFORM");
        sub.getStyleClass().add("login-brand-sub");
        HBox brand = new HBox(12, mark, new VBox(1, title, sub));
        brand.setAlignment(Pos.CENTER_LEFT);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        // đổi Sáng / Tối ngay cả khi chưa đăng nhập
        Button themeBtn = new Button();
        themeBtn.getStyleClass().add("login-theme-toggle");
        Runnable label = () -> themeBtn.setText(Theme.isDark() ? "☀  Sáng" : "☾  Tối");
        label.run();
        themeBtn.setOnAction(e -> {
            Theme.setDark(!Theme.isDark());
            label.run();
        });
        HBox bar = new HBox(brand, gap, themeBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(26, 36, 22, 36));
        return bar;
    }

    private VBox hero() {
        Label hello = new Label("Chào mừng trở lại");
        hello.getStyleClass().add("login-hero-hello");
        Text a = new Text("Sẵn sàng ");
        Text b = new Text("học tập");
        Text c = new Text("\nvà chinh phục thử thách");
        for (Text t : new Text[] {a, b, c}) t.getStyleClass().add("login-hero-title");
        b.getStyleClass().add("accent");
        TextFlow title = new TextFlow(a, b, c);
        Label desc = new Label("Luyện cấu trúc dữ liệu, giải thuật và SQL ngay trong phòng máy: xem từng bước chạy bằng"
                + " animation, nộp bài chấm tự động, hỏi giáo viên ngay trong bài.");
        desc.getStyleClass().add("login-hero-desc");
        desc.setWrapText(true);
        Region push = new Region();
        VBox.setVgrow(push, Priority.ALWAYS);
        VBox box = new VBox(10, hello, title, desc, push, illustration());
        box.getStyleClass().add("login-hero");
        box.setPrefWidth(430);
        box.setMinWidth(400);
        box.setPadding(new Insets(36, 30, 10, 36));
        return box;
    }

    /** Màn hình với biểu đồ tiến độ và thẻ code, vẽ bằng hình khối (không dùng ảnh ngoài). */
    private static Node illustration() {
        Rectangle base = new Rectangle(250, 34);
        base.setArcWidth(30);
        base.setArcHeight(30);
        base.setFill(Color.web("#2A2366"));
        base.setTranslateY(162);
        base.setTranslateX(20);
        Rectangle screen = new Rectangle(220, 150);
        screen.setArcWidth(22);
        screen.setArcHeight(22);
        screen.setFill(new LinearGradient(
                0,
                0,
                1,
                1,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#3B2F8F")),
                new Stop(1, Color.web("#1C1850"))));
        screen.setStroke(Color.web("#6D5BFF", 0.55));
        screen.setTranslateX(30);
        screen.setTranslateY(8);
        Rectangle bar = new Rectangle(220, 22);
        bar.setArcWidth(22);
        bar.setArcHeight(22);
        bar.setFill(Color.web("#7C5CFF"));
        bar.setTranslateX(30);
        bar.setTranslateY(8);
        Group dots = new Group();
        for (int i = 0; i < 3; i++) {
            Circle d = new Circle(3, Color.web("#EDE9FE"));
            d.setTranslateX(46 + i * 11);
            d.setTranslateY(19);
            dots.getChildren().add(d);
        }
        Polyline chart = new Polyline(48, 128, 82, 110, 112, 118, 146, 86, 180, 94, 226, 52);
        chart.setStroke(Color.web("#A78BFA"));
        chart.setStrokeWidth(3);
        chart.setFill(null);
        Group bars = new Group();
        double[] hs = {18, 30, 24, 40};
        for (int i = 0; i < hs.length; i++) {
            Rectangle r = new Rectangle(12, hs[i], Color.web("#5B4BD6"));
            r.setArcWidth(4);
            r.setArcHeight(4);
            r.setTranslateX(56 + i * 20);
            r.setTranslateY(146 - hs[i]);
            bars.getChildren().add(r);
        }
        Rectangle chip = new Rectangle(54, 46);
        chip.setArcWidth(16);
        chip.setArcHeight(16);
        chip.setFill(Color.web("#3B82F6"));
        chip.setTranslateX(232);
        chip.setTranslateY(112);
        SVGPath code = new SVGPath();
        code.setContent("M8 7 L3 12 L8 17 M16 7 L21 12 L16 17 M14 5 L10 19");
        code.setStroke(Color.WHITE);
        code.setStrokeWidth(2.2);
        code.setFill(null);
        code.setTranslateX(247);
        code.setTranslateY(123);
        Group g = new Group(base, screen, bar, dots, bars, chart, chip, code);
        g.setOpacity(0.95);
        g.getStyleClass().add("login-illustration");
        return g;
    }

    // ---------- Cột chọn lớp ----------

    private VBox classPane(Consumer<LoginController.LoggedIn> onLoggedIn) {
        Label title = new Label("Chọn lớp trong mạng");
        title.getStyleClass().add("login-pane-title");
        HBox head = new HBox(10, icon(NETWORK, "login-title-icon"), title);
        head.setAlignment(Pos.CENTER_LEFT);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Button refresh = new Button("↻ Tìm lại");
        refresh.getStyleClass().add("ghost");
        refresh.setOnAction(e -> discover());
        head.getChildren().addAll(gap, refresh);
        Label sub = new Label("Chọn lớp bạn muốn vào rồi đăng nhập ở khung bên cạnh");
        sub.getStyleClass().add("login-pane-sub");

        servers.getStyleClass().add("login-servers");
        servers.setFixedCellSize(80);
        servers.setPrefHeight(80 * 3); // vừa ba lớp, nhiều hơn thì cuộn
        servers.setMinHeight(120);
        Label empty = new Label("Chưa thấy lớp nào.\nBấm \"Tìm lại\" hoặc nhập địa chỉ máy giáo viên bên dưới.");
        empty.getStyleClass().add("placeholder");
        empty.setWrapText(true);
        servers.setPlaceholder(empty);
        servers.setCellFactory(lv -> {
            ListCell<ServerInfo> cell = new ListCell<>() {
                @Override
                protected void updateItem(ServerInfo s, boolean empty) {
                    super.updateItem(s, empty);
                    setText(null);
                    setGraphic(empty || s == null ? null : serverCell(s));
                }
            };
            // ô rộng đúng bằng danh sách, không giãn theo chữ: tên lớp dài cắt "…" thay vì đẩy thẻ ra ngoài
            cell.setPrefWidth(0);
            return cell;
        });
        // Chọn lớp trong danh sách hay gõ địa chỉ tay: chỉ một trong hai có hiệu lực, nên làm cái này thì xoá
        // cái kia (trước đây ô nhập tay còn chữ thì lớp đã chọn bị bỏ qua mà không báo gì).
        servers.setOnMouseClicked(e -> {
            if (servers.getSelectionModel().getSelectedItem() == null) {
                return;
            }
            manual.clear();
            if (e.getClickCount() >= 2
                    && !user.getText().isBlank()
                    && !pass.getText().isEmpty()) {
                login(onLoggedIn);
            } else {
                (user.getText().isBlank() ? user : pass).requestFocus();
            }
        });
        manual.textProperty().addListener((obs, o, n) -> {
            if (!n.isBlank()) {
                servers.getSelectionModel().clearSelection();
            }
        });
        manual.setPromptText("Địa chỉ máy giáo viên, ví dụ 192.168.1.10:7000");
        Button go = new Button();
        go.setGraphic(icon(ARROW, "login-go-icon"));
        go.getStyleClass().add("login-go");
        go.setOnAction(e -> (user.getText().isBlank() ? user : pass).requestFocus());
        HBox manualRow = new HBox(8, iconField(manual, SERVER), go);
        HBox.setHgrow(manualRow.getChildren().get(0), Priority.ALWAYS);
        manualRow.setAlignment(Pos.CENTER_LEFT);

        Label tipTitle = new Label("Mẹo");
        tipTitle.getStyleClass().add("login-tip-title");
        HBox tipHead = new HBox(8, icon(BULB, "login-tip-icon"), tipTitle);
        tipHead.setAlignment(Pos.CENTER_LEFT);
        Label tip =
                new Label("Không thấy lớp của bạn? Kiểm tra máy đã nối đúng mạng phòng máy, bấm \"Tìm lại\", hoặc hỏi"
                        + " giáo viên địa chỉ máy rồi gõ vào ô trên.");
        tip.setWrapText(true);
        tip.getStyleClass().add("login-tip-text");
        VBox tipBox = new VBox(6, tipHead, tip);
        tipBox.getStyleClass().add("login-tip");

        VBox box = new VBox(14, head, sub, servers, divider("hoặc nhập tay"), manualRow, tipBox);
        box.getStyleClass().add("login-classes");
        box.setPrefWidth(560);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private static HBox serverCell(ServerInfo s) {
        Circle dot = new Circle(4);
        dot.getStyleClass().add("server-dot");
        boolean sql = s.className().toLowerCase(Locale.ROOT).contains("sql");
        StackPane avatar = new StackPane(icon(sql ? DATABASE : PEOPLE, "login-class-icon"));
        avatar.getStyleClass().addAll("login-class-avatar", sql ? "sql" : "dsa");
        Label name = new Label(s.className());
        name.getStyleClass().add("server-name");
        HBox meta = new HBox(6);
        meta.setAlignment(Pos.CENTER_LEFT);
        if (!s.teacherName().isEmpty()) {
            Label t = new Label(s.teacherName() + "  ·");
            t.getStyleClass().add("server-meta");
            meta.getChildren().add(t);
        }
        Label addr = new Label(s.host() + ":" + s.port());
        addr.getStyleClass().add("server-meta");
        meta.getChildren().addAll(icon(SERVER, "login-meta-icon"), addr);
        // tên lớp dài: cắt "…" cho mũi tên khỏi bị đẩy ra ngoài, rê chuột xem đủ
        name.setMinWidth(0);
        name.setTooltip(new javafx.scene.control.Tooltip(s.className()));
        VBox text = new VBox(3, name, meta);
        text.setAlignment(Pos.CENTER_LEFT);
        text.setMinWidth(0);
        text.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox row = new HBox(14, dot, avatar, text, icon(CHEVRON, "login-chevron"));
        row.setFillHeight(false);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("login-class-card");
        row.setMinWidth(0);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setMinHeight(72); // ô danh sách cao 80, chừa 8 làm khe giữa hai thẻ
        row.setPrefHeight(72);
        return row;
    }

    // ---------- Cột đăng nhập ----------

    private VBox formPane(Consumer<LoginController.LoggedIn> onLoggedIn) {
        HBox tabs = new HBox(4);
        tabs.getStyleClass().add("login-role-tabs");
        for (Role r : new Role[] {Role.STUDENT, Role.TEACHER, Role.ADMIN}) {
            ToggleButton b = new ToggleButton(roleName(r));
            b.setGraphic(icon(roleIcon(r), "login-role-tab-icon"));
            b.setUserData(r);
            b.setToggleGroup(roles);
            b.getStyleClass().add("login-role-tab");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setId("login-role-" + r.name().toLowerCase(Locale.ROOT));
            HBox.setHgrow(b, Priority.ALWAYS);
            tabs.getChildren().add(b);
        }
        roles.selectedToggleProperty().addListener((o, was, now) -> {
            if (now == null) {
                was.setSelected(true); // luôn có một vai trò được chọn
                return;
            }
            selectRole((Role) now.getUserData());
        });

        roleBadge.getStyleClass().add("login-role-badge");
        formTitle.getStyleClass().add("login-form-title");
        formSub.getStyleClass().add("login-form-sub");
        formSub.setWrapText(true);
        formSub.setMinHeight(Region.USE_PREF_SIZE);

        serverInfo.getStyleClass().add("login-server-info");
        serverInfo.setWrapText(true);
        serverInfo.setMinHeight(Region.USE_PREF_SIZE);
        serverToggle.getStyleClass().add("login-forgot");
        serverToggle.setOnAction(e -> {
            pickServer = !pickServer;
            layoutForRole();
        });
        Region lineGap = new Region();
        HBox.setHgrow(lineGap, Priority.ALWAYS);
        HBox.setHgrow(serverInfo, Priority.SOMETIMES);
        serverLine.getChildren().setAll(icon(SERVER, "login-input-icon"), serverInfo, lineGap, serverToggle);
        serverLine.setAlignment(Pos.CENTER_LEFT);
        serverLine.getStyleClass().add("login-server-line");
        serverLine.managedProperty().bind(serverLine.visibleProperty());
        servers.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> refreshServerInfo());
        servers.getItems().addListener((javafx.collections.ListChangeListener<ServerInfo>) c -> refreshServerInfo());
        manual.textProperty().addListener((o, a, b) -> refreshServerInfo());

        user.setPromptText("Tên đăng nhập (mã sinh viên)");
        pass.setPromptText("Mật khẩu");
        passShown.setPromptText("Mật khẩu");
        passShown.textProperty().bindBidirectional(pass.textProperty());
        passShown.setVisible(false);
        passShown.setManaged(false);
        ToggleButton eye = new ToggleButton();
        SVGPath eyeIcon = svg(EYE, "login-eye-icon");
        eye.setGraphic(new Group(eyeIcon));
        eye.getStyleClass().add("login-eye");
        eye.setFocusTraversable(false);
        eye.selectedProperty().addListener((o, a, shown) -> {
            passShown.setVisible(shown);
            passShown.setManaged(shown);
            pass.setVisible(!shown);
            pass.setManaged(!shown);
            eyeIcon.setContent(shown ? EYE_OFF : EYE);
            (shown ? passShown : pass).requestFocus();
        });
        StackPane passBox = new StackPane(iconField(pass, LOCK), iconField(passShown, LOCK), eye);
        StackPane.setAlignment(eye, Pos.CENTER_RIGHT);
        StackPane.setMargin(eye, new Insets(0, 8, 0, 0));

        remember.getStyleClass().add("login-remember");
        forgotLink.getStyleClass().add("login-forgot");
        forgotLink.setOnAction(e -> forgot());
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox row = new HBox(remember, gap, forgotLink);
        row.setAlignment(Pos.CENTER_LEFT);

        loginBtn.getStyleClass().addAll("primary", "login-submit");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setDefaultButton(true);
        loginBtn.setOnAction(e -> login(onLoggedIn));
        status.getStyleClass().add("form-status");
        status.setWrapText(true);
        status.setMinHeight(Region.USE_PREF_SIZE);

        Label noAccount = new Label("Chưa có tài khoản? Tài khoản do quản trị cấp, hãy hỏi giáo viên của bạn.");
        noAccount.getStyleClass().add("login-no-account");
        noAccount.setWrapText(true);
        noAccount.setMaxWidth(Double.MAX_VALUE);
        Region push = new Region();
        VBox.setVgrow(push, Priority.ALWAYS);

        VBox box = new VBox(
                14,
                tabs,
                spacer(6),
                roleBadge,
                formTitle,
                formSub,
                serverLine,
                spacer(4),
                iconField(user, USER),
                passBox,
                row,
                loginBtn,
                status,
                push,
                noAccount);
        box.getStyleClass().add("login-form");
        box.setPrefWidth(440);
        box.setMinWidth(380);
        HBox.setHgrow(box, Priority.ALWAYS); // thẻ Quản trị chỉ còn khung này: phủ hết bề ngang
        return box;
    }

    /** Đổi thẻ vai trò: màu nhấn, tiêu đề, lời giới thiệu, tên đăng nhập đã nhớ của vai trò đó. */
    private void selectRole(Role r) {
        if (r == role) return;
        role = r;
        for (var t : roles.getToggles()) {
            if (t.getUserData() == r && !t.isSelected()) t.setSelected(true);
        }
        getStyleClass().removeAll("login-role-student", "login-role-teacher", "login-role-admin");
        getStyleClass().add("login-role-" + r.name().toLowerCase(Locale.ROOT));
        formTitle.setText("Đăng nhập " + roleName(r));
        formSub.setText(
                switch (r) {
                    case ADMIN -> "Quản lý hệ thống, tài khoản giáo viên và sinh viên, lớp học, bài tập và báo cáo toàn"
                            + " nền tảng. Không cần chọn lớp: vào rồi đổi lớp ở trang chủ.";
                    case TEACHER -> "Mở lớp, giao bài tập, chiếu bài cho cả lớp, theo dõi tiến độ và hỗ trợ sinh viên.";
                    default -> "Luyện tập bài tập, xem animation từng bước, theo dõi tiến độ và chinh phục các thử"
                            + " thách.";
                });
        user.setPromptText(r == Role.STUDENT ? "Tên đăng nhập (mã sinh viên)" : "Tên đăng nhập");
        roleBadge.getChildren().setAll(icon(roleIcon(r), "login-role-badge-icon"));
        String saved = prefs("user." + r.name());
        remember.setSelected(saved != null);
        user.setText(saved == null ? "" : saved);
        pass.clear();
        setStatus("", false);
        pickServer = false;
        layoutForRole();
    }

    /**
     * Quản trị coi được mọi lớp nên không có cột chọn lớp: nối tới máy chủ đầu tiên tìm thấy (hay địa chỉ gõ tay),
     * chọn lớp sau ở trang chủ. "Đổi máy chủ" hiện lại cột chọn lớp khi mạng có nhiều máy chủ.
     */
    private void layoutForRole() {
        if (classColumn == null) return;
        boolean admin = role == Role.ADMIN;
        boolean showClasses = !admin || pickServer;
        classColumn.setVisible(showClasses);
        classColumn.setManaged(showClasses);
        shell.setMaxWidth(showClasses ? 1080 : 520);
        shell.getStyleClass().remove("solo");
        if (!showClasses) shell.getStyleClass().add("solo");
        serverLine.setVisible(admin);
        serverToggle.setText(pickServer ? "Ẩn danh sách" : "Đổi máy chủ");
        refreshServerInfo();
    }

    private void refreshServerInfo() {
        String m = manual.getText() == null ? "" : manual.getText().strip();
        ServerInfo s = servers.getSelectionModel().getSelectedItem();
        if (!m.isEmpty()) serverInfo.setText("Máy chủ: " + m);
        else if (s != null) serverInfo.setText("Máy chủ: " + s.host() + ":" + s.port());
        else if (servers.getItems().isEmpty()) serverInfo.setText("Chưa thấy máy chủ LabCast nào trong mạng");
        else serverInfo.setText("Chọn máy chủ để đăng nhập");
    }

    private static String roleIcon(Role r) {
        return switch (r) {
            case ADMIN -> SHIELD;
            case TEACHER -> BOARD;
            default -> CAP;
        };
    }

    // ---------- Phần dùng chung ----------

    /** Icon nét; bọc trong Group để thu phóng bằng CSS ({@code -fx-scale-x}) đổi luôn chỗ chiếm trong bố cục. */
    private static Group icon(String path, String style) {
        return new Group(svg(path, style));
    }

    private static SVGPath svg(String path, String style) {
        SVGPath p = new SVGPath();
        p.setContent(path);
        p.getStyleClass().addAll("login-icon", style);
        return p;
    }

    /** Ô nhập có icon bên trái (icon nằm đè, ô chừa lề trái). */
    private static StackPane iconField(TextField field, String path) {
        field.getStyleClass().add("login-input");
        Group i = icon(path, "login-input-icon");
        i.setMouseTransparent(true);
        StackPane box = new StackPane(field, i);
        StackPane.setAlignment(i, Pos.CENTER_LEFT);
        StackPane.setMargin(i, new Insets(0, 0, 0, 15));
        box.visibleProperty().bind(field.visibleProperty());
        box.managedProperty().bind(field.managedProperty());
        return box;
    }

    private static HBox divider(String text) {
        Region l = new Region();
        Region r = new Region();
        l.getStyleClass().add("divider-line");
        r.getStyleClass().add("divider-line");
        HBox.setHgrow(l, Priority.ALWAYS);
        HBox.setHgrow(r, Priority.ALWAYS);
        Label t = new Label(text);
        t.getStyleClass().add("divider-text");
        HBox box = new HBox(10, l, t, r);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private static Region spacer(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }

    private static String prefs(String key) {
        try {
            return Preferences.userNodeForPackage(LoginView.class).get(key, null);
        } catch (Exception | Error e) { // máy chặn ghi registry: coi như chưa nhớ gì
            return null;
        }
    }

    private static void savePrefs(String key, String value) {
        try {
            var p = Preferences.userNodeForPackage(LoginView.class);
            if (value == null) p.remove(key);
            else p.put(key, value);
        } catch (Exception | Error ignored) {
            // không nhớ được thì thôi
        }
    }

    private void setStatus(String text, boolean error) {
        status.setText(text);
        status.getStyleClass().remove("error");
        if (error) {
            status.getStyleClass().add("error");
        }
    }

    private void discover() {
        setStatus("Đang tìm lớp trong mạng…", false);
        LoginController.discover()
                .thenAccept(list -> Platform.runLater(() -> {
                    servers.getItems().setAll(list);
                    if (!list.isEmpty() && manual.getText().isBlank()) {
                        servers.getSelectionModel().select(0);
                        (user.getText().isBlank() ? user : pass).requestFocus(); // đã có lớp: gõ tên đăng nhập
                    }
                    setStatus(list.isEmpty() ? "Không tìm thấy lớp nào trong mạng." : "", false);
                }));
    }

    /** Lớp đang chọn hoặc địa chỉ gõ tay; không hợp lệ thì báo ở dòng trạng thái và trả {@code null}. */
    private Target target() {
        String host;
        int port = 7000;
        String m = manual.getText().strip();
        if (!m.isEmpty() && !m.contains(".") && !m.contains(":") && !m.equalsIgnoreCase("localhost")) {
            // hay gặp: gõ tên đăng nhập nhầm vào ô địa chỉ
            setStatus(
                    "\"" + m + "\" không giống địa chỉ máy giáo viên (ví dụ 192.168.1.10:7000). Đã chọn lớp ở trên"
                            + " thì để trống ô này.",
                    true);
            manual.requestFocus();
            return null;
        }
        if (!m.isEmpty()) {
            int colon = m.lastIndexOf(':');
            host = colon > 0 ? m.substring(0, colon) : m;
            if (colon > 0) {
                try {
                    port = Integer.parseInt(m.substring(colon + 1));
                } catch (NumberFormatException e) {
                    setStatus("Cổng phải là số", true);
                    return null;
                }
            }
        } else if (servers.getSelectionModel().getSelectedItem() != null) {
            ServerInfo s = servers.getSelectionModel().getSelectedItem();
            host = s.host();
            port = s.port();
        } else {
            setStatus("Chọn một lớp hoặc nhập địa chỉ máy giáo viên.", true);
            return null;
        }
        return new Target(host, port);
    }

    /**
     * Quên mật khẩu: gửi yêu cầu cho giáo viên lớp đang chọn. Giáo viên đặt mật khẩu tạm, học viên đăng nhập bằng
     * mật khẩu đó rồi app bắt đặt mật khẩu của riêng mình.
     */
    private void forgot() {
        if (user.getText().isBlank()) {
            setStatus("Gõ tên đăng nhập của bạn rồi bấm \"Quên mật khẩu?\" lần nữa.", true);
            user.requestFocus();
            return;
        }
        Target t = target();
        if (t == null) return;
        String u = user.getText().strip();
        forgotLink.setDisable(true);
        setStatus("Đang gửi yêu cầu…", false);
        Thread th = new Thread(
                () -> {
                    String msg;
                    boolean error = false;
                    try {
                        msg = LoginController.forgot(t.host(), t.port(), u);
                    } catch (LoginController.LoginFailed ex) {
                        msg = ex.getMessage();
                        error = true;
                    }
                    String m = msg;
                    boolean err = error;
                    Platform.runLater(() -> {
                        setStatus(m, err);
                        forgotLink.setDisable(false);
                    });
                },
                "quen-mat-khau");
        th.setDaemon(true);
        th.start();
    }

    private void login(Consumer<LoginController.LoggedIn> onLoggedIn) {
        if (user.getText().isBlank() || pass.getText().isEmpty()) {
            setStatus("Nhập tên đăng nhập và mật khẩu rồi bấm Đăng nhập.", true);
            (user.getText().isBlank() ? user : pass).requestFocus();
            return;
        }
        Target target = target();
        if (target == null) return;
        String h = target.host();
        int p = target.port();
        String u = user.getText().strip();
        String pw = pass.getText();
        Role wanted = role;
        loginBtn.setDisable(true);
        setStatus("Đang đăng nhập…", false);
        Thread t = new Thread(
                () -> {
                    try {
                        LoginController.LoggedIn li = LoginController.login(h, p, u, pw);
                        Platform.runLater(() -> {
                            if (li.role() != wanted) {
                                // đúng mật khẩu nhưng sai cửa: chuyển sang thẻ đúng, giữ mật khẩu để bấm lại
                                li.client().close();
                                selectRole(li.role());
                                user.setText(u);
                                pass.setText(pw);
                                setStatus(
                                        "Tài khoản " + u + " là "
                                                + roleName(li.role()).toLowerCase(Locale.ROOT)
                                                + ", không phải "
                                                + roleName(wanted).toLowerCase(Locale.ROOT)
                                                + ". Đã chuyển sang thẻ " + roleName(li.role())
                                                + ", bấm Đăng nhập lần nữa.",
                                        true);
                                loginBtn.setDisable(false);
                                return;
                            }
                            savePrefs("user." + wanted.name(), remember.isSelected() ? u : null);
                            if (!li.mustChangePassword()) {
                                onLoggedIn.accept(li);
                                return;
                            }
                            // mật khẩu tạm giáo viên đưa: đặt mật khẩu của riêng mình rồi mới vào
                            setStatus("Đặt mật khẩu của riêng bạn để tiếp tục.", false);
                            PasswordDialogs.chooseOwn(
                                    getScene() == null ? null : getScene().getWindow(),
                                    li.client(),
                                    pw,
                                    () -> onLoggedIn.accept(li),
                                    () -> {
                                        li.client().close();
                                        pass.clear();
                                        setStatus("Chưa đặt mật khẩu mới nên chưa vào được lớp.", true);
                                        loginBtn.setDisable(false);
                                    });
                        });
                    } catch (LoginController.LoginFailed ex) {
                        Platform.runLater(() -> {
                            setStatus(ex.getMessage(), true);
                            loginBtn.setDisable(false);
                        });
                    }
                },
                "dang-nhap");
        t.setDaemon(true);
        t.start();
    }

    // Icon nét (stroke) 24×24
    private static final String USER =
            "M12 12 A4 4 0 1 0 12 4 A4 4 0 0 0 12 12 M4 20 C4 16 8 14.5 12 14.5 C16 14.5 20 16 20 20";
    private static final String LOCK = "M6 11 H18 V20 H6 Z M8.5 11 V8 A3.5 3.5 0 0 1 15.5 8 V11";
    private static final String EYE = "M2 12 C5 6 19 6 22 12 C19 18 5 18 2 12 Z M12 9 A3 3 0 1 0 12 15 A3 3 0 0 0 12 9";
    private static final String EYE_OFF = EYE + " M4 4 L20 20";
    private static final String PEOPLE = "M9 11 A3 3 0 1 0 9 5 A3 3 0 0 0 9 11 M3 19 V17.5 C3 15.5 5.7 14 9 14 C12.3"
            + " 14 15 15.5 15 17.5 V19 M16 5.3 A3 3 0 0 1 16 10.7 M18 14.2 C20 14.8 21 16 21 17.5 V19";
    private static final String DATABASE = "M5 6 C5 3.5 19 3.5 19 6 C19 8.5 5 8.5 5 6 Z M5 6 V18 C5 20.5 19 20.5 19 18"
            + " V6 M5 12 C5 14.5 19 14.5 19 12";
    private static final String SERVER = "M4 4 H20 V10 H4 Z M4 14 H20 V20 H4 Z M7.5 7 H8.5 M7.5 17 H8.5";
    private static final String CHEVRON = "M9 6 L15 12 L9 18";
    private static final String ARROW = "M5 12 H19 M13 6 L19 12 L13 18";
    private static final String BULB = "M9 18 H15 M10 21 H14 M12 3 A6 6 0 0 0 8 13.5 C9 14.5 9 15.5 9 16 H15 C15"
            + " 15.5 15 14.5 16 13.5 A6 6 0 0 0 12 3";
    private static final String NETWORK = "M10 12 A2 2 0 1 0 14 12 A2 2 0 1 0 10 12 M7.8 7.8 A6 6 0 0 0 7.8 16.2"
            + " M16.2 7.8 A6 6 0 0 1 16.2 16.2 M5 5 A10 10 0 0 0 5 19 M19 5 A10 10 0 0 1 19 19";
    private static final String SHIELD =
            "M12 3 L19.5 6 V11.5 C19.5 16 16.5 19.5 12 21 C7.5 19.5 4.5 16 4.5 11.5 V6 Z" + " M9 12 L11 14 L15 10";
    private static final String BOARD = "M3 4 H21 V15 H3 Z M8 20 L12 15 L16 20 M7 8 H14 M7 11 H11";
    private static final String CAP =
            "M2 9 L12 4.5 L22 9 L12 13.5 Z M6.5 11 V16 C6.5 18.5 17.5 18.5 17.5 16 V11 M22 9 V14";
}
