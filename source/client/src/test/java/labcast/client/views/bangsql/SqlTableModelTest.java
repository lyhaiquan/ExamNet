package labcast.client.views.bangsql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.List;
import labcast.client.views.bangsql.SqlTableModel.Mark;
import labcast.common.protocol.JsonPayload;
import labcast.common.trace.TraceEvent;
import org.junit.jupiter.api.Test;

class SqlTableModelTest {

    static TraceEvent ev(String json) throws Exception {
        ObjectNode n = (ObjectNode) JsonPayload.MAPPER.readTree(json.replace('\'', '"'));
        return new TraceEvent(n.path("t").asText(), n);
    }

    static SqlTableModel loaded() throws Exception {
        var m = new SqlTableModel();
        m.apply(ev("{'t':'sql.table','v':'sql','name':'sinh_vien','alias':'s','cols':['id','ho_ten','lop_id'],"
                + "'rows':[[1,'An',1],[2,'Bình',1],[3,'Cường',2],[4,'Dung',2],[5,'Em',null]]}"));
        return m;
    }

    @Test
    void keepGiu2Tren5_baDongBiLoai() throws Exception {
        var m = loaded();
        m.apply(ev("{'t':'sql.mark','v':'sql','rows':[0],'ok':[true]}"));
        m.apply(ev("{'t':'sql.mark','v':'sql','rows':[1],'ok':[false]}"));
        assertEquals(List.of(Mark.OK, Mark.BAD, Mark.NONE, Mark.NONE, Mark.NONE), m.marks());
        var ch = m.apply(ev("{'t':'sql.keep','v':'sql','rows':[0,3]}"));
        var rm = assertInstanceOf(SqlTableModel.Remove.class, ch.get(0));
        assertEquals(List.of(1, 2, 4), rm.removed());
        assertEquals(2, m.rows().size());
        assertEquals("Dung", m.rows().get(1).get(1));
        assertEquals(List.of(Mark.NONE, Mark.NONE), m.marks(), "loại xong thì xoá dấu, mệnh đề sau bắt đầu sạch");
    }

    @Test
    void ghepBang_hienBangPhai_noiTungCap_roiThanhMotBang() throws Exception {
        var m = loaded();
        m.apply(ev("{'t':'sql.rename','v':'sql','cols':['s.id','s.ho_ten','s.lop_id']}"));
        m.apply(ev("{'t':'sql.table','v':'sql','name':'lop','alias':'l','side':true,'cols':['id','ten_lop'],"
                + "'rows':[[1,'D22CN01'],[2,'D22CN02']]}"));
        assertEquals("lop l", m.side().title());
        m.apply(ev("{'t':'sql.pair','v':'sql','l':0,'r':0}"));
        m.apply(ev("{'t':'sql.pair','v':'sql','l':2,'r':1}"));
        assertEquals(2, m.pairs().size());
        assertTrue(Arrays.equals(new int[] {2, 1}, m.currentPair()));
        m.apply(ev("{'t':'sql.join','v':'sql','kind':'INNER','pairs':[[0,0],[2,1]],"
                + "'cols':['s.id','s.ho_ten','s.lop_id','l.id','l.ten_lop'],"
                + "'rows':[[1,'An',1,1,'D22CN01'],[3,'Cường',2,2,'D22CN02']]}"));
        assertNull(m.side());
        assertTrue(m.pairs().isEmpty());
        assertEquals(5, m.cols().size());
        assertEquals(2, m.rows().size());
    }

    @Test
    void gomNhom_xepDongCungNhomLienNhau_toMauTheoNhom() throws Exception {
        var m = loaded();
        var ch = m.apply(
                ev("{'t':'sql.group','v':'sql','groups':[[0,1],[2,3],[4]],'keys':[[1],[2],[null]],'by':'lop_id'}"));
        assertEquals(List.of(0, 0, 1, 1, 2), m.groups());
        assertInstanceOf(SqlTableModel.Reorder.class, ch.get(0));
        m = loaded();
        m.apply(ev("{'t':'sql.group','v':'sql','groups':[[1,3],[0,2,4]],'keys':[['a'],['b']],'by':'x'}"));
        assertEquals("Bình", m.rows().get(0).get(1), "nhóm đầu là dòng 1 và 3");
        assertEquals("Dung", m.rows().get(1).get(1));
        assertEquals(List.of(0, 0, 1, 1, 1), m.groups());
    }

