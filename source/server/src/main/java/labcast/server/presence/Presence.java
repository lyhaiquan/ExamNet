package labcast.server.presence;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.server.net.Connection;
import labcast.server.net.Priority;
import labcast.server.session.Session;

/**
 * Heartbeat và trạng thái có mặt của từng học viên. Giai đoạn 1: chu kỳ cố định 2 s; giai đoạn 2 đổi
 * sang thích nghi theo SRTT/RTTVAR (task 2.11).
 *
 * <p>Server gửi {@code HEARTBEAT(serverTime)}, app trả {@code HEARTBEAT_ACK(echo, fields)}. RTT là
 * {@code now − echo}, đo hoàn toàn bằng đồng hồ server nên không phụ thuộc đồng hồ máy học viên.
 */
public final class Presence {
    public static final long PERIOD_MS = 2_000;
    static final int SUSPECT_AFTER = 2;
    static final int DISCONNECT_AFTER = 5;

    private static final class State {
        long srtt = -1;
        int missed;
        boolean awaiting;
        PresenceState state = PresenceState.ALIVE;
    }

    private final Supplier<Collection<Session>> sessions;
    private final LongSupplier clock;
    private final Map<Long, State> states = new ConcurrentHashMap<>();
    private final List<BiConsumer<Session, Map<String, Long>>> heartbeatListeners = new CopyOnWriteArrayList<>();
    private final List<BiConsumer<Session, PresenceState>> stateListeners = new CopyOnWriteArrayList<>();

    public Presence(Supplier<Collection<Session>> sessions, LongSupplier clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    public void start(ScheduledExecutorService scheduler) {
        scheduler.scheduleAtFixedRate(this::tick, PERIOD_MS, PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    /** Một chu kỳ: đếm heartbeat bị lỡ, đổi trạng thái, gửi heartbeat mới. Test gọi trực tiếp. */
    public void tick() {
        long now = clock.getAsLong();
        for (Session s : sessions.get()) {
            Connection c = s.connection();
            if (c == null || c.isClosed()) {
                continue;
            }
            State st = states.computeIfAbsent(s.userId(), k -> new State());
            PresenceState next;
            synchronized (st) {
                if (st.awaiting) {
                    st.missed++;
                }
                next = st.missed >= DISCONNECT_AFTER
                        ? PresenceState.DISCONNECTED
                        : st.missed >= SUSPECT_AFTER ? PresenceState.SUSPECT : PresenceState.ALIVE;
                st.awaiting = true;
            }
            changeState(s, st, next);
            if (next == PresenceState.DISCONNECTED) {
                c.close(); // phiên vẫn giữ để app RESUME khi mạng có lại
                continue;
            }
            // CRITICAL: không xếp hàng sau dữ liệu khác, để RTT phản ánh mạng chứ không phải hàng đợi
            s.send(Frame.of(MessageType.HEARTBEAT, new PayloadWriter().i64(now).toBytes()), Priority.CRITICAL);
        }
    }

    /** Handler {@code HEARTBEAT_ACK} gọi. {@code now} là lúc luồng đọc nhận frame. */
    public void ack(Session s, long echoServerTime, Map<String, Long> fields, long now) {
        State st = states.computeIfAbsent(s.userId(), k -> new State());
        long rtt = now - echoServerTime;
        synchronized (st) {
            if (rtt >= 0) {
                st.srtt = st.srtt < 0 ? rtt : (7 * st.srtt + rtt) / 8;
            }
            st.missed = 0;
            st.awaiting = false;
        }
        changeState(s, st, PresenceState.ALIVE);
        for (var l : heartbeatListeners) {
            l.accept(s, fields);
        }
    }

    /** SRTT (ms) của học viên; −1 nếu chưa đo được. ĐG4 dùng để bù độ trễ. */
    public long srtt(long userId) {
        State st = states.get(userId);
        if (st == null) {
            return -1;
        }
        synchronized (st) {
            return st.srtt;
        }
    }

    public PresenceState state(long userId) {
        State st = states.get(userId);
        if (st == null) {
            return PresenceState.DISCONNECTED;
        }
        synchronized (st) {
            return st.state;
        }
    }

    /** Trường mở rộng của {@code HEARTBEAT_ACK}, ví dụ {@code lastCastSeq} của Người 4. */
    public void onHeartbeat(BiConsumer<Session, Map<String, Long>> listener) {
        heartbeatListeners.add(listener);
    }

    public void onStateChange(BiConsumer<Session, PresenceState> listener) {
        stateListeners.add(listener);
    }

    private void changeState(Session s, State st, PresenceState next) {
        boolean changed;
        synchronized (st) {
            changed = st.state != next;
            st.state = next;
        }
        if (changed) {
            for (var l : stateListeners) {
                l.accept(s, next);
            }
        }
    }
}
