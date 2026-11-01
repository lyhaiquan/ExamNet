package labcast.bench;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Proxy TCP đứng giữa app và server để cấy điều kiện mạng: trễ cố định mỗi chiều (có thể lệch nhau) và
 * "rút dây" — đóng mọi kết nối đang có, từ chối kết nối mới tới khi cắm lại.
 *
 * <p>Trễ làm bằng hàng đợi: luồng đọc ghi kèm thời điểm được phép gửi, luồng ghi chờ tới lúc đó. Trễ cố
 * định nên thứ tự byte giữ nguyên như TCP thật.
 */
public final class DelayProxy implements AutoCloseable {
    private final ServerSocket listen;
    private final InetSocketAddress target;
    private final List<Socket> open = new CopyOnWriteArrayList<>();
    private volatile long upMs;
    private volatile long downMs;
    private volatile boolean unplugged;
    private volatile boolean closed;

    /** {@code upMs}: trễ chiều app → server; {@code downMs}: chiều server → app. */
    public DelayProxy(int targetPort, long upMs, long downMs) throws IOException {
        this.target = new InetSocketAddress("127.0.0.1", targetPort);
        this.upMs = upMs;
        this.downMs = downMs;
        this.listen = new ServerSocket(0);
        Thread t = new Thread(this::acceptLoop, "proxy-accept");
        t.setDaemon(true);
        t.start();
    }

    public int port() {
        return listen.getLocalPort();
    }

    /** Rút dây: cắt mọi kết nối, kết nối mới bị đóng ngay. */
    public void unplug() {
        unplugged = true;
        for (Socket s : open) {
            closeQuietly(s);
        }
        open.clear();
    }

    public void plug() {
        unplugged = false;
    }

    private void acceptLoop() {
        while (!closed) {
            try {
                Socket app = listen.accept();
                if (unplugged) {
                    closeQuietly(app);
                    continue;
                }
                Socket server = new Socket();
                server.connect(target, 3_000);
                app.setTcpNoDelay(true);
                server.setTcpNoDelay(true);
                open.add(app);
                open.add(server);
                pipe(app, server, true);
                pipe(server, app, false);
            } catch (IOException e) {
                // listen đóng khi close(); lỗi nối một cặp thì bỏ cặp đó
            }
        }
    }

    private record Chunk(long sendAt, byte[] data) {}

    private void pipe(Socket from, Socket to, boolean up) {
        LinkedBlockingQueue<Chunk> q = new LinkedBlockingQueue<>();
        Thread reader = new Thread(
                () -> {
                    byte[] buf = new byte[16 * 1024];
                    try (InputStream in = from.getInputStream()) {
                        int n;
                        while ((n = in.read(buf)) > 0) {
                            long delay = up ? upMs : downMs;
                            q.put(new Chunk(System.nanoTime() + delay * 1_000_000L, java.util.Arrays.copyOf(buf, n)));
                        }
                    } catch (IOException | InterruptedException e) {
                        // một đầu đóng
                    }
                    q.add(new Chunk(0, null));
                },
                "proxy-doc");
        Thread writer = new Thread(
                () -> {
                    try (OutputStream out = to.getOutputStream()) {
                        while (true) {
                            Chunk c = q.take();
                            if (c.data == null) break;
                            long wait = c.sendAt - System.nanoTime();
                            if (wait > 0) Thread.sleep(wait / 1_000_000L, (int) (wait % 1_000_000L));
                            out.write(c.data);
                            out.flush();
                        }
                    } catch (IOException | InterruptedException e) {
                        // đầu kia đóng
                    }
                    closeQuietly(from);
                    closeQuietly(to);
                },
                "proxy-ghi");
        reader.setDaemon(true);
        writer.setDaemon(true);
        reader.start();
        writer.start();
    }

    private static void closeQuietly(Socket s) {
        try {
            s.close();
        } catch (IOException ignored) {
            // đã đóng
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
        unplug();
        listen.close();
    }
}
