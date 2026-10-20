package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import labcast.client.editor.CodeEditor;
import labcast.client.net.LcpClient;
import labcast.client.player.Player;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.trace.Trace;
import labcast.common.trace.TraceReader;

/** Không gắn lessonId, không có Nộp bài, không đánh dấu tiến độ. Chạy Python tùy ý với Trace Table. */
public final class PlaygroundScreen extends BorderPane {
    private static final String DEFAULT_CODE = ""; // Mở editor trống lần đầu; nháp cũ vẫn được lưu.
    private static final Path WORK_DIR = Path.of(System.getProperty("user.home"), ".labcast");
    // Mỗi ngôn ngữ có file bản nháp riêng; giữ tên playground.py để tương thích dữ liệu cũ.
    private static final List<String> LANGUAGES =
            List.of("Python 3.12", "Java 17 · nháp", "C++17 · nháp", "JavaScript · nháp");
    private static final Path INPUT_FILE = WORK_DIR.resolve("playground-stdin.txt");
    private final LcpClient client;
    private final CodeEditor editor = new CodeEditor();
    private final ComboBox<String> languagePicker = new ComboBox<>();
    private final Label codeTitle = new Label("main.py");
    private final Label draftNotice =
            new Label("Ngôn ngữ này mới hỗ trợ soạn thảo và lưu nháp; Run/Trace hiện chỉ hỗ trợ Python.");
    private final Map<String, String> drafts = new HashMap<>();
    private final Map<String, String> persistedDrafts = new HashMap<>();
    private String currentLanguage = "python";
    private final TextArea input = new TextArea();
    private final TextArea output = new TextArea();
    private final TraceTablePane traceTable = new TraceTablePane();
    private final TabPane rightTabs = new TabPane();
    private final Tab traceTab = new Tab("Trace Table", traceTable);
    private final Button run = new Button("▶ Chạy code");
    private final Button trace = new Button("☷ Trace từng dòng");
    private final Label status = new Label("Sẵn sàng");
    private final List<String> lines = new ArrayList<>();
    private final Timeline ticker;
    private final Timeline autosave;
    private Player player;
    private volatile boolean inFlight;
    private volatile long runId = -1;
    private String submittedCode = "";
    private String savedInput = "";

    private final Consumer<Frame> queued = this::queued;
    private final Consumer<Frame> chunk = this::chunk;
    private final Consumer<Frame> result = this::result;
    private final Consumer<Frame> error = this::error;

