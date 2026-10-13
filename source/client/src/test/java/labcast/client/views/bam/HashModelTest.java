package labcast.client.views.bam;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import labcast.common.trace.TraceEvent;
import org.junit.jupiter.api.Test;

class HashModelTest {
    private static final ObjectMapper M = new ObjectMapper();

    private static List<HashModel.Change> ap(HashModel m, String json) throws Exception {
        ObjectNode o = (ObjectNode) M.readTree(json.replace('\'', '"'));
        return m.apply(new TraceEvent(o.get("t").asText(), o));
    }

    @Test
    void dayChuyen_themSoXoa() throws Exception {
        HashModel m = new HashModel();
        m.reset(M.readTree("{\"kieu\":\"bam\",\"mo\":false,\"o\":[[],[],[]]}"));
        ap(m, "{'t':'bucket','v':'T','i':1,'op':'add','x':4,'k':0}");
        ap(m, "{'t':'bucket','v':'T','i':1,'op':'add','x':7,'k':1}");
        assertEquals(List.of(new HashModel.Read(1, 1)), ap(m, "{'t':'bucket','v':'T','i':1,'op':'read','k':1}"));
        assertEquals(List.of(new HashModel.Removed(1, 0, "4")), ap(m, "{'t':'bucket','v':'T','i':1,'op':'del','k':0}"));
        assertEquals("0:[] 1:[7] 2:[]", m.describe());
    }

    @Test
    void diaChiMo_ghiVaXoa() throws Exception {
        HashModel m = new HashModel();
        m.reset(M.readTree("{\"kieu\":\"bam\",\"mo\":true,\"o\":[null,5,null]}"));
        ap(m, "{'t':'set','v':'T','i':2,'x':8}");
        ap(m, "{'t':'set','v':'T','i':1,'x':'XOA'}");
        assertEquals("mở 0:- 1:XOA 2:8", m.describe());
        assertEquals(List.of(new HashModel.Read(0, 0)), ap(m, "{'t':'read','v':'T','i':0}"));
    }
}
