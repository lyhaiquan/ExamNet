package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import labcast.client.cast.CastSession;
import labcast.client.journal.JournalClient;
import labcast.client.net.LcpClient;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;

/**
 * Màn chính sau đăng nhập: danh sách bài bên trái, bài đang mở ở giữa, thanh trạng thái kết nối ở
 * trên. Giáo viên có thêm nút nhập danh sách lớp. Giai đoạn 2 tách thành màn học viên và màn giáo
 * viên đầy đủ (sơ đồ lớp, chiếu, câu hỏi nhanh).
 */
public final class HomeScreen extends BorderPane {
    private final LoginController.LoggedIn li;
    private final JournalClient journal;
    // Sidebar: nguồn dữ liệu đầy đủ độc lập với các bộ lọc và trạng thái cây.
    private final List<JsonNode> allLessons = new ArrayList<>();
    private final TreeView<SidebarRow> lessons = new TreeView<>();
    private final TextField lessonSearch = new TextField();
    private final ComboBox<String> lessonStatusFilter = new ComboBox<>();
    private final Label lessonCount = new Label("0 bài");
    private final Label lessonProgress = new Label("0/0 hoàn thành");
    private final Label progressPercent = new Label("0%");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Set<String> collapsedGroups = new HashSet<>();
    private boolean rebuildingSidebar;

    // groupKey chỉ có ở nhóm, lesson chỉ có ở dòng bài học.
    private record SidebarRow(String label, String groupKey, JsonNode lesson) {
        boolean isLesson() {
            return lesson != null;
        }
    }

    private final VBox sidebar;
    // Đường kéo ở mép phải sidebar; không làm mất nút thu gọn bên trong.
    private final Region sidebarResizeHandle = new Region();
    private final HBox sidebarFrame = new HBox();
    private double sidebarUserWidth = Double.NaN;
    private double sidebarDragStartX;
    private double sidebarDragStartWidth;
    // Thanh chứa các chức năng bên dưới tiêu đề BÀI HỌC.
    private final VBox sidebarBody = new VBox();
    private final Label sidebarHeading = new Label("BÀI HỌC");
    private final Region sidebarHeaderGap = new Region();
    // Nút luôn nằm trong sidebar; khi thu gọn, giữ lại một thanh mỏng để mở lại.
    private final Button sidebarToggle = new Button();
    private final Button themeToggle = new Button();
    // Mục Code tự do là chức năng độc lập nằm trong SIDEBAR (không thuộc bài học).
    private final Button playgroundButton = new Button("Code tự do");
    // Hủy các phản hồi LESSON_FETCH cũ khi chuyển màn hình.
    private long lessonRequestVersion;
    private PlaygroundScreen playground;
    private boolean playgroundOpen;
    // null: theo kích thước cửa sổ; true/false: lựa chọn thủ công của người dùng.
    private Boolean sidebarUserChoice = null;
    private final Label status = new Label();
    private LessonScreen current;
    /** Mã bài đang mở, null khi chưa mở bài nào. LabCastApp nghe để khoá toàn màn hình. */
    private final ReadOnlyStringWrapper openLesson = new ReadOnlyStringWrapper();
    /** Phiên chiếu của app (ĐG1); null thì màn này không có phần chiếu. */
    private final CastSession cast;

    private final Button castChip = new Button("📡 Giáo viên đang chiếu");
    private CastView castView;
    private boolean castShown;
    private boolean castWasActive;
    private final Runnable onLogout;
    private boolean loggedOut;

    public HomeScreen(LoginController.LoggedIn li, JournalClient journal, Runnable onSessionEnded) {
        this(li, journal, onSessionEnded, null, null);
    }

    /**
     * @param cast phiên chiếu: học viên thấy màn giáo viên chiếu (tự mở khi đang không làm bài), giáo viên
     *     có nút "Chiếu cho lớp" trong màn bài học
     */
    public HomeScreen(LoginController.LoggedIn li, JournalClient journal, Runnable onSessionEnded, CastSession cast) {
        this(li, journal, onSessionEnded, cast, null);
    }

