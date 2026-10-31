package labcast.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExerciseCatalogTest {
    static final List<JsonNode> KHO = List.of(
            DashboardStatsTest.bai("dsa.sap-xep.a", "MASTERED", true),
            DashboardStatsTest.bai("dsa.sap-xep.b", "OPEN", true),
            DashboardStatsTest.bai("dsa.cay.c", "NOT_STARTED", true),
            DashboardStatsTest.bai("dsa.cay.d", "NOT_STARTED", false),
            DashboardStatsTest.bai("sql.co-ban.e", "NOT_STARTED", true));

    static List<String> ids(List<JsonNode> l) {
        return l.stream().map(n -> n.path("id").asText()).toList();
    }

    static ExerciseCatalog.Query q() {
        return new ExerciseCatalog.Query("", null, Set.of(), "Tất cả", "");
    }

    @Test
    void lopChiCoMotMon_khongCoLuaChonMonKhac() {
        assertEquals(List.of("DSA"), ExerciseCatalog.subjects(KHO.subList(0, 4)));
        assertEquals(List.of("DSA", "SQL"), ExerciseCatalog.subjects(KHO));
    }

    @Test
    void locTheoMon_vaChuong() {
        var dsa = new ExerciseCatalog.Query("dsa", null, Set.of(), "Tất cả", "");
        assertEquals(4, ExerciseCatalog.select(KHO, dsa, false).size());
        var cay = new ExerciseCatalog.Query("dsa", "cay", Set.of(), "Tất cả", "");
        assertEquals(List.of("dsa.cay.c", "dsa.cay.d"), ids(ExerciseCatalog.select(KHO, cay, false)));
    }

    @Test
    void locTheoTrangThai_hocVienKhongThayBaiAn() {
        var daGiai = new ExerciseCatalog.Query("", null, Set.of(), "Đã giải", "");
        assertEquals(List.of("dsa.sap-xep.a"), ids(ExerciseCatalog.select(KHO, daGiai, true)));
        var lamTiep = new ExerciseCatalog.Query("", null, Set.of(), "Đang làm", "");
        assertEquals(List.of("dsa.sap-xep.b"), ids(ExerciseCatalog.select(KHO, lamTiep, true)));
        assertEquals(4, ExerciseCatalog.select(KHO, q(), true).size(), "bài đang ẩn không hiện với học viên");
        var dangAn = new ExerciseCatalog.Query("", null, Set.of(), "Đang ẩn", "");
        assertEquals(List.of("dsa.cay.d"), ids(ExerciseCatalog.select(KHO, dangAn, false)));
    }

    @Test
    void timKhongDau_vaLocLoai() {
        var tim = new ExerciseCatalog.Query("", null, Set.of(), "Tất cả", "CAY");
        assertEquals(2, ExerciseCatalog.select(KHO, tim, false).size());
        var giangGiai = new ExerciseCatalog.Query("", null, Set.of("giang-giai"), "Tất cả", "");
        assertEquals(0, ExerciseCatalog.select(KHO, giangGiai, false).size());
        var baiTap = new ExerciseCatalog.Query("", null, Set.of("bai-tap"), "Tất cả", "");
        assertEquals(5, ExerciseCatalog.select(KHO, baiTap, false).size());
    }

    @Test
    void maTuTenTiengViet() {
        assertEquals("hang-doi-uu-tien", ExerciseCatalog.slug("Hàng đợi ưu tiên"));
        assertEquals("cay-nhi-phan-2", ExerciseCatalog.slug("  Cây nhị phân (2) "));
    }

    @Test
    void tenChuong_uuTienTenGiaoVienDat_vaBaiSai() {
        var moi = DashboardStatsTest.bai("dsa.hang-doi-uu-tien.a", "OPEN", true).put("chuongTen", "Hàng đợi ưu tiên");
        assertEquals("Hàng đợi ưu tiên", ExerciseCatalog.chapterName(moi));
        assertEquals("Ngăn xếp", ExerciseCatalog.chapterName(DashboardStatsTest.bai("dsa.ngan-xep.a", "OPEN", true)));
        assertEquals(true, ExerciseCatalog.isWrong(moi));
        assertEquals(false, ExerciseCatalog.isWrong(KHO.get(0)));
    }

    private static JsonNode voiDoKho(String id, String doKho) {
        var n = (com.fasterxml.jackson.databind.node.ObjectNode)
                DashboardStatsTest.bai(id, "NOT_STARTED", true).deepCopy();
        n.put("doKho", doKho);
        return n;
    }

    @Test
    void locTheoDoKho_vaChuaPhanLoai() {
        var kho = List.of(voiDoKho("dsa.a.1", "de"), voiDoKho("dsa.a.2", "kho"), voiDoKho("dsa.a.3", ""));
        var de = new ExerciseCatalog.Query("", null, Set.of(), "Tất cả", "", "de");
        assertEquals(List.of("dsa.a.1"), ids(ExerciseCatalog.select(kho, de, false)));
        var chua = new ExerciseCatalog.Query("", null, Set.of(), "Tất cả", "", "-");
        assertEquals(List.of("dsa.a.3"), ids(ExerciseCatalog.select(kho, chua, false)));
        var moi = new ExerciseCatalog.Query("", null, Set.of(), "Tất cả", "", "*");
        assertEquals(3, ExerciseCatalog.select(kho, moi, false).size());
    }

    @Test
    void bamNhanDoKho_xoayVong() {
        assertEquals("de", ExerciseCatalog.nextDoKho(""));
        assertEquals("vua", ExerciseCatalog.nextDoKho("de"));
        assertEquals("kho", ExerciseCatalog.nextDoKho("vua"));
        assertEquals("", ExerciseCatalog.nextDoKho("kho"));
    }

    @Test
    void doKhoLa_xemNhuChuaPhanLoai() {
        assertEquals("", ExerciseCatalog.doKho(voiDoKho("dsa.a.9", "sieu-kho")));
        assertEquals("Khó", ExerciseCatalog.doKhoLabel("kho"));
    }
}
