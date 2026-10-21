package labcast.server.grade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.Language;
import labcast.server.lesson.Lesson;
import labcast.server.lesson.LessonModule;
import labcast.server.lesson.LessonPreparer;
import labcast.server.run.Job;
import labcast.server.run.Outcome;
import labcast.server.run.RunModule;
import labcast.server.run.Sandbox;
import labcast.server.run.ScriptRunner;

/**
 * Chấm một bài nộp (spec §8.2, task 3.8). Chủ: Người 3.
 *
 * <ol>
 *   <li>{@code runner/cham.py} trong container học viên: kiểm hàm cấm bằng {@code ast} rồi chạy từng test ở
 *       một tiến trình con. Mỗi dòng kết quả phải mang khoá bí mật của lượt chấm này; dòng không có khoá
 *       (code học viên in giả) bị bỏ, test không có dòng kết quả nào tính là lỗi, không bao giờ là đúng.
 *   <li>So output với đáp án ở đây, ngoài container — đáp án không bao giờ vào container chạy code học viên.
 *   <li>Qua hết test và bài có {@code luat.py}: chạy code học viên có ghi trace với test {@code tb}, đưa trace
 *       cho {@code runner/kiem_luat.py} trong container tin cậy.
 * </ol>
 *
 * <p>Test ẩn: kết quả chỉ có id, verdict, thời gian — không input, không output, không thông báo lỗi (code
 * học viên có thể cố ném lỗi kèm input của test ẩn để lấy ra).
 */
