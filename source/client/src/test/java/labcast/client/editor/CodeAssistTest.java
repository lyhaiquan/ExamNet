package labcast.client.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CodeAssistTest {

    @Test
    void xuongDong_giuThut_vaThutThemSauHaiChamHoacNgoac() {
        assertEquals("\n    ", CodeAssist.newline("def f(a):"));
        assertEquals("\n    ", CodeAssist.newline("x = 1\n    y = 2"));
        assertEquals("\n        ", CodeAssist.newline("def f(a):\n    for i in a:  "));
        assertEquals("\n    ", CodeAssist.newline("int main() {"));
        assertEquals("\n", CodeAssist.newline("print(1)"));
    }

    @Test
    void tuDongNgoac_vaBuocQuaNgoacDong() {
        assertEquals(")", CodeAssist.closing('('));
        assertEquals("}", CodeAssist.closing('{'));
        assertEquals("", CodeAssist.closing('a'));
        assertTrue(CodeAssist.skipsOver(')', ")\n"));
        assertFalse(CodeAssist.skipsOver(')', "x)"));
        assertFalse(CodeAssist.skipsOver('a', "a"));
    }

    @Test
    void dinhDangSql_tuKhoaHoa_moiMenhDeMotDong() {
        String sql = "select ho_ten, count(*) as so from sinh_vien sv join lop l on l.id = sv.lop_id "
                + "where que = 'hà nội and x' and nam > 2000 or nam is null group by ho_ten order by so desc;";
        assertEquals(
                "SELECT ho_ten, count(*) AS so\n"
                        + "FROM sinh_vien sv\n"
                        + "JOIN lop l ON l.id = sv.lop_id\n"
                        + "WHERE que = 'hà nội and x'\n"
                        + "    AND nam > 2000\n"
                        + "    OR nam IS NULL\n"
                        + "GROUP BY ho_ten\n"
                        + "ORDER BY so DESC;",
                CodeAssist.formatSql(sql));
    }

    @Test
    void dinhDangSql_leftJoin_vaNhieuCau_vaChuThich() {
        String sql = "-- lấy khách\nselect * from a left join b on a.id=b.id;\nupdate a set x = 1 where id = 2;";
        assertEquals(
                "-- lấy khách\nSELECT *\nFROM a\nLEFT JOIN b ON a.id=b.id;\n\nUPDATE a\nSET x = 1\nWHERE id = 2;",
                CodeAssist.formatSql(sql));
    }
}
