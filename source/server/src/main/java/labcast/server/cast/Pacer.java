package labcast.server.cast;

import java.util.function.LongSupplier;

/**
 * Giãn nhịp theo thùng token tính bằng byte (spec §11). Phát dồn cả trace một lúc sẽ tràn bộ đệm nhận
 * của máy học viên và gây mất gói, nên mỗi datagram phải xin đủ token trước khi gửi; thiếu thì ngủ.
 * Chỉ gọi từ luồng phát chiếu riêng: hàm này ngủ.
 */
final class Pacer {
    /** Ngủ đúng ngần ấy nano giây; test thay bằng đồng hồ giả. */
    @FunctionalInterface
    interface Sleeper {
        void sleepNanos(long nanos) throws InterruptedException;
    }

    private final double bytesPerNano;
    private final long burstBytes;
    private final LongSupplier nanoTime;
    private final Sleeper sleeper;
    private double tokens;
    private long last;

    Pacer(long bytesPerSecond, long burstBytes, LongSupplier nanoTime, Sleeper sleeper) {
        this.bytesPerNano = bytesPerSecond / 1e9;
        this.burstBytes = burstBytes;
        this.nanoTime = nanoTime;
        this.sleeper = sleeper;
        this.tokens = burstBytes;
        this.last = nanoTime.getAsLong();
    }

    /** 2 MB/s, đợt tối đa 64 KB (spec §11). */
    static Pacer standard() {
        return new Pacer(2L * 1024 * 1024, 64 * 1024, System::nanoTime, n -> {
            if (n > 0) {
                Thread.sleep(n / 1_000_000, (int) (n % 1_000_000));
            }
        });
    }

    synchronized void acquire(int bytes) throws InterruptedException {
        long now = nanoTime.getAsLong();
        tokens = Math.min(burstBytes, tokens + (now - last) * bytesPerNano);
        last = now;
        tokens -= bytes;
        if (tokens < 0) {
            // Ngủ đúng phần thiếu; lần xin sau cộng lại thời gian ngủ nên token về 0, không bị tính hai lần
            sleeper.sleepNanos((long) Math.ceil(-tokens / bytesPerNano));
        }
    }
}
