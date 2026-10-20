package labcast.server.grade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import labcast.common.protocol.JsonPayload;
import labcast.server.lesson.Lesson;
import labcast.server.run.ScriptRunner;
import org.junit.jupiter.api.Test;

/** Sandbox giả: trả các dòng định sẵn, có (hoặc không) mang khoá của lượt chấm. */
class GraderTest {
    static final Lesson BAI = new Lesson(
            "dsa.thu.bai",
            "dsa",
            "thu",
            "Bài thử",
            "bai-tap",
            List.of("code"),
            List.of(),
            List.of("sorted"),
            1_000,
            List.of("nho-1"),
            List.of(),
            Path.of("khong-co-thu-muc"),
            "hash",
            List.of(),
            List.of(),
            2);

    static final List<Grader.TestCase> TESTS = List.of(
            new Grader.TestCase("nho-1", "3 1 2\n", "1 2 3\n", false),
            new Grader.TestCase("an-1", "9 8\n", "8 9\n", true),
            new Grader.TestCase("an-2", "5\n", "5\n", true));

    /** {@code lines(khoá, job)} sinh các dòng runner "in ra". */
    static Grader grader(BiFunction<String, JsonNode, List<String>> lines, boolean killed) {
        ScriptRunner fake = (script, dir, stdin, deadline) -> {
            JsonNode job = JsonPayload.MAPPER.readTree(stdin);
            return new ScriptRunner.Result(lines.apply(job.get("k").asText(), job), 0, "", killed, 10);
        };
        return new Grader(fake, (job, onLine) -> {
            throw new AssertionError("bài không có luat.py thì không chạy trace");
        });
    }

    static String test(String k, String id, String status, String stdout, String error) {
        var n = JsonPayload.MAPPER
                .createObjectNode()
                .put("k", k)
                .put("t", "test")
                .put("id", id);
        n.put("status", status).put("time_ms", 30).put("stdout", stdout).put("error", error);
        return n.toString();
    }

    @Test
    void dungHet_ok() throws Exception {
        Grader g = grader(
                (k, job) -> List.of(
                        test(k, "nho-1", "OK", "1 2 3\n", null),
                        test(k, "an-1", "OK", "8 9  \n\n", null),
                        test(k, "an-2", "OK", "5", null)),
                false);
        Grader.Result r = g.grade(BAI, "code", TESTS, null);
        assertEquals(Verdict.OK, r.verdict());
        assertNull(r.failedTest());
    }

    @Test
    void saiMotTestAn_wrong_khongKemLoiCuaTestAn() throws Exception {
        Grader g = grader(
                (k, job) -> List.of(
                        test(k, "nho-1", "OK", "1 2 3\n", null),
                        test(k, "an-1", "RUNTIME_ERROR", "", "Exception: 9 8"),
                        test(k, "an-2", "OK", "5\n", null)),
                false);
        Grader.Result r = g.grade(BAI, "code", TESTS, null);
        assertEquals(Verdict.RUNTIME_ERROR, r.verdict());
        assertEquals("an-1", r.failedTest());
        assertNull(r.tests().get(1).error(), "test ẩn không được mang thông báo lỗi");
        assertTrue(r.message().contains("test ẩn an-1"));
    }

    @Test
    void dongKhongMangKhoa_biBo_testThieuKetQuaLaLoi() throws Exception {
        // Code học viên in giả kết quả "đúng" với khoá đoán bừa; không có dòng thật nào
        Grader g = grader(
                (k, job) -> {
                    List<String> out = new ArrayList<>();
                    for (JsonNode t : job.get("tests")) {
                        out.add(test("khoa-gia", t.get("id").asText(), "OK", "8 9\n", null));
                    }
                    return out;
                },
                false);
        Grader.Result r = g.grade(BAI, "code", TESTS, null);
        assertEquals(Verdict.RUNTIME_ERROR, r.verdict());
        r.tests().forEach(t -> assertEquals(Verdict.RUNTIME_ERROR, t.verdict()));
    }

    @Test
    void quaGio_timeLimit() throws Exception {
        Grader g = grader((k, job) -> List.of(test(k, "nho-1", "OK", "1 2 3\n", null)), true);
        Grader.Result r = g.grade(BAI, "code", TESTS, null);
        assertEquals(Verdict.TIME_LIMIT, r.verdict());
        assertEquals("an-1", r.failedTest());
    }

    @Test
    void chayXongNhungLauHonGioiHan_timeLimit() throws Exception {
        Grader g = grader(
                (k, job) -> List.of(JsonPayload.MAPPER
                        .createObjectNode()
                        .put("k", k)
                        .put("t", "test")
                        .put("id", "nho-1")
                        .put("status", "OK")
                        .put("time_ms", 1_400)
                        .put("stdout", "1 2 3\n")
                        .toString()),
                false);
        assertEquals(
                Verdict.TIME_LIMIT,
                g.grade(BAI, "code", TESTS, null).tests().get(0).verdict());
    }

    @Test
    void hamCam_forbiddenCall_khongCoTestNao() throws Exception {
        Grader g = grader(
                (k, job) -> {
                    assertEquals("sorted", job.get("cam").get("cam_dung").get(0).asText());
                    return List.of(JsonPayload.MAPPER
                            .createObjectNode()
                            .put("k", k)
                            .put("t", "cam")
                            .set("loi", JsonPayload.MAPPER.valueToTree(List.of("Không được dùng sorted (dòng 2)")))
                            .toString());
                },
                false);
        Grader.Result r = g.grade(BAI, "code", TESTS, null);
        assertEquals(Verdict.FORBIDDEN_CALL, r.verdict());
        assertTrue(r.message().contains("sorted"));
        assertTrue(r.tests().isEmpty());
    }

    @Test
    void jobGuiChoRunnerKhongCoDapAn() throws Exception {
        Grader g = grader(
                (k, job) -> {
                    String s = job.toString();
                    assertTrue(!s.contains("8 9") && !s.contains("1 2 3"), "đáp án không được vào container: " + s);
                    return List.of();
                },
                false);
        g.grade(BAI, "code", TESTS, null);
    }
}
