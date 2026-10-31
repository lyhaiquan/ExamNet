package labcast.server.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.JsonPayload;
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

/** Trang thông báo bảo mật của quản trị: yêu cầu quên mật khẩu, sự kiện đổi mật khẩu, số đỏ. */
class NoticesTest {
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
        classes.enroll(classes.classOf(gv), users.create("sv1", "Học viên A", Role.STUDENT, "mk-sv1"));
        users.create("sv-moi", "Chưa vào lớp", Role.STUDENT, "mk-moi"); // chưa ghi danh lớp nào
    }

    @AfterEach
    void tatServer() throws Exception {
        server.close();
        db.close();
    }

    private void forgot(String user) throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.send(
                    MessageType.HELLO,
                    new PayloadWriter().str("test").str("may-test").toBytes());
            c.readUntil(MessageType.HELLO_ACK);
            c.send(MessageType.PASSWORD_FORGOT, new PayloadWriter().str(user).toBytes());
            c.readUntil(MessageType.ADMIN_OK, MessageType.ERROR);
        }
    }

    private JsonNode notices(TestClient c, boolean markRead) throws Exception {
        c.send(MessageType.NOTICES_REQ, new PayloadWriter().bool(markRead).toBytes());
        return JsonPayload.tree(c.readUntil(MessageType.NOTICES_DATA));
    }

    @Test
    void yeuCauMoi_adminDangOnlineNhanNoticeNew() throws Exception {
        try (var admin = TestClient.connect(server.port())) {
            admin.login("ad", "mk-ad");
            forgot("sv1");
            var n = JsonPayload.tree(admin.readUntil(MessageType.NOTICE_NEW));
            assertEquals("PASSWORD_FORGOT", n.path("action").asText());
            assertEquals("sv1", n.path("actor").asText());
        }
    }

    @Test
    void hocVienChuaVaoLop_chiAdminThay_giaoVienKhongThay() throws Exception {
        forgot("sv-moi");
        try (var gv = TestClient.connect(server.port())) {
            gv.login("gv", "mk-gv");
            gv.send(MessageType.PASSWORD_REQUESTS_REQ, new byte[0]);
            assertEquals(
                    0,
                    JsonPayload.tree(gv.readUntil(MessageType.PASSWORD_REQUESTS_DATA))
                            .path("requests")
                            .size());
        }
        try (var ad = TestClient.connect(server.port())) {
            ad.login("ad", "mk-ad");
            var pending = notices(ad, false).path("pending");
            assertEquals(1, pending.size());
            assertEquals("sv-moi", pending.get(0).path("username").asText());
        }
    }

    @Test
    void quenMatKhauTenLa_adminThayGiaoVienKhong() throws Exception {
        forgot("khong-co-ai");
        try (var ad = TestClient.connect(server.port())) {
            ad.login("ad", "mk-ad");
            var events = notices(ad, false).path("events");
            assertEquals(1, events.size());
            assertEquals("PASSWORD_FORGOT_UNKNOWN", events.get(0).path("action").asText());
            assertEquals("khong-co-ai", events.get(0).path("detail").asText());
        }
        try (var gv = TestClient.connect(server.port())) {
            gv.login("gv", "mk-gv");
            gv.send(MessageType.NOTICES_REQ, new PayloadWriter().bool(false).toBytes());
            assertEquals(
                    ErrorCode.FORBIDDEN,
                    ErrorMessage.from(gv.readUntil(MessageType.ERROR)).code());
        }
    }

    @Test
    void tuDoiMatKhau_hienTrongLichSu_khongLoMatKhau() throws Exception {
        try (var sv = TestClient.connect(server.port())) {
            sv.login("sv1", "mk-sv1");
            sv.send(
                    MessageType.PASSWORD_CHANGE,
                    new PayloadWriter().str("mk-sv1").str("bi-mat-moi-1").toBytes());
            sv.readUntil(MessageType.ADMIN_OK);
        }
        try (var ad = TestClient.connect(server.port())) {
            ad.login("ad", "mk-ad");
            String json = notices(ad, false).toString();
            assertTrue(json.contains("PASSWORD_CHANGE"));
            assertFalse(json.contains("bi-mat-moi-1"));
            assertFalse(json.contains("mk-sv1"));
        }
    }

    @Test
    void danhDauDaDoc_soDoVeKhong_yeuCauDangChoVanTinh() throws Exception {
        forgot("sv1");
        try (var sv = TestClient.connect(server.port())) {
            sv.login("sv1", "mk-sv1");
            sv.send(
                    MessageType.PASSWORD_CHANGE,
                    new PayloadWriter().str("mk-sv1").str("moi-12345").toBytes());
            sv.readUntil(MessageType.ADMIN_OK);
        }
        forgot("ai-do"); // tên lạ: một sự kiện chưa đọc
        try (var ad = TestClient.connect(server.port())) {
            ad.login("ad", "mk-ad");
            // yêu cầu của sv1 đã bị xoá khi tự đổi; còn lại: PASSWORD_CHANGE + PASSWORD_FORGOT_UNKNOWN
            assertEquals(2, notices(ad, false).path("badge").asInt());
            assertEquals(0, notices(ad, true).path("badge").asInt());
            assertEquals(0, notices(ad, false).path("badge").asInt());
        }
    }
}
