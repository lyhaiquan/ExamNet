package labcast.server.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.TestClient;
import labcast.server.classroom.ClassRepository;
import labcast.server.store.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tự đổi mật khẩu, giáo viên/quản trị đặt lại mật khẩu, cờ cứu tài khoản {@code --dat-lai-mat-khau}. */
class PasswordTest {
    private Database db;
    private LabCastServer server;

    @BeforeEach
    void batServer() throws Exception {
        db = Database.open(":memory:");
        var cfg = ServerConfig.forTest()
                .withLop("Lớp A")
                .withTaoGiaoVien("gv:mk-gv:Cô A")
                .withTaoAdmin("ad:mk-ad:Quản trị");
        server = new LabCastServer(cfg, db, List.of(new AccountModule()));
        server.start();
        UserRepository users = new UserRepository(db);
        ClassRepository classes = new ClassRepository(db);
        long gv = users.byUsername("gv").orElseThrow().id();
        classes.enroll(classes.classOf(gv), users.create("sv1", "Học viên lớp A", Role.STUDENT, "mk-sv1"));
        // giáo viên thứ hai, lớp khác, có học viên riêng
        long gv2 = users.create("gv2", "Thầy B", Role.TEACHER, "mk-gv2");
        classes.enroll(classes.ensure("Lớp B", gv2), users.create("sv2", "Học viên lớp B", Role.STUDENT, "mk-sv2"));
    }

    @AfterEach
    void tatServer() throws Exception {
        server.close();
        db.close();
    }

    private static Frame send(TestClient c, MessageType t, String a, String b) throws Exception {
        c.send(t, new PayloadWriter().str(a).str(b).toBytes());
        return c.readUntil(MessageType.ADMIN_OK, MessageType.ERROR);
    }

    private static ErrorCode code(Frame f) throws Exception {
        assertEquals(MessageType.ERROR, f.type());
        return ErrorMessage.from(f).code();
    }

    private boolean canLogin(String user, String pwd) throws Exception {
        try (var c = TestClient.connect(server.port())) {
            return c.login(user, pwd).type() == MessageType.AUTH_OK;
        }
    }

