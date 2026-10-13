package labcast.server.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import labcast.common.protocol.CodeDelta;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
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
import labcast.server.lesson.LessonPreparer;
import labcast.server.lesson.LessonRepository;
import labcast.server.run.LocalPythonSandbox;
import labcast.server.run.RunModule;
import labcast.server.store.Database;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Các kiểu gian lận học viên hay thử, đi trọn đường thật: socket → nộp → runner (Python cục bộ, chế độ không
 * sandbox) → chấm → tiến độ. Bài nổi bọt có cam_dung [sorted, sort], 10 test ẩn và luat.py.
 */
class SubmitFlowTest {
    static final String BAI = "dsa.sap-xep.noi-bot";

    static final String DUNG =
            """
            import viz

            def sap_xep(a):
                for i in range(len(a) - 1):
                    for j in range(len(a) - 1 - i):
                        if a[j] > a[j + 1]:
                            a[j], a[j + 1] = a[j + 1], a[j]

            a = viz.Mang(list(map(int, input().split())), ten="a")
            sap_xep(a)
            print(*a)
            """;

    static final String DUNG_SORTED =
            """
            a = list(map(int, input().split()))
            print(*sorted(a))
            """;

    /** Chỉ in đúng đáp án của test nhìn thấy (nho-1: 3 2 1 → 1 2 3). */
    static final String IN_SAN_DAP_AN = """
            input()
            print("1 2 3")
            """;

    static final String SAP_XEP_CHON =
            """
            import viz
            a = viz.Mang(list(map(int, input().split())), ten="a")
            n = len(a)
            for i in range(n):
                m = i
                for j in range(i + 1, n):
                    if a[j] < a[m]:
                        m = j
                a[i], a[m] = a[m], a[i]
            print(*a)
            """;

    /** Test nhỏ thì làm đúng; test lớn (ẩn) thì ném lỗi kèm input, mong lỗi gửi input test ẩn về máy mình. */
    static final String MOC_INPUT_TEST_AN =
            """
            import viz
            s = input()
            if len(s.split()) > 20:
                raise Exception("INPUT_AN:" + s)
            a = viz.Mang(list(map(int, s.split())), ten="a")
            for i in range(len(a) - 1):
                for j in range(len(a) - 1 - i):
                    if a[j] > a[j + 1]:
                        a[j], a[j + 1] = a[j + 1], a[j]
            print(*a)
            """;

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

    /** Đăng nhập một học viên mới, thay khung code bằng {@code code} (seq 1). */
    static TestClient hocVien(String user, String code) throws Exception {
        return hocVien(user, BAI, code);
    }

    static TestClient hocVien(String user, String bai, String code) throws Exception {
        users.create(user, user, Role.STUDENT, "1");
        TestClient c = TestClient.connect(server.port());
        assertEquals(MessageType.AUTH_OK, c.login(user, "1").type());
        c.send(MessageType.LESSON_FETCH, new PayloadWriter().str(bai).toBytes());
        String khung = JsonPayload.tree(c.readUntil(MessageType.LESSON_DATA))
                .get("code")
                .asText();
        c.send(new CodeDelta(bai, 1, 0, khung.length(), code, 0).toFrame());
        c.readUntil(MessageType.CODE_ACK);
        return c;
    }

    static Frame nop(TestClient c) throws Exception {
        return nop(c, BAI);
    }

    static Frame nop(TestClient c, String bai) throws Exception {
        c.send(MessageType.SUBMIT_REQ, new PayloadWriter().str(bai).i64(1).toBytes());
        return c.readUntil(MessageType.SUBMIT_RESULT, MessageType.ERROR);
    }

    static JsonNode ketQua(Frame f) throws Exception {
        assertEquals(MessageType.SUBMIT_RESULT, f.type(), () -> {
            try {
                return "nhận ERROR: " + ErrorMessage.from(f).message();
            } catch (Exception e) {
                return f.type().toString();
            }
        });
        return JsonPayload.tree(f);
    }

    @Test
    void dungSorted_biBatHamCam_khongChayTestNao() throws Exception {
        try (var c = hocVien("sv-sorted", DUNG_SORTED)) {
            JsonNode r = ketQua(nop(c));
            assertEquals("FORBIDDEN_CALL", r.get("verdict").asText());
            assertTrue(r.get("message").asText().contains("sorted"), r.toString());
            assertEquals(0, r.get("tests").size());
        }
    }

    @Test
    void inSanDapAnTestNhinThay_saiOTestAn_vaPhaiChoMoiNopLai() throws Exception {
        try (var c = hocVien("sv-in-san", IN_SAN_DAP_AN)) {
            JsonNode r = ketQua(nop(c));
            assertEquals("WRONG", r.get("verdict").asText());
            assertTrue(r.get("failedTest").asText().length() > 0);
            JsonNode nho1 = r.get("tests").get(0);
            assertEquals("nho-1", nho1.get("id").asText());
            assertEquals("OK", nho1.get("verdict").asText(), "test nhìn thấy thì qua");
            assertEquals(15_000, r.get("cooldownMs").asLong());

            Frame lai = nop(c);
            assertEquals(MessageType.ERROR, lai.type());
            ErrorMessage e = ErrorMessage.from(lai);
            assertEquals(ErrorCode.COOLDOWN, e.code());
            assertTrue(e.retryAfterMs() > 10_000 && e.retryAfterMs() <= 15_000, "retryAfterMs " + e.retryAfterMs());
        }
    }

