package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import labcast.client.cast.CastSession;
import labcast.client.editor.CodeEditor;
import labcast.client.journal.JournalClient;
import labcast.client.net.LcpClient;
import labcast.client.player.Player;
import labcast.client.player.PlayerControls;
import labcast.client.player.PlayerPane;
import labcast.client.player.View;
import labcast.client.practice.RunPanel;
import labcast.client.presenter.PresenterController;
import labcast.client.views.Views;
import labcast.common.protocol.Language;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;
import labcast.common.trace.Trace;
import labcast.common.trace.TraceReader;

/**
 * Màn một bài học — chỉ LẮP GHÉP ba phần của ba người, không chứa nghiệp vụ:
 *
 * <pre>
 * Breadcrumb / Tên bài / Nút Chiếu cho lớp / Đóng bài
 * ┌ Code Editor + Chạy thử ───────┐ ┌ [Animation | Đề bài] ─────────┐
 * │ Mã nguồn và chọn ngôn ngữ     │ │ PlayerPane hoặc Markdown     │
 * │ RunPanel (Python / SQL)       │ │ Hai tab trong cùng một khung │
 * └───────────────────────────────┘ └───────────────────────────────┘
 * </pre>
 */
public final class LessonScreen extends BorderPane {
    private static final String SAVED_HINT = "Tự động lưu khi gõ";

    private final String lessonId;
    private final JsonNode lesson;
    private final PlayerPane player = new PlayerPane();
    /** Bài SQL: animation vẽ các bước của câu lệnh, không có Trace Table. */
    private final boolean sql;

    private final CodeEditor editor = new CodeEditor();
    private final Label traceSource = new Label();
    private final TabPane tabs = new TabPane();
    private final Tab animTab;
    private final TraceTablePane traceTable = new TraceTablePane();
    private final Tab traceTab = new Tab("Trace Table", traceTable);
    private final RunPanel runPanel;
    private VBox edCard;
    private VBox codeColumn;
    private FocusLayer focus;
    private DiscussionPane discussion;
    private final SplitPane split = new SplitPane();
    private final Button talkButton = new Button("Thảo luận");
    /** Ngôn ngữ đã chọn ở mỗi bài trong phiên làm việc: mở lại bài thì về đúng ngôn ngữ đang viết. */
    private static final Map<String, Language> LAST_LANGUAGE = new ConcurrentHashMap<>();

    private final LcpClient client;
    private final JournalClient journal;
    /** Ngôn ngữ của code trong ô soạn; bài SQL luôn là PYTHON (không dùng). */
    private Language language = Language.PYTHON;
    /** Các ngôn ngữ bài cho chọn, cùng thứ tự với các mục của {@code languagePicker}. */
    private final List<Language> choices = new ArrayList<>();

    private final ComboBox<String> languagePicker = new ComboBox<>();
    private Label saved;
    private Label fileName;
    /** Chiếu cho cả lớp (chỉ giáo viên, khi app có phần nhận chiếu); null nếu không chiếu được. */
    private final CastSession cast;

    private final PresenterController presenter;
    private final Button castBtn = new Button(CAST_START);
    private final Button demoBtn = new Button("↺ Xem lời giải mẫu");

    private static final String CAST_START = "📡 Chiếu cho lớp";
    private static final String CAST_STOP = "■ Dừng chiếu";

    /**
     * @param lesson payload {@code LESSON_DATA} loại "lesson"
     * @param studentMode học viên làm bài: chỉ dán được đoạn copy trong ô code (giáo viên thì không khoá)
     * @param onClose nút "Đóng bài"
     */
    public LessonScreen(
            LcpClient client, JournalClient journal, JsonNode lesson, boolean studentMode, Runnable onClose) {
        this(client, journal, lesson, studentMode, onClose, null);
    }