    @Test
    void hamGop_moiNhomMotDong_giuMauNhom_havingLoaiNhom() throws Exception {
        var m = loaded();
        m.apply(ev("{'t':'sql.group','v':'sql','groups':[[0,1],[2,3]],'keys':[[1],[2]],'by':'lop_id'}"));
        m.apply(ev("{'t':'sql.agg','v':'sql','cols':['lop_id','so'],'rows':[[1,2],[2,2]]}"));
        assertEquals(List.of(0, 1), m.groups());
        assertEquals(List.of("lop_id", "so"), m.cols());
        m.apply(ev("{'t':'sql.mark','v':'sql','rows':[0,1],'ok':[false,true]}"));
        m.apply(ev("{'t':'sql.keep','v':'sql','rows':[1]}"));
        assertEquals(List.of(1), m.groups(), "nhóm còn lại giữ màu của nó");
    }

    @Test
    void sapXep_boTrung_gioiHan() throws Exception {
        var m = loaded();
        var ch = m.apply(ev("{'t':'sql.order','v':'sql','perm':[4,3,2,1,0]}"));
        assertEquals("Em", m.rows().get(0).get(1));
        var re = assertInstanceOf(SqlTableModel.Reorder.class, ch.get(0));
        assertEquals(List.of(4, 3, 2, 1, 0), re.from(), "dòng mới i đến từ dòng cũ from[i]");
        m.apply(ev("{'t':'sql.distinct','v':'sql','dup':[1]}"));
        assertEquals(4, m.rows().size());
        var lim = m.apply(ev("{'t':'sql.limit','v':'sql','offset':1,'count':2}"));
        assertEquals(List.of(0, 3), ((SqlTableModel.Remove) lim.get(0)).removed());
        assertEquals(2, m.rows().size());
    }

    @Test
    void dml_suaDungO_xoaGachBo_themDongMoi() throws Exception {
        var m = loaded();
        m.apply(ev("{'t':'sql.diff','v':'sql','changes':["
                + "{'kind':'sua','i':0,'cells':[1],'row':[1,'An Nguyễn',1]},"
                + "{'kind':'xoa','i':4},"
                + "{'kind':'them','row':[6,'Giang',3]}]}"));
        assertEquals("An Nguyễn", m.rows().get(0).get(1));
        assertEquals(Mark.CHANGED, m.marks().get(0));
        assertTrue(m.changedCells(0).contains(1));
        assertEquals(Mark.DELETED, m.marks().get(4));
        assertEquals(Mark.NEW, m.marks().get(5));
        assertEquals(6, m.rows().size(), "dòng xoá vẫn hiện (gạch bỏ) để thấy được");
    }

    @Test
    void ketQua_vaApLaiTuDau_raCungTrangThai() throws Exception {
        var m = loaded();
        m.apply(ev("{'t':'sql.keep','v':'sql','rows':[0]}"));
        m.apply(ev("{'t':'sql.result','v':'sql','cols':['ho_ten'],'rows':[['An']]}"));
        assertTrue(m.isResult());
        assertEquals("Kết quả", m.title());
        m.reset();
        assertTrue(m.rows().isEmpty());
        assertNull(m.side());
    }

    @Test
    void bangPhaiCuaPhepTapHop_roiKetQua() throws Exception {
        var m = new SqlTableModel();
        m.apply(ev("{'t':'sql.table','v':'sql','name':'Vế 1','alias':'','cols':['x'],'rows':[[1],[2]]}"));
        m.apply(ev("{'t':'sql.table','v':'sql','name':'Vế 2','alias':'','side':true,'cols':['x'],'rows':[[2],[3]]}"));
        m.apply(ev("{'t':'sql.setop','v':'sql','op':'UNION','cols':['x'],'rows':[[1],[2],[3]]}"));
        assertEquals("UNION", m.title());
        assertNull(m.side());
        assertEquals(3, m.rows().size());
    }
}
