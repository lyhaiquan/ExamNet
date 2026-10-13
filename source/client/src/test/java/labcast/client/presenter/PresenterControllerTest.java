package labcast.client.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import labcast.client.player.Player;
import labcast.common.cast.CastMessages;
import labcast.common.cast.CastMessages.Request;
import labcast.common.trace.TraceReader;
import org.junit.jupiter.api.Test;

class PresenterControllerTest {
    static final String BAI = "dsa.sap-xep.noi-bot";

    final List<Request> sent = new ArrayList<>();

    /** 100 sự kiện, bước quan trọng ở 30 và 70. */
    static Player player() throws Exception {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            lines.add(i == 30 || i == 70 ? "{\"t\":\"key\",\"s\":\"mốc\"}" : "{\"t\":\"say\",\"s\":\"b\"}");
        }
        return new Player(TraceReader.append(TraceReader.empty(), lines), Map.of(), () -> 0L);
    }

    @Test
    void batDauChieu_guiLoad() {
        var c = new PresenterController(sent::add);
        c.start(BAI);
        assertTrue(c.casting());
        assertEquals(List.of(new Request(CastMessages.LOAD, BAI, -1, 1000)), sent);
    }

    @Test
    void bamPhat_guiPlayTuBuocDangDung_tocDoDangChon() throws Exception {
        var c = new PresenterController(sent::add);
        c.start(BAI);
        Player p = player();
        p.seek(12);
        p.speed(2);
        c.play(p);
        assertEquals(new Request(CastMessages.PLAY, BAI, 12, 2000), sent.get(1));
        assertEquals(12, p.position(), "trình phát của giáo viên không tự chạy: chờ lệnh server dội về");
        assertFalse(p.playing());
    }

    @Test
    void phatKhiDaHet_phatLaiTuDau() throws Exception {
        var c = new PresenterController(sent::add);
        c.start(BAI);
        Player p = player();
        p.seek(99);
        c.play(p);
        assertEquals(-1, sent.get(1).step());
    }

    @Test
    void tua_dung_doiTocDo_toiBuocQuanTrong_dungChieu() throws Exception {
        var c = new PresenterController(sent::add);
        c.start(BAI);
        Player p = player();
        p.seek(40);
        c.seek(p, 37);
        c.pause(p);
        c.speed(p, 0.5);
        c.nextKey(p);
        c.stop();
        assertEquals(
                List.of(
                        new Request(CastMessages.SEEK, BAI, 37, 0),
                        new Request(CastMessages.PAUSE, BAI, 40, 0),
                        new Request(CastMessages.SPEED, BAI, 40, 500),
                        new Request(CastMessages.SEEK, BAI, 70, 0),
                        new Request(CastMessages.STOP, BAI, 0, 0)),
                sent.subList(1, sent.size()));
        assertFalse(c.casting());
    }

    @Test
    void khongConBuocQuanTrong_khongGuiGi() throws Exception {
        var c = new PresenterController(sent::add);
        c.start(BAI);
        Player p = player();
        p.seek(80);
        c.nextKey(p);
        assertEquals(1, sent.size());
    }
}
