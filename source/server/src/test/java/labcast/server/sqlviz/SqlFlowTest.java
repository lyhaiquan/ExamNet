package labcast.server.sqlviz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import labcast.common.protocol.CodeDelta;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.TestClient;
import labcast.server.TestPaths;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.journal.JournalModule;
import labcast.server.lesson.LessonModule;
import labcast.server.practice.SubmitModule;
import labcast.server.run.LocalPythonSandbox;
import labcast.server.run.RunModule;
import labcast.server.store.Database;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Bài SQL đi trọn đường thật: socket → mở bài → chạy thử (trace các bước) → nộp → chấm trên dữ liệu mẫu và
 * 3 database ẩn (sinh bằng sinh_du_lieu.py qua Python cục bộ, chế độ không sandbox).
 */
class SqlFlowTest {
    static final String NHOM = "sql.gom-nhom.diem-trung-binh-lop";
    static final String NULL_BAI = "sql.null-case.chua-co-lop";

    static Database db;
    static LabCastServer server;
    static UserRepository users;

    @BeforeAll
    static void bat() throws Exception {
        assumeTrue(LocalPythonSandbox.detectPython() != null, "máy không có Python 3");
        db = Database.open(":memory:");
        server = new LabCastServer(
                ServerConfig.forTest()
                        .withContent(TestPaths.repoRoot().resolve("content"))
                        .withKhongSandbox(true),
                db,
                List.of(
                        new AccountModule(),
                        new LessonModule(),
                        new RunModule(),
                        new JournalModule(),
                        new SubmitModule()));
        server.start();
        users = new UserRepository(db);
    }

    @AfterAll
    static void tat() throws Exception {
        if (server != null) {
            server.close();
            db.close();
        }
    }

    /** Học viên mới mở bài {@code bai}, thay khung bằng {@code sql}; trả kết nối và LESSON_DATA. */
    static TestClient hocVien(String user, String bai, String sql, JsonNode[] lesson) throws Exception {
        users.create(user, user, Role.STUDENT, "1");
        TestClient c = TestClient.connect(server.port());
        assertEquals(MessageType.AUTH_OK, c.login(user, "1").type());
        c.send(MessageType.LESSON_FETCH, new PayloadWriter().str(bai).toBytes());
        JsonNode l = JsonPayload.tree(c.readUntil(MessageType.LESSON_DATA));
        if (lesson != null) {
            lesson[0] = l;
        }
        String khung = l.get("code").asText();
        c.send(new CodeDelta(bai, 1, 0, khung.length(), sql, 0).toFrame());
        c.readUntil(MessageType.CODE_ACK);
        return c;
    }

    record Run(JsonNode result, List<JsonNode> trace) {}

    static Run chay(TestClient c, String bai) throws Exception {
        c.send(MessageType.RUN_REQ, new PayloadWriter().str(bai).i64(1).str("").toBytes());
        List<JsonNode> trace = new ArrayList<>();
        while (true) {
            Frame f = c.readUntil(MessageType.TRACE_CHUNK, MessageType.RUN_RESULT, MessageType.ERROR);
            if (f.type() == MessageType.ERROR) {
                throw new AssertionError("ERROR: " + ErrorMessage.from(f).message());
            }
            if (f.type() == MessageType.RUN_RESULT) {
                return new Run(JsonPayload.tree(f), trace);
            }
            PayloadReader r = f.reader();
            r.i64();
            r.i32();
            r.bool();
            for (String line : new String(r.blob(), StandardCharsets.UTF_8).split("\n")) {
                if (!line.isBlank()) {
                    trace.add(JsonPayload.MAPPER.readTree(line));
                }
            }
        }
    }

    static JsonNode nop(TestClient c, String bai) throws Exception {
        c.send(MessageType.SUBMIT_REQ, new PayloadWriter().str(bai).i64(1).toBytes());
        Frame f = c.readUntil(MessageType.SUBMIT_RESULT, MessageType.ERROR);
        if (f.type() == MessageType.ERROR) {
            throw new AssertionError("ERROR: " + ErrorMessage.from(f).message());
        }
        return JsonPayload.tree(f);
    }

    static long count(List<JsonNode> trace, String t) {
        return trace.stream().filter(e -> e.path("t").asText().equals(t)).count();
    }

    @Test
    void moBaiGiangGiai_coTraceTungBuoc_vaKhungSql() throws Exception {
        JsonNode[] l = new JsonNode[1];
        try (var c = hocVien("sv-mo", NHOM, "SELECT 1", l)) {
            JsonNode lesson = l[0];
            assertEquals(
                    "bang-sql", lesson.path("khungNhin").get(0).path("kieu").asText());
            assertEquals("du-lieu-mau", lesson.path("testXem").get(0).path("id").asText());
            assertTrue(lesson.path("code").asText().contains("SELECT"), "khung.sql");
            String trace = lesson.path("trace").toString();
            for (String t : List.of("sql.table", "sql.join", "sql.mark", "sql.group", "sql.agg", "sql.result")) {
                assertTrue(trace.contains(t), "trace giảng giải thiếu " + t);
            }
            assertFalse(trace.contains("Chỉ hiện kết quả cuối"), "đáp án phải tách được đủ bước");
        }
    }

