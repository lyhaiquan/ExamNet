package labcast.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Cấu hình server, đọc từ dòng lệnh, file cấu hình và biến môi trường bằng {@link #parse}. */
public record ServerConfig(
        int port,
        String db,
        Path content,
        String lop,
        String mon,
        String lan,
        String taoGiaoVien,
        String taoAdmin,
        boolean khongSandbox,
        int discoveryPort,
        int statusPort) {

    public static final int DEFAULT_PORT = 7000;
    public static final int DEFAULT_DISCOVERY_PORT = 7002;

    public static final String USAGE =
            """
            Cách dùng: java -jar labcast-server.jar [tham số]
              --port <số>                   cổng TCP cho app (mặc định 7000)
              --db <file>                   file SQLite (mặc định labcast.db)
              --content <thư mục>           thư mục bài học (mặc định content)
              --lop <tên>                   tên lớp hiện trên app học viên (mặc định "Lớp thực hành")
              --mon <dsa|sql>               môn của lớp: chỉ nạp bài môn này (mặc định nạp cả hai môn)
              --lan <tên card hoặc IP>      card mạng quảng bá cho app (mặc định tự chọn card LAN)
              --tao-giao-vien <user:mật khẩu:họ tên>   tạo tài khoản giáo viên nếu chưa có
              --tao-admin <user:mật khẩu:họ tên>       tạo tài khoản quản trị (tạo lớp, giáo viên) nếu chưa có
              --port-dashboard <số>         cổng HTTP của /version và dashboard (mặc định 8080)
              --khong-sandbox               chạy code học viên bằng Python cục bộ, KHÔNG cách ly (chỉ để phát triển)
              --cau-hinh <file>             đọc các tham số trên từ file UTF-8, mỗi dòng "khoá = giá trị"
              --dat-lai-mat-khau <user:mật khẩu mới>   đặt lại mật khẩu trong database (theo --db hoặc
                                            --cau-hinh) rồi thoát, không bật server. Dùng khi quản trị quên
                                            mật khẩu, ví dụ: --cau-hinh lop.conf --dat-lai-mat-khau ad:moi123

            Trên Windows, chữ tiếng Việt như ớ, ư, ạ KHÔNG đi qua được dòng lệnh (Java nhận thành '?').
            Tên lớp, họ tên giáo viên, đường dẫn có dấu: ghi vào file cấu hình, ví dụ lop.conf

                lop = Lớp D22CQCN01 — Cấu trúc dữ liệu và giải thuật
                mon = dsa
                tao-giao-vien = gv:matkhau:Nguyễn Thị Lan
                content = D:/Bài giảng/content

            rồi chạy: java -jar labcast-server.jar --cau-hinh lop.conf
            Hoặc đặt biến môi trường LABCAST_LOP, LABCAST_MON, LABCAST_TAO_GIAO_VIEN, LABCAST_TAO_ADMIN,
            LABCAST_CONTENT,
            LABCAST_DB, LABCAST_LAN.
            Thứ tự thắng: dòng lệnh > file cấu hình > biến môi trường > mặc định.

            Mỗi lớp một server, mỗi server một môn: lớp DSA và lớp SQL là hai server, hai file cấu hình.
            Chạy cả hai trên cùng một máy thì server thứ hai cần port, port-dashboard và db riêng, ví dụ
                mon = sql
                port = 7010
                port-dashboard = 8081
                db = lop-sql.db
            """;

    /** Khoá trong file cấu hình (tên tham số bỏ "--") → biến môi trường tương ứng. */
    private static final Map<String, String> ENV = new LinkedHashMap<>();

    static {
        ENV.put("db", "LABCAST_DB");
        ENV.put("content", "LABCAST_CONTENT");
        ENV.put("lop", "LABCAST_LOP");
        ENV.put("mon", "LABCAST_MON");
        ENV.put("lan", "LABCAST_LAN");
        ENV.put("tao-giao-vien", "LABCAST_TAO_GIAO_VIEN");
        ENV.put("tao-admin", "LABCAST_TAO_ADMIN");
    }

    private static final Set<String> KEYS = Set.of(
            "port",
            "port-dashboard",
            "db",
            "content",
            "lop",
            "mon",
            "lan",
            "tao-giao-vien",
            "tao-admin",
            "khong-sandbox");

    /** Đọc cấu hình của máy đang chạy: file cấu hình đọc bằng UTF-8. */
    public static ServerConfig parse(String[] args, Map<String, String> env) {
        return parse(args, env, argsLoseUnicode(), ServerConfig::readUtf8);
    }

    /**
     * Thứ tự thắng: dòng lệnh > file {@code --cau-hinh} > biến môi trường {@code LABCAST_*} > mặc định.
     * Tham số lạ, thiếu giá trị, khoá lạ trong file → IllegalArgumentException có thông báo dễ hiểu.
     *
     * @param argsLossy hệ điều hành đổi tham số dòng lệnh sang bảng mã không có tiếng Việt (Windows dùng
     *     Cp1252): chữ ngoài bảng mã đã thành '?' trước khi tới Java, không cứu được. Gặp '?' trong một
     *     tham số chữ thì dừng và chỉ cách sửa, thay vì chạy với tên lớp sai hay tạo giáo viên với mật
     *     khẩu không gõ lại được.
     */
    static ServerConfig parse(
            String[] args, Map<String, String> env, boolean argsLossy, Function<Path, String> readFile) {
        ServerConfig c = defaults();
        for (Map.Entry<String, String> e : ENV.entrySet()) {
            String v = env.get(e.getValue());
            if (v != null && !v.isBlank()) {
                c = c.with(e.getKey(), v, "biến môi trường " + e.getValue());
            }
        }

        Map<String, String> cli = new LinkedHashMap<>();
        boolean khongSandbox = false;
        String configFile = null;
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("--khong-sandbox")) {
                khongSandbox = true;
                continue;
            }
            String key = a.startsWith("--") ? a.substring(2) : "";
            if (!KEYS.contains(key) && !key.equals("cau-hinh")) {
                throw new IllegalArgumentException("Tham số lạ: " + a + System.lineSeparator() + USAGE);
            }
            if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                throw new IllegalArgumentException("Thiếu giá trị sau " + a);
            }
            String v = args[++i];
            if (argsLossy && (v.indexOf('?') >= 0 || v.indexOf('\uFFFD') >= 0) && !key.equals("port")) {
                throw new IllegalArgumentException(lostMessage(a, key, v));
            }
            if (key.equals("cau-hinh")) {
                configFile = v;
            } else {
                cli.put(key, v);
            }
        }

        if (configFile != null) {
            c = c.applyFile(configFile, readFile.apply(Path.of(configFile)));
        }
        for (Map.Entry<String, String> e : cli.entrySet()) {
            c = c.with(e.getKey(), e.getValue(), "--" + e.getKey());
        }
        return khongSandbox ? c.withKhongSandbox(true) : c;
    }

    private ServerConfig applyFile(String name, String text) {
        ServerConfig c = this;
        String[] lines = text.replace("\uFEFF", "").split("\r?\n", -1);
        for (int n = 0; n < lines.length; n++) {
            String line = lines[n].strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int eq = line.indexOf('=');
            String where = name + " dòng " + (n + 1);
            if (eq <= 0) {
                throw new IllegalArgumentException(where + ": cần dạng \"khoá = giá trị\", gặp: " + line);
            }
            String key = line.substring(0, eq).strip();
            String value = unquote(line.substring(eq + 1).strip());
            if (!KEYS.contains(key)) {
                throw new IllegalArgumentException(where + ": khoá lạ \"" + key + "\". Các khoá: " + KEYS);
            }
            c = key.equals("khong-sandbox") ? c.withKhongSandbox(yes(value, where)) : c.with(key, value, where);
        }
        return c;
    }

    private ServerConfig with(String key, String v, String where) {
        return switch (key) {
            case "port" -> withPort(port(v, where));
            case "port-dashboard" -> withStatusPort(port(v, where));
            case "db" -> withDb(v);
            case "content" -> withContent(Path.of(v));
            case "lop" -> withLop(v);
            case "mon" -> withMon(mon(v, where));
            case "lan" -> withLan(v);
            case "tao-giao-vien" -> withTaoGiaoVien(v);
            case "tao-admin" -> withTaoAdmin(v);
            default -> throw new IllegalArgumentException(where + ": khoá lạ " + key);
        };
    }

    private static String lostMessage(String arg, String key, String v) {
        String env = ENV.get(key);
        return "Tham số " + arg + " \"" + v + "\" có dấu '?': Windows đã làm mất chữ tiếng Việt (ớ, ư, ạ… không đi qua"
                + " được dòng lệnh, Java nhận thành '?')." + System.lineSeparator()
                + "Ghi giá trị vào file UTF-8, ví dụ lop.conf có dòng \"" + (key.equals("cau-hinh") ? "lop" : key)
                + " = …\", rồi chạy --cau-hinh lop.conf"
                + (key.equals("cau-hinh") ? " (đặt file ở thư mục tên không dấu)" : "")
                + (env == null ? "." : ", hoặc đặt biến môi trường " + env + ".");
    }

    private static String mon(String v, String where) {
        String m = v.strip().toLowerCase(Locale.ROOT);
        if (!m.equals("dsa") && !m.equals("sql")) {
            throw new IllegalArgumentException(where + ": mon phải là dsa hoặc sql, nhận được: " + v);
        }
        return m;
    }

    private static String unquote(String v) {
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }

    private static boolean yes(String v, String where) {
        return switch (v.toLowerCase(Locale.ROOT)) {
            case "true", "co", "có", "1", "yes" -> true;
            case "false", "khong", "không", "0", "no" -> false;
            default -> throw new IllegalArgumentException(where + ": khong-sandbox phải là true hoặc false");
        };
    }

    /** Hệ điều hành chuyển tham số dòng lệnh sang bảng mã không phải UTF-8 (Windows: Cp1252). */
    public static boolean argsLoseUnicode() {
        String enc = System.getProperty("sun.jnu.encoding", "UTF-8");
        return !enc.equalsIgnoreCase("UTF-8") && !enc.equalsIgnoreCase("UTF8");
    }

    private static String readUtf8(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (java.nio.charset.MalformedInputException e) {
            throw new IllegalArgumentException(
                    "File cấu hình " + p + " không phải UTF-8. Mở bằng Notepad, chọn" + " Save As, Encoding: UTF-8.");
        } catch (IOException e) {
            throw new IllegalArgumentException("Không đọc được file cấu hình " + p.toAbsolutePath() + ": " + e);
        }
    }

    private static int port(String v, String where) {
        try {
            int p = Integer.parseInt(v);
            if (p < 0 || p > 65535) {
                throw new NumberFormatException();
            }
            return p;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(where + ": port phải là số từ 0 đến 65535, nhận được: " + v);
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
        return new ServerConfig(
                p, db, content, lop, mon, lan, taoGiaoVien, taoAdmin, khongSandbox, discoveryPort, statusPort);
    }

    public ServerConfig withDb(String d) {
        return new ServerConfig(
                port, d, content, lop, mon, lan, taoGiaoVien, taoAdmin, khongSandbox, discoveryPort, statusPort);
    }

    public ServerConfig withContent(Path c) {
        return new ServerConfig(
                port, db, c, lop, mon, lan, taoGiaoVien, taoAdmin, khongSandbox, discoveryPort, statusPort);
    }

    public ServerConfig withLop(String l) {
        return new ServerConfig(
                port, db, content, l, mon, lan, taoGiaoVien, taoAdmin, khongSandbox, discoveryPort, statusPort);
    }

    /** {@code null}: nạp bài của cả hai môn. */
    public ServerConfig withMon(String m) {
        return new ServerConfig(
                port, db, content, lop, m, lan, taoGiaoVien, taoAdmin, khongSandbox, discoveryPort, statusPort);
    }

    public ServerConfig withLan(String l) {
        return new ServerConfig(
                port, db, content, lop, mon, l, taoGiaoVien, taoAdmin, khongSandbox, discoveryPort, statusPort);
    }

    public ServerConfig withTaoGiaoVien(String t) {
        return new ServerConfig(port, db, content, lop, mon, lan, t, taoAdmin, khongSandbox, discoveryPort, statusPort);
    }

    public ServerConfig withTaoAdmin(String t) {
        return new ServerConfig(
                port, db, content, lop, mon, lan, taoGiaoVien, t, khongSandbox, discoveryPort, statusPort);
    }

    public ServerConfig withKhongSandbox(boolean k) {
        return new ServerConfig(port, db, content, lop, mon, lan, taoGiaoVien, taoAdmin, k, discoveryPort, statusPort);
    }

    public ServerConfig withDiscoveryPort(int p) {
        return new ServerConfig(port, db, content, lop, mon, lan, taoGiaoVien, taoAdmin, khongSandbox, p, statusPort);
    }

    public ServerConfig withStatusPort(int p) {
        return new ServerConfig(
                port, db, content, lop, mon, lan, taoGiaoVien, taoAdmin, khongSandbox, discoveryPort, p);
    }
}
