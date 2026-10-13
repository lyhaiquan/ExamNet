package labcast.server.sqlviz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SqlGuardTest {

    static String loi(String sql, boolean dml) {
        return assertThrows(SqlRejected.class, () -> SqlGuard.check(sql, dml)).getMessage();
    }

    @Test
    void xoaBang_biChan() {
        assertTrue(loi("DROP TABLE sinh_vien", true).contains("DROP"));
        loi("ATTACH DATABASE 'x.db' AS x", false);
        loi("PRAGMA writable_schema = 1", false);
        loi("CREATE TABLE t (a)", true);
    }

    @Test
    void haiCauLenh_biChan_keCaCauSauVoHai() {
        assertTrue(loi("SELECT 1; SELECT 2", false).contains("một câu"));
        loi("SELECT 1; DROP TABLE sinh_vien", false);
    }

    @Test
    void dauChamPhayTrongChuoi_vanLaMotCau() throws Exception {
        assertEquals(
                SqlGuard.Kind.SELECT,
                SqlGuard.check("SELECT 'a;b' AS x;", false).kind());
    }

    @Test
    void dmlChiChoBaiDml() throws Exception {
        assertTrue(loi("UPDATE nhan_vien SET luong = 0", false).contains("SELECT"));
        assertEquals(
                SqlGuard.Kind.DML,
                SqlGuard.check("UPDATE nhan_vien SET luong = 0", true).kind());
        assertEquals(
                SqlGuard.Kind.DML,
                SqlGuard.check("DELETE FROM don_hang WHERE id = 1", true).kind());
    }

    @Test
    void cuPhapSqliteThuongDung_phanTichDuoc() throws Exception {
        for (String sql : new String[] {
            "SELECT ho_ten FROM sinh_vien WHERE que_quan = 'Hà Nội' AND ngay_sinh >= '2005-01-01'",
            "SELECT * FROM san_pham ORDER BY gia DESC, id LIMIT 5 OFFSET 10",
            "SELECT ho_ten || ' (' || id || ')' FROM khach_hang",
            "SELECT strftime('%m', ngay_dat) AS thang, COUNT(*) FROM don_hang GROUP BY thang",
            "SELECT IIF(diem >= 4, 'Đạt', 'Trượt') FROM ket_qua",
            "SELECT CASE WHEN diem IS NULL THEN 'Chưa thi' ELSE 'Có' END FROM ket_qua",
            "SELECT a.id FROM khach_hang a RIGHT JOIN don_hang d ON d.khach_hang_id = a.id",
            "SELECT * FROM lop FULL OUTER JOIN sinh_vien ON sinh_vien.lop_id = lop.id",
            "WITH t AS (SELECT 1 AS x) SELECT x FROM t",
            "SELECT ten FROM phong_ban UNION SELECT ten FROM du_an",
        }) {
            assertEquals(SqlGuard.Kind.SELECT, SqlGuard.check(sql, false).kind(), sql);
        }
    }

    @Test
    void cauRong_hoacLoiCuPhap_baoRo() {
        assertTrue(loi("   -- chỉ có chú thích\n", false).contains("Chưa có câu lệnh"));
        assertTrue(loi("SELEC ho_ten FROM sinh_vien", false).contains("phân tích"));
    }
}
