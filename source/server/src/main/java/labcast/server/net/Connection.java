package labcast.server.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import labcast.common.protocol.ByeReason;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.FrameCodec;
import labcast.common.protocol.ProtocolException;
import labcast.server.session.Session;

/**
 * Một kết nối TCP tới một app. Hai luồng riêng:
 *
 * <pre>
 * luồng đọc:  FrameCodec.read ─► ghi receivedAt ─► đếm dồn ─► FramePolicy.check ─► pool nghiệp vụ ─► Router.route
 * luồng ghi:  OutboundQueue.take ─► FrameCodec.write ─► flush khi hàng đợi rỗng
 * </pre>
 *
 * Chỉ luồng ghi được ghi vào socket, nên không cần khoá quanh {@code write}.
 */
public final class Connection implements AutoCloseable {

    /** Báo cho server khi kết nối đóng, gửi frame hỏng hoặc gửi dồn quá mức. */
    public interface Events {
        void closed(Connection c);

        default void badFrame(Connection c, ProtocolException e) {}

        /** Bị đóng vì gửi quá {@link #FLOOD_PER_SECOND} frame/giây suốt {@link #FLOOD_SECONDS} giây. */
        default void flooded(Connection c) {}

        Events NONE = c -> {};
    }

    public static final int QUEUE_CAPACITY = 256;
    /** Quá ngần này frame trong một giây là một giây "dồn" (spec §12). */
    public static final int FLOOD_PER_SECOND = 500;
    /** Dồn liền ngần này giây thì {@code BYE(TOO_MANY_MESSAGES)} và đóng kết nối. */
    public static final int FLOOD_SECONDS = 5;

    public static final Duration NORMAL_WAIT = Duration.ofSeconds(5);

