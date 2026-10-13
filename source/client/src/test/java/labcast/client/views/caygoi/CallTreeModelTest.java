package labcast.client.views.caygoi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import labcast.common.trace.TraceEvent;
import org.junit.jupiter.api.Test;

class CallTreeModelTest {
    private static final ObjectMapper M = new ObjectMapper();

    private static void ap(CallTreeModel m, String... events) throws Exception {
        for (String json : events) {
            ObjectNode o = (ObjectNode) M.readTree(json.replace('\'', '"'));
            m.apply(new TraceEvent(o.get("t").asText(), o));
        }
    }

    @Test
    void goiTraVeDanhDau_vaNgan() throws Exception {
        CallTreeModel m = new CallTreeModel();
        ap(
                m,
                "{'t':'call','v':'fib','id':0,'p':null,'a':'fib(2)'}",
                "{'t':'call','v':'fib','id':1,'p':0,'a':'fib(1)'}");
        assertEquals(1, m.current());
        ap(
                m,
                "{'t':'ret','v':'fib','id':1,'x':'1'}",
                "{'t':'call','v':'fib','id':2,'p':0,'a':'fib(0)'}",
                "{'t':'mark','v':'fib','id':2,'m':'bo'}",
                "{'t':'ret','v':'fib','id':2,'x':'0'}");
        assertEquals("fib(2):dang fib(1)=1:xong fib(0)=0:bo ngăn[0]", m.describe());
        assertEquals(0, m.current());
    }

    @Test
    void boTri_chaGiuaHaiCon() throws Exception {
        CallTreeModel m = new CallTreeModel();
        ap(
                m,
                "{'t':'call','v':'f','id':0,'p':null,'a':'f(2)'}",
                "{'t':'call','v':'f','id':1,'p':0,'a':'f(1)'}",
                "{'t':'call','v':'f','id':2,'p':1,'a':'f(0)'}",
                "{'t':'call','v':'f','id':3,'p':0,'a':'f(0)'}");
        Map<Integer, double[]> pos = m.layout();
        assertArrayEquals(new double[] {0, 2}, pos.get(2));
        assertArrayEquals(new double[] {0, 1}, pos.get(1));
        assertArrayEquals(new double[] {1, 1}, pos.get(3));
        assertArrayEquals(new double[] {0.5, 0}, pos.get(0));
    }
}
