package labcast.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import org.junit.jupiter.api.Test;

class LessonFilterTest {

    static JsonNode bai(String id, String ten) {
        return JsonNodeFactory.instance.objectNode().put("id", id).put("ten", ten);
    }

    static final List<JsonNode> KHO = List.of(
            bai("dsa.sap-xep.noi-bot", "Sắp xếp nổi bọt"),
            bai("dsa.do-thi.dijkstra", "Đường đi ngắn nhất Dijkstra"),
            bai("dsa.cay.duyet-cay", "Duyệt cây: trước, giữa, sau, theo mức"),
            bai("dsa.do-thi.bfs", "BFS: duyệt đồ thị theo chiều rộng"),
            bai("dsa.cay.avl", "Cây AVL"),
            bai("dsa.quay-lui.n-queens", "Ăn quân hậu"));

    static List<String> ten(List<JsonNode> ds) {
        return ds.stream().map(n -> n.path("ten").asText()).toList();
    }

    @Test
    void khongGoGi_giuNguyenThuTuChuong() {
        assertEquals(ten(KHO), ten(LessonFilter.apply(KHO, "", LessonFilter.Order.CHUONG)));
        assertEquals(ten(KHO), ten(LessonFilter.apply(KHO, "   ", LessonFilter.Order.CHUONG)));
    }

    /** Gõ không dấu, sai hoa thường vẫn ra: "duong di" tìm được "Đường đi…". */
    @Test
    void timKhongDau_khongPhanBietHoaThuong() {
        assertEquals(
                List.of("Đường đi ngắn nhất Dijkstra"),
                ten(LessonFilter.apply(KHO, "duong di", LessonFilter.Order.CHUONG)));
        assertEquals(List.of("Sắp xếp nổi bọt"), ten(LessonFilter.apply(KHO, "NỔI BỌT", LessonFilter.Order.CHUONG)));
        assertEquals(
                List.of("Duyệt cây: trước, giữa, sau, theo mức", "BFS: duyệt đồ thị theo chiều rộng"),
                ten(LessonFilter.apply(KHO, "duyet", LessonFilter.Order.CHUONG)));
    }

    /** Nhiều từ: bài phải chứa đủ mọi từ, không cần liền nhau. */
    @Test
    void nhieuTu_baiPhaiChuaDuMoiTu() {
        assertEquals(
                List.of("BFS: duyệt đồ thị theo chiều rộng"),
                ten(LessonFilter.apply(KHO, "rong duyet", LessonFilter.Order.CHUONG)));
        assertEquals(List.of(), ten(LessonFilter.apply(KHO, "rong cay", LessonFilter.Order.CHUONG)));
    }

    /** Thứ tự chữ cái tiếng Việt: A < Ă < B < C < D < Đ, so cả chữ không phân biệt hoa thường. */
    @Test
    void sapXepTheoChuCaiTiengViet() {
        assertEquals(
                List.of(
                        "Ăn quân hậu",
                        "BFS: duyệt đồ thị theo chiều rộng",
                        "Cây AVL",
                        "Duyệt cây: trước, giữa, sau, theo mức",
                        "Đường đi ngắn nhất Dijkstra",
                        "Sắp xếp nổi bọt"),
                ten(LessonFilter.apply(KHO, "", LessonFilter.Order.CHU_CAI)));
    }

    @Test
    void timRoiSapXep() {
        assertEquals(
                List.of("BFS: duyệt đồ thị theo chiều rộng", "Duyệt cây: trước, giữa, sau, theo mức"),
                ten(LessonFilter.apply(KHO, "duyet", LessonFilter.Order.CHU_CAI)));
    }
}
