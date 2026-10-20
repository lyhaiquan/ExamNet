package labcast.client.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import labcast.common.protocol.CodeDelta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeltaBatcherTest {
    private final AtomicLong clock = new AtomicLong(1_000);
    private final List<CodeDelta> sent = new ArrayList<>();
    private DeltaBatcher b;

    @BeforeEach
    void tao() {
        b = new DeltaBatcher("dsa.x", clock::get, sent::add);
    }

    @Test
    void goLienBaKyTu_raMotDelta() {
        b.onChange(0, "", "a");
        clock.addAndGet(30);
        b.onChange(1, "", "b");
        clock.addAndGet(30);
        b.onChange(2, "", "c");
        b.tick(); // mới 60 ms: chưa xả
        assertEquals(0, sent.size());
        clock.set(1_110);
        b.tick();
        assertEquals(1, sent.size());
        assertEquals("abc", sent.get(0).insText());
        assertEquals(0, sent.get(0).pos());
    }

    @Test
    void nhayVeDauRoiGo_haiDelta() {
        b.onChange(5, "", "a");
        b.onChange(0, "", "x");
        b.flush();
        assertEquals(2, sent.size());
        assertEquals(new CodeDelta("dsa.x", 1, 5, 0, "a", 1_000), sent.get(0));
        assertEquals(0, sent.get(1).pos());
    }

    @Test
    void backspaceLienTiep_gopThanhMotLanXoa() {
        // văn bản "abcd", con trỏ ở cuối, Backspace 3 lần
        b.onChange(3, "d", "");
        b.onChange(2, "c", "");
        b.onChange(1, "b", "");
        b.flush();
        assertEquals(1, sent.size());
        assertEquals(1, sent.get(0).pos());
        assertEquals(3, sent.get(0).delLen());
    }

    @Test
    void deleteLienTiep_gop() {
        b.onChange(1, "b", "");
        b.onChange(1, "c", "");
        b.flush();
        assertEquals(1, sent.size());
        assertEquals(2, sent.get(0).delLen());
    }

    @Test
    void thayTheChon_laDeltaRieng() {
        b.onChange(0, "", "a");
        b.onChange(0, "a", "xyz"); // chọn rồi gõ đè
        b.flush();
        assertEquals(2, sent.size());
        assertEquals(1, sent.get(1).delLen());
        assertEquals("xyz", sent.get(1).insText());
    }

    @Test
    void seqLienTiep_vaBatDauTuSeqServer() {
        b.setLastSeq(10);
        for (int i = 0; i < 3; i++) {
            b.onChange(i * 10, "", "x");
            b.flush();
        }
        assertEquals(List.of(11L, 12L, 13L), sent.stream().map(CodeDelta::seq).toList());
        assertEquals(13, b.lastSeq());
    }

    @Test
    void flushKhiKhongCoGi_khongGui() {
        b.flush();
        b.tick();
        assertEquals(0, sent.size());
    }
}
