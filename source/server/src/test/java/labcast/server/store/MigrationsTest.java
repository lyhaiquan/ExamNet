package labcast.server.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * DB của bản trước ({@code schema-v0.sql}: lược đồ trước giai đoạn 6, chưa có {@code user_version}) mở bằng bản
 * mới phải giữ nguyên dữ liệu và lên đúng phiên bản.
 */
class MigrationsTest {

    private static int count(Database db, String sql) throws SQLException {
        return db.read(c -> {
            try (var st = c.createStatement();
                    ResultSet rs = st.executeQuery(sql)) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        });
    }

    private static int userVersion(Database db) throws SQLException {
        return count(db, "PRAGMA user_version");
    }

    /** Tạo DB theo lược đồ cũ, có một giáo viên, một học viên, một lớp, một phiên đăng nhập trỏ tới học viên. */
    private static void taoDbBanCu(Path file) throws Exception {
        String schema;
        try (InputStream in = MigrationsTest.class.getResourceAsStream("schema-v0.sql")) {
            schema = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file);
                Statement st = c.createStatement()) {
            for (String sql : SqlScript.split(schema)) {
                st.execute(sql);
            }
            st.executeUpdate("INSERT INTO users (id, username, full_name, role, pwd_hash, salt, iterations, created_at)"
                    + " VALUES (7, 'gv', 'Giáo viên', 'TEACHER', x'01', x'02', 3, 10),"
                    + " (9, 'sv01', 'Học viên', 'STUDENT', x'04', x'05', 6, 20)");
            st.executeUpdate("INSERT INTO classes (id, name, teacher_id, created_at) VALUES (1, 'Lớp A', 7, 30)");
            st.executeUpdate("INSERT INTO enrollments (class_id, user_id) VALUES (1, 9)");
            st.executeUpdate("INSERT INTO sessions (token, user_id, created_at, last_seen) VALUES ('tok', 9, 40, 50)");
        }
    }

    @Test
    void dbTrong_chayHet_dungPhienBan() throws Exception {
        try (Database db = Database.open(":memory:")) {
            assertEquals(Migrations.LATEST, userVersion(db));
            assertTrue(Migrations.LATEST >= 1);
        }
    }

    @Test
    void dbBanCu_giuDuLieu_lenPhienBanMoi(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("cu.db");
        taoDbBanCu(file);
        try (Database db = Database.open(file.toString())) {
            assertEquals(Migrations.LATEST, userVersion(db));
            assertEquals(2, count(db, "SELECT count(*) FROM users"));
            assertEquals(
                    1,
                    count(
                            db,
                            "SELECT count(*) FROM users WHERE id = 9 AND username = 'sv01'"
                                    + " AND pwd_hash = x'04' AND salt = x'05' AND iterations = 6"));
            assertEquals(1, count(db, "SELECT count(*) FROM sessions WHERE user_id = 9"));
            assertEquals(0, count(db, "SELECT count(*) FROM pragma_foreign_key_check"));
            // Bảng của lớp và phiên thực hành giờ nằm trong schema.sql: DB cũ cũng có
            assertEquals(1, count(db, "SELECT count(*) FROM sqlite_master WHERE name = 'practice_rooms'"));
        }
    }

    @Test
    void dbBanCu_coCotPhaiDoiMatKhau_macDinhKhong(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("cu.db");
        taoDbBanCu(file);
        try (Database db = Database.open(file.toString())) {
            assertEquals(0, count(db, "SELECT count(*) FROM users WHERE must_change_pwd <> 0"));
            assertEquals(2, count(db, "SELECT count(*) FROM users WHERE must_change_pwd = 0"));
            assertEquals(1, count(db, "SELECT count(*) FROM sqlite_master WHERE name = 'password_requests'"));
        }
    }

    @Test
    void dbBanCu_sauMigration_khoaNgoaiVanBat(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("cu.db");
        taoDbBanCu(file);
        try (Database db = Database.open(file.toString())) {
            assertThrows(
                    SQLException.class,
                    () -> db.tx(c -> c.createStatement()
                            .executeUpdate("INSERT INTO sessions (token, user_id, created_at, last_seen)"
                                    + " VALUES ('x', 999, 0, 0)")));
        }
    }

    @Test
    void dbBanCu_nhanVaiTroAdmin(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("cu.db");
        taoDbBanCu(file);
        try (Database db = Database.open(file.toString())) {
            db.tx(c -> c.createStatement()
                    .executeUpdate("INSERT INTO users (username, full_name, role, pwd_hash, salt, iterations,"
                            + " created_at) VALUES ('ad', 'Admin', 'ADMIN', x'00', x'00', 1, 0)"));
            assertEquals(1, count(db, "SELECT count(*) FROM users WHERE role = 'ADMIN'"));
            // id mới không đụng id cũ (AUTOINCREMENT giữ được mốc sau khi dựng lại bảng)
            assertTrue(count(db, "SELECT id FROM users WHERE username = 'ad'") > 9);
        }
    }

    @Test
    void moHaiLan_khongChayLaiMigration(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("cu.db");
        taoDbBanCu(file);
        try (Database db = Database.open(file.toString())) {
            assertEquals(Migrations.LATEST, userVersion(db));
        }
        try (Database db = Database.open(file.toString())) {
            assertEquals(Migrations.LATEST, userVersion(db));
            assertEquals(2, count(db, "SELECT count(*) FROM users"));
        }
    }

    @Test
    void dbCuaBanMoiHon_baoLoiRo(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("moi.db");
        try (Database db = Database.open(file.toString())) {
            db.read(c -> c.createStatement().executeUpdate("PRAGMA user_version = " + (Migrations.LATEST + 1)));
        }
        SQLException e = assertThrows(SQLException.class, () -> Database.open(file.toString()));
        assertTrue(e.getMessage().contains("bản LabCast mới hơn"), e.getMessage());
    }
}
