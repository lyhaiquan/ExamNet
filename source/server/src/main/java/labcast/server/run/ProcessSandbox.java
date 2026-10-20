package labcast.server.run;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Phần chung của hai sandbox: chạy một tiến trình, ghi JSON vào stdin rồi đóng stdin, đọc stdout từng dòng,
 * quá hạn thì giết. {@link #run} chạy {@code run.py} (chạy thử, có trace); {@link #runScript} chạy các
 * script khác của runner (chấm bài, chuẩn bị test ẩn, kiểm luật).
 */
abstract class ProcessSandbox implements Sandbox, ScriptRunner {
    static final long GRACE_MS = 1_500;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_STDERR = 16 * 1024;

    /** Lệnh chạy {@code run.py} cho một lượt; {@code name} là tên duy nhất (Docker dùng làm tên container). */
    protected abstract List<String> command(String name);

    /** Lệnh chạy {@code runner/<script>.py}; {@code lessonDir} khác null thì gắn chỉ đọc (xem {@link ScriptRunner}). */
    protected abstract List<String> scriptCommand(String name, String script, Path lessonDir);

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
        JsonNode[] result = new JsonNode[1];
        Exec e = exec(
                command(name),
                name,
                MAPPER.writeValueAsBytes(jobJson(job)),
                job.timeLimitMs() + GRACE_MS + startupBudgetMs(),
                line -> {
                    if (line.startsWith("{\"t\": \"result\"") || line.startsWith("{\"t\":\"result\"")) {
                        try {
                            result[0] = MAPPER.readTree(line);
                        } catch (IOException ignored) {
                            // dòng hỏng: coi như không có kết quả
                        }
                    } else {
                        onLine.accept(line);
                    }
                });
        if (e.killed || (result[0] == null && hetGioCpu(e, job.timeLimitMs()))) {
            return new Outcome(false, "", "Quá thời gian " + job.timeLimitMs() + " ms", false, e.ms, true);
        }
        if (result[0] == null) {
            String err = e.stderr.strip();
            IOException infra = infrastructureFailure(e.exitCode, err);
            if (infra != null) {
                throw infra;
            }
            return new Outcome(
                    false,
                    "",
                    err.isEmpty() ? "Chương trình kết thúc bất thường (mã " + e.exitCode + ")" : err,
                    false,
                    e.ms,
                    false);
        }
        JsonNode r = result[0];
        return new Outcome(
                r.path("ok").asBoolean(false),
                r.path("stdout").asText(""),
                r.path("error").isNull() ? null : r.path("error").asText(null),
                r.path("truncated").asBoolean(false),
                r.path("time_ms").asLong(e.ms),
                false);
    }

    @Override
    public Result runScript(String script, Path lessonDir, String stdin, long deadlineMs)
            throws IOException, InterruptedException {
        String name = "labcast-" + script.replace('_', '-') + "-" + java.util.UUID.randomUUID();
        List<String> lines = new ArrayList<>();
        Exec e = exec(
                scriptCommand(name, script, lessonDir),
                name,
                stdin.getBytes(StandardCharsets.UTF_8),
                deadlineMs + startupBudgetMs(),
                lines::add);
        if (lines.isEmpty() && !e.killed) {
            IOException infra = infrastructureFailure(e.exitCode, e.stderr.strip());
            if (infra != null) {
                throw infra;
            }
        }
        return new Result(List.copyOf(lines), e.exitCode, e.stderr.strip(), e.killed, e.ms);
    }

    private record Exec(int exitCode, String stderr, boolean killed, long ms) {}

    /**
     * run.py tự đặt RLIMIT_CPU nên vòng lặp vô tận thường chết vì giới hạn CPU trước khi đồng hồ canh giờ ở
     * đây kịp giết: SIGXCPU (mã 152) ở giới hạn mềm, hoặc SIGKILL (mã 137) ở giới hạn cứng khi run.py là
     * PID 1 trong container (PID 1 bỏ qua SIGXCPU). Đã chạy quá giới hạn của bài thì cũng là quá giờ.
     */
    private static boolean hetGioCpu(Exec e, long limitMs) {
        return (e.exitCode == 128 + 24 || e.exitCode == 128 + 9) && e.ms >= limitMs;
    }

    /** Chạy {@code cmd}, ghi {@code stdin}, gọi {@code onLine} cho mỗi dòng stdout khác rỗng. */
    private Exec exec(List<String> cmd, String name, byte[] stdin, long deadlineMs, Consumer<String> onLine)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("PYTHONIOENCODING", "utf-8");
        pb.environment().put("PYTHONDONTWRITEBYTECODE", "1");
        long start = System.nanoTime();
        Process p = pb.start();

        AtomicBoolean killed = new AtomicBoolean();
        Thread watchdog = new Thread(
                () -> {
                    try {
                        if (!p.waitFor(deadlineMs, TimeUnit.MILLISECONDS)) {
                            killed.set(true);
                            kill(name, p);
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                },
                "canh-gio-" + name);
        watchdog.setDaemon(true);
        watchdog.start();

        StringBuilder stderr = new StringBuilder();
        Thread errReader = drain(p.getErrorStream(), stderr);

        try (OutputStream in = p.getOutputStream()) {
            in.write(stdin);
        } catch (IOException ex) {
            // tiến trình chết sớm: kết quả đọc ở dưới sẽ báo
        }

        try (BufferedReader out =
                new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = out.readLine()) != null) {
                if (!line.isBlank()) {
                    onLine.accept(line);
                }
            }
        }
        p.waitFor(5, TimeUnit.SECONDS);
        watchdog.interrupt();
        errReader.join(1_000);
        long ms = (System.nanoTime() - start) / 1_000_000;
        String err;
        synchronized (stderr) {
            err = stderr.toString();
        }
        return new Exec(p.isAlive() ? -1 : p.exitValue(), err, killed.get(), ms);
    }

    private static Map<String, Object> jobJson(Job job) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", job.code());
        m.put("input", job.input());
        m.put("trace", job.trace());
        m.put("line_trace", job.lineTrace());
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