    public HomeScreen(
            LoginController.LoggedIn li,
            JournalClient journal,
            Runnable onSessionEnded,
            CastSession cast,
            Runnable onLogout) {
        this.li = li;
        this.cast = cast;
        this.onLogout = onLogout;
        Theme.apply(this);
        this.journal = journal;
        LcpClient client = li.client();

        setTop(topBar(li));

        // Nút đóng/mở đặt ở góc phải ngay trong sidebar, KHÔNG nằm trên topbar.
        sidebarToggle.setId("sidebar-toggle");
        sidebarToggle.getStyleClass().add("sidebar-collapse-button");
        sidebarToggle.setMinSize(30, 30);
        sidebarToggle.setPrefSize(30, 30);
        sidebarToggle.setMaxSize(30, 30);
        // Biểu tượng PanelLeft dạng nét, giống hình người dùng chọn.
        // Không dùng ảnh hoặc emoji để icon sắc nét trên cả Dark/Light.
        SVGPath panelIcon = new SVGPath();
        panelIcon.setContent("M7 4 H17 C18.66 4 20 5.34 20 7 V17 "
                + "C20 18.66 18.66 20 17 20 H7 "
                + "C5.34 20 4 18.66 4 17 V7 C4 5.34 5.34 4 7 4 Z M10 4 V20");
        panelIcon.getStyleClass().add("sidebar-toggle-icon");
        sidebarToggle.setGraphic(panelIcon);
        sidebarToggle.setTooltip(new Tooltip("Thu gọn danh sách bài"));
        sidebarToggle.setOnAction(e -> {
            sidebarUserChoice = !sidebarBody.isVisible();
            updateSidebar();
        });

        sidebarHeading.getStyleClass().add("section-title");
        lessonCount.getStyleClass().add("muted");
        HBox.setHgrow(sidebarHeaderGap, Priority.ALWAYS);
        lessonCount.getStyleClass().add("lesson-count");
        HBox sideHeader = new HBox(7, sidebarHeading, sidebarHeaderGap, lessonCount, sidebarToggle);
        sideHeader.setAlignment(Pos.CENTER_LEFT);
        sideHeader.getStyleClass().add("sidebar-header");

        // Lối tắt Code tự do luôn ở Sidebar, KHÔNG phải một TreeItem bài học.
        // Bấm là mở thẳng editor Python, không gửi LESSON_FETCH.
        playgroundButton.setId("sidebar-playground");
        playgroundButton.getStyleClass().add("sidebar-playground-button");
        SVGPath codeIcon = new SVGPath();
        codeIcon.setContent("M8 7 L3 12 L8 17 M16 7 L21 12 L16 17 M14 5 L10 19");
        codeIcon.getStyleClass().add("sidebar-playground-icon");
        playgroundButton.setGraphic(codeIcon);
        playgroundButton.setGraphicTextGap(9);
        playgroundButton.setMaxWidth(Double.MAX_VALUE);
        playgroundButton.setAlignment(Pos.CENTER_LEFT);
        playgroundButton.setTooltip(new Tooltip("Mở Code tự do: viết Python và xem Trace không cần chọn bài"));
        playgroundButton.setOnAction(e -> showPlayground());
        VBox.setMargin(playgroundButton, new Insets(0, 8, 9, 8));

        lessonSearch.setPromptText("Tìm bài học...");
        lessonSearch.setId("lesson-search");
        lessonSearch.getStyleClass().add("lesson-search");
        lessonSearch.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) lessonSearch.clear();
        });
        lessonSearch.textProperty().addListener((obs, old, val) -> rebuildSidebar());

        lessonStatusFilter.getItems().addAll("Tất cả", "Đã hoàn thành", "Đang học", "Chưa có kết quả");
        lessonStatusFilter.getSelectionModel().selectFirst();
        lessonStatusFilter.setMaxWidth(Double.MAX_VALUE);
        lessonStatusFilter.getStyleClass().add("lesson-filter");
        lessonStatusFilter.valueProperty().addListener((obs, old, val) -> rebuildSidebar());
        Button reload = new Button("↻");
        reload.setTooltip(new javafx.scene.control.Tooltip("Cập nhật tiến độ từ server"));
        reload.getStyleClass().add("sidebar-reload");
        reload.setOnAction(e -> loadLessons());
        HBox filters = new HBox(6, lessonStatusFilter, reload);
        filters.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(lessonStatusFilter, Priority.ALWAYS);
        VBox toolbar = new VBox(6, lessonSearch, filters);
        toolbar.getStyleClass().add("sidebar-toolbar");

        lessons.setId("lessons");
        lessons.setShowRoot(false);
        lessons.setRoot(new TreeItem<>(new SidebarRow("", "root", null)));
        lessons.getStyleClass().add("lesson-tree");
        lessons.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(SidebarRow row, boolean empty) {
                super.updateItem(row, empty);
                setText(null);
                setGraphic(null);
                getStyleClass().removeAll("lesson-tree-item", "lesson-tree-group");
                if (empty || row == null) return;
                if (row.isLesson()) {
                    getStyleClass().add("lesson-tree-item");
                    setGraphic(lessonCell(row.lesson()));
                } else {
                    getStyleClass().add("lesson-tree-group");
                    Label text = new Label(row.label());
                    text.getStyleClass().add("lesson-group-label");
                    setGraphic(text);
                }
            }
        });
        lessons.getSelectionModel().selectedItemProperty().addListener((obs, old, node) -> {
            if (!rebuildingSidebar
                    && node != null
                    && node.getValue() != null
                    && node.getValue().isLesson()) {
                JsonNode lesson = node.getValue().lesson();
                if (li.role() == Role.STUDENT && !lesson.path("active").asBoolean(true)) {
                    setStatus("Bài học này hiện chưa được mở", "warning");
                    return;
                }
                String id = lesson.path("id").asText();
                if (!id.equals(openLesson.get())) open(id);
            }
        });

        lessonProgress.getStyleClass().add("sidebar-progress-text");
        progressPercent.getStyleClass().add("sidebar-progress-percent");
        Region progressGap = new Region();
        HBox.setHgrow(progressGap, Priority.ALWAYS);
        HBox progressHeader = new HBox(6, lessonProgress, progressGap, progressPercent);
        progressHeader.setAlignment(Pos.CENTER_LEFT);
        progressBar.getStyleClass().add("sidebar-progress-bar");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(5);
        VBox progressBox = new VBox(5, progressHeader, progressBar);
        progressBox.getStyleClass().add("sidebar-progress");

        // Mục Code tự do nằm NGAY TRÊN DSA/SQL, dưới ô tìm kiếm và bộ lọc.
        // Không thuộc TreeView: không yêu cầu chọn bài và không bị bộ lọc bài ẩn đi.
        VBox.setMargin(playgroundButton, new Insets(2, 8, 6, 8));
        sidebarBody.getChildren().addAll(toolbar, playgroundButton, lessons, progressBox);
        VBox.setVgrow(lessons, Priority.ALWAYS);
        sidebar = new VBox(sideHeader, sidebarBody);
        VBox.setVgrow(sidebarBody, Priority.ALWAYS);
        sidebar.getStyleClass().add("sidebar");

        // Dải kéo rộng 6 px nằm ở mép phải sidebar.
        // Kéo sang phải để nới rộng, sang trái để thu nhỏ.
        sidebarResizeHandle.getStyleClass().add("sidebar-resize-handle");
        sidebarResizeHandle.setMinWidth(6);
        sidebarResizeHandle.setPrefWidth(6);
        sidebarResizeHandle.setMaxWidth(6);
        sidebarResizeHandle.setCursor(Cursor.H_RESIZE);
        sidebarResizeHandle.setOnMousePressed(e -> {
            if (!e.isPrimaryButtonDown()) return;
            sidebarDragStartX = e.getScreenX();
            sidebarDragStartWidth = sidebar.getWidth();
            e.consume();
        });
        sidebarResizeHandle.setOnMouseDragged(e -> {
            if (!e.isPrimaryButtonDown()) return;
            double candidate = sidebarDragStartWidth + e.getScreenX() - sidebarDragStartX;
            // Giữ đủ chỗ cho Code Editor và Đề bài khi kéo.
            double maxWidth = Math.max(180, Math.min(480, getWidth() - 380));
            sidebarUserWidth = Math.max(180, Math.min(maxWidth, candidate));
            sidebarUserChoice = true;
            updateSidebar();
            e.consume();
        });
        sidebarResizeHandle.setOnMouseReleased(e -> e.consume());
        sidebarFrame.getChildren().addAll(sidebar, sidebarResizeHandle);
        widthProperty().addListener((obs, o, w) -> updateSidebar());
        setLeft(sidebarFrame);
        setCenter(emptyState());
        updateSidebar();

        client.onStateChange(s -> Platform.runLater(() -> {
            if (loggedOut) {
                return;
            }
            switch (s) {
                case RECONNECTING -> setStatus("Mất kết nối, đang nối lại… code đang gõ vẫn được giữ", "warn");
                case CONNECTED -> setStatus("Đã nối lại", "ok");
                case CLOSED -> onSessionEnded.run();
            }
        }));
        client.on(MessageType.ERROR, f -> {
            if (loggedOut) {
                return;
            }
            try {
                String msg = ErrorMessage.from(f).message();
                Platform.runLater(() -> setStatus(msg, "error"));
            } catch (Exception ignored) {
                // bỏ
            }
        });
        loadLessons();
        if (cast != null && li.role() == Role.STUDENT) {
            cast.onChange(this::onCastChange);
        }
    }

    /** Giáo viên bắt đầu/dừng chiếu. Học viên đang không làm bài thì tự mở màn chiếu. */
    private void onCastChange() {
        boolean active = cast.active();
        if (active && !castWasActive && !castShown && current == null && !playgroundOpen) {
            showCast();
        }
        castWasActive = active;
        castChip.setVisible(active && !castShown);
    }

    /** Mở màn chiếu; đang làm bài thì đóng bài (code đã lưu liên tục lên server). */
    private void showCast() {
        if (current != null) {
            closeLesson();
        }
        if (playgroundOpen) hidePlayground();
        if (castView == null) {
            castView = new CastView(cast, this::lessonName, this::hideCast);
        }
        setCenter(castView);
        castShown = true;
        castChip.setVisible(false);
        cast.attach(castView);
    }

    private void hideCast() {
        cast.detachScreen();
        castShown = false;
        setCenter(emptyState());
        castChip.setVisible(cast.active());
    }

    /**
     * Chuyển sang editor tự do, không cần lessonId, không gửi LESSON_FETCH.
     * Khởi tạo màn trước khi đóng bài hiện tại, tránh mất nội dung khi lỗi tạo editor.
     */
    private void showPlayground() {
        lessonRequestVersion++; // Vô hiệu phản hồi mở bài đang chờ.
        try {
            if (playground == null) {
                playground = new PlaygroundScreen(li.client());
            }
            if (current != null) {
                current.dispose();
                current = null;
                openLesson.set(null);
            }
            if (castShown) {
                cast.detachScreen();
                castShown = false;
                castChip.setVisible(cast != null && cast.active());
            }
            // Gọi setCenter mỗi lần, kể cả khi playgroundOpen=true nhưng một màn khác
            // vô tình đã thay thế center; không quay về màn "Chọn bài" khi bấm lại.
            playgroundOpen = true;
            lessons.getSelectionModel().clearSelection();
            setCenter(playground);
            refreshPlaygroundButton();
            updateSidebar();
            setStatus("", null);
        } catch (RuntimeException | LinkageError ex) {
            // Không để sự kiện click thất bại im lặng trong JavaFX.
            ex.printStackTrace();
            playgroundOpen = false;
            refreshPlaygroundButton();
            setStatus("Không mở được Code tự do: " + ex.getMessage(), "error");
            alert(Alert.AlertType.ERROR, "Không mở được trình soạn thảo Code tự do.\n" + ex);
        }
    }

    /** Khi chọn bài DSA/SQL khác, quay lại luồng chọn bài bình thường. */
    private void hidePlayground() {
        if (!playgroundOpen) return;
        playgroundOpen = false;
        if (playground != null) playground.saveDraft();
        refreshPlaygroundButton();
        if (current == null && !castShown) setCenter(emptyState());
    }

    private void refreshPlaygroundButton() {
        playgroundButton.getStyleClass().remove("sidebar-playground-active");
        if (playgroundOpen) playgroundButton.getStyleClass().add("sidebar-playground-active");
    }

    /** Tên bài theo mã, lấy từ danh sách bài bên trái. */
    private String lessonName(String id) {
        for (JsonNode n : allLessons) {
            if (n.path("id").asText().equals(id)) {
                return n.path("ten").asText();
            }
        }
        return id;
    }

    /**
     * Sidebar thu gọn bên trong chính nó: luôn giữ lại thanh 50px chứa nút mở.
     * Màn hình hẹp tự thu khi đang làm bài; người dùng có thể ghi đè bằng nút.
     */
    private void updateSidebar() {
        double w = getWidth();
        boolean autoExpand = current == null || w <= 0 || w >= 1060;
        boolean expanded = sidebarUserChoice == null ? autoExpand : sidebarUserChoice;

        // Không setLeft(null): nếu ẩn cả sidebar sẽ không còn nút để mở lại.
        // Khi thu gọn chỉ còn thanh icon 44 px, nội dung làm bài rộng hơn.
        double defaultWidth = w < 1100 ? 220 : 260;
        double preferredWidth = Double.isNaN(sidebarUserWidth) ? defaultWidth : sidebarUserWidth;
        double maxWidth = w > 0 ? Math.max(180, Math.min(480, w - 380)) : 480;
        double expandedWidth = Math.max(180, Math.min(maxWidth, preferredWidth));
        double sidebarWidth = expanded ? expandedWidth : 44;

        // Pref/Min/Max đồng nhất để Sidebar thật sự thay đổi chiều rộng.
        sidebar.setPrefWidth(sidebarWidth);
        sidebar.setMinWidth(sidebarWidth);
        sidebar.setMaxWidth(sidebarWidth);

        // Khi thu gọn, dải kéo biến mất; icon mở lại vẫn còn 44 px.
        sidebarResizeHandle.setVisible(expanded);
        sidebarResizeHandle.setManaged(expanded);

        for (javafx.scene.Node node :
                new javafx.scene.Node[] {sidebarHeading, sidebarHeaderGap, lessonCount, sidebarBody}) {
            node.setVisible(expanded);
            node.setManaged(expanded);
        }

        // Code tự do thuộc sidebarBody; khi thu gọn, chỉ để lại nút mở sidebar.
        // Khi mở lại, nút luôn nằm phía trên nhóm DSA.
        playgroundButton.setMinWidth(0);

        sidebar.getStyleClass().remove("sidebar-collapsed");
        if (!expanded) sidebar.getStyleClass().add("sidebar-collapsed");
        // Cả hai trạng thái đều dùng biểu tượng PanelLeft, không đổi sang emoji.
        sidebarToggle.setAccessibleText(expanded ? "Thu gọn bài học" : "Mở bài học");
        sidebarToggle.getTooltip().setText(expanded ? "Thu gọn danh sách bài" : "Mở danh sách bài");
    }

    /** Thanh trên cùng: logo, tên lớp, trạng thái kết nối, người đang đăng nhập. */
    private HBox topBar(LoginController.LoggedIn li) {
        Label mark = new Label("LC");
        mark.getStyleClass().add("brand-mark");
        Label brand = new Label("LabCast");
        brand.getStyleClass().add("brand");
        Label cls = new Label("⌑  " + li.className());
        cls.getStyleClass().add("class-chip");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        status.getStyleClass().add("status-pill");
        status.managedProperty().bind(status.visibleProperty());
        status.setVisible(false);

        Label avatar = new Label(initial(li.fullName()));
        avatar.getStyleClass().add("avatar");
        Label name = new Label(li.fullName());
        name.getStyleClass().add("user-name");
        Label role = new Label(li.role() == Role.TEACHER ? "Giáo viên" : "Học viên");
        role.getStyleClass().add("user-role");
        VBox who = new VBox(0, name, role);
        who.setAlignment(Pos.CENTER_LEFT);

        // Nut doi Dark/Light khong can tai lai ung dung.
        themeToggle.setId("theme-toggle");
        themeToggle.getStyleClass().add("on-dark");
        themeToggle.getStyleClass().add("theme-switch-modern");
        themeToggle.setText(Theme.isDark() ? "☀  Light" : "☾  Dark");
        themeToggle.setTooltip(new Tooltip("Đổi giao diện Sáng / Tối"));
        themeToggle.setOnAction(e -> {
            Theme.setDark(!Theme.isDark());
            themeToggle.setText(Theme.isDark() ? "☀  Light" : "☾  Dark");
        });

        // Code tự do nay nằm riêng trong Sidebar, không chèn vào topbar.
        HBox bar = new HBox(10, mark, brand, cls, themeToggle, spacer, status);
        bar.getStyleClass().add("topbar-modern");
        if (cast != null && li.role() == Role.STUDENT) {
            castChip.setId("cast-chip");
            castChip.getStyleClass().add("cast-chip");
            castChip.managedProperty().bind(castChip.visibleProperty());
            castChip.setVisible(false);
            castChip.setOnAction(e -> showCast());
            bar.getChildren().add(castChip);
        }
        if (li.role() == Role.TEACHER) {
            Button uploadBtn = new Button("Tải lên bài tập (.zip)…");
            uploadBtn.getStyleClass().add("on-dark");
            uploadBtn.setOnAction(e -> uploadLessonZip());

            Button importBtn = new Button("Nhập danh sách lớp (CSV)…");
            importBtn.getStyleClass().add("on-dark");
            importBtn.setOnAction(e -> importRoster());
            bar.getChildren().addAll(uploadBtn, importBtn);
        }
        bar.getChildren().addAll(avatar, who);

        Button logoutBtn = new Button("Đăng xuất");
        logoutBtn.setId("logout-btn");
        logoutBtn.getStyleClass().addAll("on-dark", "logout");
        logoutBtn.setOnAction(e -> logout());
        bar.getChildren().add(logoutBtn);

        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("topbar");
        return bar;
    }

    /** Chữ cái đầu của tên gọi (từ cuối trong họ tên Việt). */
    static String initial(String fullName) {
        String[] parts = fullName.strip().split("\\s+");
        String last = parts[parts.length - 1];
        return last.isEmpty() ? "?" : last.substring(0, 1).toUpperCase();
    }

    /** Một bài học = hàng gọn 2 dòng; icon trạng thái thay cho chip dài. */
    private VBox lessonCell(JsonNode n) {
        String fullTitle = n.path("ten").asText();
        Label title = new Label(fullTitle);
        title.getStyleClass().add("lesson-title");
        title.setWrapText(false);
        title.setTextOverrun(OverrunStyle.ELLIPSIS);
        title.setMinWidth(0);
        title.setMaxWidth(Double.MAX_VALUE);
        // Chừa chỗ cho icon trạng thái và scrollbar; dùng được bề ngang nới thêm.
        title.maxWidthProperty().bind(lessons.widthProperty().subtract(65));
        title.setTooltip(new Tooltip(fullTitle));
        HBox.setHgrow(title, Priority.ALWAYS);

        boolean isTeacher = li.role() == Role.TEACHER;
        boolean active = n.path("active").asBoolean(true);
        String lessonId = n.path("id").asText();

        String value = n.path("status").asText("NOT_STARTED");
        boolean isInactiveForStudent = (!isTeacher && !active);

        String iconText;
        String fullStatus;
        if (isInactiveForStudent) {
            iconText = "—";
            fullStatus = "Chưa mở";
        } else {
            iconText = switch (value) {
                case "MASTERED" -> "✓";
                case "OPEN" -> "●";
                default -> "○";
            };
            fullStatus = switch (value) {
                case "MASTERED" -> "Đã hoàn thành";
                case "OPEN" -> "Đang học";
                default -> "Chưa có kết quả";
            };
        }

        Label state = new Label(iconText);
        state.getStyleClass().add("lesson-state-dot");
        if (isInactiveForStudent) {
            state.getStyleClass().add("lesson-state-disabled");
        } else {
            state.getStyleClass().add(switch (value) {
                case "MASTERED" -> "lesson-state-done";
                case "OPEN" -> "lesson-state-progress";
                default -> "lesson-state-new";
            });
        }
        state.setTooltip(new Tooltip(fullStatus));
        HBox firstLine = new HBox(4, title, state);
        firstLine.setAlignment(Pos.CENTER_LEFT);

        String type = LessonScreen.loaiLabel(n.path("loai").asText());
        Label typeLabel = new Label(type);
        typeLabel.getStyleClass().add("lesson-type-compact");
        Label statusText = new Label(fullStatus);
        statusText.getStyleClass().add("lesson-detail-status");
        if (!isInactiveForStudent && "MASTERED".equals(value) && n.path("score").isNumber()) {
            statusText.setText(n.path("score").asText() + " điểm");
        }
        statusText.setTooltip(new Tooltip(fullStatus));
        HBox secondLine = new HBox(6, typeLabel, statusText);
        secondLine.setAlignment(Pos.CENTER_LEFT);

        if (isTeacher) {
            Label toggleBadge = new Label(active ? "● Đang mở" : "○ Tạm ẩn");
            toggleBadge.getStyleClass().addAll("lesson-toggle-badge", active ? "lesson-toggle-active" : "lesson-toggle-inactive");
            toggleBadge.setTooltip(new Tooltip(active ? "Nhấn để tạm ẩn bài tập đối với học viên" : "Nhấn để kích hoạt mở bài tập cho học viên"));
            toggleBadge.setOnMouseClicked(e -> {
                e.consume();
                toggleLessonActive(lessonId, !active);
            });
            secondLine.getChildren().add(toggleBadge);
        }

        VBox cell = new VBox(2, firstLine, secondLine);
        cell.getStyleClass().add("lesson-compact-row");
        if (isInactiveForStudent) {
            cell.getStyleClass().add("lesson-row-inactive");
        }
        return cell;
    }

    private static String normalize(String text) {
        String raw = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD);
        return raw.replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private static String chapterName(String text) {
        if (text == null || text.isBlank()) return "Khác";
        String pretty = LessonScreen.chapterLabel(text);
        return Character.toUpperCase(pretty.charAt(0)) + pretty.substring(1);
    }

    /** Nhóm DSA/SQL → chương → bài, hỗ trợ lọc nhưng không làm mất dữ liệu gốc. */
    private void rebuildSidebar() {
        if (rebuildingSidebar) return;
        rebuildingSidebar = true;
        try {
            String query = normalize(lessonSearch.getText());
            String selection = lessonStatusFilter.getValue();
            Map<String, Map<String, List<JsonNode>>> groups = new LinkedHashMap<>();
            int done = 0;
            List<JsonNode> ordered = new ArrayList<>(allLessons);
            ordered.sort(Comparator.comparingInt((JsonNode l) -> {
                        int i = LessonScreen.CHAPTERS.indexOf(l.path("chuong").asText());
                        return i < 0 ? Integer.MAX_VALUE : i;
                    })
                    .thenComparingInt(l -> LessonScreen.loaiRank(l.path("loai").asText())));
            for (JsonNode lesson : ordered) {
                if ("MASTERED".equals(lesson.path("status").asText())) done++;
                String statusValue = lesson.path("status").asText("NOT_STARTED");
                if ("Đã hoàn thành".equals(selection) && !"MASTERED".equals(statusValue)) continue;
                if ("Đang học".equals(selection) && !"OPEN".equals(statusValue)) continue;
                if ("Chưa có kết quả".equals(selection)
                        && !"NOT_STARTED".equals(statusValue)
                        && !"LOCKED".equals(statusValue)) continue;
                String haystack = normalize(lesson.path("ten").asText() + " "
                        + lesson.path("id").asText() + " " + lesson.path("mon").asText()
                        + " " + lesson.path("chuong").asText());
                if (!query.isEmpty() && !haystack.contains(query)) continue;
                String mon = lesson.path("mon").asText("khac").toUpperCase(Locale.ROOT);
                String chapter = lesson.path("chuong").asText("khac");
                groups.computeIfAbsent(mon, k -> new LinkedHashMap<>())
                        .computeIfAbsent(chapter, k -> new ArrayList<>())
                        .add(lesson);
            }
            progressBar.setProgress(allLessons.isEmpty() ? 0 : (double) done / allLessons.size());
            lessonProgress.setText(done + "/" + allLessons.size() + " hoàn thành");
            progressPercent.setText(allLessons.isEmpty() ? "0%" : Math.round(done * 100.0 / allLessons.size()) + "%");

            TreeItem<SidebarRow> root = new TreeItem<>(new SidebarRow("", "root", null));
            int visible = 0;
            for (var subject : groups.entrySet()) {
                int subjectCount = subject.getValue().values().stream()
                        .mapToInt(List::size)
                        .sum();
                String monKey = "mon:" + subject.getKey();
                TreeItem<SidebarRow> mon =
                        new TreeItem<>(new SidebarRow(subject.getKey() + "  ·  " + subjectCount, monKey, null));
                mon.setExpanded(!collapsedGroups.contains(monKey) || !query.isEmpty());
                mon.expandedProperty().addListener((obs, old, val) -> {
                    if (query.isEmpty()) rememberCollapse(monKey, val);
                });
                for (var chapter : subject.getValue().entrySet()) {
                    String chapterKey = monKey + ":" + chapter.getKey();
                    TreeItem<SidebarRow> group = new TreeItem<>(new SidebarRow(
                            chapterName(chapter.getKey()) + "  ·  "
                                    + chapter.getValue().size(),
                            chapterKey,
                            null));
                    group.setExpanded(!collapsedGroups.contains(chapterKey) || !query.isEmpty());
                    group.expandedProperty().addListener((obs, old, val) -> {
                        if (query.isEmpty()) rememberCollapse(chapterKey, val);
                    });
                    for (JsonNode l : chapter.getValue()) {
                        TreeItem<SidebarRow> leaf =
                                new TreeItem<>(new SidebarRow(l.path("ten").asText(), null, l));
                        group.getChildren().add(leaf);
                        visible++;
                    }
                    mon.getChildren().add(group);
                }
                root.getChildren().add(mon);
            }
            lessons.setRoot(root);
            lessonCount.setText(visible + " bài");
            String activeId = openLesson.get();
            if (activeId != null) {
                for (TreeItem<SidebarRow> subject : root.getChildren()) {
                    for (TreeItem<SidebarRow> chapter : subject.getChildren()) {
                        for (TreeItem<SidebarRow> leaf : chapter.getChildren()) {
                            if (activeId.equals(
                                    leaf.getValue().lesson().path("id").asText())) {
                                lessons.getSelectionModel().select(leaf);
                            }
                        }
                    }
                }
            }
        } finally {
            rebuildingSidebar = false;
        }
    }

    private void rememberCollapse(String key, boolean expanded) {
        if (expanded) collapsedGroups.remove(key);
        else collapsedGroups.add(key);
    }

    private static VBox emptyState() {
        Label t = new Label("Chọn một bài để bắt đầu");
        t.getStyleClass().add("empty-title");
        Label d = new Label("Danh sách bài ở bên trái. Mỗi bài có animation giảng giải, đề bài và ô để em tự code.");
        d.getStyleClass().add("muted");
        d.setWrapText(true);
        VBox box = new VBox(6, t, d);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(420);
        box.getStyleClass().add("empty-state");
        return box;
    }

    /** {@code variant}: {@code ok}, {@code warn}, {@code error} hoặc null — màu ở labcast.css. */
    private void setStatus(String text, String variant) {
        status.setText(text);
        status.getStyleClass().removeAll("ok", "warn", "error");
        if (variant != null) {
            status.getStyleClass().add(variant);
        }
        status.setVisible(text != null && !text.isEmpty());
    }

    public ReadOnlyStringProperty openLessonProperty() {
        return openLesson.getReadOnlyProperty();
    }

    /** Nút "Đóng bài": về màn chọn bài, thả khoá toàn màn hình. */
    public void closeLesson() {
        lessonRequestVersion++;
        if (current != null) {
            current.dispose();
            current = null;
        }
        lessons.getSelectionModel().clearSelection();
        setCenter(emptyState());
        openLesson.set(null);
        // Đóng bài thì khôi phục danh sách để chọn bài tiếp theo.
        sidebarUserChoice = null;
        updateSidebar();
        if (!loggedOut) loadLessons(); // Đồng bộ trạng thái MASTERED sau khi nộp bài
    }

    /** Đăng xuất: đóng bài (nếu đang mở), giải phóng tài nguyên và quay về màn hình đăng nhập. */
    public void logout() {
        if (loggedOut) {
            return;
        }
        loggedOut = true;
        if (playground != null) {
            playground.saveDraft();
            playground.dispose();
            playground = null;
            playgroundOpen = false;
        }
        closeLesson();
        if (onLogout != null) {
            onLogout.run();
        }
    }

    /** Hộp thoại có chủ là cửa sổ app: không có chủ thì nó mở ra nằm sau cửa sổ toàn màn hình. */
    private void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg);
        if (getScene() != null) {
            a.initOwner(getScene().getWindow());
        }
        a.show();
    }

    private void loadLessons() {
        LcpClient c = li.client();
        var reply = c.expect(MessageType.LESSON_DATA);
        c.send(MessageType.LESSON_LIST, new byte[0]);
        reply.orTimeout(10, TimeUnit.SECONDS).thenAccept(f -> {
            try {
                JsonNode list = JsonPayload.tree(f).path("lessons");
                Platform.runLater(() -> {
                    if (loggedOut) return;
                    allLessons.clear();
                    list.forEach(allLessons::add);
                    rebuildSidebar();
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Không đọc được danh sách bài", "error"));
            }
        });
    }

    private void open(String lessonId) {
        if (li.role() == Role.STUDENT) {
            for (JsonNode n : allLessons) {
                if (lessonId.equals(n.path("id").asText()) && !n.path("active").asBoolean(true)) {
                    setStatus("Bài học này hiện chưa được mở", "warning");
                    return;
                }
            }
        }
        // Chỉ bấm vào một bài mới gửi LESSON_FETCH. Code tự do không đi qua đây.
        if (playgroundOpen) hidePlayground();
        long requestVersion = ++lessonRequestVersion;
        LcpClient c = li.client();
        var reply = c.expect(MessageType.LESSON_DATA, MessageType.ERROR);
        c.send(MessageType.LESSON_FETCH, new PayloadWriter().str(lessonId).toBytes());
        setStatus("Đang mở bài…", null);
        reply.orTimeout(30, TimeUnit.SECONDS)
                .whenComplete((f, err) -> Platform.runLater(() -> {
                    // Không cho response bài học cũ ghi đè màn Code tự do.
                    if (loggedOut || playgroundOpen || requestVersion != lessonRequestVersion) return;
                    if (err != null || f.type() != MessageType.LESSON_DATA) {
                        setStatus("Không mở được bài " + lessonId, "error");
                        return;
                    }
                    try {
                        JsonNode lesson = JsonPayload.tree(f);
                        if (current != null) {
                            current.dispose();
                        }
                        if (castShown) {
                            cast.detachScreen();
                            castShown = false;
                            castChip.setVisible(cast.active());
                        }
                        if (playgroundOpen) hidePlayground();
                        current = new LessonScreen(
                                c, journal, lesson, li.role() == Role.STUDENT, this::closeLesson, cast);
                        setCenter(current);
                        openLesson.set(lessonId);
                        updateSidebar();
                        setStatus("", null);
                    } catch (Exception e) {
                        setStatus("Bài hỏng: " + e.getMessage(), "error");
                    }
                }));
    }

    private void importRoster() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Chọn file CSV: username,họ tên,mật khẩu");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv", "*.txt"));
        File file = fc.showOpenDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        String csv;
        try {
            csv = Files.readString(file.toPath(), StandardCharsets.UTF_8).replace("﻿", "");
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Không đọc được file: " + e.getMessage());
            return;
        }
        LcpClient c = li.client();
        var reply = c.expect(MessageType.ADMIN_OK, MessageType.ERROR);
        c.send(MessageType.ADMIN_ROSTER_IMPORT, new PayloadWriter().str(csv).toBytes());
        reply.orTimeout(60, TimeUnit.SECONDS)
                .whenComplete((Frame f, Throwable err) -> Platform.runLater(() -> {
                    try {
                        String msg = err != null
                                ? "Server không trả lời"
                                : f.type() == MessageType.ADMIN_OK
                                        ? f.reader().str()
                                        : ErrorMessage.from(f).message();
                        alert(Alert.AlertType.INFORMATION, msg);
                    } catch (Exception e) {
                        alert(Alert.AlertType.ERROR, e.getMessage());
                    }
                }));
    }

    private void toggleLessonActive(String lessonId, boolean active) {
        LcpClient c = li.client();
        var reply = c.expect(MessageType.ADMIN_OK, MessageType.ERROR);
        c.send(MessageType.ADMIN_LESSON_TOGGLE, new PayloadWriter().str(lessonId).bool(active).toBytes());
        reply.orTimeout(10, TimeUnit.SECONDS)
                .whenComplete((f, err) -> Platform.runLater(() -> {
                    try {
                        if (err != null || f.type() != MessageType.ADMIN_OK) {
                            alert(Alert.AlertType.ERROR, "Không đổi được trạng thái bài: " + (err != null ? err.getMessage() : ErrorMessage.from(f).message()));
                            return;
                        }
                        for (int i = 0; i < allLessons.size(); i++) {
                            JsonNode n = allLessons.get(i);
                            if (lessonId.equals(n.path("id").asText())) {
                                if (n instanceof ObjectNode on) {
                                    on.put("active", active);
                                }
                                break;
                            }
                        }
                        rebuildSidebar();
                        setStatus(active ? "Đã mở kích hoạt bài " + lessonId : "Đã tạm ẩn bài " + lessonId, null);
                    } catch (Exception e) {
                        alert(Alert.AlertType.ERROR, e.getMessage());
                    }
                }));
    }

    private void uploadLessonZip() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Chọn file ZIP bài tập (chứa lesson.yaml, de.md...)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("File ZIP", "*.zip"));
        File file = fc.showOpenDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Không đọc được file: " + e.getMessage());
            return;
        }
        setStatus("Đang tải lên bài tập…", null);
        LcpClient c = li.client();
        var reply = c.expect(MessageType.ADMIN_OK, MessageType.ERROR);
        c.send(MessageType.ADMIN_LESSON_UPLOAD, new PayloadWriter().str(file.getName()).blob(bytes).toBytes());
        reply.orTimeout(60, TimeUnit.SECONDS)
                .whenComplete((Frame f, Throwable err) -> Platform.runLater(() -> {
                    try {
                        if (err != null) {
                            alert(Alert.AlertType.ERROR, "Server không phản hồi: " + err.getMessage());
                            setStatus("Tải lên thất bại", "error");
                            return;
                        }
                        if (f.type() == MessageType.ADMIN_OK) {
                            String msg = f.reader().str();
                            alert(Alert.AlertType.INFORMATION, msg);
                            setStatus(msg, null);
                            loadLessons();
                        } else {
                            String msg = ErrorMessage.from(f).message();
                            alert(Alert.AlertType.ERROR, msg);
                            setStatus("Lỗi: " + msg, "error");
                        }
                    } catch (Exception e) {
                        alert(Alert.AlertType.ERROR, e.getMessage());
                    }
                }));
    }
}
