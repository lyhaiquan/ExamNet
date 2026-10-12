package labcast.client.practice;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import javafx.animation.PauseTransition;
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
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;
import labcast.common.trace.Trace;
import labcast.common.trace.TraceReader;

/**
 * Nút Chạy thử và kết quả từng test. Gửi {@code RUN_REQ(lessonId, codeSeq, testId)}, gom các {@code
 * TRACE_CHUNK} của lượt chạy, rồi đưa trace của chính code học viên cho trình phát. Chủ: Người 3.
 */
public final class RunPanel extends VBox {
    private final LcpClient client;
    private final String lessonId;
    private final LongSupplier flushCode;
    private final Consumer<Trace> onTrace;
    private final ComboBox<String> tests = new ComboBox<>();
    private final Button runBtn = new Button("▶ Chạy thử");
    private final Label verdict = new Label();
    private final TextArea output = new TextArea();
    private final List<String> lines = new ArrayList<>();
    private volatile long runId = -1;
    /** Đang chờ kết quả. Bất kỳ ERROR nào tới lúc này (SANDBOX_DOWN, NOT_FOUND…) kết thúc lượt chạy. */
    private volatile boolean inFlight;

    private final PauseTransition timeout = new PauseTransition(javafx.util.Duration.seconds(30));

    private final Consumer<Frame> onQueued = this::queued;
    private final Consumer<Frame> onChunk = this::chunk;
    private final Consumer<Frame> onResult = this::result;
    private final Consumer<Frame> onError = this::error;

    /**
     * @param flushCode xả delta đang gộp trong ô soạn, trả seq code cuối — server chạy đúng phiên bản đó
     * @param onTrace nhận trace của lượt chạy (gọi trên luồng giao diện)
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
        output.setPrefRowCount(4);
        output.setWrapText(true);
        output.setPromptText("Kết quả in ra của code sẽ hiện ở đây");
        output.getStyleClass().add("console");
        verdict.setId("verdict");
        verdict.getStyleClass().add("verdict");
        verdict.managedProperty().bind(verdict.visibleProperty());
        verdict.setVisible(false);
        runBtn.setId("run");
        runBtn.getStyleClass().add("success");
        runBtn.setOnAction(e -> run());
        Label testLabel = new Label("Test");
        testLabel.getStyleClass().add("muted");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox bar = new HBox(8, runBtn, testLabel, tests, gap, verdict);
        bar.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(bar, output);

        client.on(MessageType.RUN_QUEUED, onQueued);
        client.on(MessageType.TRACE_CHUNK, onChunk);
        client.on(MessageType.RUN_RESULT, onResult);
        client.on(MessageType.ERROR, onError);
        timeout.setOnFinished(e -> fail("Server không trả kết quả sau 30 giây"));
    }

    /** Gỡ listener khi rời màn bài học. */
    public void dispose() {
        client.off(MessageType.RUN_QUEUED, onQueued);
        client.off(MessageType.TRACE_CHUNK, onChunk);
        client.off(MessageType.RUN_RESULT, onResult);
        client.off(MessageType.ERROR, onError);
        timeout.stop();
    }

    private void run() {
        long seq = flushCode.getAsLong();
        String testId = tests.getSelectionModel().getSelectedItem();
        synchronized (lines) {
            lines.clear();
        }
        runId = 0; // đang chờ RUN_QUEUED
        inFlight = true;
        timeout.playFromStart();
        runBtn.setDisable(true);
        showVerdict("Đang chạy…", "pending");
        client.send(
                MessageType.RUN_REQ,
                new PayloadWriter()
                        .str(lessonId)
                        .i64(seq)
                        .str(testId == null ? "" : testId)
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
            runBtn.setDisable(false);
            String v = r.path("tests").path(0).path("verdict").asText("?");
            showVerdict(
                    switch (v) {
                        case "OK" -> "✔ Đúng";
                        case "WRONG" -> "✘ Sai kết quả";
                        case "TIME_LIMIT" -> "⏱ Quá thời gian";
                        case "RUNTIME_ERROR" -> "✘ Lỗi khi chạy";
                        default -> v;
                    },
                    v.equals("OK") ? "ok" : "fail");
            String err = r.path("error").isNull() ? "" : r.path("error").asText("");
            output.setText(r.path("stdout").asText("") + (err.isEmpty() ? "" : "\n" + err));
            onTrace.accept(trace);
        });
    }

    private void error(Frame f) {
        if (!inFlight) {
            return;
        }
        String msg;
        try {
            msg = ErrorMessage.from(f).message();
        } catch (IOException e) {
            msg = "Lỗi không rõ";
        }
        String m = msg;
        Platform.runLater(() -> fail(m));
    }

    /** Kết thúc lượt chạy vì lỗi phía server hoặc hết giờ chờ: mở lại nút để học viên thử lại. */
    private void fail(String message) {
        inFlight = false;
        runId = -1;
        timeout.stop();
        runBtn.setDisable(false);
        showVerdict("⚠ " + message, "fail");
    }

    /** {@code variant}: {@code pending}, {@code ok} hoặc {@code fail} — màu ở labcast.css. */
    private void showVerdict(String text, String variant) {
        verdict.setText(text);
        verdict.getStyleClass().removeAll("pending", "ok", "fail");
        verdict.getStyleClass().add(variant);
        verdict.setVisible(true);
    }
}
