package labcast.server.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import labcast.common.transport.SocketOptions;

/** Vòng accept trên cổng TCP 7000. Mỗi kết nối được giao cho {@code factory} dựng và chạy. */
public final class TcpServer implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(TcpServer.class.getName());

    private final ServerSocket serverSocket;
    private final Function<Socket, Connection> factory;
    private final Set<Connection> live = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    /** {@code port} 0 để hệ điều hành chọn cổng trống (test). {@code bind} null = mọi card. */
    public TcpServer(InetAddress bind, int port, Function<Socket, Connection> factory) throws IOException {
        this.serverSocket = new ServerSocket(port, 100, bind);
        this.factory = factory;
    }

    public void start() {
        Thread t = new Thread(this::acceptLoop, "accept-" + port());
        t.setDaemon(true);
        t.start();
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public Collection<Connection> connections() {
        return live;
    }

    /** Gọi khi kết nối đóng, để danh sách không giữ kết nối chết. */
    public void forget(Connection c) {
        live.remove(c);
    }

    private void acceptLoop() {
        while (!closed) {
            Socket s;
            try {
                s = serverSocket.accept();
            } catch (IOException e) {
                if (!closed) {
                    LOG.log(Level.WARNING, "accept lỗi", e);
                }
                continue;
            }
            try {
                SocketOptions.applyTcp(s);
                Connection c = factory.apply(s);
                live.add(c);
                c.start();
            } catch (IOException | RuntimeException e) {
                LOG.log(Level.WARNING, "Không dựng được kết nối từ " + s.getRemoteSocketAddress(), e);
                try {
                    s.close();
                } catch (IOException ignored) {
                    // bỏ
                }
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        try {
            serverSocket.close();
        } catch (IOException ignored) {
            // bỏ
        }
        for (Connection c : live) {
            c.close();
        }
    }
}
