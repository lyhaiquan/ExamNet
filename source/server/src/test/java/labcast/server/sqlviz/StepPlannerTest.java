package labcast.server.sqlviz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import labcast.common.protocol.JsonPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StepPlannerTest {
    static final String SCHEMA =
            """
            CREATE TABLE lop (id INTEGER PRIMARY KEY, ten_lop TEXT, khoa TEXT);
            CREATE TABLE sinh_vien (id INTEGER PRIMARY KEY, ho_ten TEXT, que_quan TEXT, ngay_sinh TEXT, lop_id INTEGER);
            CREATE TABLE ket_qua (sinh_vien_id INTEGER, mon_hoc_id INTEGER, lan_thi INTEGER, diem REAL);
            """;
    static final String DATA =
            """
            INSERT INTO lop VALUES (1, 'D22CN01', 'CNTT'), (2, 'D22CN02', 'CNTT'), (3, 'D22AT01', 'ATTT');
            INSERT INTO sinh_vien VALUES
              (1, 'An', 'Hà Nội', '2005-03-01', 1),
              (2, 'Bình', 'Nam Định', '2004-07-12', 1),
              (3, 'Cường', 'Hà Nội', '2004-11-30', 2),
              (4, 'Dung', 'Hà Nội', '2005-09-09', 2),
              (5, 'Em', 'Hải Phòng', '2005-01-20', NULL);
            INSERT INTO ket_qua VALUES (1, 1, 1, 8), (2, 1, 1, 5.5), (3, 1, 1, 9), (4, 1, 1, 7.5), (5, 1, 1, 5), (1, 1, 2, 9);
            """;
    static final Duration T = Duration.ofSeconds(2);

    @TempDir
    Path tmp;

    List<JsonNode> explain(String sql) throws Exception {
        Path db = SqliteEngine.build(tmp, SCHEMA, DATA);
        List<String> lines = new StepPlanner(new SqliteEngine(), db, T).explainSelect("x", SqlGuard.check(sql, false));
        List<JsonNode> out = new ArrayList<>();
        for (String l : lines) {
            out.add(JsonPayload.MAPPER.readTree(l));
        }
        return out;
    }

    static List<JsonNode> of(List<JsonNode> ev, String t) {
        return ev.stream().filter(e -> e.path("t").asText().equals(t)).toList();
    }

    static List<String> cols(JsonNode e) {
        List<String> out = new ArrayList<>();
        e.path("cols").forEach(c -> out.add(c.asText()));
        return out;
    }

    static String said(List<JsonNode> ev) {
        StringBuilder sb = new StringBuilder();
        of(ev, "say").forEach(e -> sb.append(e.path("s").asText()).append('\n'));
        return sb.toString();
    }

    /** Không rơi về "chỉ hiện kết quả cuối": các bước tách ra khớp kết quả chạy thật. */
    static void stepped(List<JsonNode> ev) {
        assertFalse(said(ev).contains("Chỉ hiện kết quả cuối"), said(ev));
        assertEquals("hdr", ev.get(0).path("t").asText());
        assertEquals("bang-sql", ev.get(0).path("views").get(0).path("kieu").asText());
        for (JsonNode e : ev) {
            if (e.path("t").asText().startsWith("sql.")) {
                assertEquals("sql", e.path("v").asText(), "mọi sự kiện sql.* phải có v=sql: " + e);
            }
        }
    }

    @Test
    void where_danhDauTungDong_roiGiuDongThoa() throws Exception {
        var ev = explain("SELECT ho_ten FROM sinh_vien WHERE que_quan = 'Hà Nội' AND ngay_sinh >= '2005-01-01'");
        stepped(ev);
        assertEquals(5, of(ev, "sql.mark").size(), "bảng 5 dòng: đánh dấu từng dòng");
        assertEquals("[0,3]", of(ev, "sql.keep").get(0).path("rows").toString());
        assertEquals(1, of(ev, "sql.project").size());
        assertEquals(2, of(ev, "sql.result").get(0).path("rows").size());
        assertTrue(of(ev, "key").size() >= 3, "mỗi mệnh đề một bước quan trọng");
    }

    @Test
    void soSanhVoiNull_khongDongNaoThoa_isNullThi_co() throws Exception {
        var eq = explain("SELECT ho_ten FROM sinh_vien WHERE lop_id = NULL");
        stepped(eq);
        assertEquals("[]", of(eq, "sql.keep").get(0).path("rows").toString());
        var is = explain("SELECT ho_ten FROM sinh_vien WHERE lop_id IS NULL");
        assertEquals("[4]", of(is, "sql.keep").get(0).path("rows").toString());
    }

    @Test
    void innerJoin_dongKhongCoCapBiLoai() throws Exception {
        var ev = explain("SELECT s.ho_ten, l.ten_lop FROM sinh_vien s JOIN lop l ON s.lop_id = l.id");
        stepped(ev);
        JsonNode j = of(ev, "sql.join").get(0);
        assertEquals("INNER", j.path("kind").asText());
        assertEquals(4, j.path("pairs").size(), "Em không có lớp");
        assertEquals(4, of(ev, "sql.pair").size());
        assertTrue(of(ev, "sql.table").get(1).path("side").asBoolean(), "bảng bên phải hiện bên cạnh");
        assertEquals("s.ho_ten", j.path("cols").get(1).asText(), "sau khi ghép, tên cột kèm tên bảng");
    }

    @Test
    void leftJoin_dongKhongCoCapVanGiu_benPhaiNull() throws Exception {
        var ev = explain("SELECT s.ho_ten, l.ten_lop FROM sinh_vien s LEFT JOIN lop l ON s.lop_id = l.id");
        stepped(ev);
        JsonNode j = of(ev, "sql.join").get(0);
        assertEquals(5, j.path("pairs").size());
        assertEquals("[4,-1]", j.path("pairs").get(4).toString());
        JsonNode em = j.path("rows").get(4);
        assertTrue(em.get(em.size() - 1).isNull(), "phần bên phải là NULL");
        assertEquals(
                List.of("s.id", "s.ho_ten", "s.lop_id", "l.id", "l.ten_lop"),
                cols(j),
                "chỉ hiện cột câu lệnh dùng tới và cột khoá đầu");
    }

    @Test
    void groupByHaving_dungSoNhom_loaiDungNhom() throws Exception {
        var ev = explain("SELECT l.ten_lop, AVG(k.diem) AS tb FROM ket_qua k JOIN sinh_vien s ON k.sinh_vien_id = s.id"
                + " JOIN lop l ON s.lop_id = l.id WHERE k.lan_thi = 1 GROUP BY l.ten_lop HAVING AVG(k.diem) > 7");
        stepped(ev);
        JsonNode g = of(ev, "sql.group").get(0);
        assertEquals(2, g.path("groups").size(), "hai lớp có sinh viên thi lần 1");
        assertEquals("[\"D22CN01\"]", g.path("keys").get(0).toString());
        assertEquals(2, g.path("groups").get(0).size());
        JsonNode agg = of(ev, "sql.agg").get(0);
        assertEquals(6.75, agg.path("rows").get(0).get(1).asDouble(), 1e-9);
        assertEquals(
                "[1]",
                of(ev, "sql.keep")
                        .get(of(ev, "sql.keep").size() - 1)
                        .path("rows")
                        .toString());
        assertTrue(said(ev).contains("HAVING"));
        assertEquals(2, of(ev, "sql.join").size());
    }

    @Test
    void tenGoiKhacTrongGroupByOrderBy_vaLimit() throws Exception {
        var ev = explain("SELECT que_quan AS q, COUNT(*) AS so FROM sinh_vien GROUP BY q ORDER BY so DESC, q LIMIT 2");
        stepped(ev);
        assertEquals(1, of(ev, "sql.order").size());
        JsonNode lim = of(ev, "sql.limit").get(0);
        assertEquals(0, lim.path("offset").asInt());
        assertEquals(2, lim.path("count").asInt());
        assertEquals(
                "Hà Nội", of(ev, "sql.result").get(0).path("rows").get(0).get(0).asText());
    }

    @Test
    void groupByThuTuCot_vaOffset() throws Exception {
        var ev = explain("SELECT que_quan, COUNT(*) FROM sinh_vien GROUP BY 1 ORDER BY 1 LIMIT 2 OFFSET 1");
        stepped(ev);
        assertEquals(1, of(ev, "sql.limit").get(0).path("offset").asInt());
    }

    @Test
    void distinct_boDongTrung() throws Exception {
        var ev = explain("SELECT DISTINCT que_quan FROM sinh_vien");
        stepped(ev);
        assertEquals(2, of(ev, "sql.distinct").get(0).path("dup").size(), "Hà Nội lặp 3 lần: bỏ 2");
    }

    @Test
    void hamGopKhongGroupBy_caBangMotNhom() throws Exception {
        var ev = explain("SELECT COUNT(*), AVG(diem) FROM ket_qua");
        stepped(ev);
        assertEquals(1, of(ev, "sql.group").get(0).path("groups").size());
        assertEquals(6, of(ev, "sql.group").get(0).path("groups").get(0).size());
        assertEquals(1, of(ev, "sql.agg").get(0).path("rows").size());
    }

    @Test
    void union_haiVeRoiKetQua() throws Exception {
        var ev = explain("SELECT que_quan FROM sinh_vien UNION SELECT khoa FROM lop");
        stepped(ev);
        assertEquals(2, of(ev, "sql.table").size());
        JsonNode s = of(ev, "sql.setop").get(0);
        assertEquals("UNION", s.path("op").asText());
        assertEquals(5, s.path("rows").size(), "Hà Nội, Nam Định, Hải Phòng, CNTT, ATTT");
    }

    @Test
    void cauCoWith_khongLoi_chiCoKetQuaCuoi() throws Exception {
        var ev = explain("WITH t AS (SELECT ho_ten FROM sinh_vien) SELECT * FROM t");
        assertTrue(said(ev).contains("Chỉ hiện kết quả cuối"));
        assertEquals(5, of(ev, "sql.result").get(0).path("rows").size());
    }

    @Test
    void cauSaiCot_baoLoiNhuKhiChayThat() {
        assertThrows(SqlRejected.class, () -> explain("SELECT khong_co FROM sinh_vien"));
    }

    @Test
    void bangQuaLon_chiHienKetQua() throws Exception {
        StringBuilder sb = new StringBuilder("INSERT INTO lop VALUES ");
        for (int i = 10; i < 10 + StepPlanner.MAX_ANIM_ROWS + 5; i++) {
            sb.append(i > 10 ? ", " : "")
                    .append("(")
                    .append(i)
                    .append(", 'L")
                    .append(i)
                    .append("', 'K')");
        }
        Path db = SqliteEngine.build(tmp, SCHEMA, DATA, sb.toString());
        List<String> lines = new StepPlanner(new SqliteEngine(), db, T)
                .explainSelect("x", SqlGuard.check("SELECT * FROM lop", false));
        assertTrue(String.join("\n", lines).contains("quá nhiều để làm animation"));
    }

    @Test
    void update_motDongSua_dungCotBiDoi() throws Exception {
        Path db = SqliteEngine.build(tmp, SCHEMA, DATA);
        Path copy = SqliteEngine.copy(db, tmp);
        List<String> lines = new StepPlanner(new SqliteEngine(), db, T)
                .explainDml(
                        "x", SqlGuard.check("UPDATE ket_qua SET diem = diem + 1 WHERE sinh_vien_id = 2", true), copy);
        List<JsonNode> ev = new ArrayList<>();
        for (String l : lines) {
            ev.add(JsonPayload.MAPPER.readTree(l));
        }
        JsonNode changes = of(ev, "sql.diff").get(0).path("changes");
        assertEquals(1, changes.size());
        assertEquals("sua", changes.get(0).path("kind").asText());
        assertEquals("[3]", changes.get(0).path("cells").toString(), "chỉ cột diem đổi");
        assertEquals(6.5, changes.get(0).path("row").get(3).asDouble(), 1e-9);
        assertEquals(
                5.5,
                ((Number) new SqliteEngine()
                                .query(db, "SELECT diem FROM ket_qua WHERE sinh_vien_id = 2", T)
                                .rows()
                                .get(0)
                                .get(0))
                        .doubleValue(),
                1e-9,
                "bản gốc nguyên vẹn");
    }

    @Test
    void deleteVaInsert_dungLoaiThayDoi() throws Exception {
        Path db = SqliteEngine.build(tmp, SCHEMA, DATA);
        var p = new StepPlanner(new SqliteEngine(), db, T);
        String del = String.join(
                "\n",
                p.explainDml(
                        "x",
                        SqlGuard.check("DELETE FROM sinh_vien WHERE lop_id IS NULL", true),
                        SqliteEngine.copy(db, tmp)));
        assertTrue(del.contains("\"kind\":\"xoa\""));
        String ins = String.join(
                "\n",
                p.explainDml(
                        "x",
                        SqlGuard.check("INSERT INTO lop VALUES (9, 'D22KT01', 'KT')", true),
                        SqliteEngine.copy(db, tmp)));
        assertTrue(ins.contains("\"kind\":\"them\""));
    }
}
