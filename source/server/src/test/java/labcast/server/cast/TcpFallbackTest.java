package labcast.server.cast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class TcpFallbackTest {
    final AtomicLong now = new AtomicLong(1_000_000);

    /** Đang chiếu: server phát khung khoá mỗi 2 s, SEQ tăng 1 mỗi khung. Trả SEQ cuối. */
    int casting(TcpFallback f, int seconds) {
        int seq = 0;
        for (int s = 0; s < seconds; s += 2) {
            f.recordServerSeq(++seq);
            now.addAndGet(2_000);
        }
        return seq;
    }

    @Test
    void mayBaoTru1BaLanKhiDangChieu_chuyenSangTcp() {
        var f = new TcpFallback(now::get);
        casting(f, 4);
        assertFalse(f.onHeartbeat(1, 10, -1, true));
        assertFalse(f.onHeartbeat(1, 10, -1, true));
        assertTrue(f.onHeartbeat(1, 10, -1, true), "heartbeat thứ ba: chuyển");
        assertTrue(f.viaTcp(1, 10));
        assertEquals(1, f.count());
    }

    @Test
    void mayChiChamMotGoi_khongChuyen() {
        var f = new TcpFallback(now::get);
        int last = casting(f, 20);
        for (int i = 0; i < 10; i++) {
            assertFalse(f.onHeartbeat(1, 10, last - 1, true));
        }
        assertFalse(f.viaTcp(1, 10));
    }

    @Test
    void mayTutSauQua6Giay_chuyen() {
        var f = new TcpFallback(now::get);
        int last = casting(f, 20); // SEQ k phát ở giây 2(k−1); bây giờ là giây 20, 6 s trước là giây 14: SEQ 8
        assertEquals(8, f.requiredSeq());
        f.onHeartbeat(1, 10, 6, true);
        f.onHeartbeat(1, 10, 6, true);
        assertTrue(f.onHeartbeat(1, 10, 6, true));
        assertTrue(last > 6);
    }

    @Test
    void khongCoBuoiChieu_khongChuyenAi() {
        var f = new TcpFallback(now::get);
        for (int i = 0; i < 10; i++) {
            assertFalse(f.onHeartbeat(1, 10, -1, false));
        }
        assertFalse(f.viaTcp(1, 10));
    }

    @Test
    void lagXenKe_demLaiTuDau() {
        var f = new TcpFallback(now::get);
        int last = casting(f, 10);
        f.onHeartbeat(1, 10, -1, true);
        f.onHeartbeat(1, 10, -1, true);
        f.onHeartbeat(1, 10, last, true); // nhận được: đếm lại
        f.onHeartbeat(1, 10, -1, true);
        assertFalse(f.onHeartbeat(1, 10, -1, true));
    }

    @Test
    void noiLai_quayVeMulticast() {
        var f = new TcpFallback(now::get);
        casting(f, 4);
        for (int i = 0; i < 3; i++) {
            f.onHeartbeat(1, 10, -1, true);
        }
        assertTrue(f.viaTcp(1, 10));
        assertFalse(f.viaTcp(1, 11), "kết nối mới (đã nối lại): thử multicast lại từ đầu");
    }
}
