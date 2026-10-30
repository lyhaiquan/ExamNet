package labcast.client.practice;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongSupplier;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import labcast.client.net.LcpClient;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.Language;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;
import labcast.common.trace.Trace;
import labcast.common.trace.TraceReader;

/**
 * Nút Chạy thử và kết quả từng test. Gửi
 * {@code RUN_REQ(lessonId, codeSeq, testId)}, gom các {@code
 * TRACE_CHUNK} của lượt chạy, rồi đưa trace của chính code học viên cho trình
 * phát. Chủ: Người 3.
 */
public final class RunPanel extends VBox {
    private static final String SUBMIT_TEXT = "⬆ Nộp bài";
    private static final String RUN_TEXT = "▶ Chạy thử";

    private final LcpClient client;
    private final String lessonId;
    private final LongSupplier flushCode;
    private final Consumer<Trace> onTrace;
    private final ComboBox<String> tests = new ComboBox<>();
    private final Button runBtn = new Button(RUN_TEXT);
    private final Button submitBtn = new Button(SUBMIT_TEXT);
    private final Button traceBtn = new Button("⇥ Trace từng dòng");
    private final Button expandBtn = new Button("⛶");
    private Consumer<Boolean> onExpand = big -> {};
    private boolean expanded;
    /** Cỡ chữ khung kết quả, đổi bằng Ctrl + lăn chuột. */
    private double outputFont = 13;

    private boolean lastRunLineTrace;
    private String lastRunStdout = "";
    private final Label verdict = new Label();
    private final TextArea output = new TextArea();
    private final List<String> lines = new ArrayList<>();
    private volatile long runId = -1;
    /**
     * Đang chờ kết quả. Bất kỳ ERROR nào tới lúc này (SANDBOX_DOWN, NOT_FOUND…) kết
     * thúc lượt chạy.
     */
    private volatile boolean inFlight;
    /**
     * Đang chờ chấm bài nộp. Chạy thử và nộp không chạy cùng lúc, nên ERROR tới
     * thuộc về việc đang chờ.
     */
    private volatile boolean submitting;
    /** Mốc (ms, đồng hồ máy này) được nộp lại sau lần nộp sai; 0 = nộp được. */
    private long cooldownUntil;
    /**
     * Mốc được chạy thử lại sau khi server báo chạy nhanh quá (6 lượt/phút); 0 =
     * chạy được.
     */
    private long runCooldownUntil;

    /**
     * Chạy từng dòng Java dưới debugger có thể mất cỡ 20 s (hai JVM, mỗi dòng một
     * lần dừng).
     */
    private final PauseTransition timeout = new PauseTransition(javafx.util.Duration.seconds(45));

    private Language language = Language.PYTHON;
    private IntConsumer onErrorLine = line -> {
    };
    private Consumer<JsonNode> onSubmitFinished = result -> {
    };
    private boolean isolatedSubmission;
    /**
     * Chấm chạy cả test ẩn, lần đầu còn phải sinh test ẩn: chờ lâu hơn chạy thử.
     */
    private final PauseTransition submitTimeout = new PauseTransition(javafx.util.Duration.seconds(120));

    private final Timeline cooldownTicker = new Timeline(
            new KeyFrame(javafx.util.Duration.millis(250), e -> tickCooldown()));

    private final Consumer<Frame> onQueued = this::queued;
    private final Consumer<Frame> onChunk = this::chunk;
    private final Consumer<Frame> onResult = this::result;
    private final Consumer<Frame> onError = this::error;
    private final Consumer<Frame> onSubmitResult = this::submitResult;

