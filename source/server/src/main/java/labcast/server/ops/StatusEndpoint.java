package labcast.server.ops;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import labcast.server.admin.AdminApi;

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

    /** Thân POST tối đa: đủ cho file CSV vài nghìn học viên. */
    static final int MAX_BODY = 1 << 20;

    /**
     * Mở trang quản trị {@code /admin} và API {@code /api/admin/*}, chỉ cho chính máy server.
     *
     * <p>Lệnh sửa (POST) còn phải có header {@code X-LabCast-Admin: 1} và {@code Host} là localhost: trang web
     * lạ mở trên máy server không tự gửi được header này (trình duyệt hỏi CORS trước, server không cho), và
     * tên miền lạ trỏ về 127.0.0.1 (DNS rebinding) bị chặn vì sai {@code Host}.
     */
    public void enableAdmin(AdminApi api) throws IOException {
        byte[] page;
        try (InputStream in = AdminApi.class.getResourceAsStream("admin.html")) {
            page = in.readAllBytes();
        }
        http.createContext("/admin", exchange -> {
            if (!isLocal(exchange.getRemoteAddress()) || !localHost(exchange)) {
                forbidden(exchange);
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if (!path.equals("/admin") && !path.equals("/admin/")) {
                send(exchange, 404, "text/plain; charset=utf-8", "Không có trang này".getBytes(StandardCharsets.UTF_8));
                return;
            }
            exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
            send(exchange, 200, "text/html; charset=utf-8", page);
        });
        http.createContext("/api/admin/", exchange -> {
            if (!isLocal(exchange.getRemoteAddress()) || !localHost(exchange)) {
                forbidden(exchange);
                return;
            }
            String method = exchange.getRequestMethod();
            String body = "";
            if (method.equals("POST")) {
                if (!"1".equals(exchange.getRequestHeaders().getFirst("X-LabCast-Admin"))) {
                    send(
                            exchange,
                            403,
                            "application/json; charset=utf-8",
                            "{\"loi\":\"Thiếu header X-LabCast-Admin\"}".getBytes(StandardCharsets.UTF_8));
                    return;
                }
                byte[] raw;
                try (InputStream in = exchange.getRequestBody()) {
                    raw = in.readNBytes(MAX_BODY + 1);
                }
                if (raw.length > MAX_BODY) {
                    send(
                            exchange,
                            413,
                            "application/json; charset=utf-8",
                            "{\"loi\":\"Dữ liệu quá lớn\"}".getBytes(StandardCharsets.UTF_8));
                    return;
                }
                body = new String(raw, StandardCharsets.UTF_8);
            }
            String sub = exchange.getRequestURI().getPath().substring("/api/admin/".length());
            AdminApi.Reply r =
                    api.handle(method, sub, query(exchange.getRequestURI().getRawQuery()), body);
            send(
                    exchange,
                    r.status(),
                    "application/json; charset=utf-8",
                    r.json().getBytes(StandardCharsets.UTF_8));
        });
    }

    /** Header {@code Host} là localhost, 127.0.0.1 hoặc [::1] (có hoặc không kèm cổng). */
    static boolean localHost(HttpExchange exchange) {
        String host = exchange.getRequestHeaders().getFirst("Host");
        if (host == null) {
            return false;
        }
        String h = host.strip().toLowerCase(Locale.ROOT);
        if (h.startsWith("[")) {
            int end = h.indexOf(']');
            h = end < 0 ? h : h.substring(0, end + 1);
        } else if (h.contains(":")) {
            h = h.substring(0, h.indexOf(':'));
        }
        return h.equals("localhost") || h.equals("127.0.0.1") || h.equals("[::1]");
    }

    static Map<String, String> query(String raw) {
        Map<String, String> out = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        for (String part : raw.split("&")) {
            int eq = part.indexOf('=');
            String k = eq < 0 ? part : part.substring(0, eq);
            String v = eq < 0 ? "" : part.substring(eq + 1);
            out.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return out;
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
