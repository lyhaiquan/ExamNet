package labcast.server.presence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.TestClient;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.session.Session;
import labcast.server.store.AuditLog;
import labcast.server.store.Database;
import labcast.server.sync.SyncModule;
import org.junit.jupiter.api.Test;

class AppFocusTest {
    static final Session SV = new Session(1, "sv1", "Nguyễn Văn An", Role.STUDENT, "t", 1);

    static byte[] focus(boolean inApp, String lessonId) {
        return new PayloadWriter()
                .bool(inApp)
                .i64(0) // giờ máy học viên: server không dùng
                .str(lessonId)
                .toBytes();
    }

    /** Gửi TIME_SYNC_REQ và chờ trả lời: mọi frame gửi trước đó trên kết nối này đã xử lý xong. */
    static void barrier(TestClient c) throws Exception {
        c.send(MessageType.TIME_SYNC_REQ, new PayloadWriter().i64(0).toBytes());
        c.readUntil(MessageType.TIME_SYNC_RESP);
    }

    @Test
    void roiRoiQuayLai_demMotLan_thoiGianNgoaiTheoGioServer() {
        var now = new AtomicLong(10_000);
        var f = new AppFocus(null, now::get);
        assertTrue(f.update(SV, false, "dsa.sap-xep.noi-bot"));
        now.addAndGet(4_000);
        assertEquals(4_000, f.state(1).orElseThrow().awayMs(now.get()), "đang ở ngoài cũng tính");
        assertTrue(f.update(SV, true, "dsa.sap-xep.noi-bot"));
        AppFocus.State s = f.state(1).orElseThrow();
        assertTrue(s.inApp());
        assertEquals(1, s.leaves());
        assertEquals(4_000, s.awayMs(now.get() + 99_000), "đã quay lại thì không cộng thêm");
    }

    @Test
    void guiLapCungTrangThai_khongDoiGi() {
        var now = new AtomicLong(0);
        var f = new AppFocus(null, now::get);
        assertFalse(f.update(SV, true, ""), "mặc định là đang trong app");
        f.update(SV, false, "");
        now.addAndGet(1_000);
        assertFalse(f.update(SV, false, ""));
        assertEquals(1, f.state(1).orElseThrow().leaves());
        assertEquals(0, f.state(1).orElseThrow().since(), "mốc rời app giữ lần đầu");
    }

    @Test
    void batTatLienTuc_auditToiDaMotDongMoiGiay() throws Exception {
        try (Database db = Database.open(":memory:")) {
            var now = new AtomicLong(0);
            var f = new AppFocus(new AuditLog(db), now::get);
            for (int i = 0; i < 100; i++) {
                f.update(SV, i % 2 == 1, "");
                now.addAndGet(5);
            }
            long rows = db.read(c -> {
                try (var st = c.createStatement();
                        var rs = st.executeQuery("SELECT COUNT(*) FROM audit_log WHERE action LIKE 'APP_%'")) {
                    rs.next();
                    return rs.getLong(1);
                }
            });
            assertEquals(1, rows, "500 ms bật/tắt 100 lần chỉ ghi 1 dòng");
            assertEquals(50, f.state(1).orElseThrow().leaves(), "nhưng vẫn đếm đủ số lần rời");
        }
    }

    @Test
    void quaSocket_chiHocVienDaDangNhapMoiGuiDuoc() throws Exception {
        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(
                        ServerConfig.forTest(),
                        db,
                        List.of(new AccountModule(), new SyncModule(), new AppFocusModule()))) {
            server.start();
            var users = new UserRepository(db);
            long sv = users.create("sv1", "Nguyễn Văn An", Role.STUDENT, "1");
            users.create("gv", "Cô Lan", Role.TEACHER, "2");
            AppFocus focus = server.context().require(AppFocus.class);

            try (var c = TestClient.connect(server.port())) {
                c.send(MessageType.APP_FOCUS, focus(false, "x"));
                assertEquals(
                        ErrorCode.AUTH_REQUIRED,
                        ErrorMessage.from(c.readUntil(MessageType.ERROR)).code());
            }
            try (var c = TestClient.connect(server.port())) {
                assertEquals(MessageType.AUTH_OK, c.login("gv", "2").type());
                c.send(MessageType.APP_FOCUS, focus(false, "x"));
                assertEquals(
                        ErrorCode.FORBIDDEN,
                        ErrorMessage.from(c.readUntil(MessageType.ERROR)).code());
            }
            try (var c = TestClient.connect(server.port())) {
                assertEquals(MessageType.AUTH_OK, c.login("sv1", "1").type());
                c.send(MessageType.APP_FOCUS, focus(false, "dsa.sap-xep.noi-bot"));
                barrier(c);
                assertFalse(focus.state(sv).orElseThrow().inApp());
                Thread.sleep(120);
                c.send(MessageType.APP_FOCUS, focus(true, "dsa.sap-xep.noi-bot"));
                barrier(c);
                AppFocus.State s = focus.state(sv).orElseThrow();
                assertTrue(s.inApp());
                assertEquals(1, s.leaves());
                assertEquals("dsa.sap-xep.noi-bot", s.lessonId());
                assertTrue(s.awayMs() >= 100 && s.awayMs() < 5_000, "thời gian ở ngoài: " + s.awayMs());
            }
        }
    }
}