    private static final AtomicLong IDS = new AtomicLong();
    /** Hẹn giờ đóng hẳn kết nối khi luồng ghi bị kẹt lúc đang gửi lời chào tạm biệt. */
    private static final ScheduledExecutorService CLOSER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "dong-ket-noi");
        t.setDaemon(true);
        return t;
    });

    private final long id = IDS.incrementAndGet();
    private final Socket socket;
    private final Router router;
    private final SerialExecutor serial;
    private final Events events;
    private final LongSupplier clock;
    private final OutboundQueue queue = new OutboundQueue(QUEUE_CAPACITY);
    private final AtomicInteger seq = new AtomicInteger();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile boolean closing;
    private volatile Session session;
    private final Map<String, Object> attrs = new ConcurrentHashMap<>();
    // đếm frame dồn — chỉ luồng đọc đụng tới
    private long floodWindowStart = Long.MIN_VALUE / 2;
    private int floodCount;
    private int floodSeconds;
    private Thread reader;
    private Thread writer;

    /** Chưa chạy luồng nào cho tới {@link #start()} — test có thể dùng {@code new Socket()} chưa nối. */
    public Connection(Socket socket, Router router, Executor pool, Events events, LongSupplier clock) {
        this.socket = socket;
        this.router = router;
        this.serial = new SerialExecutor(pool);
        this.events = events;
        this.clock = clock;
    }

    public void start() {
        reader = new Thread(this::readLoop, "doc-" + id);
        writer = new Thread(this::writeLoop, "ghi-" + id);
        reader.setDaemon(true);
        writer.setDaemon(true);
        writer.start();
        reader.start();
    }

    public long id() {
        return id;
    }

    public String remoteAddress() {
        var a = socket.getRemoteSocketAddress();
        return a == null ? "?" : a.toString();
    }

    public Session session() {
        return session;
    }

    public void bindSession(Session s) {
        this.session = s;
    }

    /** Thông tin gắn với kết nối, ví dụ tên máy gửi trong HELLO. */
    public Object attr(String key) {
        return attrs.get(key);
    }

    public void setAttr(String key, Object value) {
        attrs.put(key, value);
    }

    public OutboundQueue queue() {
        return queue;
    }

    public boolean isClosed() {
        return closed.get();
    }

    public boolean send(Frame f, Priority p) {
        return send(f, p, null);
    }

    /**
     * Đưa frame vào hàng đợi ghi. SEQ gán lúc ghi ra socket (frame có SEQ 0), nên SEQ trên dây luôn
     * tăng dần kể cả khi CRITICAL chen hàng; frame đã có SEQ riêng (gói chiếu qua TCP) giữ nguyên.
     */
    public boolean send(Frame f, Priority p, String conflateKey) {
        return send(f, p, conflateKey, NORMAL_WAIT);
    }

    boolean send(Frame f, Priority p, String conflateKey, Duration wait) {
        if (closed.get() || closing) {
            return false;
        }
        try {
            return queue.offer(f, p, conflateKey, wait);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Gửi một frame cuối (ERROR, BYE) rồi đóng khi đã ghi xong; quá 2 s thì đóng luôn. */
    public void sendAndClose(Frame last) {
        if (closed.get() || closing) {
            return;
        }
        try {
            queue.offer(last, Priority.CRITICAL, null, Duration.ZERO);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        closing = true;
        if (writer == null) {
            close();
            return;
        }
        CLOSER.schedule(this::close, 2, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // đang đóng
        }
        if (writer != null) {
            writer.interrupt();
        }
        events.closed(this);
    }

    private void readLoop() {
        // KHÔNG dùng try-with-resources: đóng InputStream của socket là đóng cả socket, khi đó luồng
        // ghi không kịp gửi ERROR(BAD_FRAME) cuối cùng.
        try {
            InputStream in = new BufferedInputStream(socket.getInputStream());
            while (!closed.get() && !closing) {
                Frame f = FrameCodec.read(in);
                if (f == null) {
                    break;
                }
                long at = clock.getAsLong();
                if (flooding(at)) {
                    events.flooded(this);
                    sendAndClose(ErrorMessage.bye(ByeReason.TOO_MANY_MESSAGES));
                    return;
                }
                FramePolicy.Decision d = router.policy().check(this, f.type());
                if (!d.allowed()) {
                    if (d.code() != null) {
                        String msg = d.message() != null ? d.message() : "Bị chặn: " + f.type();
                        send(
                                new ErrorMessage(d.code(), msg, d.retryAfterMs()).toFrame(),
                                Priority.NORMAL,
                                null,
                                Duration.ZERO);
                    }
                    continue;
                }
                Ctx ctx = new Ctx(this, at);
                serial.execute(() -> router.route(ctx, f));
            }
        } catch (ProtocolException e) {
            events.badFrame(this, e);
            sendAndClose(new ErrorMessage(ErrorCode.BAD_FRAME, e.getMessage(), 0).toFrame());
            return;
        } catch (IOException e) {
            // hết giờ đọc, mất kết nối, hoặc đóng giữa frame: đều là kết nối chết
        }
        if (!closing) {
            close();
        }
    }

    /**
     * Đếm frame theo từng giây (đồng hồ của kết nối), tính cả frame bị luật chặn và frame trước khi đăng
     * nhập. True khi đã dồn quá {@link #FLOOD_PER_SECOND} frame/giây suốt {@link #FLOOD_SECONDS} giây liền.
     * Chỉ luồng đọc gọi.
     */
    private boolean flooding(long now) {
        long elapsed = now - floodWindowStart;
        if (elapsed >= 1_000) {
            // giây vừa qua có dồn không; cách hơn một giây (có giây im lặng ở giữa) thì đếm lại từ đầu
            floodSeconds = floodCount > FLOOD_PER_SECOND && elapsed < 2_000 ? floodSeconds + 1 : 0;
            floodWindowStart = now;
            floodCount = 0;
        }
        floodCount++;
        return floodCount > FLOOD_PER_SECOND && floodSeconds >= FLOOD_SECONDS - 1;
    }

    private void writeLoop() {
        try (OutputStream out = new BufferedOutputStream(socket.getOutputStream(), 64 * 1024)) {
            while (!closed.get()) {
                Frame f = queue.take();
                if (f.seq() == 0) {
                    f = f.withSeq(seq.incrementAndGet());
                }
                FrameCodec.write(out, f);
                if (queue.size() == 0) {
                    out.flush();
                    if (closing) {
                        socket.shutdownOutput();
                        break;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // socket đã đóng
        }
        close();
    }

    @Override
    public String toString() {
        return "Connection#" + id + "(" + remoteAddress() + ")";
    }
}