public final class Grader {
    private static final Logger LOG = Logger.getLogger(Grader.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();
    /** Mỗi test cộng thêm thời gian khởi động tiến trình Python con. */
    static final long PER_TEST_OVERHEAD_MS = 1_500;

    public record TestCase(String id, String input, String expected, boolean hidden) {}

    /** {@code error}: chỉ có ở test xem được; test ẩn luôn null. */
    public record TestResult(String id, boolean hidden, Verdict verdict, long timeMs, String error) {}

    /** {@code failedTest}: id test sai đầu tiên, null khi qua hết hoặc lỗi trước khi chạy test. */
    public record Result(Verdict verdict, String message, List<TestResult> tests, String failedTest) {}

    private final ScriptRunner scripts;
    private final Sandbox sandbox;

    public Grader(ScriptRunner scripts, Sandbox sandbox) {
        this.scripts = scripts;
        this.sandbox = sandbox;
    }

    /**
     * @param tb test cỡ trung bình để kiểm luật trace; null nếu bài không có
     * @throws IOException hạ tầng hỏng (Docker, Python) — không phải lỗi của học viên, không tính lượt sai
     */
    public Result grade(Lesson l, String code, List<TestCase> tests, LessonPreparer.HiddenTest tb)
            throws IOException, InterruptedException {
        return grade(l, code, Language.PYTHON, tests, tb);
    }

    /**
     * Chấm bài viết bằng {@code lang}. C/C++/Java: runner biên dịch một lần rồi chạy từng test; giới hạn thời gian
     * nhân hệ số của ngôn ngữ; không có luật soi trace (trace là của thư viện viz Python).
     */
    public Result grade(Lesson l, String code, Language lang, List<TestCase> tests, LessonPreparer.HiddenTest tb)
            throws IOException, InterruptedException {
        String key = HexFormat.of().formatHex(bytes(16));
        int limitMs = l.thoiGianMs() * lang.timeFactor();
        ObjectNode job = JsonPayload.MAPPER.createObjectNode();
        job.put("k", key);
        job.put("code", code);
        job.put("ngon_ngu", lang.code());
        job.put("time_limit_ms", l.thoiGianMs());
        ArrayNode arr = job.putArray("tests");
        for (TestCase t : tests) {
            arr.addObject().put("id", t.id()).put("input", t.input());
        }
        ObjectNode cam = job.putObject("cam");
        cam.set("cam_dung", JsonPayload.MAPPER.valueToTree(l.camDung()));
        cam.set("cam_import", JsonPayload.MAPPER.valueToTree(l.camImport()));
        cam.set("cam_cu_phap", JsonPayload.MAPPER.valueToTree(l.camCuPhap()));
        cam.set("cam_theo_ngon_ngu", JsonPayload.MAPPER.valueToTree(l.camTheoNgonNgu()));

        long deadline = 3_000 + lang.compileBudgetMs() + tests.size() * (limitMs + PER_TEST_OVERHEAD_MS);
        ScriptRunner.Result r = scripts.runScript("cham", null, job.toString(), deadline);

        Map<String, JsonNode> byId = new HashMap<>();
        for (String line : r.lines()) {
            JsonNode n;
            try {
                n = JsonPayload.MAPPER.readTree(line);
            } catch (IOException e) {
                continue;
            }
            if (!key.equals(n.path("k").asText(null))) {
                continue; // không mang khoá của lượt chấm: code học viên in giả
            }
            switch (n.path("t").asText()) {
                case "cam" -> {
                    List<String> loi = new ArrayList<>();
                    n.path("loi").forEach(x -> loi.add(x.asText()));
                    return new Result(Verdict.FORBIDDEN_CALL, String.join("; ", loi), List.of(), null);
                }
                case "bien_dich" -> {
                    return new Result(Verdict.COMPILE_ERROR, n.path("loi").asText("Lỗi biên dịch"), List.of(), null);
                }
                case "test" -> byId.putIfAbsent(n.path("id").asText(), n);
                default -> {
                    // "done"
                }
            }
        }

        List<TestResult> results = new ArrayList<>();
        TestResult firstFail = null;
        for (TestCase t : tests) {
            JsonNode n = byId.get(t.id());
            TestResult tr = judge(t, n, r.killed(), limitMs);
            results.add(tr);
            if (firstFail == null && tr.verdict() != Verdict.OK) {
                firstFail = tr;
            }
        }
        if (firstFail != null) {
            return new Result(firstFail.verdict(), message(firstFail), List.copyOf(results), firstFail.id());
        }
        if (lang == Language.PYTHON && l.hasTraceRule() && tb != null) {
            String violation = traceRule(l, code, tb);
            if (violation != null) {
                return new Result(Verdict.TRACE_RULE, violation, List.copyOf(results), null);
            }
        }
        return new Result(Verdict.OK, "Qua hết " + tests.size() + " test", List.copyOf(results), null);
    }

    private static TestResult judge(TestCase t, JsonNode n, boolean batchKilled, int limitMs) {
        if (n == null) {
            // Không có dòng kết quả: lượt chấm bị giết vì quá giờ, hoặc code học viên làm hỏng tiến trình chấm
            return new TestResult(
                    t.id(),
                    t.hidden(),
                    batchKilled ? Verdict.TIME_LIMIT : Verdict.RUNTIME_ERROR,
                    0,
                    t.hidden() ? null : "Không có kết quả cho test này");
        }
        long ms = n.path("time_ms").asLong();
        Verdict v =
                switch (n.path("status").asText()) {
                    case "OK" -> ms > limitMs
                            ? Verdict.TIME_LIMIT
                            : RunModule.normalize(n.path("stdout").asText()).equals(RunModule.normalize(t.expected()))
                                    ? Verdict.OK
                                    : Verdict.WRONG;
                    case "TIME_LIMIT" -> Verdict.TIME_LIMIT;
                    default -> Verdict.RUNTIME_ERROR;
                };
        String error = t.hidden() || v != Verdict.RUNTIME_ERROR
                ? null
                : n.path("error").asText(null);
        return new TestResult(t.id(), t.hidden(), v, ms, error);
    }

    private static String message(TestResult f) {
        String where = f.hidden() ? "test ẩn " + f.id() : "test " + f.id();
        return switch (f.verdict()) {
            case WRONG -> "Sai kết quả ở " + where;
            case TIME_LIMIT -> "Quá thời gian ở " + where;
            case RUNTIME_ERROR -> "Lỗi khi chạy " + where;
            default -> f.verdict() + " ở " + where;
        };
    }

    /** Null nếu đạt luật trace; ngược lại là lý do vi phạm. */
    private String traceRule(Lesson l, String code, LessonPreparer.HiddenTest tb)
            throws IOException, InterruptedException {
        List<String> lines = new ArrayList<>();
        // ghi trace chậm hơn chạy thường nhiều lần; test tb nhỏ nên vẫn nhanh
        Outcome o = sandbox.run(
                new Job(code, tb.input(), true, l.thoiGianMs() * 3, l.id(), LessonModule.views(l)), lines::add);
        if (o.killed()) {
            return "Quá thời gian khi chạy test tb có ghi trace";
        }
        if (o.truncated()) {
            // lời giải mẫu chạy test tb chỉ vài nghìn sự kiện (runner/tests/test_chuan_bi_luat.py)
            return "Code thao tác trên mảng quá nhiều (vượt 10 000 bước ở test tb), không đúng thuật toán đề yêu cầu";
        }
        if (!o.ok()) {
            return "Lỗi khi chạy test tb có ghi trace";
        }
        ObjectNode job = JsonPayload.MAPPER.createObjectNode();
        job.put("input", tb.input());
        job.set("trace", JsonPayload.MAPPER.valueToTree(lines));
        ScriptRunner.Result r = scripts.runScript("kiem_luat", l.dir(), job.toString(), 30_000);
        if (r.killed() || r.lines().isEmpty()) {
            throw new IOException("Không kiểm được luật trace: " + r.stderr());
        }
        JsonNode n = JsonPayload.MAPPER.readTree(r.lines().get(r.lines().size() - 1));
        if (n.hasNonNull("loi_he_thong")) {
            // luat.py của bài lỗi: lỗi của người soạn, không phạt học viên
            LOG.warning(
                    "luat.py của " + l.id() + " lỗi: " + n.path("loi_he_thong").asText());
            return null;
        }
        return n.path("ok").asBoolean(true) ? null : n.path("loi").asText("Vi phạm luật của bài");
    }

    private static byte[] bytes(int n) {
        byte[] b = new byte[n];
        RANDOM.nextBytes(b);
        return b;
    }
}
