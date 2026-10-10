package labcast.client.clock;

import java.util.function.LongSupplier;
import labcast.client.net.LcpClient;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;

/**
 * Ước lượng độ lệch giữa đồng hồ máy này và đồng hồ server, kiểu NTP (ĐG3). Bản giai đoạn 1: giữ mẫu
 * có độ trễ (delay) nhỏ nhất — mẫu đó ít bị hàng đợi mạng làm lệch nhất. Giai đoạn 2 thêm đồng bộ lại
 * định kỳ, bù trôi và chỉnh dần.
 *
 * <pre>
 * offset = ((t2 − t1) + (t3 − t4)) / 2      delay = (t4 − t1) − (t3 − t2)
 * </pre>
 *
 * t1, t4 theo đồng hồ máy này; t2, t3 theo đồng hồ server.
 */
public final class ClockSync implements Clock {
    private final LongSupplier local;
    private long offset;
    private long bestDelay = Long.MAX_VALUE;

    public ClockSync() {
        this(System::currentTimeMillis);
    }

    /** {@code local}: đồng hồ máy, thay bằng đồng hồ giả khi test. */
    public ClockSync(LongSupplier local) {
        this.local = local;
    }

    /** Thêm một mẫu; trả true nếu mẫu này tốt hơn (delay nhỏ hơn) và được dùng. Delay âm thì bỏ. */
    public synchronized boolean addSample(long t1, long t2, long t3, long t4) {
        long delay = (t4 - t1) - (t3 - t2);
        if (delay < 0 || delay >= bestDelay) {
            return false;
        }
        bestDelay = delay;
        offset = ((t2 - t1) + (t3 - t4)) / 2;
        return true;
    }

    public synchronized long offsetMs() {
        return offset;
    }

    /** Delay của mẫu đang dùng; {@code Long.MAX_VALUE} khi chưa có mẫu nào. */
    public synchronized long bestDelayMs() {
        return bestDelay;
    }

    public synchronized boolean isSynced() {
        return bestDelay != Long.MAX_VALUE;
    }

    /**
     * Đo ngay sau khi vào lớp: gửi 5 {@code TIME_SYNC_REQ(t1)} cách nhau 100 ms, mỗi {@code
     * TIME_SYNC_RESP(t1, t2, t3)} ghi {@code t4} ngay khi nhận rồi thêm mẫu. Giai đoạn 2 đo lại định kỳ.
     */
    public void start(LcpClient client) {
        client.on(MessageType.TIME_SYNC_RESP, f -> {
            long t4 = localNow();
            try {
                PayloadReader r = f.reader();
                addSample(r.i64(), r.i64(), r.i64(), t4);
            } catch (Exception ignored) {
                // mẫu hỏng: bỏ
            }
        });
        Thread t = new Thread(
                () -> {
                    for (int i = 0; i < 5; i++) {
                        client.send(
                                MessageType.TIME_SYNC_REQ,
                                new PayloadWriter().i64(localNow()).toBytes());
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                },
                "dong-bo-gio");
        t.setDaemon(true);
        t.start();
    }

    /** Giờ máy, để ghi t1, t4. */
    public long localNow() {
        return local.getAsLong();
    }

    @Override
    public synchronized long serverNow() {
        return local.getAsLong() + offset;
    }
}
