package labcast.server;

import java.nio.file.Path;
import java.util.Map;

/** Cấu hình server, đọc từ dòng lệnh bằng {@link #parse}. */
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

    public static final String USAGE =
            """
            Cách dùng: java -jar labcast-server.jar [tham số]
              --port <số>                   cổng TCP cho app (mặc định 7000)
              --db <file>                   file SQLite (mặc định labcast.db; biến LABCAST_DB thay mặc định)
              --content <thư mục>           thư mục bài học (mặc định content)
              --lop <tên>                   tên lớp hiện trên app học viên (mặc định "Lớp thực hành")
              --lan <tên card hoặc IP>      card mạng quảng bá cho app (mặc định tự chọn card LAN)
              --tao-giao-vien <user:mật khẩu:họ tên>   tạo tài khoản giáo viên nếu chưa có
              --khong-sandbox               chạy code học viên bằng Python cục bộ, KHÔNG cách ly (chỉ để phát triển)
            """;

    /**
     * Đọc tham số dòng lệnh. {@code --db} ghi rõ thắng biến môi trường {@code LABCAST_DB}, biến môi
     * trường thắng mặc định. Tham số lạ hoặc thiếu giá trị → IllegalArgumentException có thông báo
     * dễ hiểu.
     */
    public static ServerConfig parse(String[] args, Map<String, String> env) {
        ServerConfig c = defaults();
        String envDb = env.get("LABCAST_DB");
        if (envDb != null && !envDb.isBlank()) {
            c = c.withDb(envDb);
        }
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--khong-sandbox" -> c = c.withKhongSandbox(true);
                case "--port", "--db", "--content", "--lop", "--lan", "--tao-giao-vien" -> {
                    if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                        throw new IllegalArgumentException("Thiếu giá trị sau " + a);
                    }
                    String v = args[++i];
                    c = switch (a) {
                        case "--port" -> c.withPort(port(v));
                        case "--db" -> c.withDb(v);
                        case "--content" -> c.withContent(Path.of(v));
                        case "--lop" -> c.withLop(v);
                        case "--lan" -> c.withLan(v);
                        default -> c.withTaoGiaoVien(v);};
                }
                default -> throw new IllegalArgumentException("Tham số lạ: " + a + System.lineSeparator() + USAGE);
            }
        }
        return c;
    }

    private static int port(String v) {
        try {
            int p = Integer.parseInt(v);
            if (p < 0 || p > 65535) {
                throw new NumberFormatException();
            }
            return p;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--port phải là số từ 0 đến 65535, nhận được: " + v);
        }
    }

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

    /** Thư mục runner/ nằm cạnh thư mục content/ trong repo (chế độ không sandbox chạy run.py ở đây). */
    public Path runnerDir() {
        Path parent = content.toAbsolutePath().normalize().getParent();
        return parent == null ? Path.of("runner") : parent.resolve("runner");
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

    public ServerConfig withLan(String l) {
        return new ServerConfig(port, db, content, lop, l, taoGiaoVien, khongSandbox, discoveryPort, statusPort);
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
