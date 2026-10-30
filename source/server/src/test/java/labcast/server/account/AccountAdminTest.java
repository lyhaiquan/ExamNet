package labcast.server.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.TestClient;
import labcast.server.classroom.ClassRepository;
import labcast.server.store.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Quản trị tạo, sửa, khoá tài khoản giáo viên và học viên (AccountAdminModule). */
class AccountAdminTest {
    private Database db;
    private LabCastServer server;
    private long lopA;

    @BeforeEach
    void batServer() throws Exception {
        db = Database.open(":memory:");
        var cfg = ServerConfig.forTest()
                .withLop("Lớp A")
                .withTaoGiaoVien("gv:mk-gv:Cô A")
                .withTaoAdmin("ad:mk-ad:Quản trị");
        server = new LabCastServer(cfg, db, List.of(new AccountModule(), new AccountAdminModule()));
        server.start();
        lopA = new ClassRepository(db)
                .classOf(new UserRepository(db).byUsername("gv").orElseThrow().id());
    }

    @AfterEach
    void tatServer() throws Exception {
        server.close();
        db.close();
    }

    private static Frame save(TestClient c, Map<String, Object> body) throws Exception {
        c.send(JsonPayload.frame(MessageType.ACCOUNT_SAVE, body));
        return c.readUntil(MessageType.ADMIN_OK, MessageType.ERROR);
    }

    private static Frame lock(TestClient c, long id, boolean locked) throws Exception {
        c.send(
                MessageType.ACCOUNT_LOCK,
                new PayloadWriter().i64(id).bool(locked).toBytes());
        return c.readUntil(MessageType.ADMIN_OK, MessageType.ERROR);
    }

    private static ErrorCode code(Frame f) throws Exception {
        assertEquals(MessageType.ERROR, f.type(), "phải bị từ chối");
        return ErrorMessage.from(f).code();
    }

    private Frame login(String user, String pwd) throws Exception {
        try (var c = TestClient.connect(server.port())) {
            return c.login(user, pwd);
        }
    }

    private long id(String username) throws Exception {
        return new UserRepository(db).byUsername(username).orElseThrow().id();
    }

