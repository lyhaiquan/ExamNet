package labcast.server.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import labcast.common.protocol.ByeReason;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.TestClient;
import labcast.server.store.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthFlowTest {
    private Database db;
    private LabCastServer server;

    @BeforeEach
    void batServer() throws Exception {
        db = Database.open(":memory:");
        var cfg = ServerConfig.forTest().withLop("Lớp D22").withTaoGiaoVien("gv:matkhau:Cô Giáo");
        server = new LabCastServer(cfg, db, List.of(new AccountModule()));
        server.start();
        new UserRepository(db).create("sv1", "Nguyễn Văn A", Role.STUDENT, "123");
    }

    @AfterEach
    void tatServer() throws Exception {
        server.close();
        db.close();
    }

    @Test
    void dangNhapDung_nhanAuthOkVoiVaiTro() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            Frame ok = c.login("sv1", "123");
            assertEquals(MessageType.AUTH_OK, ok.type());
            PayloadReader r = ok.reader();
            assertEquals(32, r.str().length()); // token 16 byte dạng hex
            assertEquals("STUDENT", r.str());
            r.i64();
            assertEquals("Nguyễn Văn A", r.str());
        }
    }

    @Test
    void helloAck_coTenLop() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.send(
                    MessageType.HELLO,
                    new PayloadWriter().str("1.0").str("may-1").toBytes());
            PayloadReader r = c.readUntil(MessageType.HELLO_ACK).reader();
            r.i64();
            assertEquals("Lớp D22", r.str());
        }
    }

    @Test
    void saiMatKhau_hoacSaiTen_cungMotThongBao() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            Frame a = c.login("sv1", "sai");
            Frame b = c.login("khong-co", "123");
            assertEquals(MessageType.AUTH_FAIL, a.type());
            assertEquals(MessageType.AUTH_FAIL, b.type());
            assertEquals(a.reader().str(), b.reader().str());
        }
    }

    @Test
    void dangNhapMayThuHai_mayDauNhanBye() throws Exception {
        try (var c1 = TestClient.connect(server.port());
                var c2 = TestClient.connect(server.port())) {
            assertEquals(MessageType.AUTH_OK, c1.login("sv1", "123").type());
            assertEquals(MessageType.AUTH_OK, c2.login("sv1", "123").type());
            Frame bye = c1.readUntil(MessageType.BYE);
            assertEquals(ByeReason.LOGGED_IN_ELSEWHERE.name(), bye.reader().str());
        }
    }

    @Test
    void resumeVoiTokenCu_tenKetNoiMoi() throws Exception {
        String token;
        try (var c1 = TestClient.connect(server.port())) {
            token = c1.login("sv1", "123").reader().str();
        }
        try (var c2 = TestClient.connect(server.port())) {
            c2.send(
                    MessageType.RESUME_REQ,
                    new PayloadWriter().str(token).str("").i64(-1).toBytes());
            PayloadReader r = c2.readUntil(MessageType.RESUME_STATE).reader();
            assertTrue(r.bool());
            assertEquals(-1, r.i64());
            // đã gắn phiên: gửi lệnh cần đăng nhập không còn bị AUTH_REQUIRED
            c2.send(
                    MessageType.ADMIN_ROSTER_IMPORT,
                    new PayloadWriter().str("x,y,z").toBytes());
            Frame err = c2.readUntil(MessageType.ERROR);
            assertEquals(ErrorCode.FORBIDDEN, ErrorMessage.from(err).code()); // học viên, không phải chưa đăng nhập
        }
    }

    @Test
    void tokenLa_resumeThatBai() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.send(
                    MessageType.RESUME_REQ,
                    new PayloadWriter().str("abc").str("").i64(-1).toBytes());
            assertEquals(false, c.readUntil(MessageType.RESUME_STATE).reader().bool());
        }
    }

    @Test
    void giaoVienKhongNhapDanhSachLop_taiKhoanDoQuanTriTao() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            Frame ok = c.login("gv", "matkhau");
            assertEquals(MessageType.AUTH_OK, ok.type());
            c.send(
                    MessageType.ADMIN_ROSTER_IMPORT,
                    new PayloadWriter().str("sv7,Học Viên Bảy,777\n").toBytes());
            Frame err = c.readUntil(MessageType.ADMIN_OK, MessageType.ERROR);
            assertEquals(ErrorCode.FORBIDDEN, ErrorMessage.from(err).code());
        }
        try (var c = TestClient.connect(server.port())) {
            assertEquals(MessageType.AUTH_FAIL, c.login("sv7", "777").type());
        }
    }
}
