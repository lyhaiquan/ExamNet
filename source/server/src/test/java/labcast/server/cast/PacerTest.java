package labcast.server.cast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class PacerTest {
    static final long MB = 1024 * 1024;

    /** Đồng hồ giả: ngủ là cộng thẳng vào đồng hồ. */
    static final class FakeTime {
        final AtomicLong nanos = new AtomicLong(1_000_000_000L);
        final AtomicLong slept = new AtomicLong();

        Pacer pacer(long bytesPerSecond, long burst) {
            return new Pacer(bytesPerSecond, burst, nanos::get, n -> {
                slept.addAndGet(n);
                nanos.addAndGet(n);
            });
        }
    }

    @Test
    void xin4MBLienTuc_o2MBMoiGiay_ngu2Giay() throws Exception {
        var t = new FakeTime();
        Pacer p = t.pacer(2 * MB, 64 * 1024);
        for (int i = 0; i < 4 * 1024; i++) {
            p.acquire(1024);
        }
        double s = t.slept.get() / 1e9;
        assertEquals(2.0, s, 0.1, "tổng thời gian ngủ " + s + " s");
    }

    @Test
    void conToken_khongNgu() throws Exception {
        var t = new FakeTime();
        t.pacer(2 * MB, 64 * 1024).acquire(1024);
        assertEquals(0, t.slept.get());
    }

    @Test
    void nghiLauRoiPhatTiep_chiDuocDonToiDaMotDot() throws Exception {
        var t = new FakeTime();
        Pacer p = t.pacer(2 * MB, 64 * 1024);
        t.nanos.addAndGet(60_000_000_000L); // im một phút: token không tích quá 64 KB
        for (int i = 0; i < 64; i++) {
            p.acquire(1024);
        }
        assertEquals(0, t.slept.get(), "đợt 64 KB đầu đi ngay");
        p.acquire(64 * 1024);
        assertEquals(31_250_000L, t.slept.get(), 100_000L, "64 KB ở 2 MB/s = 1/32 s");
    }
}
