package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
 * ┌──────────────────────────────┬──────────────────────────────┐
 * │ PlayerPane (Người 4)          │ CodeEditor (Người 1)          │
 * ├──────────────────────────────┴──────────────────────────────┤
 * │ RunPanel (Người 3): nút Chạy thử, kết quả từng test           │
 * └─────────────────────────────────────────────────────────────┘
 * </pre>
 */
public final class LessonScreen extends BorderPane {
    private final String lessonId;
    private final JsonNode lesson;
    private final PlayerPane player = new PlayerPane();
    private final CodeEditor editor = new CodeEditor();
    private final Label traceSource = new Label();
    private final RunPanel runPanel;

    /** {@code lesson}: payload {@code LESSON_DATA} loại "lesson". */
    public LessonScreen(LcpClient client, JournalClient journal, JsonNode lesson) {
        this.lesson = lesson;
        this.lessonId = lesson.path("id").asText();

        Label title = new Label(lesson.path("ten").asText());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        TextArea de = new TextArea(lesson.path("de").asText());
        de.setEditable(false);
        de.setWrapText(true);
        de.setPrefRowCount(6);
        TitledPane dePane = new TitledPane("Đề bài", de);
        dePane.setExpanded(true);
        VBox top = new VBox(6, title, dePane);
        top.setPadding(new Insets(8));
        setTop(top);

        Button demoBtn = new Button("Animation giảng giải");
        demoBtn.setOnAction(e -> showDemo());
        demoBtn.setDisable(lesson.path("trace").size() == 0);
        HBox playerBar = new HBox(8, demoBtn, traceSource);
        playerBar.setAlignment(Pos.CENTER_LEFT);
        playerBar.setPadding(new Insets(6));
        VBox left = new VBox(playerBar, player);
        VBox.setVgrow(player, Priority.ALWAYS);
        SplitPane split = new SplitPane(left, editor.node());
        split.setDividerPositions(0.55);
        setCenter(split);

        editor.load(
                lessonId, lesson.path("code").asText(), lesson.path("codeSeq").asLong(), journal::submit);
        client.setResumeLesson(() -> lessonId);

        List<String> testIds = new ArrayList<>();
        lesson.path("testXem").forEach(t -> testIds.add(t.path("id").asText()));
        runPanel = new RunPanel(client, lessonId, testIds, editor::flush, this::showStudentTrace);
        setBottom(runPanel);

        showDemo();
    }

    public String lessonId() {
        return lessonId;
    }

    public void dispose() {
        editor.flush();
        runPanel.dispose();
    }

    private void showDemo() {
        List<String> lines = new ArrayList<>();
        lesson.path("trace").forEach(l -> lines.add(l.asText()));
        if (lines.isEmpty()) {
            traceSource.setText("Bài này không có animation giảng giải. Bấm Chạy thử để xem code của em chạy.");
            return;
        }
        try {
            player.load(TraceReader.append(TraceReader.empty(), lines), views());
            traceSource.setText("Đang xem: lời giải mẫu");
        } catch (IOException e) {
            traceSource.setText("Trace giảng giải hỏng: " + e.getMessage());
        }
    }

    private void showStudentTrace(Trace t) {
        if (t.size() == 0) {
            traceSource.setText("Lượt chạy không có trace (code chưa dùng cấu trúc viz?)");
            return;
        }
        player.load(t, views());
        traceSource.setText("Đang xem: code của em");
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
