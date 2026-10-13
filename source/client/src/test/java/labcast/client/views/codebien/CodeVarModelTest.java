package labcast.client.views.codebien;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import labcast.common.trace.TraceEvent;
import org.junit.jupiter.api.Test;

class CodeVarModelTest {
    private static final ObjectMapper M = new ObjectMapper();

    private static void ap(CodeVarModel m, String json) throws Exception {
        ObjectNode o = (ObjectNode) M.readTree(json.replace('\'', '"'));
        m.apply(new TraceEvent(o.get("t").asText(), o));
    }

    @Test
    void dongVaBien_bienVuaDoiDuocDanhDau() throws Exception {
        CodeVarModel m = new CodeVarModel();
        m.reset(M.readTree("{\"kieu\":\"code-bien\",\"src\":\"a = 1\\nb = 2\",\"dong\":0,\"ham\":\"\",\"bien\":[]}"));
        assertEquals(2, m.lines().size());
        ap(m, "{'t':'line','n':2}");
        ap(m, "{'t':'vars','f':'','d':[['a','1']]}");
        assertTrue(m.changed("a"));
        ap(m, "{'t':'line','n':3}");
        ap(m, "{'t':'vars','f':'','d':[['a','1'],['b','2']]}");
        assertTrue(m.changed("b"));
        assertFalse(m.changed("a"));
        assertEquals("dòng 3 {a=1, b=2}", m.describe());
    }

    @Test
    void doiKhung_khongToBienCuaKhungMoi() throws Exception {
        CodeVarModel m = new CodeVarModel();
        ap(m, "{'t':'vars','f':'fib','d':[['n','3']]}");
        assertFalse(m.changed("n"), "vừa vào hàm: mọi biến đều mới, không tô");
        assertEquals("dòng 0 trong fib {n=3}", m.describe());
    }
}
