package labcast.server.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.sqlite.SQLiteConfig;

/**
 * Cơ sở dữ liệu SQLite của server (spec §15).
 *
 * <p>
 * Giữ <b>một</b> {@link Connection} cho cả server. SQLite chỉ cho một người ghi
 * một lúc, nên mọi
 * thao tác đi qua {@link #tx} hoặc {@link #read}, hai hàm này dùng chung một
 * khoá. Thao tác ngắn
 * (vài câu lệnh) nên khoá không thành nút cổ chai ở quy mô một lớp ≤ 50 học
 * viên.
 */
public final class Database implements AutoCloseable {

    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection c) throws SQLException;
    }

    private final Connection conn;
    private final Object lock = new Object();

    private Database(Connection conn) {
        this.conn = conn;
    }

    /**
     * Mở (tạo nếu chưa có) và chạy {@code schema.sql}. {@code ":memory:"} cho test.
     * Mở (tạo nếu chưa có), đưa DB của bản trước lên lược đồ hiện tại ({@link Migrations}) rồi chạy {@code
     * schema.sql}. {@code ":memory:"} cho test.
     */
    public static Database open(String path) throws SQLException {
        SQLiteConfig cfg = new SQLiteConfig();
        cfg.enforceForeignKeys(true);
        cfg.setBusyTimeout(5_000);
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + path, cfg.toProperties());
        Database db = new Database(c);
        try (Statement st = c.createStatement()) {
            Migrations.upgrade(c, !hasTable(c, "users"));
            for (String sql : SqlScript.split(loadSchema())) {
                st.execute(sql);
            }
            ensurePracticeSessionColumns(st);
            Migrations.markLatest(c);
        } catch (SQLException e) {
            c.close();
            throw e;
        }
        return db;
    }

    private static void ensurePracticeSessionColumns(Statement st) throws SQLException {
        boolean hasName = false;
        boolean hasClassId = false;
        boolean hasType = false;
        try (var columns = st.executeQuery("PRAGMA table_info(practice_sessions)")) {
            while (columns.next()) {
                hasName |= "name".equals(columns.getString("name"));
                hasClassId |= "class_id".equals(columns.getString("name"));
                hasType |= "type".equals(columns.getString("name"));
            }
        }
        if (!hasName)
            st.executeUpdate("ALTER TABLE practice_sessions ADD COLUMN name TEXT NOT NULL DEFAULT 'Đợt thực hành'");
        if (!hasClassId)
            st.executeUpdate("ALTER TABLE practice_sessions ADD COLUMN class_id INTEGER REFERENCES classes(id)");
        if (!hasType)
            st.executeUpdate("ALTER TABLE practice_sessions ADD COLUMN type TEXT NOT NULL DEFAULT 'PRACTICE'");
        st.executeUpdate("UPDATE practice_sessions SET class_id = ("
                + "SELECT id FROM classes WHERE classes.teacher_id = practice_sessions.teacher_id "
                + "ORDER BY id LIMIT 1"
                + ") WHERE class_id IS NULL");
    }

    /**
     * Kết nối gốc. Chỉ dùng bên trong {@link #tx}/{@link #read}, hoặc trong test
     * chạy một luồng.
     */
    public Connection connection() {
        return conn;
    }

    /** Chạy trong một giao dịch: lỗi thì ROLLBACK và ném lại. */
    public <T> T tx(SqlWork<T> work) throws SQLException {
        synchronized (lock) {
            conn.setAutoCommit(false);
            try {
                T result = work.run(conn);
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** Đọc (hoặc một câu ghi đơn lẻ) không cần giao dịch. */
    public <T> T read(SqlWork<T> work) throws SQLException {
        synchronized (lock) {
            return work.run(conn);
        }
    }

    @Override
    public void close() throws SQLException {
        synchronized (lock) {
            conn.close();
        }
    }

    private static boolean hasTable(Connection c, String name) throws SQLException {
        try (var ps = c.prepareStatement("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            ps.setString(1, name);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static String loadSchema() {
        try (InputStream in = Database.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("Thiếu schema.sql trong classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
