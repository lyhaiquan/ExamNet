package labcast.common.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class FrameCodecTest {

    private static Frame mau() {
        return new Frame(MessageType.HELLO, Frame.FLAG_JSON, 42, "xin chào".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void ghiRoiDoc_raDungFrame() throws Exception {
        var out = new ByteArrayOutputStream();
        FrameCodec.write(out, mau());
        Frame f = FrameCodec.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(MessageType.HELLO, f.type());
        assertEquals(42, f.seq());
        assertTrue(f.has(Frame.FLAG_JSON));
        assertArrayEquals(mau().payload(), f.payload());
    }

    @Test
    void headerBatDauBangLC() {
        byte[] bytes = FrameCodec.toBytes(mau());
        assertEquals(0x4C, bytes[0]);
        assertEquals(0x43, bytes[1]);
        assertEquals(Frame.HEADER_SIZE + mau().payload().length, bytes.length);
    }

    /** TCP được phép trả về từng byte một — codec phải đọc lặp cho đủ. */
    @Test
    void docTungByteMot_vanRaDungFrame() throws Exception {
        byte[] bytes = FrameCodec.toBytes(mau());
        InputStream nhoGiot = new InputStream() {
            int i = 0;

            @Override
            public int read() {
                return i < bytes.length ? bytes[i++] & 0xFF : -1;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (i >= bytes.length) {
                    return -1;
                }
                b[off] = bytes[i++];
                return 1;
            }
        };
        assertArrayEquals(mau().payload(), FrameCodec.read(nhoGiot).payload());
    }

    @Test
    void haiFrameDinhLien_docDuocCaHai() throws Exception {
        var out = new ByteArrayOutputStream();
        FrameCodec.write(out, mau());
        FrameCodec.write(out, Frame.of(MessageType.BYE, new byte[0]));
        var in = new ByteArrayInputStream(out.toByteArray());
        assertEquals(MessageType.HELLO, FrameCodec.read(in).type());
        assertEquals(MessageType.BYE, FrameCodec.read(in).type());
        assertNull(FrameCodec.read(in));
    }

    @Test
    void saiMagic_nemProtocolException() {
        byte[] bytes = FrameCodec.toBytes(mau());
        bytes[0] = 'G'; // ví dụ ai đó gửi "GET / HTTP/1.1"
        var ex = assertThrows(ProtocolException.class, () -> FrameCodec.read(new ByteArrayInputStream(bytes)));
        assertEquals(ErrorCode.BAD_FRAME, ex.code());
    }

    @Test
    void saiVersion_hoacTypeLa_nemProtocolException() {
        byte[] v = FrameCodec.toBytes(mau());
        v[2] = 9;
        assertThrows(ProtocolException.class, () -> FrameCodec.read(new ByteArrayInputStream(v)));
        byte[] t = FrameCodec.toBytes(mau());
        t[3] = (byte) 0xFE;
        assertThrows(ProtocolException.class, () -> FrameCodec.read(new ByteArrayInputStream(t)));
    }

    /** LEN khổng lồ phải bị từ chối TRƯỚC khi cấp phát mảng. */
    @Test
    void lenVuotTran_nemTruocKhiCapPhat() {
        byte[] header = FrameCodec.toBytes(Frame.of(MessageType.HELLO, new byte[0]));
        ByteBuffer.wrap(header).putInt(9, Integer.MAX_VALUE);
        assertThrows(ProtocolException.class, () -> FrameCodec.read(new ByteArrayInputStream(header)));
        ByteBuffer.wrap(header).putInt(9, -1);
        assertThrows(ProtocolException.class, () -> FrameCodec.read(new ByteArrayInputStream(header)));
    }

    @Test
    void dutGiuaFrame_nemEOFException() {
        byte[] bytes = FrameCodec.toBytes(mau());
        byte[] cut = Arrays.copyOf(bytes, bytes.length - 3);
        assertThrows(EOFException.class, () -> FrameCodec.read(new ByteArrayInputStream(cut)));
        byte[] cutHeader = Arrays.copyOf(bytes, 5);
        assertThrows(EOFException.class, () -> FrameCodec.read(new ByteArrayInputStream(cutHeader)));
    }

    @Test
    void fromBytes_choDatagramUdp() throws Exception {
        byte[] bytes = FrameCodec.toBytes(mau());
        assertEquals(42, FrameCodec.fromBytes(bytes, 0, bytes.length).seq());
        assertThrows(ProtocolException.class, () -> FrameCodec.fromBytes(bytes, 0, 5));
        assertThrows(ProtocolException.class, () -> FrameCodec.fromBytes(bytes, 0, bytes.length - 1));
    }

    @Test
    void fromBytes_coDoLech() throws Exception {
        byte[] bytes = FrameCodec.toBytes(mau());
        byte[] buf = new byte[bytes.length + 7];
        System.arraycopy(bytes, 0, buf, 7, bytes.length);
        assertArrayEquals(
                mau().payload(), FrameCodec.fromBytes(buf, 7, bytes.length).payload());
    }
}
