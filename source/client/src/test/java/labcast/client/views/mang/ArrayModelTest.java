package labcast.client.views.mang;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import labcast.client.views.mang.ArrayModel.Change;
import labcast.client.views.mang.ArrayModel.Read;
import labcast.client.views.mang.ArrayModel.Set;
import labcast.client.views.mang.ArrayModel.Swap;
import labcast.common.trace.TraceEvent;
import org.junit.jupiter.api.Test;

class ArrayModelTest {
    private static final ObjectMapper M = new ObjectMapper();

    private static TraceEvent ev(String json) throws Exception {
        ObjectNode o = (ObjectNode) M.readTree(json);
        return new TraceEvent(o.get("t").asText(), o);
    }

    private static List<Change> apDung(ArrayModel m, String... events) throws Exception {
        List<Change> out = new ArrayList<>();
        for (String e : events) {
            out.addAll(m.apply(ev(e)));
        }
        return out;
    }

    @Test
    void haiSetDoiGiaTri_gopThanhSwap() throws Exception {
        var m = new ArrayModel();
        m.reset(List.of(5, 1, 4));
        var changes = apDung(
                m,
                "{\"t\":\"read\",\"v\":\"a\",\"i\":0}",
                "{\"t\":\"read\",\"v\":\"a\",\"i\":1}",
                "{\"t\":\"set\",\"v\":\"a\",\"i\":0,\"x\":1}",
                "{\"t\":\"set\",\"v\":\"a\",\"i\":1,\"x\":5}");
        assertEquals(List.of(new Read(0), new Read(1), new Swap(0, 1)), changes);
        assertEquals(List.of(1, 5, 4), m.values());
    }

    @Test
    void setDonLe_raSet() throws Exception {
        var m = new ArrayModel();
        m.reset(List.of(5, 1, 4));
        var changes = apDung(m, "{\"t\":\"set\",\"v\":\"a\",\"i\":2,\"x\":9}", "{\"t\":\"read\",\"v\":\"a\",\"i\":0}");
        assertEquals(List.of(new Set(2, 9), new Read(0)), changes);
        assertEquals(List.of(5, 1, 9), m.values());
    }

    @Test
    void setCuoiTrace_flushRaSet() throws Exception {
        var m = new ArrayModel();
        m.reset(List.of(1, 2));
        assertEquals(List.of(), m.apply(ev("{\"t\":\"set\",\"v\":\"a\",\"i\":0,\"x\":7}")));
        assertEquals(List.of(new Set(0, 7)), m.flush());
        assertEquals(List.of(7, 2), m.values());
    }

    @Test
    void haiSetKhongPhaiDoiCho_raHaiSet() throws Exception {
        var m = new ArrayModel();
        m.reset(List.of(5, 1));
        var changes =
                apDung(m, "{\"t\":\"set\",\"v\":\"a\",\"i\":0,\"x\":3}", "{\"t\":\"set\",\"v\":\"a\",\"i\":1,\"x\":4}");
        changes.addAll(m.flush());
        assertEquals(List.of(new Set(0, 3), new Set(1, 4)), changes);
    }

    @Test
    void conTro() throws Exception {
        var m = new ArrayModel();
        m.reset(List.of(1, 2, 3));
        assertEquals(
                List.of(new ArrayModel.Pointer("j", 2)),
                m.apply(ev("{\"t\":\"ptr\",\"v\":\"a\",\"n\":\"j\",\"i\":2}")));
    }
}
