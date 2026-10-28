package labcast.server.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseTest {

    private static int count(Database db, String sql) throws SQLException {
        return db.read(c -> {
            try (var st = c.createStatement();
                    ResultSet rs = st.executeQuery(sql)) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        });
    }

    @Test
    void moTrongBoNho_coDuBang() throws Exception {
        try (Database db = Database.open(":memory:")) {
            assertEquals(
                    23,
                    count(db, "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'"));
            assertEquals(1, count(db,
                    "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = 'practice_sessions'"));
            assertEquals(1,
                    count(db, "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = 'practice_items'"));
            assertEquals(1, count(db,
                    "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = 'practice_submissions'"));
        }
    }

    @Test
    void tachScript_boChuThichTruoc() {
        assertEquals(List.of("a", "b"), SqlScript.split("a; -- x; y\nb;"));
        assertEquals(
                List.of("CREATE TABLE t (x INTEGER)"), SqlScript.split("-- đầu\nCREATE TABLE t (x INTEGER); \n\n"));
    }

    @Test
    void moHaiLanCungFile_khongLoi(@TempDir Path dir) throws Exception {
        String path = dir.resolve("labcast.db").toString();
        try (Database db = Database.open(path)) {
            db.tx(c -> c.createStatement()
                    .executeUpdate(
                            "INSERT INTO users (username, full_name, role, pwd_hash, salt, iterations, created_at)"
                                    + " VALUES ('a', 'A', 'STUDENT', x'00', x'00', 1, 0)"));
        }
        try (Database db = Database.open(path)) {
            assertEquals(1, count(db, "SELECT count(*) FROM users"));
        }
    }

    @Test
    void tx_loiThiRollback() throws Exception {
        try (Database db = Database.open(":memory:")) {
            assertThrows(
                    SQLException.class,
                    () -> db.tx(c -> {
                        c.createStatement()
                                .executeUpdate(
                                        "INSERT INTO users (username, full_name, role, pwd_hash, salt, iterations,"
                                                + " created_at) VALUES ('a', 'A', 'STUDENT', x'00', x'00', 1, 0)");
                        return c.createStatement().executeUpdate("INSERT INTO bang_khong_co VALUES (1)");
                    }));
            assertEquals(0, count(db, "SELECT count(*) FROM users"));
        }
    }

    @Test
    void khoaNgoaiDuocBat() throws Exception {
        try (Database db = Database.open(":memory:")) {
            assertThrows(
                    SQLException.class,
                    () -> db.tx(c -> c.createStatement()
                            .executeUpdate(
                                    "INSERT INTO sessions (token, user_id, created_at, last_seen) VALUES ('t', 999, 0,"
                                            + " 0)")));
        }
    }
}