    @Test
    void giaoVien_khongDungDuocQuanLyTaiKhoan() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.login("gv", "mk-gv");
            c.send(MessageType.ACCOUNT_LIST_REQ, new byte[0]);
            assertEquals(ErrorCode.FORBIDDEN, code(c.readUntil(MessageType.ERROR)));
            assertEquals(
                    ErrorCode.FORBIDDEN,
                    code(save(c, Map.of("username", "sv9", "fullName", "A", "role", "STUDENT", "password", "1234"))));
            assertEquals(ErrorCode.FORBIDDEN, code(lock(c, 1, true)));
        }
    }

    @Test
    void quanTriTaoGiaoVien_coLopRieng_matKhauTam() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.login("ad", "mk-ad");
            Frame ok = save(
                    c,
                    Map.of(
                            "username", "gv2",
                            "fullName", "Thầy B",
                            "role", "TEACHER",
                            "password", "482915",
                            "newClassName", "Lớp B"));
            assertEquals(MessageType.ADMIN_OK, ok.type());
        }
        Frame auth = login("gv2", "482915");
        assertEquals(MessageType.AUTH_OK, auth.type());
        PayloadReader r = auth.reader();
        r.str();
        assertEquals("TEACHER", r.str());
        r.i64();
        r.str();
        assertTrue(r.i64() > 0, "giáo viên có lớp");
        assertTrue(r.bool(), "mật khẩu tạm: phải tự đặt mật khẩu mới");
    }

    @Test
    void quanTriTaoHocVien_vaoLopDaChon_roiChuyenLop() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.login("ad", "mk-ad");
            assertEquals(
                    MessageType.ADMIN_OK,
                    save(
                                    c,
                                    Map.of(
                                            "username", "sv1",
                                            "fullName", "Học viên Một",
                                            "role", "STUDENT",
                                            "password", "123456",
                                            "classId", lopA))
                            .type());
            assertEquals(lopA, new ClassRepository(db).classOf(id("sv1")));

            save(
                    c,
                    Map.of(
                            "username",
                            "gv2",
                            "fullName",
                            "Thầy B",
                            "role",
                            "TEACHER",
                            "password",
                            "1234",
                            "newClassName",
                            "Lớp B"));
            long lopB = new ClassRepository(db).classOf(id("gv2"));
            assertEquals(
                    MessageType.ADMIN_OK,
                    save(c, Map.of("id", id("sv1"), "fullName", "Học viên Một (đổi tên)", "classId", lopB))
                            .type());
            assertEquals(lopB, new ClassRepository(db).classOf(id("sv1")));
            assertEquals(
                    "Học viên Một (đổi tên)",
                    new UserRepository(db).byUsername("sv1").orElseThrow().fullName());

            c.send(MessageType.ACCOUNT_LIST_REQ, new byte[0]);
            JsonNode list = JsonPayload.tree(c.readUntil(MessageType.ACCOUNT_LIST_DATA));
            assertEquals(4, list.path("accounts").size());
            assertEquals(2, list.path("classes").size());
            for (JsonNode a : list.path("accounts")) {
                assertTrue(a.path("pwdHash").isMissingNode() && a.path("salt").isMissingNode());
            }
        }
    }

    @Test
    void tenTrung_kyTuLa_taoQuanTri_lopThieu_biTuChoi() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.login("ad", "mk-ad");
            Map<String, Object> base = Map.of("fullName", "X", "role", "STUDENT", "password", "1234", "classId", lopA);
            assertEquals(ErrorCode.BAD_FRAME, code(save(c, with(base, "username", "GV"))), "trùng gv khác hoa thường");
            assertEquals(ErrorCode.BAD_FRAME, code(save(c, with(base, "username", "sv:1"))));
            assertEquals(ErrorCode.BAD_FRAME, code(save(c, with(base, "username", "sv,1"))));
            assertEquals(
                    ErrorCode.BAD_FRAME,
                    code(save(c, Map.of("username", "ad2", "fullName", "X", "role", "ADMIN", "password", "1234"))));
            assertEquals(
                    ErrorCode.BAD_FRAME,
                    code(save(c, Map.of("username", "sv5", "fullName", "X", "role", "STUDENT", "password", "1234"))),
                    "học viên phải có lớp");
            assertEquals(ErrorCode.BAD_FRAME, code(save(c, with(base, "username", "sv6", "password", "12"))));
        }
    }

    @Test
    void khoaTaiKhoan_dangNhapBiChan_phienDangMoBiCat_moKhoaThiVaoLai() throws Exception {
        try (var gv = TestClient.connect(server.port())) {
            Frame ok = gv.login("gv", "mk-gv");
            String token = ok.reader().str();
            try (var c = TestClient.connect(server.port())) {
                c.login("ad", "mk-ad");
                assertEquals(MessageType.ADMIN_OK, lock(c, id("gv"), true).type());
                assertEquals(ErrorCode.FORBIDDEN, code(lock(c, id("ad"), true)), "không khoá quản trị");
            }
            // phiên cũ không hồi sinh được bằng token
            try (var again = TestClient.connect(server.port())) {
                again.send(
                        MessageType.RESUME_REQ,
                        new PayloadWriter().str(token).str("").i64(-1).toBytes());
                assertEquals(
                        false,
                        again.readUntil(MessageType.RESUME_STATE).reader().bool());
            }
        }
        Frame locked = login("gv", "mk-gv");
        assertEquals(MessageType.AUTH_FAIL, locked.type());
        assertEquals(AccountModule.LOCKED_MESSAGE, locked.reader().str());
        Frame wrong = login("gv", "sai");
        assertEquals(AccountModule.AUTH_FAIL_MESSAGE, wrong.reader().str(), "sai mật khẩu không lộ là bị khoá");

        try (var c = TestClient.connect(server.port())) {
            c.login("ad", "mk-ad");
            lock(c, id("gv"), false);
        }
        assertEquals(MessageType.AUTH_OK, login("gv", "mk-gv").type());
    }

    private static Map<String, Object> with(Map<String, Object> base, Object... kv) {
        Map<String, Object> m = new java.util.HashMap<>(base);
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }
}
