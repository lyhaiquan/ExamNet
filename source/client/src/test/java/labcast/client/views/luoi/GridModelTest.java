package labcast.client.views.luoi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import labcast.client.views.luoi.GridModel.Cell;
import labcast.client.views.luoi.GridModel.Change;
import labcast.client.views.luoi.GridModel.Write;
import labcast.common.trace.TraceEvent;
import org.junit.jupiter.api.Test;

class GridModelTest {
    private static final ObjectMapper M = new ObjectMapper();

    private static TraceEvent ev(String json) throws Exception {
        ObjectNode o = (ObjectNode) M.readTree(json);
        return new TraceEvent(o.get("t").asText(), o);
    }

    private static List<Change> apDung(GridModel m, String... events) throws Exception {
        List<Change> out = new ArrayList<>();
        for (String e : events) {
            out.addAll(m.apply(ev(e)));
        }
        return out;
    }

    private static GridModel bang(int rows, int cols) throws Exception {
        var m = new GridModel();
        StringBuilder sb = new StringBuilder("{\"kieu\":\"luoi\",\"o\":[");
        for (int i = 0; i < rows; i++) {
            sb.append(i == 0 ? "" : ",").append("[");
            for (int j = 0; j < cols; j++) {
                sb.append(j == 0 ? "" : ",").append(0);
            }
            sb.append("]");
        }
        m.reset(M.readTree(sb.append("]}").toString()));
        return m;
    }

    @Test
    void haiODocTruocKhiGhi_laOPhuThuoc() throws Exception {
        var m = bang(2, 3);
        var c = apDung(
                m,
                "{\"t\":\"cell_r\",\"v\":\"dp\",\"i\":1,\"j\":1}",
                "{\"t\":\"cell_r\",\"v\":\"dp\",\"i\":0,\"j\":1}",
                "{\"t\":\"cell_w\",\"v\":\"dp\",\"i\":1,\"j\":2,\"x\":3}");
        assertEquals(new Write(1, 2, "3", List.of(new Cell(1, 1), new Cell(0, 1))), c.get(2));
        assertEquals("3", m.text(1, 2));
        assertTrue(m.written(1, 2));
    }

    @Test
    void ghiKhongCoDocTruoc_khongMuiTen_vaDocCuDaBiXoaSauLanGhi() throws Exception {
        var m = bang(2, 2);
        var c = apDung(
                m,
                "{\"t\":\"cell_w\",\"v\":\"dp\",\"i\":0,\"j\":0,\"x\":1}",
                "{\"t\":\"cell_r\",\"v\":\"dp\",\"i\":0,\"j\":0}",
                "{\"t\":\"cell_w\",\"v\":\"dp\",\"i\":0,\"j\":1,\"x\":1}",
                "{\"t\":\"cell_w\",\"v\":\"dp\",\"i\":1,\"j\":1,\"x\":2}");
        assertEquals(List.of(), ((Write) c.get(0)).deps());
        assertEquals(List.of(new Cell(0, 0)), ((Write) c.get(2)).deps());
        assertEquals(List.of(), ((Write) c.get(3)).deps());
    }

    @Test
    void chinhOD_angGhi_khongTinhLaPhuThuoc_vaGioiHanSoMuiTen() throws Exception {
        var m = bang(1, 10);
        List<String> evs = new ArrayList<>();
        for (int j = 0; j < 10; j++) {
            evs.add("{\"t\":\"cell_r\",\"v\":\"dp\",\"i\":0,\"j\":" + j + "}");
        }
        evs.add("{\"t\":\"cell_w\",\"v\":\"dp\",\"i\":0,\"j\":9,\"x\":5}");
        var c = apDung(m, evs.toArray(new String[0]));
        Write w = (Write) c.get(c.size() - 1);
        assertEquals(GridModel.MAX_DEPS, w.deps().size());
        assertFalse(w.deps().contains(new Cell(0, 9)));
        assertEquals(new Cell(0, 8), w.deps().get(w.deps().size() - 1), "giữ các ô đọc gần nhất");
    }

    @Test
    void anhChup_coNhan_oDaGhi_oDanhDau() throws Exception {
        var m = new GridModel();
        m.reset(M.readTree("{\"kieu\":\"luoi\",\"o\":[[\"#\",\".\"],[1.5,null]],\"hang\":[\"A\",\"B\"],"
                + "\"cot\":[\"x\",\"y\"],\"ghi\":[[1,0]],\"dau\":[[0,1]]}"));
        assertEquals(2, m.rows());
        assertEquals("#", m.text(0, 0));
        assertEquals("1.5", m.text(1, 0));
        assertEquals("None", m.text(1, 1));
        assertEquals("B", m.rowLabel(1));
        assertEquals("y", m.colLabel(1));
        assertTrue(m.written(1, 0));
        assertTrue(m.marked(0, 1));
        m.reset(M.readTree("[[1,2],[3,4]]"));
        assertNull(m.rowLabel(0));
        assertFalse(m.hasLabels());
        assertEquals("4", m.text(1, 1));
    }

    @Test
    void oNgoaiBang_boQua() throws Exception {
        var m = bang(1, 1);
        assertEquals(List.of(), apDung(m, "{\"t\":\"cell_w\",\"v\":\"dp\",\"i\":3,\"j\":0,\"x\":1}"));
        assertEquals(List.of(), apDung(m, "{\"t\":\"mark\",\"v\":\"dp\",\"i\":0,\"j\":4}"));
    }
}
