package examnet.server.ops;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Cổng trạng thái HTTP {@code :8080/version} — trả về bản build đang chạy.
 *
 * <p>Đây là hạ tầng vận hành, KHÔNG phải một phần của hệ thống thi: bước deploy trong
 * {@code .github/workflows/deploy.yml} gọi vào đây để xác nhận VPS đang chạy đúng commit vừa
 * merge. Server thật ở phase 2 vẫn phải khởi động lớp này.
 *
 * <p>Dùng {@code com.sun.net.httpserver} có sẵn trong JDK để không đụng tới cổng 5000/5001 —
 * hai cổng đó thuộc về protocol EXP/1.0 và WebSocket do nhóm tự viết.
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

    public int port() {
        return http.getAddress().getPort();
    }

    @Override
    public void close() {
        http.stop(0);
    }
}
