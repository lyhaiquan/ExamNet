package labcast.common.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PayloadTest {

    @Test
    void ghiRoiDoc_dungThuTuVaGiaTri() throws Exception {
        byte[] b = new PayloadWriter()
                .u8(200)
                .u16(65_000)
                .i32(-5)
                .i64(1L << 40)
                .bool(true)
                .str("Lê Hải")
                .blob(new byte[] {1, 2})
                .toBytes();
        var r = new PayloadReader(b);
        assertEquals(200, r.u8());
        assertEquals(65_000, r.u16());
        assertEquals(-5, r.i32());
        assertEquals(1L << 40, r.i64());
        assertTrue(r.bool());
        assertEquals("Lê Hải", r.str());
        assertArrayEquals(new byte[] {1, 2}, r.blob());
        assertFalse(r.hasRemaining());
    }

    @Test
    void i64Am_khuHoiDung() throws Exception {
        var r = new PayloadReader(
                new PayloadWriter().i64(-1).i64(Long.MIN_VALUE).toBytes());
        assertEquals(-1, r.i64());
        assertEquals(Long.MIN_VALUE, r.i64());
    }

    @Test
    void thieuByte_nemProtocolException() {
        var r = new PayloadReader(new byte[] {0, 5, 'a'}); // khai báo chuỗi 5 byte nhưng chỉ có 1
        assertThrows(ProtocolException.class, r::str);
        assertThrows(ProtocolException.class, () -> new PayloadReader(new byte[3]).i32());
        assertThrows(ProtocolException.class, () -> new PayloadReader(new byte[] {-1, -1, -1, -1}).blob());
    }

    @Test
    void chuoiQuaDai_bichanKhiGhi() {
        assertThrows(IllegalArgumentException.class, () -> new PayloadWriter().str("x".repeat(70_000)));
    }

    @Test
    void json_khuHoi() throws Exception {
        record Bai(String id, int so) {}
        Frame f = JsonPayload.frame(MessageType.LESSON_DATA, new Bai("dsa.x", 3));
        assertTrue(f.has(Frame.FLAG_JSON));
        assertEquals(new Bai("dsa.x", 3), JsonPayload.read(f, Bai.class));
        assertEquals("dsa.x", JsonPayload.tree(f).get("id").asText());
    }

    @Test
    void jsonHong_nemProtocolException() {
        var f = new Frame(MessageType.LESSON_DATA, Frame.FLAG_JSON, 0, new byte[] {'{', 'x'});
        assertThrows(ProtocolException.class, () -> JsonPayload.tree(f));
        assertThrows(ProtocolException.class, () -> JsonPayload.tree(Frame.of(MessageType.LESSON_DATA, new byte[0])));
    }

    @Test
    void frameReader_docPayload() throws Exception {
        Frame f = Frame.of(
                MessageType.AUTH, new PayloadWriter().str("gv").str("mk").toBytes());
        var r = f.reader();
        assertEquals("gv", r.str());
        assertEquals("mk", r.str());
    }
}
