package labcast.server.cast;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RecentFramesTest {

    static byte[] b(int seq) {
        return new byte[] {(byte) seq, (byte) (seq >> 8)};
    }

    @Test
    void dat5000Goi_goiCuBiDe_goiMoiCon() {
        var r = new RecentFrames(4096);
        for (int s = 1; s <= 5000; s++) {
            r.put(s, b(s));
        }
        assertTrue(r.get(1).isEmpty(), "gói 1 đã bị gói 4097 đè");
        assertTrue(r.get(904).isEmpty());
        assertArrayEquals(b(905), r.get(905).orElseThrow(), "gói cũ nhất còn giữ: 5000 − 4096 + 1");
        assertArrayEquals(b(4999), r.get(4999).orElseThrow());
    }

    @Test
    void soChuaGui_rong() {
        var r = new RecentFrames(4096);
        r.put(1, b(1));
        assertTrue(r.get(2).isEmpty());
        assertTrue(r.get(4097).isEmpty(), "cùng ô với gói 1 nhưng chưa gửi");
        assertTrue(r.get(0).isEmpty());
        assertTrue(r.get(-5).isEmpty());
    }
}
