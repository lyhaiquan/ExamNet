package labcast.server.practice;

import java.time.Duration;

/**
 * Thùng token: chứa tối đa {@code burst} token, cứ {@code every} nạp lại một token. Mỗi lượt lấy một token;
 * thùng rỗng thì lượt đó bị từ chối kèm số ms phải chờ tới token kế. Ví dụ {@code RUN_REQ} 6 lượt/phút,
 * đợt tối đa 3: {@code new TokenBucket(3, Duration.ofSeconds(10), now)}.
 *
 * <p>Thời gian do người gọi đưa vào (đồng hồ server), nên test dùng đồng hồ giả được. An toàn đa luồng.
 */
public final class TokenBucket {
    private final long everyMs;
    private final long capacityMs;
    /** Token tính bằng ms đã tích được: một token = {@code everyMs}. Số nguyên nên nạp lẻ không bị làm tròn mất. */
    private long creditMs;

    private long last;

    public TokenBucket(int burst, Duration every, long now) {
        if (burst < 1 || every.toMillis() < 1) {
            throw new IllegalArgumentException("burst ≥ 1, every ≥ 1 ms");
        }
        this.everyMs = every.toMillis();
        this.capacityMs = burst * everyMs;
        this.creditMs = capacityMs;
        this.last = now;
    }

    /** Lấy một token: trả 0 nếu được, hoặc số ms (≥ 1) phải chờ tới khi có token. */
    public synchronized long tryTake(long now) {
        if (now > last) {
            creditMs = Math.min(capacityMs, creditMs + (now - last));
            last = now;
        }
        if (creditMs >= everyMs) {
            creditMs -= everyMs;
            return 0;
        }
        return everyMs - creditMs;
    }
}
