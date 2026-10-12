package labcast.server.lesson;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import labcast.common.protocol.JsonPayload;
import labcast.server.run.ScriptRunner;

/**
 * Sinh test ẩn của một bài bằng MỘT lần chạy container tin cậy ({@code runner/chuan_bi.py}), lưu đệm ở
 * {@code <cacheDir>/<lessonId>/<content_hash>.json} (spec §8.3; phần test của task 3.7). Khởi động lại
 * server mà nội dung bài không đổi thì đọc đệm, không chạy lại. Chủ: Người 3.
 *
 * <p>Output của test ẩn chỉ nằm trên máy server; không gửi cho app, không đưa vào container chạy code học
 * viên (so đúng/sai làm bên Java).
 */
public final class LessonPreparer {
    private static final Logger LOG = Logger.getLogger(LessonPreparer.class.getName());

    public record HiddenTest(String id, String input, String output) {}

    /** {@code tb}: test cỡ trung bình để chạy code học viên có ghi trace và kiểm {@code luat.py}; có thể null. */
    public record Prepared(List<HiddenTest> tests, HiddenTest tb, long maxMs) {}

    private final ScriptRunner runner;
    private final Path cacheDir;
    private final Map<String, Object> locks = new ConcurrentHashMap<>();
    private final Map<String, Prepared> memory = new ConcurrentHashMap<>();

    public LessonPreparer(ScriptRunner runner, Path cacheDir) {
        this.runner = runner;
        this.cacheDir = cacheDir;
    }

    /**
     * Test ẩn của bài, sinh nếu chưa có. Gọi đồng thời cho cùng một bài thì chỉ một lần chạy sandbox.
     *
     * @throws IOException hạ tầng hỏng, hoặc bộ sinh test / lời giải mẫu của bài lỗi (lỗi của người soạn)
     */
    public Prepared prepare(Lesson l) throws IOException, InterruptedException {
        String key = l.id() + "@" + l.contentHash();
        Prepared hit = memory.get(key);
        if (hit != null) {
            return hit;
        }
        synchronized (locks.computeIfAbsent(l.id(), k -> new Object())) {
            hit = memory.get(key);
            if (hit != null) {
                return hit;
            }
            Path dir = cacheDir.resolve(l.id());
            Path file = dir.resolve(l.contentHash() + ".json");
            Prepared p;
            if (Files.exists(file)) {
                p = parse(JsonPayload.MAPPER.readTree(file.toFile()));
            } else {
                String job = JsonPayload.MAPPER
                        .createObjectNode()
                        .put("so_test_an", l.soTestAn())
                        .put("time_limit_ms", l.thoiGianMs())
                        .toString();
                // lời giải mẫu được rộng giờ gấp 5 mỗi test, cộng thời gian chạy bộ sinh test
                long deadline = 30_000 + (l.soTestAn() + 1L) * (5L * l.thoiGianMs() + 3_000);
                ScriptRunner.Result r = runner.runScript("chuan_bi", l.dir(), job, deadline);
                if (r.killed()) {
                    throw new IOException("Chuẩn bị test ẩn bài " + l.id() + " quá " + deadline / 1000 + " s");
                }
                if (r.lines().isEmpty()) {
                    throw new IOException("chuan_bi.py không in kết quả: " + r.stderr());
                }
                JsonNode out =
                        JsonPayload.MAPPER.readTree(r.lines().get(r.lines().size() - 1));
                if (!out.path("t").asText().equals("chuan_bi")) {
                    throw new IOException("Bài " + l.id() + " lỗi khi sinh test ẩn: "
                            + out.path("loi").asText());
                }
                p = parse(out);
                Files.createDirectories(dir);
                Path tmp = Files.createTempFile(dir, "dang-ghi", ".json");
                Files.writeString(tmp, out.toString());
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                removeStale(dir, file);
                LOG.info("Đã sinh " + p.tests().size() + " test ẩn cho " + l.id() + " (lời giải mẫu chậm nhất "
                        + p.maxMs() + " ms)");
            }
            memory.put(key, p);
            return p;
        }
    }

    /** Sinh trước test ẩn cho mọi bài (gọi ở luồng nền lúc server khởi động), để lượt nộp đầu không phải chờ. */
    public void warmUp(List<Lesson> lessons) {
        for (Lesson l : lessons) {
            if (l.soTestAn() <= 0 && !l.hasTraceRule()) {
                continue;
            }
            try {
                prepare(l);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Không sinh được test ẩn cho " + l.id() + ": " + e.getMessage());
            }
        }
    }

    private static Prepared parse(JsonNode n) {
        List<HiddenTest> tests = new ArrayList<>();
        for (JsonNode t : n.path("tests")) {
            tests.add(new HiddenTest(
                    t.path("id").asText(),
                    t.path("input").asText(),
                    t.path("output").asText()));
        }
        JsonNode tb = n.path("tb");
        HiddenTest tbTest = tb.isObject()
                ? new HiddenTest(
                        "tb", tb.path("input").asText(), tb.path("output").asText())
                : null;
        return new Prepared(List.copyOf(tests), tbTest, n.path("max_ms").asLong());
    }

    /** Bỏ bản đệm của nội dung cũ (hash cũ). */
    private static void removeStale(Path dir, Path keep) {
        try (var files = Files.list(dir)) {
            files.filter(f -> !f.equals(keep)).forEach(f -> {
                try {
                    Files.deleteIfExists(f);
                } catch (IOException ignored) {
                    // để lần sau
                }
            });
        } catch (IOException ignored) {
            // không sao
        }
    }
}