    @Test
    void chayThuCauDung_dungTrenDuLieuMau_traCacBuoc() throws Exception {
        String sql = "SELECT l.ten_lop, AVG(k.diem) FROM ket_qua k JOIN sinh_vien s ON k.sinh_vien_id = s.id"
                + " JOIN lop l ON s.lop_id = l.id WHERE k.lan_thi = 1 GROUP BY l.ten_lop HAVING AVG(k.diem) > 7";
        try (var c = hocVien("sv-chay", NHOM, sql, null)) {
            Run r = chay(c, NHOM);
            assertTrue(r.result().path("ok").asBoolean());
            assertEquals("OK", r.result().path("tests").get(0).path("verdict").asText());
            assertTrue(
                    r.result().path("stdout").asText().contains("D22CQCN02"),
                    r.result().toString());
            assertEquals(2, count(r.trace(), "sql.join"));
            assertEquals(1, count(r.trace(), "sql.group"));
            assertEquals("hdr", r.trace().get(0).path("t").asText());
        }
    }

    @Test
    void lamTronDiem_khacDapAn() throws Exception {
        String sql = "SELECT l.ten_lop, ROUND(AVG(k.diem), 1) FROM ket_qua k JOIN sinh_vien s ON k.sinh_vien_id = s.id"
                + " JOIN lop l ON s.lop_id = l.id WHERE k.lan_thi = 1 GROUP BY l.ten_lop HAVING AVG(k.diem) > 7";
        try (var c = hocVien("sv-tron", NHOM, sql, null)) {
            assertEquals(
                    "WRONG",
                    chay(c, NHOM).result().path("tests").get(0).path("verdict").asText());
        }
    }

    @Test
    void cauSaiCuPhap_vaCauXoaBang_baoLoiChoHocVien() throws Exception {
        try (var c = hocVien("sv-loi", NULL_BAI, "SELECT ho_ten FORM sinh_vien", null)) {
            Run r = chay(c, NULL_BAI);
            assertFalse(r.result().path("ok").asBoolean());
            assertEquals(
                    "RUNTIME_ERROR",
                    r.result().path("tests").get(0).path("verdict").asText());
            assertFalse(r.result().path("error").asText().isBlank());
        }
        try (var c = hocVien("sv-drop", NULL_BAI, "DROP TABLE sinh_vien", null)) {
            Run r = chay(c, NULL_BAI);
            assertTrue(
                    r.result().path("error").asText().contains("Không cho phép"),
                    r.result().toString());
        }
    }

    @Test
    void soSanhBangNull_saiNgayKhiChayThu() throws Exception {
        try (var c = hocVien("sv-eq-null", NULL_BAI, "SELECT id, ho_ten FROM sinh_vien WHERE lop_id = NULL", null)) {
            Run r = chay(c, NULL_BAI);
            assertEquals(
                    "WRONG", r.result().path("tests").get(0).path("verdict").asText());
            assertTrue(count(r.trace(), "sql.mark") > 0, "vẫn có animation để thấy mọi dòng bị ✗");
        }
    }

    @Test
    void inSanDongCuaDuLieuMau_chayThuDung_nopThiSaiODuLieuAn() throws Exception {
        String sql = "SELECT 9 AS id, 'Đỗ Văn Minh' AS ho_ten UNION ALL SELECT 10, 'Ngô Thị Ngọc'";
        try (var c = hocVien("sv-in-san", NULL_BAI, sql, null)) {
            assertEquals(
                    "OK",
                    chay(c, NULL_BAI)
                            .result()
                            .path("tests")
                            .get(0)
                            .path("verdict")
                            .asText());
            JsonNode r = nop(c, NULL_BAI);
            assertEquals("WRONG", r.path("verdict").asText(), r.toString());
            assertTrue(r.path("failedTest").asText().startsWith("an-"), r.toString());
            assertTrue(r.path("message").asText().contains("dữ liệu ẩn"), r.toString());
            assertEquals("OK", r.path("tests").get(0).path("verdict").asText(), "dữ liệu mẫu thì qua");
        }
    }

    @Test
    void lamDung_qua4BoDuLieu_dat100Diem() throws Exception {
        try (var c = hocVien("sv-dung", NULL_BAI, "SELECT id, ho_ten FROM sinh_vien WHERE lop_id IS NULL;", null)) {
            JsonNode r = nop(c, NULL_BAI);
            assertEquals("OK", r.path("verdict").asText(), r.toString());
            assertEquals(100, r.path("score").asInt());
            assertEquals(4, r.path("tests").size(), "dữ liệu mẫu + 3 database ẩn");
            assertFalse(r.path("tests").get(0).path("hidden").asBoolean());
            assertTrue(r.path("tests").get(3).path("hidden").asBoolean());
        }
    }
}
