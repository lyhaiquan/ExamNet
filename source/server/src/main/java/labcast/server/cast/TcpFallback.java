package labcast.server.cast;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Máy không nhận được multicast (tường lửa, switch chặn IGMP, Wi-Fi kém) tự chuyển sang nhận chiếu qua
 * TCP (spec §12). App gửi kèm heartbeat {@code lastCastSeq} — SEQ lớn nhất nó đã nhận. Đang chiếu mà
 * một máy báo −1, hoặc tụt sau SEQ server đã phát cách đây {@link #LAG_MS} (3 chu kỳ khung khoá), trong
 * {@link #LAG_HEARTBEATS} heartbeat liền, thì từ đó mọi gói chiếu cũng gửi cho máy đó qua TCP.
 *
 * <p>Máy ở chế độ TCP tới khi nối lại (kết nối mới thì thử multicast lại từ đầu).
 */
final class TcpFallback {
    static final int LAG_HEARTBEATS = 3;
    static final long LAG_MS = 3 * CastModule.KEYFRAME_MS;

    private record Mark(long at, int seq) {}

    private final LongSupplier now;
    private final ArrayDeque<Mark> history = new ArrayDeque<>();
    private final Map<Long, Integer> lagCount = new ConcurrentHashMap<>();
    /** userId → id kết nối lúc chuyển. */
    private final Map<Long, Long> viaTcp = new ConcurrentHashMap<>();

    TcpFallback(LongSupplier now) {
        this.now = now;
    }

    /** Ghi SEQ server đã phát tới, mỗi lần phát khung khoá. */
    synchronized void recordServerSeq(int lastSeq) {
        long t = now.getAsLong();
        history.addLast(new Mark(t, lastSeq));
        // Bỏ mốc đầu khi mốc thứ hai cũng đã cũ hơn LAG_MS: vẫn giữ đúng một mốc cũ hơn LAG_MS để biết
        // "6 s trước server ở đâu"
        while (history.size() > 1) {
            Iterator<Mark> it = history.iterator();
            it.next();
            if (t - it.next().at < LAG_MS) {
                break;
            }
            history.removeFirst();
        }
    }

    /** SEQ server đã phát tới cách đây {@link #LAG_MS}; 0 nếu buổi chiếu chưa dài tới đó. */
    synchronized int requiredSeq() {
        long t = now.getAsLong();
        int seq = 0;
        for (Mark m : history) {
            if (t - m.at >= LAG_MS) {
                seq = m.seq;
            }
        }
        return seq;
    }

    /** Một heartbeat của học viên. Trả {@code true} nếu máy vừa được chuyển sang TCP ở lần này. */
    boolean onHeartbeat(long userId, long connectionId, long lastCastSeq, boolean casting) {
        if (!casting) {
            lagCount.remove(userId);
            return false;
        }
        if (viaTcp(userId, connectionId)) {
            return false;
        }
        boolean lag = lastCastSeq < 0 || lastCastSeq < requiredSeq();
        if (!lag) {
            lagCount.remove(userId);
            return false;
        }
        int n = lagCount.merge(userId, 1, Integer::sum);
        if (n >= LAG_HEARTBEATS) {
            lagCount.remove(userId);
            viaTcp.put(userId, connectionId);
            return true;
        }
        return false;
    }

    boolean viaTcp(long userId, long connectionId) {
        Long c = viaTcp.get(userId);
        return c != null && c == connectionId;
    }

    /** Số máy đang nhận chiếu qua TCP (có thể gồm máy đã nối lại, chưa dọn). */
    int count() {
        return viaTcp.size();
    }
}
