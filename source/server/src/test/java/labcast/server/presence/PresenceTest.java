package labcast.server.presence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.Role;
import labcast.server.net.Connection;
import labcast.server.net.Router;
import labcast.server.session.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PresenceTest {
    private final AtomicLong now = new AtomicLong(10_000);
    private Session s;
    private Connection c;
    private Presence p;

    @BeforeEach
    void dung() {
        c = new Connection(new Socket(), new Router(), Runnable::run, Connection.Events.NONE, now::get);
        s = new Session(7, "sv", "SV", Role.STUDENT, "t", 1);
        s.attach(c);
        p = new Presence(() -> List.of(s), now::get);
    }

    @Test
    void tick_guiHeartbeatMangGioServer() throws Exception {
        p.tick();
        Frame hb = c.queue().poll(1, TimeUnit.SECONDS);
        assertEquals(MessageType.HEARTBEAT, hb.type());
        assertEquals(10_000, hb.reader().i64());
    }

    @Test
    void srtt_dungCongThuc() {
        p.tick();
        p.ack(s, now.get(), Map.of(), now.get() + 100); // 100
        assertEquals(100, p.srtt(7));
        p.ack(s, now.get(), Map.of(), now.get() + 100); // 100
        assertEquals(100, p.srtt(7));
        p.ack(s, now.get(), Map.of(), now.get() + 200); // 7/8·100 + 1/8·200 = 112,5
        assertEquals(112, p.srtt(7));
    }

    @Test
    void chuaDoDuoc_srttLaAmMot() {
        assertEquals(-1, p.srtt(7));
    }

    @Test
    void loHaiChuKy_suspect_namChuKy_disconnected() {
        List<PresenceState> doi = new ArrayList<>();
        p.onStateChange((ss, st) -> doi.add(st));
        p.tick(); // gửi lần đầu
        p.tick(); // lỡ 1
        assertEquals(PresenceState.ALIVE, p.state(7));
        p.tick(); // lỡ 2
        assertEquals(PresenceState.SUSPECT, p.state(7));
        p.tick();
        p.tick();
        p.tick(); // lỡ 5
        assertEquals(PresenceState.DISCONNECTED, p.state(7));
        assertTrue(c.isClosed());
        assertEquals(List.of(PresenceState.SUSPECT, PresenceState.DISCONNECTED), doi);
    }

    @Test
    void biNgatRoiNoiLai_ketNoiMoiKhongBiDongNgay() throws Exception {
        for (int i = 0; i < 6; i++) {
            p.tick();
        }
        assertEquals(PresenceState.DISCONNECTED, p.state(7));
        assertTrue(c.isClosed());

        Connection moi = new Connection(new Socket(), new Router(), Runnable::run, Connection.Events.NONE, now::get);
        s.attach(moi);
        p.tick();
        assertFalse(moi.isClosed(), "kết nối mới phải được chờ heartbeat như bình thường");
        assertEquals(PresenceState.ALIVE, p.state(7));
        assertEquals(
                MessageType.HEARTBEAT, moi.queue().poll(1, TimeUnit.SECONDS).type());
    }

    @Test
    void ackLaiSauSuspect_veAlive() {
        p.tick();
        p.tick();
        p.tick();
        assertEquals(PresenceState.SUSPECT, p.state(7));
        p.ack(s, now.get(), Map.of(), now.get() + 50);
        assertEquals(PresenceState.ALIVE, p.state(7));
    }

    @Test
    void listenerNhanDungTruongMoRong() {
        List<Map<String, Long>> nhan = new ArrayList<>();
        p.onHeartbeat((ss, fields) -> nhan.add(fields));
        p.ack(s, now.get(), Map.of("lastCastSeq", 42L), now.get() + 10);
        assertEquals(Map.of("lastCastSeq", 42L), nhan.get(0));
    }
}
