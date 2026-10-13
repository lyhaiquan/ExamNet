package labcast.client.views.dslk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import labcast.common.trace.TraceEvent;
import org.junit.jupiter.api.Test;

class LinkedListModelTest {
    private static final ObjectMapper M = new ObjectMapper();

    private static void ap(LinkedListModel m, String... events) throws Exception {
        for (String json : events) {
            ObjectNode o = (ObjectNode) M.readTree(json.replace('\'', '"'));
            m.apply(new TraceEvent(o.get("t").asText(), o));
        }
    }

    /** 1 → 2 → 3. */
    private static LinkedListModel baNut() throws Exception {
        LinkedListModel m = new LinkedListModel();
        m.reset(M.readTree(
                "{\"kieu\":\"dslk\",\"dau\":0,\"nut\":[[0,1,1,null],[1,2,2,null],[2,3,null,null]],\"tro\":[]}"));
        return m;
    }

    @Test
    void daoNguoc_nutDungYenChoToiKhiXong() throws Exception {
        LinkedListModel m = baNut();
        ap(m, "{'t':'ptr','p':[['cur',0]]}", "{'t':'link','id':0,'w':null}");
        assertEquals(List.of(0, 1, 2), m.order(), "đang đảo dở: giữ thứ tự tạo nút");
        ap(
                m,
                "{'t':'link','id':1,'w':0}",
                "{'t':'link','id':2,'w':1}",
                "{'t':'link','id':null,'w':2}",
                "{'t':'ptr','p':[['prev',2]]}");
        assertEquals(List.of(2, 1, 0), m.order(), "đảo xong, đi từ đầu qua đủ nút: vẽ theo thứ tự danh sách");
        assertEquals("3→2 2→1 1→None đầu=3 prev=3", m.describe());
    }

    @Test
    void chenGiua_nutMoiVaoDungCho() throws Exception {
        LinkedListModel m = baNut();
        ap(m, "{'t':'node','id':3,'x':9}", "{'t':'link','id':3,'w':2}");
        assertEquals(List.of(0, 1, 2, 3), m.order());
        ap(m, "{'t':'link','id':1,'w':3}");
        assertEquals(List.of(0, 1, 3, 2), m.order());
    }

    @Test
    void xoaNut_veMo() throws Exception {
        LinkedListModel m = baNut();
        ap(m, "{'t':'link','id':0,'w':2}");
        assertTrue(m.orphan(1));
        assertFalse(m.orphan(2));
        ap(m, "{'t':'ptr','p':[['p',1]]}");
        assertFalse(m.orphan(1), "còn biến trỏ vào thì chưa mờ");
    }

    @Test
    void docGhiDanhDau() throws Exception {
        LinkedListModel m = baNut();
        ap(m, "{'t':'set','id':1,'x':5}", "{'t':'mark','id':0,'m':'xong'}");
        assertEquals("1→5:xong 5→3 3→None đầu=1", m.describe());
    }
}
