package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import labcast.client.editor.CodeEditor;
import labcast.client.journal.JournalClient;
import labcast.client.net.LcpClient;
import labcast.client.player.PlayerPane;
import labcast.client.player.View;
import labcast.client.practice.RunPanel;
import labcast.client.views.mang.ArrayView;
import labcast.common.trace.Trace;
import labcast.common.trace.TraceReader;

/**
 * Màn một bài học — chỉ LẮP GHÉP ba phần của ba người, không chứa nghiệp vụ:
 *
 * <pre>
 * Tên bài  [Giảng giải] [Python]
 * ┌ [Animation | Đề bài] ─────────┐ ┌ Code của em ─────────────────┐
 * │ PlayerPane (Người 4)          │ │ CodeEditor (Người 1)         │
 * │   hoặc đề bài markdown        │ ├──────────────────────────────┤
 * │                               │ │ RunPanel (Người 3)           │
 * └───────────────────────────────┘ └──────────────────────────────┘
 * </pre>
 */
public final class LessonScreen extends BorderPane {
    private final String lessonId;
    private final JsonNode lesson;
    private final PlayerPane player = new PlayerPane();
    private final CodeEditor editor = new CodeEditor();
    private final Label traceSource = new Label();
    private final TabPane tabs = new TabPane();
    private final Tab animTab;
    private final RunPanel runPanel;

    /** {@code lesson}: payload {@code LESSON_DATA} loại "lesson". */
    public LessonScreen(LcpClient client, JournalClient journal, JsonNode lesson) {
        this.lesson = lesson;
        this.lessonId = lesson.path("id").asText();
        getStyleClass().add("lesson-screen");

        // Đầu màn: tên bài và nhãn loại bài
        Label title = new Label(lesson.path("ten").asText());
        title.getStyleClass().add("h1");
        String loai = lesson.path("loai").asText();
        HBox tags = new HBox(6, badge(loaiLabel(loai), "badge-" + loai), badge("Python", null));
        tags.setAlignment(Pos.CENTER_LEFT);
        HBox header = new HBox(12, title, tags);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("lesson-header");
        setTop(header);

        // Trái: hai thẻ Animation và Đề bài
        traceSource.setId("trace-source");
        traceSource.getStyleClass().add("source-chip");
        Button demoBtn = new Button("↺ Xem lời giải mẫu");
        demoBtn.getStyleClass().add("ghost");
        demoBtn.setOnAction(e -> showDemo());
        demoBtn.setDisable(lesson.path("trace").size() == 0);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox playerBar = new HBox(8, traceSource, gap, demoBtn);
        playerBar.setAlignment(Pos.CENTER_LEFT);
        playerBar.getStyleClass().add("player-bar");
        VBox anim = new VBox(playerBar, player);
        VBox.setVgrow(player, Priority.ALWAYS);

        ScrollPane de = new ScrollPane(MarkdownView.render(lesson.path("de").asText(), true));
        de.setFitToWidth(true);
        de.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        animTab = new Tab("Animation", anim);
        Tab deTab = new Tab("Đề bài", de);
        tabs.getTabs().addAll(animTab, deTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("lesson-tabs");
        // Bài không có animation giảng giải thì mở thẳng đề bài
        tabs.getSelectionModel().select(lesson.path("trace").size() == 0 ? deTab : animTab);

        // Phải: ô soạn code và khung chạy thử
        Label edTitle = new Label("Code của em");
        edTitle.getStyleClass().add("card-title");
        Label saved = new Label("Tự động lưu khi gõ");
        saved.getStyleClass().add("muted");
        Region gap2 = new Region();
        HBox.setHgrow(gap2, Priority.ALWAYS);
        HBox edHeader = new HBox(8, edTitle, gap2, saved);
        edHeader.setAlignment(Pos.CENTER_LEFT);
        edHeader.getStyleClass().add("card-header");
        VBox edCard = new VBox(edHeader, editor.node());
        VBox.setVgrow(editor.node(), Priority.ALWAYS);
        edCard.getStyleClass().addAll("card", "editor-card");

        editor.load(
                lessonId, lesson.path("code").asText(), lesson.path("codeSeq").asLong(), journal::submit);
        client.setResumeLesson(() -> lessonId);

        List<String> testIds = new ArrayList<>();
        lesson.path("testXem").forEach(t -> testIds.add(t.path("id").asText()));
        runPanel = new RunPanel(client, lessonId, testIds, editor::flush, this::showStudentTrace);
        runPanel.getStyleClass().add("card");

        VBox right = new VBox(12, edCard, runPanel);
        VBox.setVgrow(edCard, Priority.ALWAYS);

        SplitPane split = new SplitPane(tabs, right);
        split.getStyleClass().add("lesson-split");
        split.setDividerPositions(0.52);
        setCenter(split);

        showDemo();
    }

    public String lessonId() {
        return lessonId;
    }

    public void dispose() {
        editor.flush();
        runPanel.dispose();
    }

    private static Label badge(String text, String variant) {
        Label l = new Label(text);
        l.getStyleClass().add("badge");
        if (variant != null) {
            l.getStyleClass().add(variant);
        }
        return l;
    }

    static String loaiLabel(String loai) {
        return switch (loai) {
            case "giang-giai" -> "Giảng giải";
            case "bai-tap" -> "Bài tập";
            case "luyen-tap" -> "Luyện tập";
            default -> loai;
        };
    }

    private void setSource(String text, String variant) {
        traceSource.setText(text);
        traceSource.getStyleClass().removeAll("mine", "none");
        if (variant != null) {
            traceSource.getStyleClass().add(variant);
        }
    }

    private void showDemo() {
        List<String> lines = new ArrayList<>();
        lesson.path("trace").forEach(l -> lines.add(l.asText()));
        if (lines.isEmpty()) {
            setSource("Bài này không có animation giảng giải. Bấm Chạy thử để xem code của em chạy.", "none");
            return;
        }
        try {
            player.load(TraceReader.append(TraceReader.empty(), lines), views());
            setSource("Đang xem: lời giải mẫu", null);
        } catch (IOException e) {
            setSource("Trace giảng giải hỏng: " + e.getMessage(), "none");
        }
    }

    private void showStudentTrace(Trace t) {
        tabs.getSelectionModel().select(animTab);
        if (t.size() == 0) {
            setSource("Lượt chạy không có trace (code chưa dùng cấu trúc viz?)", "none");
            return;
        }
        player.load(t, views());
        setSource("Đang xem: code của em", "mine");
    }

    /** Một bộ vẽ cho mỗi biến khai báo trong khung_nhin. Bộ vẽ chưa có thì bỏ qua. */
    private Map<String, View> views() {
        Map<String, View> m = new LinkedHashMap<>();
        for (JsonNode v : lesson.path("khungNhin")) {
            String bien = v.path("bien").asText();
            String kieu = v.path("kieu").asText();
            if (kieu.equals("mang")) {
                m.put(bien, new ArrayView(bien));
            }
        }
        return m;
    }
}
