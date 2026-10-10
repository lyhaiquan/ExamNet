package labcast.server;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import labcast.server.net.Router;
import labcast.server.store.Database;

/**
 * Những gì một module cần khi cắm vào server. Module cung cấp dịch vụ cho module khác (ví dụ {@code
 * CodeStore} cho chạy code) thì {@link #register} nó; bên dùng lấy bằng {@link #require} lúc xử lý
 * thông điệp, nên thứ tự cắm module không quan trọng.
 */
public final class ServerContext {
    private final Router router;
    private final Database db;
    private final ScheduledExecutorService scheduler;
    private final ServerConfig config;
    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    public ServerContext(Router router, Database db, ScheduledExecutorService scheduler, ServerConfig config) {
        this.router = router;
        this.db = db;
        this.scheduler = scheduler;
        this.config = config;
    }

    public Router router() {
        return router;
    }

    public Database db() {
        return db;
    }

    public ScheduledExecutorService scheduler() {
        return scheduler;
    }

    public ServerConfig config() {
        return config;
    }

    public <T> void register(Class<T> type, T service) {
        if (services.putIfAbsent(type, service) != null) {
            throw new IllegalStateException("Đã có dịch vụ " + type.getSimpleName());
        }
    }

    public <T> Optional<T> find(Class<T> type) {
        return Optional.ofNullable(type.cast(services.get(type)));
    }

    public <T> T require(Class<T> type) {
        return find(type)
                .orElseThrow(() -> new IllegalStateException("Chưa có module nào cung cấp " + type.getSimpleName()));
    }
}
