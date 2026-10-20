package labcast.server.sqlviz;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResultComparerTest {

    static QueryResult r(List<String> cols, Object[]... rows) {
        return new QueryResult(cols, Arrays.stream(rows).map(Arrays::asList).toList(), false);
    }

    @Test
    void khongXetThuTu_giongNhau_xetThuTu_khacNhau() {
        var a = r(List.of("x"), new Object[] {1L}, new Object[] {2L});
        var b = r(List.of("y"), new Object[] {2L}, new Object[] {1L});
        assertTrue(ResultComparer.same(a, b, false), "tên cột không cần khớp");
        assertFalse(ResultComparer.same(a, b, true));
    }

    @Test
    void daTap_dongTrungPhaiDuSoLan() {
        var a = r(List.of("x"), new Object[] {1L}, new Object[] {1L}, new Object[] {2L});
        var b = r(List.of("x"), new Object[] {1L}, new Object[] {2L}, new Object[] {2L});
        assertFalse(ResultComparer.same(a, b, false));
    }

    @Test
    void soThuc_so_toi1em6_soNguyenBangSoThuc() {
        var a = r(List.of("x"), new Object[] {7.333333333}, new Object[] {8L});
        var b = r(List.of("x"), new Object[] {7.3333334}, new Object[] {8.0});
        assertTrue(ResultComparer.same(a, b, true));
        var c = r(List.of("x"), new Object[] {7.3334}, new Object[] {8.0});
        assertFalse(ResultComparer.same(a, c, true));
    }

    @Test
    void nullBangNull_khacChuoiRong() {
        var a = r(List.of("x"), new Object[] {null});
        var b = r(List.of("x"), new Object[] {null});
        var c = r(List.of("x"), new Object[] {""});
        assertTrue(ResultComparer.same(a, b, false));
        assertFalse(ResultComparer.same(a, c, false));
    }

    @Test
    void soCotKhac_khacNhau() {
        var a = r(List.of("x"), new Object[] {1L});
        var b = r(List.of("x", "y"), new Object[] {1L, 2L});
        assertFalse(ResultComparer.same(a, b, false));
    }
}
