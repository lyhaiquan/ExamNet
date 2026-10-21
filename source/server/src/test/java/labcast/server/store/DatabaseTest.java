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
    void moTrongBoNho_coDu20Bang() throws Exception {
        try (Database db = Database.open(":memory:")) {
            assertEquals(
                    20,
                    count(db, "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'"));
        }
    }

    @Test
    void baiGiao_dongTruocMo_biTuChoi() throws Exception {
        try (Database db = Database.open(":memory:")) {
            db.tx(c -> c.createStatement()
                    .executeUpdate(
                            "INSERT INTO users (id, username, full_name, role, pwd_hash, salt, iterations, created_at)"
                                    + " VALUES (1, 'gv', 'GV', 'TEACHER', x'00', x'00', 1, 0);"));
            db.tx(c -> c.createStatement()
                    .executeUpdate("INSERT INTO classes (id, name, teacher_id, created_at) VALUES (1, 'DSA1', 1, 0)"));
            String giao =
                    "INSERT INTO assignments (class_id, lesson_id, loai, opens_at, closes_at, minutes, created_by,"
                            + " created_at) VALUES (1, 'dsa.x', 'kiem-tra', %d, %d, 60, 1, 0)";
            db.tx(c -> c.createStatement().executeUpdate(giao.formatted(1000, 2000)));
            assertThrows(
                    SQLException.class,
                    () -> db.tx(c -> c.createStatement().executeUpdate(giao.formatted(2000, 1000))));
            assertThrows(
                    SQLException.class,
                    () -> db.tx(c -> c.createStatement()
                            .executeUpdate("INSERT INTO class_schedule (class_id, weekday, start_min, end_min)"
                                    + " VALUES (1, 8, 600, 700)")));
            assertEquals(1, count(db, "SELECT count(*) FROM assignments"));
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
