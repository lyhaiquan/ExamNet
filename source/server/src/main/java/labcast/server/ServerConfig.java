package labcast.server;

import java.nio.file.Path;

/** Cấu hình server. Đọc từ dòng lệnh ở {@code ServerMain} (task 2.9). */
public record ServerConfig(
        int port,
        String db,
        Path content,
        String lop,
        String lan,
        String taoGiaoVien,
        boolean khongSandbox,
        int discoveryPort,
        int statusPort) {

    public static final int DEFAULT_PORT = 7000;
    public static final int DEFAULT_DISCOVERY_PORT = 7002;

    public static ServerConfig defaults() {
        return new ServerConfig(
                DEFAULT_PORT,
                "labcast.db",
                Path.of("content"),
                "Lớp thực hành",
                null,
                null,
                false,
                DEFAULT_DISCOVERY_PORT,
                8080);
    }

    /** Cho test: cổng 0 (hệ điều hành chọn), database trong bộ nhớ. */
    public static ServerConfig forTest() {
        return defaults().withPort(0).withDb(":memory:").withDiscoveryPort(0);
    }

    public ServerConfig withPort(int p) {
        return new ServerConfig(p, db, content, lop, lan, taoGiaoVien, khongSandbox, discoveryPort, statusPort);
    }

    public ServerConfig withDb(String d) {
        return new ServerConfig(port, d, content, lop, lan, taoGiaoVien, khongSandbox, discoveryPort, statusPort);
    }

    public ServerConfig withContent(Path c) {
        return new ServerConfig(port, db, c, lop, lan, taoGiaoVien, khongSandbox, discoveryPort, statusPort);
    }

    public ServerConfig withLop(String l) {
        return new ServerConfig(port, db, content, l, lan, taoGiaoVien, khongSandbox, discoveryPort, statusPort);
    }

    public ServerConfig withTaoGiaoVien(String t) {
        return new ServerConfig(port, db, content, lop, lan, t, khongSandbox, discoveryPort, statusPort);
    }

    public ServerConfig withKhongSandbox(boolean k) {
        return new ServerConfig(port, db, content, lop, lan, taoGiaoVien, k, discoveryPort, statusPort);
    }

    public ServerConfig withDiscoveryPort(int p) {
        return new ServerConfig(port, db, content, lop, lan, taoGiaoVien, khongSandbox, p, statusPort);
    }
}
