package labcast.server.ops;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * Cổng trạng thái HTTP {@code :8080/version} — trả về bản build đang chạy.
 *
 * <p>Đây là hạ tầng vận hành, KHÔNG phải một phần của phòng học: bước deploy trong
 * {@code .github/workflows/deploy.yml} gọi vào đây để xác nhận VPS đang chạy đúng commit vừa
 * merge. Server thật từ giai đoạn 1 vẫn phải khởi động lớp này.
 *
 * <p>Dùng {@code com.sun.net.httpserver} có sẵn trong JDK, tách hẳn khỏi cổng 7000–7002 — các cổng
 * đó thuộc về protocol LCP/1.0, multicast và tìm server do nhóm tự viết.
 *
 * <p>Cùng cổng này có trang dashboard {@code /} và số liệu {@code /api/dashboard} ({@link Dashboard}).
 * Hai đường này CHỈ trả lời yêu cầu từ chính máy server (loopback): trang có tên học viên, kết quả
 * chạy code và nhật ký, mà học viên cùng mạng LAN gõ được địa chỉ cổng 8080. {@code /version} thì
 * mở cho mọi nơi vì bước deploy gọi từ ngoài vào.
 */
public final class StatusEndpoint implements AutoCloseable {

    public static final int DEFAULT_PORT = 8080;

    private final HttpServer http;

    private StatusEndpoint(HttpServer http) {
        this.http = http;
    }

    /** Mở cổng trạng thái. Truyền {@code port = 0} để hệ điều hành tự chọn cổng trống (dùng khi test). */
    public static StatusEndpoint start(int port, BuildInfo info) throws IOException {
        HttpServer http = HttpServer.create(new InetSocketAddress(port), 0);
        byte[] body = info.toJson().getBytes(StandardCharsets.UTF_8);
        http.createContext("/version", exchange -> reply(exchange, body));
        http.start();
        return new StatusEndpoint(http);
    }

    private static void reply(HttpExchange exchange, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    /** Mở trang dashboard và API số liệu, chỉ cho truy cập từ chính máy server. */
    public void enableDashboard(Supplier<String> json) throws IOException {
        byte[] page;
        try (InputStream in = StatusEndpoint.class.getResourceAsStream("dashboard.html")) {
            page = in.readAllBytes();
        }
        http.createContext("/", exchange -> {
            if (!isLocal(exchange.getRemoteAddress())) {
                forbidden(exchange);
                return;
            }
            if (!exchange.getRequestURI().getPath().equals("/")) {
                send(exchange, 404, "text/plain; charset=utf-8", "Không có trang này".getBytes(StandardCharsets.UTF_8));
                return;
            }
            send(exchange, 200, "text/html; charset=utf-8", page);
        });
        http.createContext("/api/dashboard", exchange -> {
            if (!isLocal(exchange.getRemoteAddress())) {
                forbidden(exchange);
                return;
            }
            send(exchange, 200, "application/json; charset=utf-8", json.get().getBytes(StandardCharsets.UTF_8));
        });
    }

    /** Yêu cầu đến từ chính máy này (127.0.0.1, ::1). */
    static boolean isLocal(InetSocketAddress remote) {
        return remote != null
                && remote.getAddress() != null
                && remote.getAddress().isLoopbackAddress();
    }

    private static void forbidden(HttpExchange exchange) throws IOException {
        send(
                exchange,
                403,
                "text/plain; charset=utf-8",
                "Dashboard chỉ mở được trên chính máy chạy server: http://localhost:8080/"
                        .getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange exchange, int code, String type, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        if (exchange.getRequestMethod().equals("HEAD")) {
            exchange.sendResponseHeaders(code, -1); // HEAD không có thân
            exchange.close();
            return;
        }
        exchange.sendResponseHeaders(code, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    public int port() {
        return http.getAddress().getPort();
    }

    @Override
    public void close() {
        http.stop(0);
    }
}