    public PlaygroundScreen(LcpClient client) {
        this.client = client;
        getStyleClass().add("playground-screen");

        Label title = new Label("Code tự do");
        title.getStyleClass().addAll("h1", "playground-title");
        Label sub = new Label("Viết code riêng • lưu từng ngôn ngữ • không chấm điểm bài học");
        sub.getStyleClass().add("muted");
        VBox top = new VBox(3, title, sub);
        top.getStyleClass().add("playground-heading");
        setTop(top);

        codeTitle.getStyleClass().add("card-title");
        languagePicker.getItems().addAll(LANGUAGES);
        languagePicker.getSelectionModel().selectFirst();
        languagePicker.getStyleClass().add("language-picker");
        languagePicker.setId("playground-language-picker");
        languagePicker.setMinWidth(Region.USE_PREF_SIZE);
        languagePicker.setMaxWidth(Region.USE_PREF_SIZE);
        languagePicker.setTooltip(new Tooltip("Chọn ngôn ngữ soạn thảo"));
        languagePicker.valueProperty().addListener((obs, previous, next) -> {
            if (next != null) switchLanguage(languageFor(next));
        });
        draftNotice.getStyleClass().add("muted");
        draftNotice.setVisible(false);
        draftNotice.setManaged(false);
        status.getStyleClass().add("muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(8, run, trace);
        buttons.setAlignment(Pos.CENTER_LEFT);
        run.getStyleClass().add("success");
        trace.getStyleClass().add("trace-run-button");
        run.setOnAction(e -> execute(false));
        trace.setOnAction(e -> execute(true));
        HBox editHeader = new HBox(10, codeTitle, spacer, languagePicker, status);
        editHeader.setAlignment(Pos.CENTER_LEFT);
        editHeader.getStyleClass().add("card-header");

        editor.setPasteGuard(false);
        for (String language : List.of("python", "java", "cpp", "javascript")) {
            String saved = readDraft(codeFile(language), DEFAULT_CODE);
            drafts.put(language, saved);
            persistedDrafts.put(language, saved);
        }
        savedInput = readDraft(INPUT_FILE, "");
        editor.setSyntaxLanguage("python");
        editor.load("free-code-local-python", drafts.get("python"), 0, d -> {});

        input.setText(savedInput);
        input.setPromptText("Dữ liệu stdin, ví dụ: 5 10");
        input.setPrefRowCount(3);
        input.getStyleClass().add("console");
        Label inputLabel = new Label("Input (stdin)");
        inputLabel.getStyleClass().add("card-title");
        VBox codeCard = new VBox(8, editHeader, draftNotice, editor.node(), inputLabel, input, buttons);
        VBox.setVgrow(editor.node(), Priority.ALWAYS);
        codeCard.getStyleClass().addAll("card", "playground-editor-card");
        codeCard.setMinWidth(0);

        output.setEditable(false);
        output.setWrapText(true);
        output.setPromptText("Bấm Chạy code hoặc Trace từng dòng để xem kết quả");
        output.getStyleClass().add("console");
        Tab outputTab = new Tab("Kết quả / Console", output);
        outputTab.setClosable(false);
        traceTab.setClosable(false);
        // Code tự do chỉ có Console và Trace Table, tuyệt đối không tạo tab Đề bài.
        rightTabs.getTabs().setAll(outputTab, traceTab);
        rightTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        rightTabs.getStyleClass().add("lesson-tabs");

        SplitPane split = new SplitPane(codeCard, rightTabs);
        split.getStyleClass().add("lesson-split");
        split.setDividerPositions(0.56);
        setCenter(split);

        client.on(MessageType.PLAYGROUND_RUN_QUEUED, queued);
        client.on(MessageType.PLAYGROUND_TRACE_CHUNK, chunk);
        client.on(MessageType.PLAYGROUND_RUN_RESULT, result);
        client.on(MessageType.ERROR, error);
        // Phát lại sự kiện line/vars trong Player; cập nhật bảng và tô sáng dòng.
        ticker = new Timeline(new KeyFrame(javafx.util.Duration.millis(50), e -> {
            if (player != null) player.tick();
        }));
        ticker.setCycleCount(Timeline.INDEFINITE);
        ticker.play();
        autosave = new Timeline(new KeyFrame(javafx.util.Duration.seconds(5), e -> saveDraft()));
        autosave.setCycleCount(Timeline.INDEFINITE);
        autosave.play();
        // Stop autosave with this screen. Timeline references the pane while window is open.
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) saveDraft();
        });
    }

    private void execute(boolean lineTrace) {
        if (inFlight) return;
        if (!"python".equals(currentLanguage)) {
            status.setText("Chưa có Runner cho " + currentLanguage + ". Hãy chuyển sang Python.");
            return;
        }
        submittedCode = editor.text();
        if (submittedCode.isBlank()) {
            status.setText("Hãy nhập code Python trước khi chạy");
            return;
        }
        if (submittedCode.length() > 30_000 || input.getText().length() > 20_000) {
            status.setText("Code tối đa 30.000 ký tự; stdin tối đa 20.000 ký tự");
            return;
        }
        saveDraft();
        traceTable.clear();
        player = null;
        synchronized (lines) {
            lines.clear();
        }
        rightTabs
                .getSelectionModel()
                .select(lineTrace ? traceTab : rightTabs.getTabs().get(0));
        output.clear();
        runId = 0;
        setBusy(true);
        status.setText(lineTrace ? "Đang thu thập Trace..." : "Đang chạy Python...");
        client.send(JsonPayload.frame(
                MessageType.PLAYGROUND_RUN_REQ,
                Map.of("code", submittedCode, "input", input.getText(), "trace", lineTrace)));
    }

    private void queued(Frame f) {
        try {
            long id = f.reader().i64();
            if (inFlight && runId == 0) runId = id;
        } catch (IOException ignored) {
        }
    }

    private void chunk(Frame f) {
        try {
            PayloadReader r = f.reader();
            long id = r.i64();
            r.i32();
            r.bool();
            String text = new String(r.blob(), StandardCharsets.UTF_8);
            if (inFlight && runId == id && !text.isBlank()) {
                synchronized (lines) {
                    lines.addAll(Arrays.asList(text.split("\\n")));
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void result(Frame f) {
        JsonNode response;
        try {
            response = JsonPayload.tree(f);
        } catch (IOException ignored) {
            return;
        }
        if (!inFlight || response.path("runId").asLong(-1) != runId) return;
        List<String> data;
        synchronized (lines) {
            data = List.copyOf(lines);
        }
        Platform.runLater(() -> {
            if (!inFlight) return;
            setBusy(false);
            boolean ok = response.path("ok").asBoolean(false);
            String stdout = response.path("stdout").asText("");
            String err = response.path("error").asText("");
            output.setText(stdout + (err.isBlank() ? "" : (stdout.isBlank() ? "" : "\\n") + err));
            status.setText((ok ? "Hoàn thành" : "Có lỗi") + " • "
                    + response.path("timeMs").asLong() + " ms");
            try {
                Trace t = TraceReader.append(TraceReader.empty(), data);
                if (!t.events().isEmpty()) {
                    player = new Player(t, Map.of(), System::currentTimeMillis);
                    traceTable.load(t, submittedCode, player, editor::highlightTraceLine);
                }
            } catch (IOException e) {
                status.setText("Không đọc được dữ liệu trace: " + e.getMessage());
            }
        });
    }

    private void error(Frame f) {
        if (!inFlight) return;
        try {
            String message = ErrorMessage.from(f).message();
            Platform.runLater(() -> {
                if (inFlight) {
                    setBusy(false);
                    status.setText(message);
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void setBusy(boolean busy) {
        inFlight = busy;
        run.setDisable(busy || !"python".equals(currentLanguage));
        trace.setDisable(busy || !"python".equals(currentLanguage));
        languagePicker.setDisable(busy);
    }

    private static String languageFor(String option) {
        return switch (option) {
            case "Java 17 · nháp" -> "java";
            case "C++17 · nháp" -> "cpp";
            case "JavaScript · nháp" -> "javascript";
            default -> "python";
        };
    }

    private static Path codeFile(String language) {
        return switch (language) {
            case "java" -> WORK_DIR.resolve("playground.java");
            case "cpp" -> WORK_DIR.resolve("playground.cpp");
            case "javascript" -> WORK_DIR.resolve("playground.js");
            default -> WORK_DIR.resolve("playground.py");
        };
    }

    private static String displayFile(String language) {
        return switch (language) {
            case "java" -> "Main.java";
            case "cpp" -> "main.cpp";
            case "javascript" -> "main.js";
            default -> "main.py";
        };
    }

    private void switchLanguage(String nextLanguage) {
        if (inFlight || nextLanguage.equals(currentLanguage)) return;
        saveDraft();
        currentLanguage = nextLanguage;
        // Giữ các phiên bản nháp độc lập; đổi ngôn ngữ không ghi nội dung vào Server.
        editor.setSyntaxLanguage(nextLanguage);
        editor.load("free-code-local-" + nextLanguage, drafts.getOrDefault(nextLanguage, ""), 0, d -> {});
        codeTitle.setText(displayFile(nextLanguage));
        boolean python = "python".equals(nextLanguage);
        draftNotice.setManaged(!python);
        draftNotice.setVisible(!python);
        output.clear();
        traceTable.clear();
        player = null;
        rightTabs.getSelectionModel().selectFirst();
        status.setText(python ? "Sẵn sàng · Python" : "Bản nháp · chưa hỗ trợ chạy");
        setBusy(false);
    }

    public void saveDraft() {
        String code = editor.text();
        drafts.put(currentLanguage, code);
        String stdin = input.getText();
        if (!code.equals(persistedDrafts.get(currentLanguage))) {
            if (writeDraft(codeFile(currentLanguage), code)) {
                persistedDrafts.put(currentLanguage, code);
            }
        }
        if (!stdin.equals(savedInput)) {
            if (writeDraft(INPUT_FILE, stdin)) savedInput = stdin;
        }
    }

    private static String readDraft(Path path, String fallback) {
        try {
            return Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : fallback;
        } catch (IOException ignored) {
            return fallback;
        }
    }

    private static boolean writeDraft(Path path, String content) {
        try {
            Files.createDirectories(WORK_DIR);
            Files.writeString(path, content, StandardCharsets.UTF_8);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public void dispose() {
        saveDraft();
        ticker.stop();
        autosave.stop();
        client.off(MessageType.PLAYGROUND_RUN_QUEUED, queued);
        client.off(MessageType.PLAYGROUND_TRACE_CHUNK, chunk);
        client.off(MessageType.PLAYGROUND_RUN_RESULT, result);
        client.off(MessageType.ERROR, error);
    }
}
