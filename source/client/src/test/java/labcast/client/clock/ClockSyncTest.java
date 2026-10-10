package labcast.client.clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ClockSyncTest {

    @Test
    void giuMauDelayNhoNhat() {
        var c = new ClockSync();
        assertTrue(c.addSample(1000, 6050, 6051, 1101)); // delay 100, offset 5000
        assertTrue(c.addSample(2000, 7010, 7011, 2021)); // delay 20, offset 5000 — tốt hơn
        assertFalse(c.addSample(3000, 8300, 8301, 3401)); // delay 400 — bỏ
        assertEquals(20, c.bestDelayMs());
        assertEquals(5000, c.offsetMs());
    }

    @Test
    void delayAm_boMau() {
        var c = new ClockSync();
        assertFalse(c.addSample(1000, 500, 600, 900));
        assertFalse(c.isSynced());
    }

    @Test
    void mauKhongDoiXung_offsetLaTrungBinh() {
        var c = new ClockSync();
        // đi mất 30 ms, về mất 10 ms, server nhanh hơn 1000 ms: offset ước lượng lệch (30 − 10) / 2 = 10
        c.addSample(0, 1030, 1030, 40);
        assertEquals(1010, c.offsetMs());
    }

    @Test
    void serverNow_bangGioMayCongOffset() {
        var local = new AtomicLong(50_000);
        var c = new ClockSync(local::get);
        c.addSample(50_000, 2_000, 2_001, 50_011);
        assertEquals(50_000 - 48_005, c.serverNow());
        local.addAndGet(1_000);
        assertEquals(51_000 - 48_005, c.serverNow());
    }
}
