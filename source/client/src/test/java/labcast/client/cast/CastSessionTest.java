package labcast.client.cast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import labcast.client.player.Player;
import labcast.client.player.View;
import labcast.common.cast.CastMessages;
import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import labcast.common.trace.Trace;
import labcast.common.trace.TraceEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CastSessionTest {
    static final int CAST = 7;
    static final String BAI = "dsa.sap-xep.noi-bot";

    /** Giờ server giả; trình phát dùng chung. */
    final AtomicLong now = new AtomicLong(100_000);

    final List<CastMessages.Nack> nacks = new ArrayList<>();
    final List<String> shown = new ArrayList<>();
    int stopped;
    Player player;
    CastSession s;

    static final class NoView implements View {
        @Override
        public String kind() {
            return "mang";
        }

        @Override
        public javafx.scene.Node node() {
            return null;
        }

        @Override
        public void reset(JsonNode snapshot) {}

        @Override
        public void apply(TraceEvent e, boolean animate, Duration d) {}
    }

    @BeforeEach
    void setUp() {
        s = new CastSession(now::get, nacks::add);
        s.attach(new CastSession.Screen() {
            @Override
            public Player show(String traceId, Trace trace) {
                shown.add(traceId);
                player = new Player(trace, Map.of("a", new NoView()), now::get);
                return player;
            }

            @Override
            public void stopped() {
                stopped++;
            }
        });
    }

    /** Trace 100 bước, chia thành các khúc nhỏ để có nhiều khúc. */
    static List<byte[]> chunks(int size) {
        List<String> lines = new ArrayList<>();
        lines.add("{\"t\":\"hdr\",\"lesson\":\"" + BAI + "\",\"views\":[{\"v\":\"a\",\"kieu\":\"mang\"}]}");
        for (int i = 0; i < 100; i++) {
            lines.add("{\"t\":\"read\",\"v\":\"a\",\"i\":" + i + "}");
        }
        lines.add("{\"t\":\"end\",\"truncated\":false}");
        byte[] z = CastMessages.compress(lines);
        List<byte[]> out = new ArrayList<>();
        for (int i = 0; i < z.length; i += size) {
            out.add(Arrays.copyOfRange(z, i, Math.min(z.length, i + size)));
        }
        return out;
    }

    static Frame data(int seq, int i, List<byte[]> parts) {
        return new Frame(
                MessageType.CAST_DATA,
                (byte) 0,
                seq,
                new CastMessages.Data(CAST, BAI, i, parts.size(), parts.get(i)).encode());
    }

    static Frame control(int seq, String action, int step, int speed, long at, boolean playing) {
        return new Frame(
                MessageType.CAST_CONTROL,
                (byte) 0,
                seq,
                new CastMessages.Control(CAST, action, BAI, step, speed, at, playing).encode());
    }

    static Frame keyframe(int seq, int total, int first, int step, boolean playing, long anchor) {
        return new Frame(
                MessageType.CAST_KEYFRAME,
                (byte) 0,
                seq,
                new CastMessages.Keyframe(CAST, BAI, total, first, step, playing, 1000, anchor, seq - 1).encode());
    }

    /** Nạp đủ trace (SEQ 1..k) rồi LOAD ở SEQ k+1. Trả SEQ kế tiếp. */
    int loadAll() {
        List<byte[]> parts = chunks(40);
        for (int i = 0; i < parts.size(); i++) {
            s.onFrame(data(1 + i, i, parts));
        }
        int seq = parts.size() + 1;
        s.onFrame(control(seq, CastMessages.LOAD, -1, 1000, now.get() + 300, false));
        return seq + 1;
    }

    @Test
    void phatHenGio_truocGioHenDungYen_sau800msTien2Buoc() {
        int seq = loadAll();
        assertEquals(List.of(BAI), shown);
        s.onFrame(control(seq, CastMessages.PLAY, 0, 1000, now.get() + 300, true));
        player.tick();
        assertEquals(-1, player.position(), "chưa tới giờ hẹn");
        now.addAndGet(299);
        player.tick();
        assertEquals(-1, player.position());
        now.addAndGet(1 + 800);
        player.tick();
        assertEquals(2, player.position(), "bắt đầu ở bước 0, 800 ms ở 1× thêm 2 bước");
        assertTrue(player.playing());
    }

    @Test
    void dungOBuoc37_moiLanTickDeuO37() {
        int seq = loadAll();
        s.onFrame(control(seq, CastMessages.PAUSE, 37, 1000, now.get() + 300, false));
        for (int i = 0; i < 5; i++) {
            now.addAndGet(1_000);
            player.tick();
            assertEquals(37, player.position());
            assertFalse(player.playing());
        }
    }

    @Test
    void tachRaTuTua_roiTheoLai_veDungViTriDangChieu() {
        int seq = loadAll();
        long at = now.get() + 300;
        s.onFrame(control(seq, CastMessages.PLAY, 10, 1000, at, true));
        now.set(at + 4_000); // đang chiếu ở bước 20
        player.tick();
        assertEquals(20, player.position());

        s.detach();
        assertFalse(s.following());
        player.seek(5);
        now.addAndGet(2_000);
        player.tick();
        assertEquals(5, player.position(), "đã tách thì không bị kéo theo giáo viên");

        s.rejoin();
        player.tick();
        assertTrue(s.following());
        assertEquals(25, player.position(), "theo lại: nhảy tới đúng bước cả lớp đang ở");
    }

    @Test
    void goiDieuKhienCuDenSau_boQua() {
        int seq = loadAll();
        s.onFrame(control(seq + 1, CastMessages.PAUSE, 50, 1000, now.get(), false));
        s.onFrame(control(seq, CastMessages.PAUSE, 3, 1000, now.get(), false)); // đến trễ, cũ hơn
        player.tick();
        assertEquals(50, player.position());
    }

    @Test
    void khungKhoaTruocKhiDuDuLieu_chuaPhat_xinDungKhucThieu() {
        List<byte[]> parts = chunks(40);
        assertTrue(parts.size() >= 4);
        s.onFrame(data(11, 0, parts));
        s.onFrame(data(13, 2, parts)); // thiếu khúc 1 (SEQ 12) và từ khúc 3 trở đi
        s.onFrame(keyframe(50, parts.size(), 11, 0, true, now.get()));
        assertTrue(shown.isEmpty(), "chưa đủ trace thì chưa hiện");
        assertEquals(new CastMessages.Nack(CAST, 12, 12), nacks.get(0));
        assertEquals(new CastMessages.Nack(CAST, 14, 10 + parts.size()), nacks.get(1));
        assertTrue(s.active());
    }

    @Test
    void vaoMuon_chiCoKhungKhoa_xinCaTrace_duKhucThiTheoDungViTri() {
        List<byte[]> parts = chunks(40);
        long anchor = now.get() - 10_000; // đã chiếu từ 10 s trước, ở 2×
        Frame kf = new Frame(
                MessageType.CAST_KEYFRAME,
                (byte) 0,
                900,
                new CastMessages.Keyframe(CAST, BAI, parts.size(), 1, 0, true, 2000, anchor, 899).encode());
        s.onFrame(kf);
        assertEquals(1, nacks.size());
        assertEquals(new CastMessages.Nack(CAST, 1, parts.size()), nacks.get(0));
        assertNull(player);

        for (int i = 0; i < parts.size(); i++) {
            s.onFrame(data(1 + i, i, parts)); // gói sửa qua TCP, SEQ cũ
        }
        assertNotNull(player);
        player.tick();
        assertEquals(50, player.position(), "10 s ở 2× = 50 bước");
    }

    @Test
    void giaoVienDungChieu_baoManHinh() {
        int seq = loadAll();
        s.onFrame(control(seq, CastMessages.STOP, 3, 1000, now.get(), false));
        assertEquals(1, stopped);
        assertFalse(s.active());
    }

    @Test
    void chuaGanManHinh_vanGomDuLieu_ganVaoThiHienNgay() {
        s.detachScreen();
        player = null;
        int seq = loadAll();
        s.onFrame(control(seq, CastMessages.PAUSE, 12, 1000, now.get(), false));
        assertNull(player);
        assertTrue(s.ready());
        s.attach(new CastSession.Screen() {
            @Override
            public Player show(String traceId, Trace trace) {
                player = new Player(trace, Map.of("a", new NoView()), now::get);
                return player;
            }

            @Override
            public void stopped() {}
        });
        player.tick();
        assertEquals(12, player.position());
    }
}
