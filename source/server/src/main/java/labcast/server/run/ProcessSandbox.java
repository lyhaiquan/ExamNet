package labcast.server.run;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Phần chung của hai sandbox: chạy một tiến trình chạy {@code run.py}, ghi job JSON vào stdin rồi đóng
 * stdin, đọc stdout từng dòng. Quá {@code timeLimitMs + 1500} ms thì giết.
 */
abstract class ProcessSandbox implements Sandbox {
    static final long GRACE_MS = 1_500;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_STDERR = 16 * 1024;

    /** Lệnh chạy cho một lượt; {@code name} là tên duy nhất của lượt (Docker dùng làm tên container). */
    protected abstract List<String> command(String name);

    /** Thời gian khởi động thêm ngoài giới hạn của bài (Docker cần dựng container). */
    protected long startupBudgetMs() {
        return 0;
    }

    /**
     * Tiến trình kết thúc mà không in dòng kết quả: lỗi của HẠ TẦNG (Docker không chạy, thiếu image,
     * thiếu Python) thì trả IOException để học viên nhận SANDBOX_DOWN, không bị chấm là code mình lỗi.
     * Trả null nếu là lỗi của bài làm.
     */
    protected IOException infrastructureFailure(int exitCode, String stderr) {
        return null;
    }

    /** Giết lượt chạy quá giờ. Mặc định giết tiến trình con. */
    protected void kill(String name, Process p) {
        p.destroyForcibly();
    }

    @Override
    public Outcome run(Job job, Consumer<String> onLine) throws IOException, InterruptedException {
        String name = "labcast-run-" + java.util.UUID.randomUUID();
        ProcessBuilder pb = new ProcessBuilder(command(name));
        pb.environment().put("PYTHONIOENCODING", "utf-8");
        pb.environment().put("PYTHONDONTWRITEBYTECODE", "1");
        long start = System.nanoTime();
        Process p = pb.start();

        AtomicBoolean killed = new AtomicBoolean();
        Thread watchdog = new Thread(
                () -> {
                    try {
                        long deadline = job.timeLimitMs() + GRACE_MS + startupBudgetMs();
                        if (!p.waitFor(deadline, TimeUnit.MILLISECONDS)) {
                            killed.set(true);
                            kill(name, p);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                },
                "canh-gio-" + name);
        watchdog.setDaemon(true);
        watchdog.start();

        StringBuilder stderr = new StringBuilder();
        Thread errReader = drain(p.getErrorStream(), stderr);

        try (OutputStream in = p.getOutputStream()) {
            in.write(MAPPER.writeValueAsBytes(jobJson(job)));
        } catch (IOException e) {
            // tiến trình chết sớm: kết quả đọc ở dưới sẽ báo
        }

        JsonNode result = null;
        try (BufferedReader out =
                new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = out.readLine()) != null) {
                if (line.startsWith("{\"t\": \"result\"") || line.startsWith("{\"t\":\"result\"")) {
                    result = MAPPER.readTree(line);
                } else if (!line.isBlank()) {
                    onLine.accept(line);
                }
            }
        }
        p.waitFor(5, TimeUnit.SECONDS);
        watchdog.interrupt();
        errReader.join(1_000);
        long ms = (System.nanoTime() - start) / 1_000_000;

        if (killed.get()) {
            return new Outcome(false, "", "Quá thời gian " + job.timeLimitMs() + " ms", false, ms, true);
        }
        if (result == null) {
            String err = stderr.toString().strip();
            IOException infra = infrastructureFailure(p.exitValue(), err);
            if (infra != null) {
                throw infra;
            }
            return new Outcome(
                    false,
                    "",
                    err.isEmpty() ? "Chương trình kết thúc bất thường (mã " + p.exitValue() + ")" : err,
                    false,
                    ms,
                    false);
        }
        return new Outcome(
                result.path("ok").asBoolean(false),
                result.path("stdout").asText(""),
                result.path("error").isNull() ? null : result.path("error").asText(null),
                result.path("truncated").asBoolean(false),
                result.path("time_ms").asLong(ms),
                false);
    }

    private static Map<String, Object> jobJson(Job job) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", job.code());
        m.put("input", job.input());
        m.put("trace", job.trace());
        m.put("time_limit_ms", job.timeLimitMs());
        m.put("lesson", job.lessonId());
        m.put("views", job.views());
        return m;
    }

    private static Thread drain(InputStream in, StringBuilder into) {
        Thread t = new Thread(
                () -> {
                    try (var r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                        char[] buf = new char[1024];
                        int n;
                        while ((n = r.read(buf)) > 0) {
                            synchronized (into) {
                                if (into.length() < MAX_STDERR) {
                                    into.append(buf, 0, Math.min(n, MAX_STDERR - into.length()));
                                }
                            }
                        }
                    } catch (IOException ignored) {
                        // tiến trình đã đóng
                    }
                },
                "stderr");
        t.setDaemon(true);
        t.start();
        return t;
    }
}
