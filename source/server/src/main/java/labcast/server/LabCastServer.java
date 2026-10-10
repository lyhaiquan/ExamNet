package labcast.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.ProtocolException;
import labcast.server.account.AccountModule;
import labcast.server.journal.JournalModule;
import labcast.server.net.Connection;
import labcast.server.net.Router;
import labcast.server.net.TcpServer;
import labcast.server.presence.Presence;
import labcast.server.session.SessionManager;
import labcast.server.store.AuditLog;
import labcast.server.store.Database;

/** Lắp các phần của server: router, pool nghiệp vụ, vòng accept và các module. */
public final class LabCastServer implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(LabCastServer.class.getName());

    /** Danh sách module của server thật. Thêm module mới: mỗi module một dòng. */
    public static List<ServerModule> modules() {
        return List.of(
                new AccountModule(), // Người 2
                new JournalModule() // Người 1
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
        AuditLog audit = new AuditLog(db);
        SessionManager sessions = new SessionManager(db, audit, System::currentTimeMillis);
        Presence presence = new Presence(sessions::all, System::currentTimeMillis);
        this.ctx = new ServerContext(new Router(), db, scheduler, cfg, sessions, audit, presence);
        installCore();
        for (ServerModule m : modules) {
            m.install(ctx);
        }
    }

    private void installCore() {
        // App thoát có báo trước: đóng kết nối. Phiên vẫn giữ để đăng nhập lại.
        ctx.router().on(MessageType.BYE, (c, f) -> c.connection().close());
        // HEARTBEAT_ACK(i64 echoServerTime, list<{str key, i64 value}> fields)
        ctx.router().on(MessageType.HEARTBEAT_ACK, (c, f) -> {
            if (c.session() == null) {
                return;
            }
            PayloadReader r = f.reader();
            long echo = r.i64();
            int n = r.hasRemaining() ? r.u16() : 0;
            Map<String, Long> fields = new HashMap<>();
            for (int i = 0; i < n; i++) {
                fields.put(r.str(), r.i64());
            }
            ctx.presence().ack(c.session(), echo, fields, c.receivedAt());
        });
    }

    public void start() throws IOException {
        tcp = new TcpServer(
                null,
                ctx.config().port(),
                s -> new Connection(s, ctx.router(), pool, new Events(), System::currentTimeMillis));
        tcp.start();
        ctx.presence().start(scheduler);
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
            ctx.sessions().onDisconnect(c);
        }

        @Override
        public void badFrame(Connection c, ProtocolException e) {
            LOG.warning("Frame hỏng từ " + c.remoteAddress() + ": " + e.getMessage());
            ctx.audit().record(c.remoteAddress(), "BAD_FRAME", e.getMessage());
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
