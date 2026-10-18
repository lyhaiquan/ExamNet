package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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
    private final ListView<JsonNode> lessons = new ListView<>();
    /** Mọi bài server gửi, theo thứ tự chương; {@code lessons} là phần đang hiện sau khi lọc và sắp. */
    private final List<JsonNode> all = new ArrayList<>();

    private final TextField search = new TextField();
    private final ComboBox<String> order = new ComboBox<>();
    private final Label count = new Label();
    /** Đang thay danh sách hiện: thay đổi chọn bài lúc này không phải người dùng bấm. */
    private boolean refreshing;

    private final VBox sidebar;
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

        Label header = new Label("BÀI HỌC");
        header.getStyleClass().add("section-title");
        count.getStyleClass().add("muted");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox sideHeader = new HBox(header, gap, count);
        sideHeader.getStyleClass().add("sidebar-header");

        search.setId("lesson-search");
        search.setPromptText("Tìm bài…");
        search.textProperty().addListener((obs, o, n) -> refresh());
        search.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                search.clear();
            }
        });
        order.setId("lesson-order");
        order.getItems().setAll(ORDER_AZ, ORDER_CHUONG);
        order.getSelectionModel().select(ORDER_AZ);
        order.setOnAction(e -> refresh());
        order.setMaxWidth(Double.MAX_VALUE);
        VBox tools = new VBox(6, search, order);
        tools.getStyleClass().add("sidebar-tools");

        lessons.setId("lessons");
        lessons.getStyleClass().add("lesson-list");
        lessons.setPlaceholder(new Label("Đang tải danh sách bài…"));
        lessons.setCellFactory(lv -> {
            ListCell<JsonNode> cell = new ListCell<>() {
                @Override
                protected void updateItem(JsonNode n, boolean empty) {
                    super.updateItem(n, empty);
                    setText(null);
                    setGraphic(empty || n == null ? null : lessonCell(n));
                }
            };
            // Ô rộng đúng bằng danh sách để tên bài dài xuống dòng. Để mặc định thì ô rộng bằng tên bài
            // trên một dòng và phần thừa bị cắt ở mép phải.
            cell.setPrefWidth(0);
            return cell;
        });
        lessons.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null && !refreshing) {
                open(n.path("id").asText());
            }
        });
        sidebar = new VBox(sideHeader, tools, lessons);
        VBox.setVgrow(lessons, Priority.ALWAYS);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(260);
        widthProperty().addListener((obs, o, w) -> updateSidebar());
        setLeft(sidebar);
        setCenter(emptyState());

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
        if (active && !castWasActive && !castShown && current == null) {
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

    /** Tên bài theo mã, lấy từ danh sách bài bên trái (kể cả bài đang bị lọc ẩn). */
    private String lessonName(String id) {
        for (JsonNode n : all) {
            if (n.path("id").asText().equals(id)) {
                return n.path("ten").asText();
            }
        }
        return id;
    }

    /**
     * Cửa sổ hẹp hoặc phóng chữ to: danh sách bài hẹp lại (tên bài tự xuống dòng); hẹp hơn nữa mà đang mở
     * bài thì ẩn hẳn để animation và ô code đủ chỗ nằm cạnh nhau. Nút "Đóng bài" đưa danh sách trở lại.
     */
    private void updateSidebar() {
        double w = getWidth();
        sidebar.setPrefWidth(w < 1100 ? 200 : 260);
        boolean show = current == null || w <= 0 || w >= 1060;
        if (show != (getLeft() == sidebar)) {
            setLeft(show ? sidebar : null);
        }
    }

    /** Thanh trên cùng: logo, tên lớp, trạng thái kết nối, người đang đăng nhập. */
    private HBox topBar(LoginController.LoggedIn li) {
        Label mark = new Label("LC");
        mark.getStyleClass().add("brand-mark");
        Label brand = new Label("LabCast");
        brand.getStyleClass().add("brand");
        Label cls = new Label(li.className());
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

        HBox bar = new HBox(10, mark, brand, cls, spacer, status);
        if (cast != null && li.role() == Role.STUDENT) {
            castChip.setId("cast-chip");
            castChip.getStyleClass().add("cast-chip");
            castChip.managedProperty().bind(castChip.visibleProperty());
            castChip.setVisible(false);
            castChip.setOnAction(e -> showCast());
            bar.getChildren().add(castChip);
        }
        if (li.role() == Role.TEACHER) {
            Button importBtn = new Button("Nhập danh sách lớp (CSV)…");
            importBtn.getStyleClass().add("on-dark");
            importBtn.setOnAction(e -> importRoster());
            bar.getChildren().add(importBtn);
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

    private static VBox lessonCell(JsonNode n) {
        Label title = new Label(n.path("ten").asText());
        title.getStyleClass().add("lesson-title");
        title.setWrapText(true);
        String loai = n.path("loai").asText();
        Label badge = new Label(LessonScreen.loaiLabel(loai));
        badge.getStyleClass().addAll("badge", "badge-" + loai);
        Label mon = new Label(n.path("mon").asText().toUpperCase());
        mon.getStyleClass().add("lesson-meta");
        HBox meta = new HBox(6, badge, mon);
        meta.setAlignment(Pos.CENTER_LEFT);
        return new VBox(5, title, meta);
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
        if (current != null) {
            current.dispose();
            current = null;
        }
        lessons.getSelectionModel().clearSelection();
        setCenter(emptyState());
        openLesson.set(null);
        updateSidebar();
    }

    /** Đăng xuất: đóng bài (nếu đang mở), giải phóng tài nguyên và quay về màn hình đăng nhập. */
    public void logout() {
        if (loggedOut) {
            return;
        }
        loggedOut = true;
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

    private static final String ORDER_AZ = "Sắp theo tên: A → Z";
    private static final String ORDER_CHUONG = "Sắp theo chương";

    /** Lọc theo ô tìm, sắp theo lựa chọn; bài đang mở mà vẫn còn trong danh sách thì giữ được chọn. */
    private void refresh() {
        LessonFilter.Order o =
                ORDER_CHUONG.equals(order.getValue()) ? LessonFilter.Order.CHUONG : LessonFilter.Order.CHU_CAI;
        List<JsonNode> shown = LessonFilter.apply(all, search.getText(), o);
        String openId = openLesson.get();
        refreshing = true;
        try {
            lessons.getItems().setAll(shown);
            lessons.getSelectionModel().clearSelection();
            for (JsonNode n : shown) {
                if (n.path("id").asText().equals(openId)) {
                    lessons.getSelectionModel().select(n);
                }
            }
        } finally {
            refreshing = false;
        }
        count.setText(shown.size() == all.size() ? String.valueOf(all.size()) : shown.size() + "/" + all.size());
        lessons.setPlaceholder(new Label(all.isEmpty() ? "Đang tải danh sách bài…" : "Không có bài nào khớp"));
    }

    private void loadLessons() {
        LcpClient c = li.client();
        var reply = c.expect(MessageType.LESSON_DATA);
        c.send(MessageType.LESSON_LIST, new byte[0]);
        reply.orTimeout(10, TimeUnit.SECONDS).thenAccept(f -> {
            try {
                JsonNode list = JsonPayload.tree(f).path("lessons");
                Platform.runLater(() -> {
                    all.clear();
                    list.forEach(all::add);
                    refresh();
                });
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Không đọc được danh sách bài", "error"));
            }
        });
    }

    private void open(String lessonId) {
        LcpClient c = li.client();
        var reply = c.expect(MessageType.LESSON_DATA, MessageType.ERROR);
        c.send(MessageType.LESSON_FETCH, new PayloadWriter().str(lessonId).toBytes());
        setStatus("Đang mở bài…", null);
        reply.orTimeout(30, TimeUnit.SECONDS)
                .whenComplete((f, err) -> Platform.runLater(() -> {
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
}
