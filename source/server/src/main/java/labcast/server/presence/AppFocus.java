package labcast.server.presence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;
import labcast.server.session.Session;
import labcast.server.store.AuditLog;

/**
 * Học viên đang ở trong app hay đã rời (Alt+Tab, phím Windows, thu nhỏ) — theo tin {@code APP_FOCUS} từ
 * app học viên. Chủ: Người 2. Sơ đồ lớp (task 2.12) đọc {@link #state(long)} để hiện cờ "rời app".
 *
 * <p>Mọi mốc giờ lấy theo đồng hồ server lúc nhận tin, không tin giờ của máy học viên (spec R3). Chỉ đổi
 * trạng thái mới tính: gửi lặp cùng một trạng thái không làm gì. Ghi {@code audit_log} tối đa một dòng
 * mỗi giây cho mỗi học viên, để một app bị sửa gửi bật/tắt liên tục không làm nghẽn database.
 */
public final class AppFocus {
    static final long AUDIT_MIN_GAP_MS = 1_000;

    /**
     * @param since lúc đổi sang trạng thái hiện tại (giờ server)
     * @param leaves số lần rời app từ khi server chạy
     * @param awayMs tổng thời gian ở ngoài app, CHƯA tính lần rời đang diễn ra
     */
    public record State(boolean inApp, String lessonId, long since, int leaves, long awayMs) {
        /** Tổng thời gian ở ngoài app tính tới {@code now}, kể cả lần rời đang diễn ra. */
        public long awayMs(long now) {
            return awayMs + (inApp ? 0 : Math.max(0, now - since));
        }
    }

    private final Map<Long, State> byUser = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastAudit = new ConcurrentHashMap<>();
    private final List<BiConsumer<Session, State>> listeners = new CopyOnWriteArrayList<>();
    private final AuditLog audit;
    private final LongSupplier clock;

    public AppFocus(AuditLog audit, LongSupplier clock) {
        this.audit = audit;
        this.clock = clock;
    }

    /** Nhận một tin {@code APP_FOCUS}. Trả true nếu trạng thái đổi. */
    public boolean update(Session s, boolean inApp, String lessonId) {
        long now = clock.getAsLong();
        State prev = byUser.getOrDefault(s.userId(), new State(true, "", now, 0, 0));
        if (prev.inApp() == inApp) {
            return false;
        }
        State next = inApp
                ? new State(true, lessonId, now, prev.leaves(), prev.awayMs(now))
                : new State(false, lessonId, now, prev.leaves() + 1, prev.awayMs());
        byUser.put(s.userId(), next);
        Long last = lastAudit.get(s.userId());
        if (audit != null && (last == null || now - last >= AUDIT_MIN_GAP_MS)) {
            lastAudit.put(s.userId(), now);
            audit.record(
                    s.username(),
                    inApp ? "APP_RETURN" : "APP_LEAVE",
                    (lessonId.isEmpty() ? "" : lessonId + ", ")
                            + (inApp ? "ở ngoài " + (now - prev.since()) / 1000 + " s" : "lần " + next.leaves()));
        }
        listeners.forEach(l -> l.accept(s, next));
        return true;
    }

    public Optional<State> state(long userId) {
        return Optional.ofNullable(byUser.get(userId));
    }

    /** Nghe mỗi lần học viên rời hoặc quay lại (sơ đồ lớp đẩy cho giáo viên). */
    public void onChange(BiConsumer<Session, State> listener) {
        listeners.add(listener);
    }
}
