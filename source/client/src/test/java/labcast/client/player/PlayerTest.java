package labcast.client.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import labcast.common.trace.Trace;
import labcast.common.trace.TraceEvent;
import labcast.common.trace.TraceReader;
import org.junit.jupiter.api.Test;

class PlayerTest {

    /** View giả ghi lại mọi lời gọi. */
    static final class ViewGia implements View {
        final List<JsonNode> resets = new ArrayList<>();
        final List<Boolean> applies = new ArrayList<>();
        final String kind;

        ViewGia(String kind) {
            this.kind = kind;
        }

        @Override
        public String kind() {
            return kind;
        }

        @Override
        public javafx.scene.Node node() {
            return null;
        }

        @Override
        public void reset(JsonNode snapshot) {
            resets.add(snapshot);
        }

        @Override
        public void apply(TraceEvent e, boolean animate, Duration d) {
            applies.add(animate);
        }
    }

    /** snap ở 0 và 400, mọi chỗ khác là read của biến a; key ở 600. */
    private static Trace trace1000() throws Exception {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            if (i == 0 || i == 400) {
                lines.add("{\"t\":\"snap\",\"state\":{\"a\":[" + i + "]}}");
            } else if (i == 600) {
                lines.add("{\"t\":\"key\",\"s\":\"mốc\"}");
            } else {
                lines.add("{\"t\":\"read\",\"v\":\"a\",\"i\":0}");
            }
        }
        lines.add("{\"t\":\"end\",\"truncated\":false}");
        return TraceReader.append(TraceReader.empty(), lines);
    }

    @Test
    void seek_resetTuSnapshotRoiApDung50SuKien() throws Exception {
        var v = new ViewGia("mang");
        var p = new Player(trace1000(), Map.of("a", v), () -> 0L);
        v.resets.clear();
        p.seek(450);
        assertEquals(1, v.resets.size());
        assertEquals(400, v.resets.get(0).get(0).asInt());
        assertEquals(50, v.applies.size());
        assertFalse(v.applies.contains(true));
        assertEquals(450, p.position());
    }

    @Test
    void play_haiGiay_tienNamBuoc_toc2x_tienMuoi() throws Exception {
        var clock = new AtomicLong(1_000);
        var p = new Player(trace1000(), Map.of("a", new ViewGia("mang")), clock::get);
        p.play();
        clock.addAndGet(2_000);
        p.tick();
        assertEquals(4, p.position()); // từ −1 tiến 5 bước

        p.speed(2);
        clock.addAndGet(2_000);
        p.tick();
        assertEquals(14, p.position());
    }

    @Test
    void tickNho_tichLuyPhanLe() throws Exception {
        var clock = new AtomicLong(0);
        var p = new Player(trace1000(), Map.of("a", new ViewGia("mang")), clock::get);
        p.play();
        for (int i = 0; i < 10; i++) {
            clock.addAndGet(40); // 10 khung hình 40 ms = 400 ms = 1 bước
            p.tick();
        }
        assertEquals(0, p.position());
    }

    @Test
    void pause_khongTien() throws Exception {
        var clock = new AtomicLong(0);
        var p = new Player(trace1000(), Map.of("a", new ViewGia("mang")), clock::get);
        p.play();
        p.pause();
        clock.addAndGet(5_000);
        p.tick();
        assertEquals(-1, p.position());
    }

    @Test
    void nextKeyStep_nhayDungSuKienKey() throws Exception {
        var p = new Player(trace1000(), Map.of("a", new ViewGia("mang")), () -> 0L);
        p.nextKeyStep();
        assertEquals(600, p.position());
        p.nextKeyStep(); // không còn key nào
        assertEquals(600, p.position());
    }

    @Test
    void say_dualenThuyetMinh() throws Exception {
        Trace t = TraceReader.append(
                TraceReader.empty(),
                List.of("{\"t\":\"say\",\"s\":\"5 > 1 nên đổi chỗ\"}", "{\"t\":\"line\",\"n\":3}"));
        List<String> said = new ArrayList<>();
        var code = new ViewGia("code-bien");
        var p = new Player(t, Map.of("__code", code), () -> 0L);
        p.onNarration(said::add);
        p.seek(1);
        assertEquals(List.of("5 > 1 nên đổi chỗ"), said);
        assertEquals(1, code.applies.size());
    }

    @Test
    void seekVeDau_resetRong() throws Exception {
        var v = new ViewGia("mang");
        var p = new Player(
                TraceReader.append(TraceReader.empty(), List.of("{\"t\":\"read\",\"v\":\"a\",\"i\":0}")),
                Map.of("a", v),
                () -> 0L);
        p.seek(-1);
        assertNull(v.resets.get(v.resets.size() - 1));
    }

    @Test
    void viTriDau_vanVeTrangThaiBanDau() throws Exception {
        var v = new ViewGia("mang");
        var p = new Player(trace1000(), Map.of("a", v), () -> 0L);
        p.seek(-1);
        assertEquals(0, v.resets.get(v.resets.size() - 1).get(0).asInt()); // keyframe ở chỉ số 0
        assertEquals(-1, p.position());
    }
}
