package labcast.server.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.Role;
import labcast.server.net.Connection;
import labcast.server.net.FramePolicy.Decision;
import labcast.server.net.Router;
import labcast.server.session.Session;
import org.junit.jupiter.api.Test;

class RateLimitPolicyTest {
    final AtomicLong now = new AtomicLong(1_000_000);
    final RateLimitPolicy policy = new RateLimitPolicy(now::get);

    static Connection ketNoi(Session s) {
        var c = new Connection(new Socket(), new Router(), Runnable::run, Connection.Events.NONE, () -> 0L);
        if (s != null) {
            c.bindSession(s);
        }
        return c;
    }

    static Session nguoi(long id, Role role) {
        return new Session(id, "u" + id, "Người " + id, role, "token-" + id, 1);
    }

    @Test
    void chayThu_baLuotLien_luotThuTuPhaiCho10Giay() {
        var c = ketNoi(nguoi(1, Role.STUDENT));
        for (int i = 0; i < 3; i++) {
            assertTrue(policy.check(c, MessageType.RUN_REQ).allowed(), "lượt " + (i + 1));
        }
        Decision d = policy.check(c, MessageType.RUN_REQ);
        assertFalse(d.allowed());
        assertEquals(ErrorCode.RATE_LIMITED, d.code());
        assertEquals(10_000, d.retryAfterMs());
        assertTrue(d.message().contains("10 s"), d.message());

        now.addAndGet(10_000);
        assertTrue(policy.check(c, MessageType.RUN_REQ).allowed());
    }

    @Test
    void noiLai_khongDuocThungMoi() {
        Session s = nguoi(2, Role.STUDENT);
        var c1 = ketNoi(s);
        for (int i = 0; i < 3; i++) {
            policy.check(c1, MessageType.RUN_REQ);
        }
        var c2 = ketNoi(s);
        assertFalse(policy.check(c2, MessageType.RUN_REQ).allowed());
    }

    @Test
    void moiNguoiMotThung() {
        var a = ketNoi(nguoi(3, Role.STUDENT));
        var b = ketNoi(nguoi(4, Role.STUDENT));
        for (int i = 0; i < 3; i++) {
            policy.check(a, MessageType.RUN_REQ);
        }
        assertFalse(policy.check(a, MessageType.RUN_REQ).allowed());
        assertTrue(policy.check(b, MessageType.RUN_REQ).allowed());
    }

    @Test
    void giaoVien_chayMauKhongBiGioiHan() {
        var c = ketNoi(nguoi(5, Role.TEACHER));
        for (int i = 0; i < 20; i++) {
            assertTrue(policy.check(c, MessageType.RUN_REQ).allowed());
            now.addAndGet(100); // vẫn dưới 50 thông điệp/s
        }
    }

    @Test
    void traLoiDuDoan_motLuotMoi3Giay() {
        var c = ketNoi(nguoi(6, Role.STUDENT));
        assertTrue(policy.check(c, MessageType.PREDICT_ANSWER).allowed());
        Decision d = policy.check(c, MessageType.PREDICT_ANSWER);
        assertFalse(d.allowed());
        assertEquals(3_000, d.retryAfterMs());
    }

    @Test
    void thongDiepKhac_50MoiGiay_baoMotLanRoiBoImLang() {
        var c = ketNoi(nguoi(7, Role.STUDENT));
        for (int i = 0; i < 50; i++) {
            assertTrue(policy.check(c, MessageType.LESSON_FETCH).allowed(), "lượt " + (i + 1));
        }
        Decision first = policy.check(c, MessageType.LESSON_FETCH);
        assertFalse(first.allowed());
        assertEquals(ErrorCode.RATE_LIMITED, first.code());
        Decision second = policy.check(c, MessageType.LESSON_FETCH);
        assertFalse(second.allowed());
        assertNull(second.code(), "trong cùng giây không báo lại");

        now.addAndGet(1_000);
        assertTrue(policy.check(c, MessageType.LESSON_FETCH).allowed());
    }

    @Test
    void truocKhiDangNhap_vanDemTheoKetNoi() {
        var c = ketNoi(null);
        for (int i = 0; i < 50; i++) {
            assertTrue(policy.check(c, MessageType.HELLO).allowed());
        }
        assertFalse(policy.check(c, MessageType.HELLO).allowed());
        // kết nối khác của cùng máy không bị vạ lây
        assertTrue(policy.check(ketNoi(null), MessageType.HELLO).allowed());
    }

    @Test
    void deltaCode_vaHeartbeat_khongBaoGioBiChan() {
        var c = ketNoi(nguoi(8, Role.STUDENT));
        for (int i = 0; i < 1_000; i++) {
            assertTrue(policy.check(c, MessageType.CODE_DELTA).allowed());
            assertTrue(policy.check(c, MessageType.HEARTBEAT_ACK).allowed());
        }
    }
}