    /** @param cast phiên chiếu của app; giáo viên có thêm nút "Chiếu cho lớp" khi khác null */
    public LessonScreen(
            LcpClient client,
            JournalClient journal,
            JsonNode lesson,
            boolean studentMode,
            Runnable onClose,
            CastSession cast) {
        this.cast = studentMode ? null : cast;
        this.client = client;
        this.journal = journal;
        this.presenter = new PresenterController(r -> client.send(MessageType.CAST_CONTROL, r.encode()));
        this.lesson = lesson;
        this.lessonId = lesson.path("id").asText();
        getStyleClass().add("lesson-screen");

        // Breadcrumb: dung ten chuong tu server; neu payload LESSON_DATA thieu "chuong",
        // doc tu lessonId (vi du dsa.bam.day-chuyen -> "Băm").
        sql = lesson.path("mon").asText().equals("sql") || lessonId.startsWith("sql.");
        String monLabel = sql ? "SQL" : "DSA";
        String chapterKey = lesson.path("chuong").asText("").trim();
        if (chapterKey.isBlank()) {
            String[] parts = lessonId.split("\\.");
            chapterKey = parts.length > 1 ? parts[1] : "";
        }
        chapterKey =
                chapterKey.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        String chapter = chapterLabel(chapterKey);
        Label breadcrumb = new Label(monLabel + "   ›   " + chapter);
        breadcrumb.getStyleClass().add("lesson-breadcrumb");

        Label title = new Label(lesson.path("ten").asText());
        title.getStyleClass().addAll("h1", "lesson-main-title");
        title.setMinWidth(0);
        Marquee.install(title);
        Label typeBadge = badge(
                loaiLabel(lesson.path("loai").asText()),
                "badge-" + lesson.path("loai").asText());
        HBox tags = new HBox(6, typeBadge);
        tags.setAlignment(Pos.CENTER_LEFT);
        tags.setMinWidth(Region.USE_PREF_SIZE);
        Region headGap = new Region();
        HBox.setHgrow(headGap, Priority.ALWAYS);
        Button closeBtn = new Button("✕  Đóng bài");
        closeBtn.setId("close-lesson");
        closeBtn.getStyleClass().add("lesson-close-modern");
        closeBtn.setOnAction(e -> onClose.run());
        closeBtn.setMinWidth(Region.USE_PREF_SIZE);
        HBox headerLine = new HBox(10, title, tags, headGap, closeBtn);
        if (this.cast != null) {
            // GIU NGUYEN CHUC NANG CHIEU CHO LOP VA TRINH XU LY.
            castBtn.setId("cast-toggle");
            castBtn.getStyleClass().add("cast");
            castBtn.setMinWidth(Region.USE_PREF_SIZE);
            castBtn.setDisable(lesson.path("trace").size() == 0);
            castBtn.setOnAction(e -> {
                if (presenter.casting()) {
                    presenter.stop();
                    endCast();
                } else {
                    startCast();
                }
            });
            headerLine.getChildren().add(headerLine.getChildren().indexOf(closeBtn), castBtn);
        }
        headerLine.setAlignment(Pos.CENTER_LEFT);
        VBox header = new VBox(2, breadcrumb, headerLine);
        header.getStyleClass().addAll("lesson-header", "lesson-header-modern");
        setTop(header);

        // Phải: hai thẻ Animation và Đề bài
        traceSource.setId("trace-source");
        traceSource.getStyleClass().add("source-chip");
        demoBtn.getStyleClass().add("ghost");
        demoBtn.setOnAction(e -> showDemo());
        demoBtn.setDisable(lesson.path("trace").size() == 0);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        demoBtn.setMinWidth(Region.USE_PREF_SIZE);
        HBox playerBar = new HBox(8, traceSource, gap, demoBtn);
        playerBar.setAlignment(Pos.CENTER_LEFT);
        playerBar.getStyleClass().add("player-bar");
        VBox anim = new VBox(playerBar, player);
        // cho khung trái co hẹp được: chú thích màu tự xuống dòng thay vì bị cắt mất chữ
        anim.setMinWidth(0);
        player.setMinWidth(0);
        tabs.setMinWidth(0);
        VBox.setVgrow(player, Priority.ALWAYS);

        ScrollPane de = new ScrollPane(MarkdownView.render(lesson.path("de").asText(), true));
        de.setFitToWidth(true);
        de.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        animTab = new Tab("Animation", anim);
        Tab deTab = new Tab("Đề bài", de);
        tabs.getTabs().addAll(animTab, deTab, traceTab);
        // Thảo luận của lớp ở bài này (cả DSA và SQL); số bình luận hiện trên tên thẻ
        // Thảo luận: khung trượt bên phải, mở/đóng bằng nút 💬 trên tiêu đề; nút đếm bình luận chưa đọc
        Label unreadBadge = new Label();
        unreadBadge.getStyleClass().add("discuss-badge");
        unreadBadge.setVisible(false);
        unreadBadge.setMouseTransparent(true);
        discussion = new DiscussionPane(client, lessonId, !studentMode, () -> toggleDiscussion(false), n -> {
            unreadBadge.setText(n > 99 ? "99+" : Integer.toString(n));
            unreadBadge.setVisible(n > 0);
        });
        talkButton.getStyleClass().add("discuss-toggle");
        talkButton.setGraphic(DiscussionPane.icon(DiscussionPane.CHAT, "discuss-icon"));
        talkButton.setTooltip(new Tooltip("Mở / đóng thảo luận của lớp ở bài này"));
        talkButton.setOnAction(e -> toggleDiscussion(!split.getItems().contains(discussion)));
        javafx.scene.layout.StackPane talk = new javafx.scene.layout.StackPane(talkButton, unreadBadge);
        javafx.scene.layout.StackPane.setAlignment(unreadBadge, Pos.TOP_RIGHT);
        talk.setMinWidth(Region.USE_PREF_SIZE);
        headerLine.getChildren().add(headerLine.getChildren().indexOf(closeBtn), talk);
        if (lesson.path("loiGiai").size() > 0) {
            tabs.getTabs().add(new Tab("Lời giải", solutionPane(lesson.path("loiGiai"))));
        }
        traceTab.setDisable(sql);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("lesson-tabs");
        // Bài không có animation giảng giải thì mở thẳng đề bài
        tabs.getSelectionModel().select(lesson.path("trace").size() == 0 ? deTab : animTab);

        // Code ben trai trong vung hoc, sidebar van nam ben trai cua toan app.
        Label edTitle = new Label(sql ? "Câu SQL của em" : "Code của em");
        edTitle.getStyleClass().add("card-title");
        saved = new Label(SAVED_HINT);
        saved.getStyleClass().add("muted");
        fileName = new Label(sql ? "query.sql" : Language.PYTHON.fileName());
        fileName.getStyleClass().add("editor-file-name");
        Region gap2 = new Region();
        HBox.setHgrow(gap2, Priority.ALWAYS);
        languagePicker.getStyleClass().add("language-picker");
        languagePicker.setId("editor-language");
        languagePicker.setFocusTraversable(false);
        languagePicker.setMinWidth(Region.USE_PREF_SIZE);
        if (sql) {
            languagePicker.getItems().add("SQL · SQLite");
            languagePicker.getSelectionModel().selectFirst();
            languagePicker.setDisable(true);
        } else {
            for (JsonNode n : lesson.path("ngonNgu")) {
                Language l = Language.of(n.asText());
                if (l != null) {
                    choices.add(l);
                }
            }
            if (!choices.contains(Language.PYTHON)) {
                choices.add(0, Language.PYTHON); // server cũ không gửi ngonNgu; code mở sẵn là Python
            }
            choices.forEach(l -> languagePicker.getItems().add(l.label()));
            languagePicker.getSelectionModel().select(choices.indexOf(Language.PYTHON));
            languagePicker.setDisable(choices.size() < 2);
            languagePicker.setTooltip(new Tooltip(
                    "Viết bài bằng ngôn ngữ khác: mỗi ngôn ngữ một bản code riêng. Animation chỉ có với Python"));
        }
        Button expandEditor = new Button("⛶");
        expandEditor.getStyleClass().add("editor-expand-button");
        expandEditor.setTooltip(new Tooltip("Mở rộng / khôi phục Code Editor"));
        // khung hẹp: các nhãn chạy chữ thay vì cắt "…"
        for (Label l : new Label[] {edTitle, saved, fileName}) {
            l.setMinWidth(40);
            Marquee.install(l);
        }
        HBox edHeader = new HBox(8, edTitle, gap2, saved, fileName, languagePicker, expandEditor);
        edHeader.setAlignment(Pos.CENTER_LEFT);
        edHeader.getStyleClass().add("card-header");
        edCard = new VBox(edHeader, editor.node());
        VBox.setVgrow(editor.node(), Priority.ALWAYS);
        edCard.getStyleClass().addAll("card", "editor-card");
        edCard.setMinWidth(0);

        editor.setPasteGuard(studentMode);
        editor.setSqlMode(sql);
        PauseTransition clearNotice = new PauseTransition(javafx.util.Duration.seconds(4));
        clearNotice.setOnFinished(e -> {
            saved.setText(SAVED_HINT);
            saved.getStyleClass().remove("notice");
        });
        editor.onBlockedPaste(() -> {
            saved.setText("Không dán được chữ copy từ ngoài ô code");
            if (!saved.getStyleClass().contains("notice")) {
                saved.getStyleClass().add("notice");
            }
            clearNotice.playFromStart();
        });
        editor.load(
                lessonId, lesson.path("code").asText(), lesson.path("codeSeq").asLong(), journal::submit);
        client.setResumeLesson(() -> lessonId);

        List<String> testIds = new ArrayList<>();
        lesson.path("testXem").forEach(t -> testIds.add(t.path("id").asText()));
        runPanel = new RunPanel(client, lessonId, testIds, editor::flush, this::showStudentTrace);
        runPanel.setOnErrorLine(editor::highlightTraceLine);
        runPanel.getStyleClass().add("card");
        runPanel.enableLineTrace(!sql);

        codeColumn = new VBox(10, edCard, runPanel);
        VBox.setVgrow(edCard, Priority.ALWAYS);
        codeColumn.setMinWidth(0);
        // Đề bài / Animation / Trace bên trái, ô code ở giữa, thảo luận (khi mở) bên phải
        split.getItems().setAll(tabs, codeColumn);
        split.getStyleClass().add("lesson-split");
        split.setDividerPositions(0.42);
        setCenter(split);
        // ⛶ hoặc bấm vào ô code: chế độ tập trung (đề + code nổi lên 90% màn, phần còn lại mờ)
        expandEditor.setTooltip(new Tooltip("Chế độ tập trung: chỉ đề bài và ô code"));
        expandEditor.setOnAction(e -> enterFocus());
        editor.node().addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (focus == null || !focus.isOpen()) Platform.runLater(this::enterFocus);
        });

        // Đổi ngôn ngữ: nạp bản code đã lưu của ngôn ngữ đó (CODE_FETCH), từ đó gõ là lưu lên server như Python
        if (!sql) {
            languagePicker.getSelectionModel().selectedIndexProperty().addListener((obs, old, idx) -> {
                int i = idx.intValue();
                if (i >= 0 && i < choices.size() && choices.get(i) != language) {
                    switchLanguage(choices.get(i));
                }
            });
            Language last = LAST_LANGUAGE.get(lessonId);
            if (last != null && last != Language.PYTHON && choices.contains(last)) {
                languagePicker.getSelectionModel().select(choices.indexOf(last));
            }
        }

        showDemo();
    }

    /**
     * Chế độ tập trung: đề bài bên trái, ô code bên phải, nổi lên giữa màn chiếm 90%, nền mờ. "Chạy thử" thì trở
     * lại giao diện thường rồi chạy; Esc hoặc bấm ra ngoài thì chỉ trở lại.
     */
    private void enterFocus() {
        if (focus != null && focus.isOpen() || getScene() == null) return;
        Region placeholder = new Region();
        placeholder.setPrefHeight(edCard.getHeight());
        VBox.setVgrow(placeholder, Priority.ALWAYS);
        int at = codeColumn.getChildren().indexOf(edCard);
        codeColumn.getChildren().set(at, placeholder);

        Label title = new Label(lesson.path("ten").asText());
        title.getStyleClass().add("focus-title");
        Label hint = new Label("Chế độ tập trung · Esc để thoát");
        hint.getStyleClass().add("author-hint");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Button exit = new Button("Thoát");
        exit.getStyleClass().add("dash-ghost");
        Button run = new Button("▶  Chạy thử");
        run.getStyleClass().add("dash-primary");
        HBox head = new HBox(10, new VBox(2, title, hint), gap, exit, run);
        head.setAlignment(Pos.CENTER_LEFT);

        ScrollPane de = new ScrollPane(MarkdownView.render(lesson.path("de").asText(), true));
        de.setFitToWidth(true);
        de.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        de.getStyleClass().add("focus-statement");
        SplitPane body = new SplitPane(de, edCard);
        body.setDividerPositions(0.38);
        body.getStyleClass().add("focus-split");
        VBox box = new VBox(12, head, body);
        VBox.setVgrow(body, Priority.ALWAYS);
        box.getStyleClass().add("focus-panel");

        boolean[] runAfter = {false};
        focus = FocusLayer.open(this, box, 0.9, () -> {
            codeColumn.getChildren().set(codeColumn.getChildren().indexOf(placeholder), edCard);
            VBox.setVgrow(edCard, Priority.ALWAYS);
            if (runAfter[0]) runPanel.runCode();
        });
        exit.setOnAction(e -> focus.close());
        run.setOnAction(e -> {
            runAfter[0] = true;
            focus.close();
        });
        Platform.runLater(editor::focus);
    }

    private void toggleDiscussion(boolean show) {
        boolean shown = split.getItems().contains(discussion);
        if (show == shown) return;
        if (show) {
            split.getItems().add(discussion);
            split.setDividerPositions(0.28, 0.72);
        } else {
            split.getItems().remove(discussion);
            split.setDividerPositions(0.42);
        }
        discussion.setOpen(show);
        talkButton.getStyleClass().remove("discuss-toggle-on");
        if (show) talkButton.getStyleClass().add("discuss-toggle-on");
    }

    /** Ngôn ngữ của code đang soạn. */
    public Language language() {
        return language;
    }

    private void switchLanguage(Language l) {
        editor.flush();
        language = l;
        LAST_LANGUAGE.put(lessonId, l);
        runPanel.setLanguage(l);
        fileName.setText(l.fileName());
        editor.setEditable(false);
        saved.setText("Đang mở code " + l.label() + "…");
        String key = Language.docKey(lessonId, l);
        // CODE_DATA(str docKey, i64 seq, str code)
        client.expect(MessageType.CODE_DATA).thenAccept(f -> {
            String k;
            long seq;
            String code;
            try {
                PayloadReader r = f.reader();
                k = r.str();
                seq = r.i64();
                code = r.str();
            } catch (IOException e) {
                return;
            }
            Platform.runLater(() -> {
                if (language != l || !k.equals(key)) {
                    return; // đã đổi tiếp sang ngôn ngữ khác trong lúc chờ
                }
                editor.setSyntaxLanguage(l.code());
                editor.load(key, code, seq, journal::submit);
                editor.setEditable(true);
                saved.setText(SAVED_HINT);
                if (l != Language.PYTHON) {
                    setSource(
                            "Animation chỉ có với Python — code " + l.label() + " xem từng dòng ở Trace Table", "none");
                }
            });
        });
        client.send(MessageType.CODE_FETCH, new PayloadWriter().str(key).toBytes());
    }

    /** Tab "Lời giải": lời giải mẫu C/C++/Java của bài giảng giải, chỉ đọc, chọn được ngôn ngữ. */
    private static Node solutionPane(JsonNode loiGiai) {
        CodeEditor view = new CodeEditor();
        view.setEditable(false);
        List<Language> ds = new ArrayList<>();
        ComboBox<String> chon = new ComboBox<>();
        chon.setId("solution-language");
        loiGiai.fieldNames().forEachRemaining(k -> {
            Language l = Language.of(k);
            if (l != null) {
                ds.add(l);
                chon.getItems().add(l.label());
            }
        });
        chon.getSelectionModel().selectedIndexProperty().addListener((obs, old, idx) -> {
            int i = idx.intValue();
            if (i >= 0) {
                Language l = ds.get(i);
                view.setSyntaxLanguage(l.code());
                view.load("", loiGiai.path(l.code()).asText(), 0, d -> {});
            }
        });
        chon.getSelectionModel().selectFirst();
        Label note = new Label("Lời giải mẫu để đọc. Animation chạy theo bản Python.");
        note.getStyleClass().add("muted");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox bar = new HBox(8, note, gap, chon);
        bar.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(8, bar, view.node());
        VBox.setVgrow(view.node(), Priority.ALWAYS);
        box.getStyleClass().add("solution-pane");
        return box;
    }

    public String lessonId() {
        return lessonId;
    }

    public void dispose() {
        discussion.dispose();
        editor.flush();
        runPanel.dispose();
        if (presenter.casting()) {
            presenter.stop();
            endCast();
        }
    }

    /**
     * Giáo viên bấm "Chiếu cho lớp": server phát trace giảng giải của bài cho cả lớp. Trình phát trên máy
     * giáo viên chuyển sang theo buổi chiếu (nhận bản sao qua TCP), các nút của nó gửi lệnh cho server.
     */
    private void startCast() {
        presenter.start(lessonId);
        player.setControls(presenter);
        castBtn.setText(CAST_STOP);
        demoBtn.setDisable(true);
        setSource("📡 Đang gửi bài cho cả lớp…", "casting");
        cast.attach(castScreen);
    }

    private void endCast() {
        cast.detachScreen();
        player.setControls(PlayerControls.LOCAL);
        castBtn.setText(CAST_START);
        demoBtn.setDisable(lesson.path("trace").size() == 0);
        showDemo();
    }

    private final CastSession.Screen castScreen = new CastSession.Screen() {
        @Override
        public Player show(String traceId, Trace trace) {
            Player p = player.load(trace, Views.create(trace.header().path("views"), "v"));
            setSource("📡 Đang chiếu cho cả lớp", "casting");
            return p;
        }

        @Override
        public void stopped() {
            if (presenter.casting()) {
                presenter.stop(); // lệnh dừng đến từ chỗ khác (máy giáo viên khác)
            }
            endCast();
        }
    };

    private static Label badge(String text, String variant) {
        Label l = new Label(text);
        l.getStyleClass().add("badge");
        if (variant != null) {
            l.getStyleClass().add(variant);
        }
        return l;
    }

    /** Thứ tự chương trong danh sách bài: theo thứ tự học, không theo bảng chữ cái của mã chương. */
    static final List<String> CHAPTERS = List.of(
            "sap-xep",
            "tim-kiem",
            "dslk",
            "bam",
            "ngan-xep",
            "hang-doi",
            "de-quy",
            "quay-lui",
            "quy-hoach-dong",
            "cay",
            "do-thi",
            "co-ban",
            "gom-nhom",
            "null-case",
            "dml",
            "ket-noi",
            "truy-van-con",
            "tap-hop",
            "tong-hop");

    /** Tên chương hiện cho học viên, từ mã chương trong {@code lesson.yaml}. */
    static String chapterLabel(String chapterKey) {
        return switch (chapterKey) {
            case "bam" -> "Băm";
            case "cay" -> "Cây";
            case "de-quy" -> "Đệ quy";
            case "do-thi" -> "Đồ thị";
            case "dslk" -> "Danh sách liên kết";
            case "hang-doi" -> "Hàng đợi";
            case "ngan-xep" -> "Ngăn xếp";
            case "sap-xep" -> "Sắp xếp";
            case "tim-kiem" -> "Tìm kiếm";
            case "quay-lui" -> "Quay lui";
            case "quy-hoach-dong" -> "Quy hoạch động";
            case "co-ban" -> "Cơ bản";
            case "dml" -> "Thao tác dữ liệu";
            case "ket-noi" -> "Kết nối";
            case "gom-nhom" -> "Gom nhóm";
            case "truy-van-con" -> "Truy vấn con";
            case "tap-hop" -> "Tập hợp";
            case "null-case" -> "NULL và CASE";
            case "tong-hop" -> "Tổng hợp";
            default -> chapterKey.isBlank() ? "Bài học" : chapterKey.replace('-', ' ');
        };
    }

    /** Giảng giải trước, rồi bài tập, rồi luyện tập. */
    static int loaiRank(String loai) {
        return switch (loai) {
            case "giang-giai" -> 0;
            case "bai-tap" -> 1;
            case "luyen-tap" -> 2;
            default -> 3;
        };
    }

    static String loaiLabel(String loai) {
        return switch (loai) {
            case "giang-giai" -> "Bài mẫu";
            case "bai-tap" -> "Bài tập";
            case "luyen-tap" -> "Luyện tập";
            default -> loai;
        };
    }

    private void setSource(String text, String variant) {
        traceSource.setText(text);
        traceSource.getStyleClass().removeAll("mine", "none", "casting");
        if (variant != null) {
            traceSource.getStyleClass().add(variant);
        }
    }

    private void showDemo() {
        List<String> lines = new ArrayList<>();
        lesson.path("trace").forEach(l -> lines.add(l.asText()));
        if (lines.isEmpty()) {
            traceTable.clear();
            setSource("Bài này không có animation giảng giải. Bấm Chạy thử để xem code của em chạy.", "none");
            return;
        }
        try {
            player.load(TraceReader.append(TraceReader.empty(), lines), views());
            traceTable.clear();
            setSource("Đang xem: lời giải mẫu", null);
        } catch (IOException e) {
            setSource("Trace giảng giải hỏng: " + e.getMessage(), "none");
        }
    }

    private void showStudentTrace(Trace t) {
        if (presenter.casting()) {
            presenter.stop(); // giáo viên chạy thử code: trình phát chuyển sang trace đó, thôi chiếu
            endCast();
        }
        if (language != Language.PYTHON) {
            // C/C++/Java không có animation: trace (nếu có) chỉ là chạy từng dòng
            if (t.size() == 0) {
                setSource(
                        "Animation chỉ có với Python. Bấm Trace từng dòng để xem code " + language.label()
                                + " chạy từng dòng",
                        "none");
                return;
            }
            Player p = player.load(t, Map.of());
            traceTable.load(
                    t,
                    t.header().path("source").asText(editor.text()),
                    runPanel.lastRunStdout(),
                    p,
                    editor::highlightTraceLine);
            tabs.getSelectionModel().select(traceTab);
            setSource("Animation chỉ có với Python — code " + language.label() + " xem ở Trace Table", "none");
            return;
        }
        if (t.size() == 0) {
            // Bỏ trace đang hiện (thường là lời giải mẫu): để lại thì bấm Phát lại chạy lời giải, như thể là code
            // của học viên
            player.load(TraceReader.empty(), views());
            traceTable.clear();
            setSource(sql ? "Câu SQL lỗi: chưa có animation" : "Lượt chạy không có animation", "none");
            return;
        }
        Player currentPlayer = player.load(t, views());
        traceTable.load(
                t,
                t.header().path("source").asText(editor.text()),
                runPanel.lastRunStdout(),
                currentPlayer,
                editor::highlightTraceLine);
        tabs.getSelectionModel().select(runPanel.lastRunLineTrace() && traceTable.hasSteps() ? traceTab : animTab);
        setSource("Đang xem: code của em", "mine");
    }

    /** Một bộ vẽ cho mỗi biến khai báo trong khung_nhin. Bộ vẽ chưa có thì bỏ qua. */
    private Map<String, View> views() {
        return Views.create(lesson.path("khungNhin"), "bien");
    }
}
