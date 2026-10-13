package labcast.client.views.dothi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import labcast.common.trace.TraceEvent;
import org.junit.jupiter.api.Test;

class GraphModelTest {
    private static final ObjectMapper M = new ObjectMapper();

    private static TraceEvent ev(String json) throws Exception {
        ObjectNode o = (ObjectNode) M.readTree(json.replace('\'', '"'));
        return new TraceEvent(o.get("t").asText(), o);
    }

    private static GraphModel tu(String snap) throws Exception {
        GraphModel m = new GraphModel();
        m.reset(M.readTree(snap.replace('\'', '"')));
        return m;
    }

    @Test
    void themCanhXetCanhDanhDau() throws Exception {
        GraphModel m = tu("{'kieu':'do-thi','co_huong':false,'dinh':[1,2,3],'canh':[]}");
        m.apply(ev("{'t':'edge','v':'g','a':1,'b':2,'x':null}"));
        m.apply(ev("{'t':'edge','v':'g','a':2,'b':3,'x':5}"));
        assertEquals(List.of(new GraphModel.ReadEdge("2", "1")), m.apply(ev("{'t':'read','v':'g','a':2,'b':1}")));
        m.apply(ev("{'t':'mark','v':'g','a':2,'b':1,'m':'xong'}"));
        m.apply(ev("{'t':'mark','v':'g','id':1,'m':'xong'}"));
        m.apply(ev("{'t':'set','v':'g','id':3,'x':'2'}"));
        assertEquals("[1, 2, 3] 1-2:xong 2-3(5) {1=xong} {3=2}", m.describe());
    }

    @Test
    void coHuong_khongNhanCanhNguocChieu() throws Exception {
        GraphModel m = tu("{'kieu':'do-thi','co_huong':true,'dinh':[0,1],'canh':[[0,1,null]]}");
        assertNotNull(m.find("0", "1"));
        assertNull(m.find("1", "0"));
    }

    @Test
    void boTriVongTron_vaChoNguoiSoanDat() throws Exception {
        GraphModel m = tu("{'kieu':'do-thi','dinh':[0,1,2,3],'canh':[],'vt':[[3,0.1,0.2]]}");
        double[] dau = m.layout().get("0");
        assertEquals(0.5, dau[0], 1e-9);
        assertEquals(0.05, dau[1], 1e-9); // đỉnh đầu ở trên cùng
        assertEquals(0.1, m.layout().get("3")[0], 1e-9);
    }

    @Test
    void anhChup_dungLaiDuMauVaNhan() throws Exception {
        GraphModel m = tu("{'kieu':'do-thi','co_huong':false,'dinh':[0,1],'canh':[[0,1,2]],'mau':[[0,'xong']],"
                + "'mau_canh':[[1,0,'loai']],'nhan':[[1,'d=1']]}");
        assertEquals("[0, 1] 0-1(2):loai {0=xong} {1=d=1}", m.describe());
    }
}
