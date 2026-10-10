package labcast.server.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import labcast.common.protocol.CodeDelta;
import labcast.common.protocol.ProtocolException;
import org.junit.jupiter.api.Test;

class TextOpsTest {

    @Test
    void chenDauGiuaCuoi() {
        assertEquals("xabc", TextOps.apply("abc", 0, 0, "x"));
        assertEquals("axbc", TextOps.apply("abc", 1, 0, "x"));
        assertEquals("abcx", TextOps.apply("abc", 3, 0, "x"));
    }

    @Test
    void xoaVaThay() {
        assertEquals("ac", TextOps.apply("abc", 1, 1, ""));
        assertEquals("aXYc", TextOps.apply("abc", 1, 1, "XY"));
        assertEquals("", TextOps.apply("abc", 0, 3, ""));
    }

    @Test
    void ngoaiPhamVi_nemLoi() {
        assertThrows(IllegalArgumentException.class, () -> TextOps.apply("abc", -1, 0, "x"));
        assertThrows(IllegalArgumentException.class, () -> TextOps.apply("abc", 2, 2, ""));
        assertThrows(IllegalArgumentException.class, () -> TextOps.apply("abc", 4, 0, "x"));
    }

    @Test
    void tiengViet_demTheoChar() {
        String s = "in(\"Xin chào\")";
        int pos = s.indexOf("chào");
        assertEquals("in(\"Xin bạn\")", TextOps.apply(s, pos, 4, "bạn"));
    }

    @Test
    void codeDelta_khuHoiQuaFrame() throws Exception {
        var d = new CodeDelta("dsa.x", 3, 5, 1, "ơi", 123L);
        assertEquals(d, CodeDelta.from(d.toFrame()));
    }

    @Test
    void codeDelta_seqKhongHopLe_biTuChoi() {
        var d = new CodeDelta("dsa.x", 0, 5, 1, "a", 1L);
        assertThrows(ProtocolException.class, () -> CodeDelta.from(d.toFrame()));
    }
}
