package labcast.client.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import labcast.common.protocol.Frame;
import labcast.common.protocol.FrameCodec;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.common.transport.SocketOptions;

/**
 * Client LCP/1.0 của app. Một luồng đọc, một luồng ghi; mất kết nối thì tự nối lại sau 0,5 / 1 / 2 / 4
 * s (tối đa 5 s một lần), nối được thì gửi {@code HELLO} rồi {@code RESUME_REQ(token…)} để gắn lại
 * đúng phiên cũ.
 *
 * <p>Listener chạy trên luồng đọc mạng. Ai cập nhật giao diện thì tự bọc {@code Platform.runLater}.
 */
public final class LcpClient implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(LcpClient.class.getName());
    private static final long[] BACKOFF_MS = {500, 1_000, 2_000, 4_000, 5_000};
    public static final String CLIENT_VERSION = DiscoveryClient.CLIENT_VERSION;

    public enum State {
        CONNECTED,
        RECONNECTING,
        CLOSED
    }

    private record Waiter(Set<MessageType> types, CompletableFuture<Frame> future) {}

    private final Map<MessageType, List<Consumer<Frame>>> listeners = new ConcurrentHashMap<>();
    private final List<Waiter> waiters = new CopyOnWriteArrayList<>();
    private final List<Consumer<State>> stateListeners = new CopyOnWriteArrayList<>();
    private final Heartbeat heartbeat = new Heartbeat();
    private final BlockingQueue<Frame> outbox = new LinkedBlockingQueue<>();
    private final String device;

    private volatile String host;
    private volatile int port;
    private volatile Socket socket;
    private volatile boolean closed;
    private volatile State state = State.CLOSED;
    private volatile String token;
    private volatile Supplier<String> resumeLesson = () -> "";
    private int seq;

    public LcpClient() {
        this(defaultDevice());
    }

    public LcpClient(String device) {
        this.device = device;
        // Server chủ động chia tay (đăng nhập ở máy khác, server tắt): KHÔNG tự nối lại.
        on(MessageType.BYE, f -> {
            closed = true;
            closeSocket();
            setState(State.CLOSED);
        });
        on(MessageType.HEARTBEAT, f -> {
            try {
                send(heartbeat.ackFor(f));
            } catch (Exception e) {
                LOG.log(Level.FINE, "HEARTBEAT hỏng", e);
            }
        });
        Thread w = new Thread(this::writeLoop, "lcp-ghi");
        w.setDaemon(true);
        w.start();
    }

    /** Nối tới server rồi gửi {@code HELLO}. Ném IOException nếu không nối được (lần đầu không tự thử lại). */
    public void connect(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        this.closed = false;
        openSocket();
        setState(State.CONNECTED);
    }

    private void openSocket() throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), 3_000);
        SocketOptions.applyTcp(s);
        synchronized (this) {
            socket = s;
            seq = 0;
        }
        Thread r = new Thread(() -> readLoop(s), "lcp-doc");
        r.setDaemon(true);
        r.start();
        sendNow(Frame.of(
                MessageType.HELLO,
                new PayloadWriter().str(CLIENT_VERSION).str(device).toBytes()));
    }

    public void send(MessageType t, byte[] payload) {
        send(Frame.of(t, payload));
    }

    /** Xếp frame vào hàng gửi. Đang mất kết nối thì frame chờ trong hàng tới khi nối lại. */
    public void send(Frame f) {
        outbox.add(f);
    }

    /** Một listener được gọi cho MỌI frame loại {@code t}. */
    public void on(MessageType t, Consumer<Frame> listener) {
        listeners.computeIfAbsent(t, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void off(MessageType t, Consumer<Frame> listener) {
        List<Consumer<Frame>> l = listeners.get(t);
        if (l != null) {
            l.remove(listener);
        }
    }

    /** Frame kế tiếp thuộc một trong các loại. Gọi TRƯỚC khi gửi yêu cầu để không lỡ câu trả lời. */
    public CompletableFuture<Frame> expect(MessageType... types) {
        Set<MessageType> set = EnumSet.noneOf(MessageType.class);
        set.addAll(List.of(types));
        CompletableFuture<Frame> f = new CompletableFuture<>();
        waiters.add(new Waiter(set, f));
        return f;
    }

    public Heartbeat heartbeat() {
        return heartbeat;
    }

    public void onStateChange(Consumer<State> l) {
        stateListeners.add(l);
    }

    public State state() {
        return state;
    }

    /** Token phiên (sau {@code AUTH_OK}), dùng để {@code RESUME_REQ} khi nối lại. */
    public void setToken(String token) {
        this.token = token;
    }

    /** Bài đang mở, gửi kèm {@code RESUME_REQ} để server trả đúng seq code của bài đó. */
    public void setResumeLesson(Supplier<String> lesson) {
        this.resumeLesson = lesson;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            sendNow(Frame.of(
                    MessageType.BYE, new PayloadWriter().str("CLIENT_EXIT").toBytes()));
        } catch (IOException ignored) {
            // đã mất kết nối
        }
        closeSocket();
        setState(State.CLOSED);
    }

    private void readLoop(Socket s) {
        try {
            InputStream in = new BufferedInputStream(s.getInputStream());
            Frame f;
            while ((f = FrameCodec.read(in)) != null) {
                dispatch(f);
            }
        } catch (IOException e) {
            // mất kết nối
        }
        if (s == socket && !closed) {
            reconnect();
        }
    }

    private void dispatch(Frame f) {
        for (Waiter w : waiters) {
            if (w.types.contains(f.type()) && waiters.remove(w)) {
                w.future.complete(f);
                break;
            }
        }
        for (Consumer<Frame> l : listeners.getOrDefault(f.type(), List.of())) {
            try {
                l.accept(f);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Listener " + f.type() + " lỗi", e);
            }
        }
    }

    /** Nối lại với thời gian chờ tăng dần, rồi gửi RESUME_REQ với token cũ. */
    private void reconnect() {
        closeSocket();
        setState(State.RECONNECTING);
        for (int attempt = 0; !closed; attempt++) {
            try {
                Thread.sleep(BACKOFF_MS[Math.min(attempt, BACKOFF_MS.length - 1)]);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                openSocket();
                String t = token;
                if (t != null) {
                    sendNow(Frame.of(
                            MessageType.RESUME_REQ,
                            new PayloadWriter()
                                    .str(t)
                                    .str(resumeLesson.get())
                                    .i64(-1)
                                    .toBytes()));
                }
                setState(State.CONNECTED);
                return;
            } catch (IOException e) {
                LOG.fine("Nối lại thất bại lần " + (attempt + 1) + ": " + e.getMessage());
            }
        }
    }

    /** Gửi ngay trên luồng gọi, trước mọi frame đang chờ trong hàng (HELLO, RESUME_REQ khi vừa nối). */
    private synchronized void sendNow(Frame f) throws IOException {
        Socket s = socket;
        if (s == null) {
            throw new IOException("chưa nối");
        }
        OutputStream out = s.getOutputStream();
        FrameCodec.write(out, f.withSeq(++seq));
        out.flush();
    }

    private void writeLoop() {
        List<Frame> batch = new ArrayList<>();
        while (true) {
            Frame f;
            try {
                f = outbox.take();
            } catch (InterruptedException e) {
                return;
            }
            batch.add(f);
            outbox.drainTo(batch);
            while (!batch.isEmpty()) {
                if (closed) {
                    batch.clear();
                    break;
                }
                try {
                    writeBatch(batch);
                    batch.clear();
                } catch (IOException e) {
                    // mất kết nối: giữ batch, chờ luồng đọc nối lại rồi gửi tiếp
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ie) {
                        return;
                    }
                }
            }
        }
    }

    private synchronized void writeBatch(List<Frame> batch) throws IOException {
        Socket s = socket;
        if (s == null || s.isClosed() || state != State.CONNECTED) {
            throw new IOException("chưa nối");
        }
        OutputStream out = new BufferedOutputStream(s.getOutputStream(), 64 * 1024);
        for (Frame f : batch) {
            FrameCodec.write(out, f.withSeq(++seq));
        }
        out.flush();
    }

    private void closeSocket() {
        Socket s = socket;
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {
                // bỏ
            }
        }
    }

    private void setState(State s) {
        if (state == s) {
            return;
        }
        state = s;
        for (Consumer<State> l : stateListeners) {
            try {
                l.accept(s);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Listener trạng thái lỗi", e);
            }
        }
    }

    private static String defaultDevice() {
        String name = System.getenv("COMPUTERNAME");
        if (name == null) {
            name = System.getenv("HOSTNAME");
        }
        return name == null ? "may-khong-ten" : name;
    }
}