    @Test
    void sapXepChon_quaHetTestNhungSaiLuatTrace() throws Exception {
        try (var c = hocVien("sv-chon", SAP_XEP_CHON)) {
            JsonNode r = ketQua(nop(c));
            assertEquals("TRACE_RULE", r.get("verdict").asText(), r.toString());
            assertTrue(r.get("message").asText().contains("kề nhau"), r.toString());
            r.get("tests").forEach(t -> assertEquals("OK", t.get("verdict").asText()));
        }
    }

    @Test
    void lamDung_dat100Diem_thanhThao_vaKhongNopDonDuoc() throws Exception {
        try (var c = hocVien("sv-dung", DUNG)) {
            JsonNode r = ketQua(nop(c));
            assertEquals("OK", r.get("verdict").asText(), r.toString());
            assertEquals(100, r.get("score").asInt());
            assertTrue(r.get("mastered").asBoolean());
            assertEquals(0, r.get("cooldownMs").asLong());
            assertEquals(12, r.get("tests").size(), "2 test cố định + 10 test ẩn");

            // Gửi dồn hai lượt: lượt sau bị từ chối khi lượt trước chưa chấm xong
            c.send(MessageType.SUBMIT_REQ, new PayloadWriter().str(BAI).i64(1).toBytes());
            c.send(MessageType.SUBMIT_REQ, new PayloadWriter().str(BAI).i64(1).toBytes());
            Frame dau = c.readUntil(MessageType.SUBMIT_RESULT, MessageType.ERROR);
            assertEquals(MessageType.ERROR, dau.type());
            assertEquals(ErrorCode.RATE_LIMITED, ErrorMessage.from(dau).code());
            assertEquals(
                    100,
                    ketQua(c.readUntil(MessageType.SUBMIT_RESULT)).get("score").asInt());
        }
    }

    @Test
    void cogLayInputTestAnQuaLoi_khongLoGiVe() throws Exception {
        try (var c = hocVien("sv-moc", MOC_INPUT_TEST_AN)) {
            Frame f = nop(c);
            String raw = new String(f.payload(), StandardCharsets.UTF_8);
            JsonNode r = ketQua(f);
            assertEquals("RUNTIME_ERROR", r.get("verdict").asText());
            assertFalse(raw.contains("INPUT_AN"), "lỗi của test ẩn không được gửi về: " + raw);
            assertFalse(raw.contains("stdout"), raw);
            var repo = server.context().require(LessonRepository.class);
            var an = server.context()
                    .require(LessonPreparer.class)
                    .prepare(repo.get(BAI).orElseThrow())
                    .tests();
            for (var t : an) {
                assertFalse(raw.contains(t.input().strip()), "lộ input của " + t.id());
                assertFalse(raw.contains(t.output().strip()), "lộ output của " + t.id());
            }
            r.get("tests").forEach(t -> {
                if (t.get("hidden").asBoolean()) {
                    assertFalse(t.has("error"));
                }
            });
        }
    }

    static final String TONG = "dsa.quay-lui.tong-tap-con";

    /**
     * Luật của bài quay lui đếm lời gọi trên cây lời gọi: lượt chạy test tb khi chấm phải bật bộ ghi cây lời gọi
     * theo khung_nhin của bài, nếu không mọi bài làm đúng đều bị chấm TRACE_RULE vì "không đệ quy".
     */
    @Test
    void quayLui_dungCatNhanhThiDat_duyetHetThiSaiLuatTrace() throws Exception {
        String loiGiai = Files.readString(
                TestPaths.repoRoot().resolve("content/dsa/quay-lui/tong-tap-con/loi_giai.py"), StandardCharsets.UTF_8);
        try (var c = hocVien("sv-tong-dung", TONG, loiGiai)) {
            JsonNode r = ketQua(nop(c, TONG));
            assertEquals("OK", r.get("verdict").asText(), r.toString());
        }
        String duyetHet =
                """
                n, S = map(int, input().split())
                a = sorted(map(int, input().split()))

                def thu(i, tong):
                    if i == n:
                        return 1 if tong == S else 0
                    return thu(i + 1, tong + a[i]) + thu(i + 1, tong)

                print(thu(0, 0))
                """;
        try (var c = hocVien("sv-tong-het", TONG, duyetHet)) {
            JsonNode r = ketQua(nop(c, TONG));
            assertEquals("TRACE_RULE", r.get("verdict").asText(), r.toString());
            assertTrue(r.get("message").asText().contains("cắt nhánh"), r.toString());
        }
    }
}
