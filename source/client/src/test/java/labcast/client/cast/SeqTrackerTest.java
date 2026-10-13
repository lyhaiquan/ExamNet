package labcast.client.cast;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SeqTrackerTest {

    @Test
    void denSaiThuTu_vanNhan_bietThieuGiVaHetThieu() {
        var t = new SeqTracker(7, 1);
        assertTrue(t.accept(7, 1, 0));
        assertTrue(t.accept(7, 3, 0), "gói đến sớm không phải chờ gói 2");
        assertArrayEquals(new int[] {2, 2}, t.gaps().get(0));
        assertTrue(t.accept(7, 2, 0));
        assertTrue(t.gaps().isEmpty());
        assertEquals(3, t.lastSeq());
    }

    @Test
    void goiCuaLanChayKhac_boQua() {
        var t = new SeqTracker(7, 1);
        assertFalse(t.accept(8, 1, 0));
        assertEquals(-1, t.lastSeq());
    }

    @Test
    void goiTrung_khongNhanHaiLan() {
        var t = new SeqTracker(7, 1);
        assertTrue(t.accept(7, 1, 0));
        assertFalse(t.accept(7, 1, 0));
        t.accept(7, 5, 0);
        assertTrue(t.accept(7, 4, 0));
        assertFalse(t.accept(7, 4, 0));
        assertEquals(2, t.duplicates());
    }

    @Test
    void thieu_cho30msMoiXin_xinLaiSau200ms_baLanKhongToiThiBo() {
        var t = new SeqTracker(7, 1);
        t.accept(7, 1, 0);
        t.accept(7, 5, 0);
        assertTrue(t.due(29).isEmpty(), "gói có thể chỉ đến muộn: chờ 30 ms");
        assertArrayEquals(new int[] {2, 4}, t.due(30).get(0));
        assertTrue(t.due(100).isEmpty(), "vừa xin xong, chưa xin lại");
        t.accept(7, 3, 150); // gói 3 tới nhờ lần sửa đầu
        List<int[]> lan2 = t.due(230);
        assertEquals(2, lan2.size(), "xin lại 2 và 4, không xin 3");
        assertArrayEquals(new int[] {2, 2}, lan2.get(0));
        assertArrayEquals(new int[] {4, 4}, lan2.get(1));
        assertEquals(2, t.due(430).size());
        assertTrue(t.due(630).isEmpty(), "đã xin 3 lần: bỏ, khung khoá sẽ bù trạng thái");
        assertTrue(t.gaps().isEmpty());
        assertEquals(2, t.givenUp(), "bỏ gói 2 và 4");
    }

    @Test
    void loLon_chiaDaiToiDa256Goi() {
        var t = new SeqTracker(7, 1);
        t.accept(7, 1, 0);
        t.accept(7, 1000, 0);
        List<int[]> ranges = t.due(30);
        int total = 0;
        int next = 2;
        for (int[] r : ranges) {
            assertTrue(r[1] - r[0] < 256, "dải " + r[0] + ".." + r[1]);
            assertEquals(next, r[0]);
            next = r[1] + 1;
            total += r[1] - r[0] + 1;
        }
        assertEquals(998, total);
    }

    @Test
    void loQuaVongDemServer_chiTheoDoiCacGoiConXinDuoc() {
        var t = new SeqTracker(7, 1);
        t.accept(7, 1, 0);
        t.accept(7, 100_000, 0);
        int missing = t.gaps().stream().mapToInt(r -> r[1] - r[0] + 1).sum();
        assertEquals(SeqTracker.MAX_TRACKED, missing);
    }

    @Test
    void vaoMuon_batDauTuGoiDauTienThay_khongXinCaLichSu() {
        var t = new SeqTracker(7, 5_000);
        assertTrue(t.accept(7, 5_000, 0));
        assertTrue(t.gaps().isEmpty());
        assertFalse(t.accept(7, 12, 0), "gói cũ hơn mốc bắt đầu");
    }
}
