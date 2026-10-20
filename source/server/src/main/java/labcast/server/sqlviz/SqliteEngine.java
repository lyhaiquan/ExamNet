package labcast.server.sqlviz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.sqlite.ProgressHandler;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteLimits;

/**
 * Chạy câu SQL trên SQLite (spec §7.6). Mọi kết nối đều:
 *
 * <ul>
 *   <li>chặn {@code ATTACH} (trần 0 database gắn thêm), không bật {@code load_extension};
 *   <li>trần độ dài chuỗi/blob {@link #MAX_VALUE_BYTES}: {@code zeroblob(1e9)} báo lỗi thay vì làm tràn bộ
 *       nhớ server;
 *   <li>ngắt câu chạy quá giờ ngay trong SQLite (progress handler), kể cả câu đệ quy vô hạn;
 *   <li>đọc tối đa {@link #MAX_ROWS} dòng kết quả.
 * </ul>
 *
 * SELECT mở chỉ đọc kèm {@code PRAGMA query_only}; câu DML chỉ chạy trên bản sao ({@link #copy}).
 */
public final class SqliteEngine {
    public static final int MAX_ROWS = 1000;
    static final int MAX_VALUE_BYTES = 100_000;
    /** Ô chuỗi dài hơn thế thì cắt khi trả về (kết quả để hiện và so, không cần cả trang chữ). */
    static final int MAX_CELL_CHARS = 1_000;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Dựng database mới (file tạm trong {@code dir}) từ các script, ví dụ schema.sql rồi du_lieu_mau.sql. */
    public static Path build(Path dir, String... scripts) throws IOException, SqlRejected {
        Files.createDirectories(dir);
        Path db = Files.createTempFile(dir, "csdl-", ".db");
        try (Connection c = open(db, false);
                Statement st = c.createStatement()) {
            c.setAutoCommit(false);
            for (String script : scripts) {
                for (String sql : SqlSplitter.split(script)) {
                    st.execute(sql);
                }
            }
            c.commit();
        } catch (SQLException e) {
            Files.deleteIfExists(db);
            throw new SqlRejected("Dữ liệu bài lỗi: " + e.getMessage());
        }
        return db;
    }

    /** Bản sao để chạy câu DML hoặc câu của học viên mà không đụng bản gốc. */
    public static Path copy(Path db, Path dir) throws IOException {
        Path c = Files.createTempFile(dir, "ban-sao-", ".db");
        Files.copy(db, c, StandardCopyOption.REPLACE_EXISTING);
        return c;
    }

    /** Chạy một câu SELECT (đã qua {@link SqlGuard}) ở chế độ chỉ đọc. */
    public QueryResult query(Path db, String sql, Duration timeout) throws SqlRejected {
        try (Connection c = open(db, true)) {
            return run(c, sql, timeout);
        } catch (SQLException e) {
            throw new SqlRejected(message(e));
        }
    }

    /** Chạy một câu INSERT/UPDATE/DELETE trên {@code dbCopy}; trả số dòng bị đổi. */
    public int execute(Path dbCopy, String sql, Duration timeout) throws SqlRejected {
        try (Connection c = open(dbCopy, false);
                Statement st = c.createStatement()) {
            long deadline = System.nanoTime() + timeout.toNanos();
            ProgressHandler.setHandler(c, 1_000, new Deadline(deadline));
            try {
                return st.executeUpdate(sql);
            } catch (SQLException e) {
                throw new SqlRejected(System.nanoTime() > deadline ? overtime(timeout) : message(e));
            }
        } catch (SQLException e) {
            throw new SqlRejected(message(e));
        }
    }

    private static QueryResult run(Connection c, String sql, Duration timeout) throws SqlRejected, SQLException {
        long deadline = System.nanoTime() + timeout.toNanos();
        ProgressHandler.setHandler(c, 1_000, new Deadline(deadline));
        try (Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            int n = md.getColumnCount();
            List<String> cols = new ArrayList<>(n);
            for (int i = 1; i <= n; i++) {
                cols.add(md.getColumnLabel(i));
            }
            List<List<Object>> rows = new ArrayList<>();
            boolean truncated = false;
            while (rs.next()) {
                if (rows.size() == MAX_ROWS) {
                    truncated = true;
                    break;
                }
                List<Object> row = new ArrayList<>(n);
                for (int i = 1; i <= n; i++) {
                    row.add(normalize(rs.getObject(i)));
                }
                rows.add(row);
            }
            return new QueryResult(cols, rows, truncated);
        } catch (SQLException e) {
            throw new SqlRejected(System.nanoTime() > deadline ? overtime(timeout) : message(e));
        }
    }

    /** Số nguyên → Long, số thực → Double, blob → mô tả ngắn, chuỗi quá dài → cắt. */
    static Object normalize(Object v) {
        if (v == null || v instanceof Long || v instanceof Double) {
            return v;
        }
        if (v instanceof Integer || v instanceof Short || v instanceof Byte) {
            return ((Number) v).longValue();
        }
        if (v instanceof Float f) {
            return f.doubleValue();
        }
        if (v instanceof Number num) {
            return num.doubleValue();
        }
        if (v instanceof byte[] b) {
            return "<blob " + b.length + " byte>";
        }
        String s = v.toString();
        return s.length() > MAX_CELL_CHARS ? s.substring(0, MAX_CELL_CHARS) + "…" : s;
    }

    private static Connection open(Path db, boolean readOnly) throws SQLException {
        SQLiteConfig cfg = new SQLiteConfig();
        cfg.setReadOnly(readOnly);
        cfg.enableLoadExtension(false);
        cfg.setBusyTimeout(2_000);
        SQLiteConnection c = (SQLiteConnection) cfg.createConnection("jdbc:sqlite:" + db.toAbsolutePath());
        c.setLimit(SQLiteLimits.SQLITE_LIMIT_ATTACHED, 0);
        c.setLimit(SQLiteLimits.SQLITE_LIMIT_LENGTH, MAX_VALUE_BYTES);
        if (readOnly) {
            try (Statement st = c.createStatement()) {
                st.execute("PRAGMA query_only = ON"); // thêm một lớp: kể cả câu lọt qua SqlGuard cũng không ghi được
            }
        }
        return c;
    }

    private static String overtime(Duration timeout) {
        return "Câu lệnh chạy quá " + timeout.toSeconds()
                + " giây nên bị dừng (có thể lặp vô hạn, hoặc ghép bảng sinh quá nhiều dòng).";
    }

    /** Lời nhắn của SQLite, bỏ phần mã lỗi ở đầu cho dễ đọc. */
    private static String message(SQLException e) {
        String m = String.valueOf(e.getMessage());
        int i = m.indexOf("] ");
        String s = i >= 0 && m.startsWith("[") ? m.substring(i + 2) : m;
        if (s.contains("string or blob too big")) {
            return "Giá trị quá lớn (chuỗi hoặc blob dài quá " + MAX_VALUE_BYTES / 1000 + " KB).";
        }
        if (s.contains("attempt to write a readonly database")) {
            return "Câu SELECT không được thay đổi dữ liệu.";
        }
        return "Lỗi SQLite: " + s;
    }

    /** Ngắt câu đang chạy khi quá hạn: SQLite gọi mỗi 1000 lệnh máy ảo. */
    private static final class Deadline extends ProgressHandler {
        private final long nanos;

        Deadline(long nanos) {
            this.nanos = nanos;
        }

        @Override
        protected int progress() {
            return System.nanoTime() > nanos ? 1 : 0;
        }
    }
}
