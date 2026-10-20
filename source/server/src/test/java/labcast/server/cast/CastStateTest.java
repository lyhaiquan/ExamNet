package labcast.server.cast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import labcast.common.cast.CastMessages;
import labcast.common.cast.CastMessages.Request;
import org.junit.jupiter.api.Test;

class CastStateTest {

    /** Trace 50 sự kiện. */
    static CastState loaded() throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("{\"t\":\"hdr\",\"lesson\":\"x\",\"views\":[]}");
        for (int i = 0; i < 50; i++) {
            lines.add("{\"t\":\"say\",\"s\":\"b" + i + "\"}");
        }
        lines.add("{\"t\":\"end\",\"truncated\":false}");
        CastState s = new CastState();
        s.load(CastState.prepare("x", lines), 1);
        return s;
    }

    static Request req(String action, int step, int speed) {
        return new Request(action, "x", step, speed);
    }

    @Test
    void phat_coHieuLucSau300ms_roiTienMoiBuoc400ms() throws Exception {
        CastState s = loaded();
        var c = s.apply(7, req(CastMessages.PLAY, 0, 1000), 1_000);
        assertEquals(1_300, c.at());
        assertTrue(c.playing());
        assertEquals(0, s.position(1_200), "chưa tới giờ hẹn thì đứng yên");
        assertEquals(0, s.position(1_699));
        assertEquals(1, s.position(1_700));
        assertEquals(2, s.position(2_100), "800 ms ở 1× là 2 bước");
        assertEquals(49, s.position(1_000_000), "không vượt cuối trace");
    }

    @Test
    void dung_serverTinhBuocOGioHen_caLopDungCungBuoc() throws Exception {
        CastState s = loaded();
        s.apply(7, req(CastMessages.PLAY, 0, 1000), 1_000); // chạy từ 1300
        var c = s.apply(7, req(CastMessages.PAUSE, 999, 0), 2_000); // giáo viên gửi bước bậy: bỏ qua
        assertEquals(2_300, c.at());
        assertEquals(2, c.step(), "ở 2300 đã chạy 1000 ms = 2 bước");
        assertFalse(c.playing());
        assertEquals(2, s.position(99_999));
    }

    @Test
    void doiTocDo_giuNguyenViTri_tuDoChayNhanhHon() throws Exception {
        CastState s = loaded();
        s.apply(7, req(CastMessages.PLAY, 10, 1000), 0); // chạy từ 300
        var c = s.apply(7, req(CastMessages.SPEED, 0, 2000), 500); // có hiệu lực ở 800: bước 11
        assertEquals(11, c.step());
        assertEquals(2000, c.speedMilli());
        assertEquals(13, s.position(800 + 400), "2× là 200 ms một bước");
    }

    @Test
    void phatKhiDaHet_phatLaiTuDau_tuaChanTrongTrace() throws Exception {
        CastState s = loaded();
        assertEquals(-1, s.apply(7, req(CastMessages.PLAY, 49, 1000), 0).step());
        assertEquals(49, s.apply(7, req(CastMessages.SEEK, 500, 0), 10).step());
        assertEquals(-1, s.apply(7, req(CastMessages.SEEK, -9, 0), 20).step());
    }

    @Test
    void gioHenKhongBaoGioLui() throws Exception {
        CastState s = loaded();
        long a = s.apply(7, req(CastMessages.PLAY, 0, 1000), 5_000).at();
        long b = s.apply(7, req(CastMessages.PAUSE, 0, 0), 4_000).at(); // đồng hồ lùi
        assertTrue(b > a);
    }

    @Test
    void khungKhoa_vaChiSoKhuc() throws Exception {
        CastState s = loaded();
        s.apply(7, req(CastMessages.PLAY, 3, 1000), 0);
        var k = s.keyframe(7, 42);
        assertEquals(3, k.step());
        assertEquals(300, k.anchorAt());
        assertEquals(1, k.firstDataSeq());
        assertEquals(42, k.lastSeq());
        assertEquals(0, s.chunkIndexOf(1));
        assertEquals(-1, s.chunkIndexOf(1 + k.totalChunks()));
        assertEquals(-1, s.chunkIndexOf(0));
    }
}