    @Test
    void tuDoiMatKhau_dungMatKhauCu_thiMatKhauMoiDangNhapDuoc() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.login("sv1", "mk-sv1");
            assertEquals(
                    MessageType.ADMIN_OK,
                    send(c, MessageType.PASSWORD_CHANGE, "mk-sv1", "moi-1234").type());
        }
        assertTrue(canLogin("sv1", "moi-1234"));
        assertEquals(false, canLogin("sv1", "mk-sv1"));
    }

    @Test
    void tuDoiMatKhau_saiMatKhauCu_hoacMatKhauMoiQuaNgan_biTuChoi() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.login("sv1", "mk-sv1");
            assertEquals(ErrorCode.FORBIDDEN, code(send(c, MessageType.PASSWORD_CHANGE, "sai", "moi-1234")));
            assertEquals(ErrorCode.BAD_FRAME, code(send(c, MessageType.PASSWORD_CHANGE, "mk-sv1", "12")));
        }
        assertTrue(canLogin("sv1", "mk-sv1"));
    }

    @Test
    void chuaDangNhap_khongDoiDuoc() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.send(
                    MessageType.HELLO,
                    new PayloadWriter().str("test").str("may-test").toBytes());
            c.readUntil(MessageType.HELLO_ACK);
            assertEquals(ErrorCode.AUTH_REQUIRED, code(send(c, MessageType.PASSWORD_CHANGE, "x", "moi-1234")));
        }
    }

    @Test
    void giaoVien_datLaiChoHocVienLopMinh_khongDuocChoLopKhacHayGiaoVienQuanTri() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.login("gv", "mk-gv");
            assertEquals(
                    MessageType.ADMIN_OK,
                    send(c, MessageType.PASSWORD_RESET, "sv1", "dat-lai-1").type());
            assertEquals(ErrorCode.FORBIDDEN, code(send(c, MessageType.PASSWORD_RESET, "sv2", "dat-lai-1")));
            assertEquals(ErrorCode.FORBIDDEN, code(send(c, MessageType.PASSWORD_RESET, "gv2", "dat-lai-1")));
            assertEquals(ErrorCode.FORBIDDEN, code(send(c, MessageType.PASSWORD_RESET, "ad", "dat-lai-1")));
            assertEquals(ErrorCode.NOT_FOUND, code(send(c, MessageType.PASSWORD_RESET, "khong-co", "dat-lai-1")));
        }
        assertTrue(canLogin("sv1", "dat-lai-1"));
        assertTrue(canLogin("sv2", "mk-sv2"));
        assertTrue(canLogin("gv2", "mk-gv2"));
    }

    @Test
    void quanTri_datLaiChoMoiNguoi_hocVienThiKhong() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.login("ad", "mk-ad");
            assertEquals(
                    MessageType.ADMIN_OK,
                    send(c, MessageType.PASSWORD_RESET, "gv2", "moi-gv2").type());
            assertEquals(
                    MessageType.ADMIN_OK,
                    send(c, MessageType.PASSWORD_RESET, "sv2", "moi-sv2").type());
        }
        assertTrue(canLogin("gv2", "moi-gv2"));
        assertTrue(canLogin("sv2", "moi-sv2"));
        try (var c = TestClient.connect(server.port())) {
            c.login("sv1", "mk-sv1");
            assertEquals(ErrorCode.FORBIDDEN, code(send(c, MessageType.PASSWORD_RESET, "sv2", "hack-1234")));
        }
    }

    /** Đăng nhập; trả cờ "phải đổi mật khẩu" trong AUTH_OK. */
    private boolean mustChange(String user, String pwd) throws Exception {
        try (var c = TestClient.connect(server.port())) {
            Frame ok = c.login(user, pwd);
            assertEquals(MessageType.AUTH_OK, ok.type());
            var r = ok.reader();
            r.str();
            r.str();
            r.i64();
            r.str();
            r.i64();
            return r.bool();
        }
    }

    private Frame forgot(String user) throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.send(
                    MessageType.HELLO,
                    new PayloadWriter().str("test").str("may-test").toBytes());
            c.readUntil(MessageType.HELLO_ACK);
            c.send(MessageType.PASSWORD_FORGOT, new PayloadWriter().str(user).toBytes());
            return c.readUntil(MessageType.ADMIN_OK, MessageType.ERROR);
        }
    }

    private List<String> requests(String user, String pwd) throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.login(user, pwd);
            c.send(MessageType.PASSWORD_REQUESTS_REQ, new byte[0]);
            var json = labcast.common.protocol.JsonPayload.tree(c.readUntil(MessageType.PASSWORD_REQUESTS_DATA));
            List<String> out = new java.util.ArrayList<>();
            json.path("requests").forEach(n -> out.add(n.path("username").asText()));
            return out;
        }
    }

    @Test
    void matKhauTam_dangNhapBiBatDoi_doiXongThiHet() throws Exception {
        assertEquals(false, mustChange("sv1", "mk-sv1"));
        try (var c = TestClient.connect(server.port())) {
            c.login("gv", "mk-gv");
            send(c, MessageType.PASSWORD_RESET, "sv1", "123456");
        }
        assertEquals(true, mustChange("sv1", "123456"));
        try (var c = TestClient.connect(server.port())) {
            c.login("sv1", "123456");
            // giữ nguyên mật khẩu tạm thì không được
            assertEquals(ErrorCode.BAD_FRAME, code(send(c, MessageType.PASSWORD_CHANGE, "123456", "123456")));
            assertEquals(
                    MessageType.ADMIN_OK,
                    send(c, MessageType.PASSWORD_CHANGE, "123456", "meo-con").type());
        }
        assertEquals(false, mustChange("sv1", "meo-con"));
    }

    @Test
    void quenMatKhau_khongCanDangNhap_traLoiGiongNhau_giaoVienThayHocVienLopMinh() throws Exception {
        Frame co = forgot("sv1");
        Frame khong = forgot("khong-co-ai");
        assertEquals(MessageType.ADMIN_OK, co.type());
        assertEquals(MessageType.ADMIN_OK, khong.type());
        assertEquals(co.reader().str(), khong.reader().str(), "không dò được tên đăng nhập có thật hay không");
        forgot("sv1"); // gửi lại: vẫn một yêu cầu
        forgot("sv2");
        forgot("gv2");

        assertEquals(List.of("sv1"), requests("gv", "mk-gv"));
        assertEquals(List.of("sv2"), requests("gv2", "mk-gv2"));
        assertEquals(java.util.Set.of("sv1", "sv2", "gv2"), java.util.Set.copyOf(requests("ad", "mk-ad")));

        try (var c = TestClient.connect(server.port())) {
            c.login("gv", "mk-gv");
            send(c, MessageType.PASSWORD_RESET, "sv1", "tam-1234");
        }
        assertEquals(List.of(), requests("gv", "mk-gv"), "đặt lại xong thì yêu cầu biến mất");
        try (var c = TestClient.connect(server.port())) {
            c.login("sv1", "tam-1234");
            c.send(MessageType.PASSWORD_REQUESTS_REQ, new byte[0]);
            assertEquals(ErrorCode.FORBIDDEN, code(c.readUntil(MessageType.ERROR)));
        }
    }

    @Test
    void nhapCsv_matKhauTrongFileLaMatKhauTam() throws Exception {
        long lop = new ClassRepository(db)
                .classOf(new UserRepository(db).byUsername("gv").orElseThrow().id());
        new RosterImporter(new UserRepository(db), new ClassRepository(db))
                .importCsv("sv9,Học viên Chín,mk-sv9\n", lop);
        assertEquals(true, mustChange("sv9", "mk-sv9"));
    }

    @Test
    void kiemTraMatKhauMoi() {
        assertNull(AccountModule.checkNewPassword("abcd"));
        assertTrue(AccountModule.checkNewPassword("abc") != null);
        assertTrue(AccountModule.checkNewPassword("    ") != null);
        assertTrue(AccountModule.checkNewPassword("x".repeat(AccountModule.MAX_PASSWORD + 1)) != null);
    }

    @Test
    void coCuuTaiKhoan_tachThamSo_vaDatLaiTrongFileDatabase(@TempDir Path dir) throws Exception {
        var split = PasswordRescue.split(
                new String[] {"--cau-hinh", "lop.conf", PasswordRescue.FLAG, "ad:moi-ad1", "--khong-sandbox"});
        assertEquals("ad:moi-ad1", split.spec());
        assertEquals(List.of("--cau-hinh", "lop.conf", "--khong-sandbox"), List.of(split.rest()));
        assertThrows(IllegalArgumentException.class, () -> PasswordRescue.split(new String[] {PasswordRescue.FLAG}));

        String file = dir.resolve("lop.db").toString();
        try (Database d = Database.open(file)) {
            new UserRepository(d).create("ad", "Quản trị", Role.ADMIN, "quen-mat");
        }
        try (Database d = Database.open(file)) {
            assertTrue(PasswordRescue.run("ad:moi-ad1", d).contains("ad"));
            assertThrows(IllegalArgumentException.class, () -> PasswordRescue.run("khong-co:moi-ad1", d));
            assertThrows(IllegalArgumentException.class, () -> PasswordRescue.run("ad:12", d));
            assertThrows(IllegalArgumentException.class, () -> PasswordRescue.run("khong-dau-hai-cham", d));
        }
        try (Database d = Database.open(file)) {
            User ad = new UserRepository(d).byUsername("ad").orElseThrow();
            assertTrue(PasswordHasher.verify("moi-ad1", ad.password()));
        }
        assertTrue(Files.exists(Path.of(file)));
    }
}
