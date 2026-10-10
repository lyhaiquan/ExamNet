package labcast.client.practice;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import labcast.client.net.LcpClient;
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

    private final Consumer<Frame> onQueued = this::queued;
    private final Consumer<Frame> onChunk = this::chunk;
    private final Consumer<Frame> onResult = this::result;

    /**
     * @param flushCode xả delta đang gộp trong ô soạn, trả seq code cuối — server chạy đúng phiên bản đó
     * @param onTrace nhận trace của lượt chạy (gọi trên luồng giao diện)
     */
    public RunPanel(
            LcpClient client, String lessonId, List<String> testIds, LongSupplier flushCode, Consumer<Trace> onTrace) {
        super(6);
        this.client = client;
        this.lessonId = lessonId;
        this.flushCode = flushCode;
        this.onTrace = onTrace;
        setPadding(new Insets(8));
        tests.getItems().setAll(testIds);
        if (!testIds.isEmpty()) {
            tests.getSelectionModel().select(0);
        }
        output.setEditable(false);
        output.setPrefRowCount(4);
        output.setStyle("-fx-font-family: 'Consolas', monospace;");
        verdict.setStyle("-fx-font-weight: bold;");
        runBtn.setOnAction(e -> run());
        HBox bar = new HBox(8, runBtn, new Label("Test:"), tests, verdict);
        bar.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(bar, output);

        client.on(MessageType.RUN_QUEUED, onQueued);
        client.on(MessageType.TRACE_CHUNK, onChunk);
        client.on(MessageType.RUN_RESULT, onResult);
    }

    /** Gỡ listener khi rời màn bài học. */
    public void dispose() {
        client.off(MessageType.RUN_QUEUED, onQueued);
        client.off(MessageType.TRACE_CHUNK, onChunk);
        client.off(MessageType.RUN_RESULT, onResult);
    }

    private void run() {
        long seq = flushCode.getAsLong();
        String testId = tests.getSelectionModel().getSelectedItem();
        synchronized (lines) {
            lines.clear();
        }
        runId = 0; // đang chờ RUN_QUEUED
        runBtn.setDisable(true);
        verdict.setText("Đang chạy…");
        verdict.setStyle("-fx-font-weight: bold; -fx-text-fill: #7f8c8d;");
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
            runBtn.setDisable(false);
            String v = r.path("tests").path(0).path("verdict").asText("?");
            verdict.setText(
                    switch (v) {
                        case "OK" -> "✔ Đúng";
                        case "WRONG" -> "✘ Sai kết quả";
                        case "TIME_LIMIT" -> "⏱ Quá thời gian";
                        case "RUNTIME_ERROR" -> "✘ Lỗi khi chạy";
                        default -> v;
                    });
            verdict.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (v.equals("OK") ? "#27ae60" : "#c0392b") + ";");
            String err = r.path("error").isNull() ? "" : r.path("error").asText("");
            output.setText(r.path("stdout").asText("") + (err.isEmpty() ? "" : "\n" + err));
            onTrace.accept(trace);
        });
    }
}
