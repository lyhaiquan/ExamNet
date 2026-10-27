package labcast.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class DiscussionModelTest {
    static ObjectNode muc(long id, long parent, boolean hidden) {
        return JsonNodeFactory.instance
                .objectNode()
                .put("id", id)
                .put("parentId", parent)
                .put("text", "c" + id)
                .put("hidden", hidden);
    }

    @Test
    void gomLuong_traLoiDiTheoGoc() {
        var m = new DiscussionModel(false);
        var list = JsonNodeFactory.instance
                .arrayNode()
                .add(muc(1, 0, false))
                .add(muc(2, 1, false))
                .add(muc(3, 0, false));
        m.reset(list);
        m.upsert(muc(4, 1, false));
        assertEquals(2, m.threads().size());
        assertEquals(2, m.threads().get(0).replies().size());
        assertEquals(4, m.size());
    }

    @Test
    void hocVien_mucBiAnThiGo_giaoVienVanThay() {
        var hv = new DiscussionModel(false);
        hv.upsert(muc(1, 0, false));
        hv.upsert(muc(2, 1, false));
        hv.upsert(muc(1, 0, true));
        assertEquals(0, hv.threads().size(), "gốc bị ẩn thì cả luồng biến mất với học viên");
        var gv = new DiscussionModel(true);
        gv.upsert(muc(1, 0, false));
        gv.upsert(muc(1, 0, true));
        assertEquals(1, gv.threads().size());
    }

    @Test
    void theoDoi_vaSapXep() {
        var m = new DiscussionModel(false);
        m.upsert(muc(1, 0, false).put("likes", 5));
        m.upsert(muc(2, 0, false).put("mine", true));
        m.upsert(muc(3, 0, false));
        m.upsert(muc(4, 3, false).put("mine", true));
        var newest = m.threads(false, DiscussionModel.Sort.NEWEST);
        assertEquals(3, newest.get(0).root().path("id").asLong(), "luồng 3 có trả lời mới nhất");
        assertEquals(
                1,
                m.threads(false, DiscussionModel.Sort.USEFUL)
                        .get(0)
                        .root()
                        .path("id")
                        .asLong());
        var following = m.threads(true, DiscussionModel.Sort.NEWEST);
        assertEquals(2, following.size(), "luồng mình hỏi và luồng mình trả lời");
    }
}
