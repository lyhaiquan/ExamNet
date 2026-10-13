package labcast.client.views.nganxep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import labcast.client.views.nganxep.StackModel.Change;
import labcast.client.views.nganxep.StackModel.Pop;
import labcast.client.views.nganxep.StackModel.Push;
import labcast.client.views.nganxep.StackModel.Read;
import labcast.common.trace.TraceEvent;
import org.junit.jupiter.api.Test;

class StackModelTest {
    private static final ObjectMapper M = new ObjectMapper();

    private static TraceEvent ev(String json) throws Exception {
        ObjectNode o = (ObjectNode) M.readTree(json);
        return new TraceEvent(o.get("t").asText(), o);
    }

    private static List<Change> apDung(StackModel m, String... events) throws Exception {
        List<Change> out = new ArrayList<>();
        for (String e : events) {
            out.addAll(m.apply(ev(e)));
        }
        return out;
    }

    @Test
    void push1_push2_pop_conLai1_thayDoiCuoiLaPop2() throws Exception {
        var m = new StackModel();
        m.reset(null, false);
        var c = apDung(
                m,
                "{\"t\":\"push\",\"v\":\"st\",\"x\":1}",
                "{\"t\":\"push\",\"v\":\"st\",\"x\":2}",
                "{\"t\":\"pop\",\"v\":\"st\",\"x\":2}");
        assertEquals(List.of("1"), m.items());
        assertEquals(new Push(1, "2"), c.get(1));
        assertEquals(new Pop(1, "2"), c.get(2));
        assertFalse(m.queue());
    }

    @Test
    void popKhiRong_baoTraceSai() throws Exception {
        var m = new StackModel();
        m.reset(null, false);
        assertThrows(IllegalStateException.class, () -> m.apply(ev("{\"t\":\"pop\",\"v\":\"st\",\"x\":1}")));
    }

    @Test
    void deq_layPhanTuDau_vaTuNhanLaHangDoi() throws Exception {
        var m = new StackModel();
        m.reset(null, false);
        var c = apDung(
                m,
                "{\"t\":\"enq\",\"v\":\"q\",\"x\":\"1\"}",
                "{\"t\":\"enq\",\"v\":\"q\",\"x\":\"10\"}",
                "{\"t\":\"deq\",\"v\":\"q\",\"x\":\"1\"}");
        assertTrue(m.queue());
        assertEquals(new Pop(0, "1"), c.get(2));
        assertEquals(List.of("10"), m.items());
    }

    @Test
    void anhChup_tuMoTaKieu_vaGiaTriKhongPhaiSo() throws Exception {
        var m = new StackModel();
        m.reset(M.readTree("{\"kieu\":\"hang-doi\",\"ds\":[[1,2],\"(\",true,null]}"), false);
        assertTrue(m.queue());
        assertEquals(List.of("(1, 2)", "(", "True", "None"), m.items());
        m.reset(M.readTree("[3, 4]"), false);
        assertFalse(m.queue());
        assertEquals(List.of("3", "4"), m.items());
    }

    @Test
    void xemDinh_ngoaiPhamVi_boQua() throws Exception {
        var m = new StackModel();
        m.reset(M.readTree("{\"kieu\":\"ngan-xep\",\"ds\":[7]}"), false);
        assertEquals(List.of(new Read(0)), apDung(m, "{\"t\":\"read\",\"v\":\"st\",\"i\":0}"));
        assertEquals(List.of(), apDung(m, "{\"t\":\"read\",\"v\":\"st\",\"i\":5}"));
    }
}
