package labcast.server.cast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import labcast.server.cast.NackAggregator.Mode;
import org.junit.jupiter.api.Test;

class NackAggregatorTest {

    @Test
    void lop10May_may2CungXin_phatLaiMotLanQuaMulticast() {
        var now = new AtomicLong(0);
        var a = new NackAggregator(() -> 10, now::get);
        assertEquals(Map.of(5, Mode.UNICAST), a.onNack(1, 5, 5));
        now.addAndGet(20);
        assertEquals(Map.of(5, Mode.MULTICAST), a.onNack(2, 5, 5), "20% của 10 máy = 2 máy");
        now.addAndGet(20);
        assertEquals(Map.of(5, Mode.NONE), a.onNack(3, 5, 5), "đã phát lại trong cửa sổ này");
        now.addAndGet(20); // 60 ms kể từ lần xin đầu: cửa sổ mới
        assertEquals(Map.of(5, Mode.UNICAST), a.onNack(4, 5, 5));
    }

    @Test
    void cungMotMayXinLai_khongTinhThanhHaiMay() {
        var now = new AtomicLong(0);
        var a = new NackAggregator(() -> 10, now::get);
        a.onNack(1, 5, 5);
        assertEquals(Map.of(5, Mode.UNICAST), a.onNack(1, 5, 5));
    }

    @Test
    void lop30May_can6May_xinCaDai_tungGoiMotQuyetDinh() {
        var now = new AtomicLong(0);
        var a = new NackAggregator(() -> 30, now::get);
        for (long u = 1; u <= 5; u++) {
            assertEquals(Mode.UNICAST, a.onNack(u, 7, 9).get(8));
        }
        Map<Integer, Mode> m = a.onNack(6, 7, 9);
        assertEquals(Map.of(7, Mode.MULTICAST, 8, Mode.MULTICAST, 9, Mode.MULTICAST), m);
    }

    @Test
    void lopNho_itNhat2May() {
        var now = new AtomicLong(0);
        var a = new NackAggregator(() -> 1, now::get);
        assertEquals(Mode.UNICAST, a.onNack(1, 3, 3).get(3), "lớp 1 máy: gửi riêng, không phát cho cả lớp");
    }
}
