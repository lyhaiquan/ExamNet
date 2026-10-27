package labcast.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardStatsTest {

    static ObjectNode bai(String id, String status, boolean active) {
        String[] p = id.split("\\.");
        ObjectNode n = JsonNodeFactory.instance
                .objectNode()
                .put("id", id)
                .put("ten", id)
                .put("mon", p[0])
                .put("chuong", p[1])
                .put("loai", "bai-tap")
                .put("status", status)
                .put("active", active);
        if (status.equals("MASTERED")) n.put("score", 100);
        return n;
    }

    static final List<JsonNode> KHO = List.of(
            bai("dsa.sap-xep.a", "MASTERED", true),
            bai("dsa.sap-xep.b", "MASTERED", true),
            bai("dsa.ngan-xep.c", "OPEN", true),
            bai("dsa.ngan-xep.d", "NOT_STARTED", true),
            bai("dsa.cay.e", "NOT_STARTED", false),
            bai("sql.truy-van.f", "NOT_STARTED", true));

    @Test
    void hocVien_khongTinhBaiDangAn() {
        DashboardStats s = DashboardStats.of(KHO, true);
        assertEquals(5, s.total());
        assertEquals(2, s.done());
        assertEquals(1, s.inProgress());
        assertEquals(40, s.percentDone());
        assertEquals(200, s.xp());
    }

    @Test
    void giaoVien_demCaBaiDangAn() {
        DashboardStats s = DashboardStats.of(KHO, false);
        assertEquals(6, s.total());
        assertEquals(1, s.hidden());
    }

    @Test
    void tienDoTheoChuong_theoThuTuChuong_vaChuongXongThanhHuyHieu() {
        DashboardStats s = DashboardStats.of(KHO, true);
        DashboardStats.ChapterProgress mang = s.chapters().get(0);
        assertEquals("sap-xep", mang.chuong());
        assertEquals(2, mang.done());
        assertEquals(2, mang.total());
        assertEquals(1, s.badges().size());
        assertTrue(
                s.badges().get(0).toLowerCase().contains("sắp xếp"), s.badges().get(0));
    }

    @Test
    void tiepTuc_uuTienBaiDangLam_roiDenBaiChuaLam() {
        assertEquals(
                "dsa.ngan-xep.c",
                DashboardStats.of(KHO, true)
                        .continueLesson()
                        .orElseThrow()
                        .path("id")
                        .asText());
        List<JsonNode> chuaLam =
                List.of(bai("dsa.sap-xep.a", "MASTERED", true), bai("dsa.sap-xep.b", "NOT_STARTED", true));
        assertEquals(
                "dsa.sap-xep.b",
                DashboardStats.of(chuaLam, true)
                        .continueLesson()
                        .orElseThrow()
                        .path("id")
                        .asText());
        assertTrue(DashboardStats.of(List.of(bai("dsa.sap-xep.a", "MASTERED", true)), true)
                .continueLesson()
                .isEmpty());
    }

    @Test
    void phanBoTheoMon() {
        List<DashboardStats.Slice> slices = DashboardStats.of(KHO, false).bySubject();
        assertEquals("DSA", slices.get(0).label());
        assertEquals(5, slices.get(0).count());
        assertEquals("SQL", slices.get(1).label());
        assertEquals(1, slices.get(1).count());
    }

    @Test
    void tyLeHoanThanhCuaLop_vaHoatDongMoiNhat() {
        ArrayNode students = JsonNodeFactory.instance.arrayNode();
        students.addObject()
                .put("fullName", "A")
                .put("completed", 2)
                .put("updatedAt", 10)
                .put("online", true);
        students.addObject()
                .put("fullName", "B")
                .put("completed", 0)
                .put("updatedAt", 30)
                .put("online", false);
        assertEquals(25, DashboardStats.classCompletion(students, 4));
        assertEquals(0, DashboardStats.classCompletion(students, 0));
        assertEquals(1, DashboardStats.online(students));
        assertEquals(
                "B", DashboardStats.recent(students, 5).get(0).path("fullName").asText());
    }
}
