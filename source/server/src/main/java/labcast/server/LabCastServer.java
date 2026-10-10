package labcast.server;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.ProtocolException;
import labcast.server.net.Connection;
import labcast.server.net.Router;
import labcast.server.net.TcpServer;
import labcast.server.store.Database;

/** Lắp các phần của server: router, pool nghiệp vụ, vòng accept và các module. */
public final class LabCastServer implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(LabCastServer.class.getName());

    /** Danh sách module của server thật. Thêm module mới: mỗi module một dòng. */
    public static List<ServerModule> modules() {
        return List.of(
                // thêm module mới ở đây, mỗi module một dòng
                );
    }

    private final ServerContext ctx;
    private final ExecutorService pool;
    private final ScheduledExecutorService scheduler;
    private TcpServer tcp;

    public LabCastServer(ServerConfig cfg, Database db, List<ServerModule> modules) {
        this.pool = Executors.newFixedThreadPool(
                Math.max(4, Runtime.getRuntime().availableProcessors() * 2), named("nghiep-vu"));
        this.scheduler = Executors.newScheduledThreadPool(2, named("hen-gio"));
        this.ctx = new ServerContext(new Router(), db, scheduler, cfg);
        installCore();
        for (ServerModule m : modules) {
            m.install(ctx);
        }
    }

    private void installCore() {
        // App thoát có báo trước: đóng kết nối. Phiên vẫn giữ để đăng nhập lại.
        ctx.router().on(MessageType.BYE, (c, f) -> c.connection().close());
    }

    public void start() throws IOException {
        tcp = new TcpServer(
                null,
                ctx.config().port(),
                s -> new Connection(s, ctx.router(), pool, new Events(), System::currentTimeMillis));
        tcp.start();
        LOG.info("LabCast nghe TCP " + tcp.port());
    }

    public int port() {
        return tcp.port();
    }

    public ServerContext context() {
        return ctx;
    }

    @Override
    public void close() {
        if (tcp != null) {
            tcp.close();
        }
        scheduler.shutdownNow();
        pool.shutdown();
        try {
            pool.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private final class Events implements Connection.Events {
        @Override
        public void closed(Connection c) {
            tcp.forget(c);
        }

        @Override
        public void badFrame(Connection c, ProtocolException e) {
            LOG.warning("Frame hỏng từ " + c.remoteAddress() + ": " + e.getMessage());
        }
    }

    private static ThreadFactory named(String prefix) {
        AtomicInteger n = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, prefix + "-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }
}
