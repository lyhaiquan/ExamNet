package labcast.client.views.cay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import labcast.common.trace.TraceEvent;
import org.junit.jupiter.api.Test;

class TreeModelTest {
    private static final ObjectMapper M = new ObjectMapper();

    private static TraceEvent ev(String json) throws Exception {
        ObjectNode o = (ObjectNode) M.readTree(json.replace('\'', '"'));
        return new TraceEvent(o.get("t").asText(), o);
    }

    private static void ap(TreeModel m, String... events) throws Exception {
        for (String e : events) {
            m.apply(ev(e));
        }
    }

    /** 5 làm gốc, 3 bên trái, 8 bên phải. */
    private static TreeModel baNut() throws Exception {
        TreeModel m = new TreeModel();
        ap(
                m,
                "{'t':'node','v':'T','id':0,'x':5}",
                "{'t':'edge','v':'T','u':null,'c':null,'w':0}",
                "{'t':'node','v':'T','id':1,'x':3}",
                "{'t':'edge','v':'T','u':0,'c':'L','w':1}",
                "{'t':'node','v':'T','id':2,'x':8}",
                "{'t':'edge','v':'T','u':0,'c':'R','w':2}");
        return m;
    }

    @Test
    void chenVaBoTri_theoDuyetGiua() throws Exception {
        TreeModel m = baNut();
        assertEquals("(3 5 8)", m.shape());
        Map<Integer, TreeModel.Pos> pos = m.layout();
        assertEquals(new TreeModel.Pos(0, 1), pos.get(1));
        assertEquals(new TreeModel.Pos(1, 0), pos.get(0));
        assertEquals(new TreeModel.Pos(2, 1), pos.get(2));
    }

    @Test
    void nutMoiTaoChuaGan_vanVeBenPhai() throws Exception {
        TreeModel m = baNut();
        ap(m, "{'t':'node','v':'T','id':3,'x':9}");
        assertEquals(new TreeModel.Pos(4, 0), m.layout().get(3)); // chừa một cột sau cây chính
    }

    @Test
    void xoayPhai_giuaChungKhongMatNut() throws Exception {
        // y=5 gốc, x=3 con trái có con trái 1: xoay phải tại gốc
        TreeModel m = new TreeModel();
        ap(
                m,
                "{'t':'node','v':'T','id':0,'x':5}",
                "{'t':'edge','v':'T','u':null,'c':null,'w':0}",
                "{'t':'node','v':'T','id':1,'x':3}",
                "{'t':'edge','v':'T','u':0,'c':'L','w':1}",
                "{'t':'node','v':'T','id':2,'x':1}",
                "{'t':'edge','v':'T','u':1,'c':'L','w':2}",
                "{'t':'edge','v':'T','u':0,'c':'L','w':null}"); // y.trai = x.phai (None)
        assertEquals(3, m.layout().size(), "x và con của nó tạm rời cây nhưng vẫn được vẽ");
        ap(m, "{'t':'edge','v':'T','u':1,'c':'R','w':0}", "{'t':'edge','v':'T','u':null,'c':null,'w':1}");
        assertEquals("(1 3 5)", m.shape());
    }

    @Test
    void nutBiXoa_khongVeNua() throws Exception {
        TreeModel m = baNut();
        ap(m, "{'t':'edge','v':'T','u':0,'c':'R','w':null}");
        assertFalse(m.layout().containsKey(2));
        assertEquals("(3 5 .)", m.shape());
    }

    @Test
    void docGhiDanhDau() throws Exception {
        TreeModel m = baNut();
        assertEquals(List.of(new TreeModel.Read(1)), m.apply(ev("{'t':'read','v':'T','id':1}")));
        m.apply(ev("{'t':'set','v':'T','id':1,'x':4}"));
        m.apply(ev("{'t':'mark','v':'T','id':2,'m':'xong'}"));
        assertEquals("4", m.node(1).text);
        assertEquals("xong", m.node(2).mark);
    }

    @Test
    void dungLaiTuAnhChup() throws Exception {
        TreeModel m = new TreeModel();
        m.reset(M.readTree("{\"kieu\":\"cay\",\"goc\":0,\"nut\":[[0,5,1,null,null,true],[1,3,null,null,\"xong\",true],"
                + "[2,7,null,null,null,true],[3,9,null,null,null,false]]}"));
        assertEquals("(3 5 .)", m.shape());
        Map<Integer, TreeModel.Pos> pos = m.layout();
        assertFalse(pos.containsKey(2), "nút 7 đã rời cây");
        assertTrue(pos.containsKey(3), "nút 9 mới tạo, chưa gắn");
    }
}