    /**
     * @param flushCode xả delta đang gộp trong ô soạn, trả seq code cuối — server
     *                  chạy đúng phiên bản đó
     * @param onTrace   nhận trace của lượt chạy (gọi trên luồng giao diện)
     */
    public RunPanel(
            LcpClient client, String lessonId, List<String> testIds, LongSupplier flushCode, Consumer<Trace> onTrace) {
        super(10);
        this.client = client;
        this.lessonId = lessonId;
        this.flushCode = flushCode;
        this.onTrace = onTrace;
        getStyleClass().add("run-panel");
        tests.getItems().setAll(testIds);
        if (!testIds.isEmpty()) {
            tests.getSelectionModel().select(0);
        }
        output.setEditable(false);
        output.setPrefRowCount(5);
        output.setWrapText(true);
        output.setPromptText("Kết quả in ra của code sẽ hiện ở đây");
        output.getStyleClass().add("console");
        // Khung kết quả cao theo panel (panel nằm trong SplitPane dọc, kéo thanh chia để đổi cỡ)
        output.setMinHeight(48);
        output.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(output, Priority.ALWAYS);
        output.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
            if (e.isControlDown() && e.getDeltaY() != 0) {
                outputFont = Math.max(9, Math.min(28, outputFont + (e.getDeltaY() > 0 ? 1 : -1)));
                output.setStyle("-fx-font-size: " + outputFont + "px;");
                e.consume();
            }
        });
        expandBtn.setId("run-expand");
        expandBtn.getStyleClass().add("run-expand-button");
        expandBtn.setTooltip(
                new javafx.scene.control.Tooltip(
                        "Phóng to / thu về khung kết quả (kéo thanh phía trên để chỉnh tuỳ ý, Ctrl + lăn chuột để đổi cỡ chữ)"));
        expandBtn.setMinWidth(Region.USE_PREF_SIZE);
        expandBtn.setOnAction(e -> setExpanded(!expanded));
        verdict.setId("verdict");
        verdict.getStyleClass().add("verdict");
        verdict.managedProperty().bind(verdict.visibleProperty());
        verdict.setVisible(false);
        runBtn.setId("run");
        runBtn.getStyleClass().add("success");
        runBtn.setOnAction(e -> run(false));
        traceBtn.getStyleClass().add("trace-run-button");
        traceBtn.setTooltip(new javafx.scene.control.Tooltip("Ghi lại từng dòng để xem Trace Table sau khi chạy"));
        traceBtn.setOnAction(e -> run(true));
        submitBtn.setId("submit");
        submitBtn.getStyleClass().add("primary");
        submitBtn.setOnAction(e -> submit());
        Label testLabel = new Label("Test");
        testLabel.getStyleClass().add("muted");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        // nút và ô chọn test giữ đủ chữ; kết quả nằm hàng riêng để không chen các nút
        runBtn.setMinWidth(Region.USE_PREF_SIZE);
        traceBtn.setMinWidth(Region.USE_PREF_SIZE);
        submitBtn.setMinWidth(Region.USE_PREF_SIZE);
        tests.setMinWidth(Region.USE_PREF_SIZE);
        testLabel.setMinWidth(Region.USE_PREF_SIZE);
        verdict.setWrapText(true);
        HBox bar = new HBox(8, runBtn, traceBtn, testLabel, tests, gap, expandBtn, submitBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(bar, verdict, output);

        client.on(MessageType.RUN_QUEUED, onQueued);
        client.on(MessageType.TRACE_CHUNK, onChunk);
        client.on(MessageType.RUN_RESULT, onResult);
        client.on(MessageType.ERROR, onError);
        client.on(MessageType.SUBMIT_RESULT, onSubmitResult);
        timeout.setOnFinished(e -> fail("Server không trả kết quả sau 45 giây"));
        submitTimeout.setOnFinished(e -> fail("Server chưa chấm xong sau 2 phút, thử nộp lại"));
        cooldownTicker.setCycleCount(Timeline.INDEFINITE);
    }

    /** Gỡ listener khi rời màn bài học. */
    /** Chạy thử như bấm nút "Chạy thử" (dùng khi thoát chế độ tập trung bằng nút Chạy). */
    public void runCode() {
        run(false);
    }

    public void dispose() {
        client.off(MessageType.RUN_QUEUED, onQueued);
        client.off(MessageType.TRACE_CHUNK, onChunk);
        client.off(MessageType.RUN_RESULT, onResult);
        client.off(MessageType.ERROR, onError);
        client.off(MessageType.SUBMIT_RESULT, onSubmitResult);
        timeout.stop();
        submitTimeout.stop();
        cooldownTicker.stop();
    }

    /** Chỉ hiển thị cho bài Python; SQL không có trace theo dòng. */
    public void enableLineTrace(boolean enabled) {
        traceBtn.setVisible(enabled);
        traceBtn.setManaged(enabled);
    }

    /**
     * Ngôn ngữ của code đang soạn: gửi kèm Chạy thử, Trace từng dòng và Nộp bài.
     */
    public void setLanguage(Language l) {
        language = l;
    }

    public Language language() {
        return language;
    }

    public void setIsolatedSubmission(boolean isolated) {
        isolatedSubmission = isolated;
    }

    /**
     * Nhận số dòng lỗi (1-based) khi lượt chạy báo được dòng, kể cả lỗi biên dịch.
     */
    public void setOnErrorLine(IntConsumer l) {
        onErrorLine = l;
    }

    /** Gọi sau khi server trả kết quả cho một lần nộp bài. */
    public void setOnSubmitFinished(Consumer<JsonNode> callback) {
        onSubmitFinished = callback == null ? result -> {
        } : callback;
    }

    public boolean lastRunLineTrace() {
        return lastRunLineTrace;
    }

    public String lastRunStdout() {
        return lastRunStdout;
    }

    private void run(boolean traceLines) {
        lastRunLineTrace = traceLines;
        lastRunStdout = "";
        long seq = flushCode.getAsLong();
        String testId = tests.getSelectionModel().getSelectedItem();
        synchronized (lines) {
            lines.clear();
        }
        runId = 0; // đang chờ RUN_QUEUED
        inFlight = true;
        timeout.playFromStart();
        setBusy(true);
        showVerdict("Đang chạy…", "pending");
        client.send(
                MessageType.RUN_REQ,
                new PayloadWriter()
                        .str(lessonId)
                        .i64(seq)
                        .str(testId == null ? "" : testId)
                        .bool(traceLines)
                        .str(language.code())
                        .toBytes());
    }

    private void queued(Frame f) {
        try {
            if (runId == 0) {
                runId = f.reader().i64();
            }
        } catch (IOException ignored) {
            // bỏ
        }
    }

    private void chunk(Frame f) {
        try {
            PayloadReader r = f.reader();
            if (r.i64() != runId) {
                return;
            }
            r.i32();
            r.bool();
            String text = new String(r.blob(), StandardCharsets.UTF_8);
            synchronized (lines) {
                if (!text.isEmpty()) {
                    lines.addAll(Arrays.asList(text.split("\n")));
                }
            }
        } catch (IOException ignored) {
            // khúc hỏng: bỏ
        }
    }

    private void result(Frame f) {
        JsonNode r;
        try {
            r = JsonPayload.tree(f);
        } catch (IOException e) {
            return;
        }
        if (r.path("runId").asLong() != runId) {
            return;
        }
        inFlight = false;
        Trace t;
        synchronized (lines) {
            try {
                t = TraceReader.append(TraceReader.empty(), List.copyOf(lines));
            } catch (IOException e) {
                t = TraceReader.empty();
            }
        }
        Trace trace = t;
        Platform.runLater(() -> {
            timeout.stop();
            setBusy(false);
            String v = r.path("tests").path(0).path("verdict").asText("?");
            boolean bienDich = r.path("errorKind").asText("").equals("bien-dich");
            showVerdict(
                    bienDich
                            ? "✘ Lỗi biên dịch"
                            : switch (v) {
                                case "OK" -> "✔ Đúng";
                                case "WRONG" -> "✘ Sai kết quả";
                                case "TIME_LIMIT" -> "⏱ Quá thời gian";
                                case "RUNTIME_ERROR" -> "✘ Lỗi khi chạy";
                                case "NO_EXPECTED" -> "✔ Chạy xong (bài chưa có đáp án để so)";
                                case "REPLACED" -> "Lượt này bị lượt chạy thử mới hơn thay";
                                default -> v;
                            },
                    v.equals("OK") ? "ok" : "fail");
            String err = r.path("error").isNull() ? "" : r.path("error").asText("");
            lastRunStdout = r.path("stdout").asText("");
            output.setText(lastRunStdout + (err.isEmpty() ? "" : "\n" + err));
            onTrace.accept(trace);
            if (r.path("errorLine").asInt(0) > 0) {
                onErrorLine.accept(r.path("errorLine").asInt());
            }
        });
    }

    private void error(Frame f) {
        if (!inFlight && !submitting) {
            return;
        }
        ErrorMessage e;
        try {
            e = ErrorMessage.from(f);
        } catch (IOException ex) {
            e = new ErrorMessage(ErrorCode.INTERNAL, "Lỗi không rõ", 0);
        }
        ErrorMessage err = e;
        boolean cooldown = submitting && err.code() == ErrorCode.COOLDOWN;
        boolean runLimited = !submitting && err.code() == ErrorCode.RATE_LIMITED && err.retryAfterMs() > 0;
        Platform.runLater(() -> {
            fail(err.message());
            if (cooldown) {
                startCooldown(err.retryAfterMs());
            }
            if (runLimited) {
                runCooldownUntil = System.currentTimeMillis() + err.retryAfterMs();
                tickCooldown();
                cooldownTicker.play();
            }
        });
    }

    /**
     * Kết thúc lượt chạy / nộp vì lỗi phía server hoặc hết giờ chờ: mở lại nút để
     * học viên thử lại.
     */
    private void fail(String message) {
        inFlight = false;
        submitting = false;
        runId = -1;
        timeout.stop();
        submitTimeout.stop();
        setBusy(false);
        showVerdict("⚠ " + message, "fail");
    }

    private void submit() {
        long seq = flushCode.getAsLong();
        submitting = true;
        submitTimeout.playFromStart();
        setBusy(true);
        showVerdict("Đang chấm cả test ẩn…", "pending");
        output.setText("");
        client.send(
                MessageType.SUBMIT_REQ,
                new PayloadWriter().str(lessonId).i64(seq).str(language.code()).bool(isolatedSubmission).toBytes());
    }

    /**
     * {@code SUBMIT_RESULT JSON{lessonId, verdict, message, score, cooldownMs, mastered, failedTest,
     * unlocked, tests:[{id, hidden, verdict, timeMs, error?}]}}.
     */
    private void submitResult(Frame f) {
        JsonNode r;
        try {
            r = JsonPayload.tree(f);
        } catch (IOException e) {
            return;
        }
        if (!submitting || !lessonId.equals(r.path("lessonId").asText())) {
            return;
        }
        submitting = false;
        Platform.runLater(() -> {
            submitTimeout.stop();
            setBusy(false);
            String v = r.path("verdict").asText();
            String failed = r.path("failedTest").asText("");
            showVerdict(
                    switch (v) {
                        case "OK" -> "✔ Đạt — " + r.path("score").asInt() + " điểm";
                        case "WRONG" -> "✘ Sai ở " + failed;
                        case "TIME_LIMIT" -> "⏱ Quá thời gian ở " + failed;
                        case "RUNTIME_ERROR" -> "✘ Lỗi khi chạy " + failed;
                        case "FORBIDDEN_CALL" -> "⛔ Dùng hàm cấm";
                        case "COMPILE_ERROR" -> "✘ Lỗi biên dịch";
                        case "TRACE_RULE" -> "✘ Sai yêu cầu của đề";
                        default -> v;
                    },
                    v.equals("OK") ? "ok" : "fail");
            output.setText(summary(r));
            long cd = r.path("cooldownMs").asLong();
            if (cd > 0) {
                startCooldown(cd);
            }
            onSubmitFinished.accept(r);
        });
    }

    /** Gọi khi người dùng bấm ⛶: {@code true} phóng to khung kết quả, {@code false} thu về cỡ thường. */
    public void setOnExpand(Consumer<Boolean> l) {
        this.onExpand = l == null ? big -> {} : l;
    }

    private void setExpanded(boolean big) {
        expanded = big;
        onExpand.accept(big);
    }

    /** Kết quả từng test cho khung output. Test ẩn chỉ có đúng/sai, không có input hay output. */
    static String summary(JsonNode r) {
        StringBuilder sb = new StringBuilder("Nộp bài: ")
                .append(r.path("message").asText())
                .append('\n');
        for (JsonNode t : r.path("tests")) {
            String v = t.path("verdict").asText();
            sb.append(v.equals("OK") ? "✔ " : "✘ ")
                    .append(t.path("id").asText())
                    .append(t.path("hidden").asBoolean() ? " (test ẩn)" : "")
                    .append("  ")
                    .append(
                            switch (v) {
                                case "OK" -> t.path("timeMs").asLong() + " ms";
                                case "WRONG" -> "sai kết quả";
                                case "TIME_LIMIT" -> "quá thời gian";
                                case "RUNTIME_ERROR" -> "lỗi khi chạy";
                                default -> v;
                            })
                    .append('\n');
            if (t.hasNonNull("error")) {
                sb.append("    ")
                        .append(t.path("error").asText().replace("\n", "\n    "))
                        .append('\n');
            }
        }
        if (r.path("verdict").asText().equals("OK")) {
            sb.append("Điểm ").append(r.path("score").asInt()).append(". Đã thành thạo bài này.\n");
            if (r.path("unlocked").size() > 0) {
                List<String> ids = new ArrayList<>();
                r.path("unlocked").forEach(x -> ids.add(x.asText()));
                sb.append("Mở bài mới: ").append(String.join(", ", ids)).append('\n');
            }
        } else if (r.path("cooldownMs").asLong() > 0) {
            sb.append("Phải chờ ").append(r.path("cooldownMs").asLong() / 1000).append(" s mới nộp lại được.\n");
        }
        return sb.toString().stripTrailing();
    }

    private void startCooldown(long ms) {
        cooldownUntil = System.currentTimeMillis() + ms;
        tickCooldown();
        cooldownTicker.play();
    }

    /**
     * Đếm ngược trên nút Nộp (sau lần nộp sai) và nút Chạy thử (sau khi chạy nhanh
     * quá).
     */
    private void tickCooldown() {
        long now = System.currentTimeMillis();
        long left = cooldownUntil - now;
        if (left <= 0) {
            cooldownUntil = 0;
            submitBtn.setText(SUBMIT_TEXT);
            submitBtn.setDisable(inFlight || submitting);
        } else {
            submitBtn.setText("Nộp lại sau " + (left + 999) / 1000 + " s");
            submitBtn.setDisable(true);
        }
        long runLeft = runCooldownUntil - now;
        if (runLeft <= 0) {
            runCooldownUntil = 0;
            runBtn.setText(RUN_TEXT);
            runBtn.setDisable(inFlight || submitting);
            traceBtn.setDisable(inFlight || submitting);
        } else {
            runBtn.setText("Chạy lại sau " + (runLeft + 999) / 1000 + " s");
            runBtn.setDisable(true);
            traceBtn.setDisable(true);
        }
        if (cooldownUntil == 0 && runCooldownUntil == 0) {
            cooldownTicker.stop();
        }
    }

    private void setBusy(boolean busy) {
        long now = System.currentTimeMillis();
        runBtn.setDisable(busy || runCooldownUntil > now);
        traceBtn.setDisable(busy || runCooldownUntil > now);
        submitBtn.setDisable(busy || cooldownUntil > now);
    }

    /**
     * {@code variant}: {@code pending}, {@code ok} hoặc {@code fail} — màu ở
     * labcast.css.
     */
    private void showVerdict(String text, String variant) {
        verdict.setText(text);
        verdict.getStyleClass().removeAll("pending", "ok", "fail");
        verdict.getStyleClass().add(variant);
        getStyleClass().removeAll("result-pending", "result-ok", "result-fail");
        getStyleClass().add("result-" + variant);
        verdict.setVisible(true);
    }
}
