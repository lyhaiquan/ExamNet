package labcast.server.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TokenBucketTest {

    @Test
    void sauLuotMotPhut_dot3_luot4PhaiCho10Giay() {
        var b = new TokenBucket(3, Duration.ofSeconds(10), 0);
        assertEquals(0, b.tryTake(0));
        assertEquals(0, b.tryTake(0));
        assertEquals(0, b.tryTake(0));
        assertEquals(10_000, b.tryTake(0));
        // chờ chưa đủ thì vẫn bị từ chối, số ms còn lại giảm dần
        assertEquals(4_000, b.tryTake(6_000));
        assertEquals(0, b.tryTake(10_000));
        assertEquals(10_000, b.tryTake(10_000));
    }

    @Test
    void napLe_tungMs_khongBiLamTronMat() {
        var b = new TokenBucket(1, Duration.ofSeconds(10), 0);
        assertEquals(0, b.tryTake(0));
        // hỏi mỗi ms: 10 000 lần nạp 1 ms vẫn đủ đúng một token
        for (long t = 1; t < 10_000; t++) {
            assertEquals(10_000 - t, b.tryTake(t));
        }
        assertEquals(0, b.tryTake(10_000));
    }

    @Test
    void nghiLau_khongTichQuaDot() {
        var b = new TokenBucket(3, Duration.ofSeconds(10), 0);
        for (int i = 0; i < 3; i++) {
            b.tryTake(0);
        }
        long later = 3_600_000;
        for (int i = 0; i < 3; i++) {
            assertEquals(0, b.tryTake(later));
        }
        assertEquals(10_000, b.tryTake(later));
    }

    @Test
    void dongHoLui_khongNapThem() {
        var b = new TokenBucket(1, Duration.ofSeconds(1), 5_000);
        assertEquals(0, b.tryTake(5_000));
        assertEquals(1_000, b.tryTake(4_000));
    }

    @Test
    void thamSoSai_baoLoi() {
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(0, Duration.ofSeconds(1), 0));
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(1, Duration.ZERO, 0));
    }
}
