package labcast.server.sqlviz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteEngineTest {
    static final String SCHEMA =
            """
            CREATE TABLE sinh_vien (id INTEGER PRIMARY KEY, ho_ten TEXT, que_quan TEXT, lop_id INTEGER);
            -- chú thích có dấu ; ở giữa
            CREATE TABLE lop (id INTEGER PRIMARY KEY, ten_lop TEXT);
            """;
    static final String DATA =
            """
            INSERT INTO lop VALUES (1, 'D22CN01'), (2, 'D22CN02');
            INSERT INTO sinh_vien VALUES (1, 'Nguyễn Văn An', 'Hà Nội', 1);
            INSERT INTO sinh_vien VALUES (2, 'Trần Thị Bình', 'Nam Định; Việt Nam', 2);
            INSERT INTO sinh_vien VALUES (3, 'Lê Văn Cường', NULL, NULL);
            """;

    @TempDir
    Path tmp;

    Path db() throws Exception {
        return SqliteEngine.build(tmp, SCHEMA, DATA);
    }

    @Test
    void dungDatabaseTuScript_chuoiCoDauChamPhay() throws Exception {
        QueryResult r =
                new SqliteEngine().query(db(), "SELECT que_quan FROM sinh_vien ORDER BY id", Duration.ofSeconds(2));
        assertEquals(List.of("que_quan"), r.columns());
        assertEquals("Nam Định; Việt Nam", r.rows().get(1).get(0));
        assertNull(r.rows().get(2).get(0));
    }

    @Test
    void giaTriSo_chuanHoa() throws Exception {
        QueryResult r = new SqliteEngine()
                .query(db(), "SELECT id, 1.5, COUNT(*) FROM sinh_vien GROUP BY id", Duration.ofSeconds(2));
        assertEquals(Arrays.asList(1L, 1.5, 1L), r.rows().get(0));
    }

    @Test
    void cauChayMai_hetGioSauKhoang2Giay() throws Exception {
        Path db = db();
        long t0 = System.nanoTime();
        SqlRejected e = assertThrows(SqlRejected.class, () -> new SqliteEngine()
                .query(
                        db,
                        "WITH RECURSIVE r(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM r) SELECT count(*) FROM r",
                        Duration.ofSeconds(2)));
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertTrue(ms >= 1_900 && ms < 4_000, "dừng sau " + ms + " ms");
        assertTrue(e.getMessage().contains("quá"), e.getMessage());
    }

    @Test
    void selectKhongGhiDuocVaoDatabase() throws Exception {
        Path db = db();
        assertThrows(
                SqlRejected.class, () -> new SqliteEngine().query(db, "DELETE FROM sinh_vien", Duration.ofSeconds(2)));
        assertEquals(
                3,
                new SqliteEngine()
                        .query(db, "SELECT * FROM sinh_vien", Duration.ofSeconds(2))
                        .rows()
                        .size());
    }

    @Test
    void chuoiKhongLo_biChanTruocKhiTranBoNho() throws Exception {
        assertThrows(SqlRejected.class, () -> new SqliteEngine()
                .query(db(), "SELECT zeroblob(1000000000)", Duration.ofSeconds(2)));
        // printf vượt trần thì SQLite trả NULL thay vì dựng chuỗi 900 MB
        assertNull(new SqliteEngine()
                .query(db(), "SELECT printf('%.*c', 900000000, 'x')", Duration.ofSeconds(2))
                .rows()
                .get(0)
                .get(0));
    }

    @Test
    void ketQuaQuaNhieuDong_catBot() throws Exception {
        QueryResult r = new SqliteEngine()
                .query(
                        db(),
                        "WITH RECURSIVE r(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM r WHERE n < 5000) SELECT n FROM r",
                        Duration.ofSeconds(2));
        assertEquals(SqliteEngine.MAX_ROWS, r.rows().size());
        assertTrue(r.truncated());
    }

    @Test
    void dmlChayTrenBanSao_banGocNguyenVen() throws Exception {
        Path db = db();
        Path copy = SqliteEngine.copy(db, tmp);
        new SqliteEngine().execute(copy, "UPDATE sinh_vien SET que_quan = 'X' WHERE id = 1", Duration.ofSeconds(2));
        var engine = new SqliteEngine();
        assertEquals(
                "X",
                engine.query(copy, "SELECT que_quan FROM sinh_vien WHERE id = 1", Duration.ofSeconds(2))
                        .rows()
                        .get(0)
                        .get(0));
        assertEquals(
                "Hà Nội",
                engine.query(db, "SELECT que_quan FROM sinh_vien WHERE id = 1", Duration.ofSeconds(2))
                        .rows()
                        .get(0)
                        .get(0));
        Files.deleteIfExists(copy);
    }

    @Test
    void loiCuPhapCuaSqlite_baoChoHocVien() throws Exception {
        SqlRejected e = assertThrows(SqlRejected.class, () -> new SqliteEngine()
                .query(db(), "SELECT khong_co_cot FROM sinh_vien", Duration.ofSeconds(2)));
        assertTrue(e.getMessage().contains("khong_co_cot"), e.getMessage());
    }
}
