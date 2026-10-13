package labcast.common.cast;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import labcast.common.protocol.Frame;
import labcast.common.protocol.FrameCodec;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.ProtocolException;
import org.junit.jupiter.api.Test;

class CastMessagesTest {

    @Test
    void dieuKhien_ghiRoiDoc_nhuCu() throws Exception {
        var c = new CastMessages.Control(7, "PLAY", "dsa.sap-xep.noi-bot", 37, 2000, 123_456_789L, true);
        assertEquals(c, CastMessages.Control.decode(c.encode()));
        assertEquals(7, CastMessages.castId(Frame.of(MessageType.CAST_CONTROL, c.encode())));
    }

    @Test
    void yeuCauCuaGiaoVien_ghiRoiDoc_nhuCu() throws Exception {
        var r = new CastMessages.Request("SEEK", "dsa.sap-xep.noi-bot", 12, 1000);
        assertEquals(r, CastMessages.Request.decode(r.encode()));
    }

    @Test
    void khungKhoa_vaNack_ghiRoiDoc_nhuCu() throws Exception {
        var k = new CastMessages.Keyframe(7, "x", 5, 101, -1, false, 500, 99L, 140);
        assertEquals(k, CastMessages.Keyframe.decode(k.encode()));
        var n = new CastMessages.Nack(7, 3, 9);
        assertEquals(n, CastMessages.Nack.decode(n.encode()));
    }

    @Test
    void khucDuLieu_giuNguyenByte() throws Exception {
        byte[] bytes = {1, 2, 3, -4};
        var d = CastMessages.Data.decode(new CastMessages.Data(7, "x", 2, 5, bytes).encode());
        assertEquals(2, d.chunkIndex());
        assertEquals(5, d.totalChunks());
        assertArrayEquals(bytes, d.bytes());
    }

    @Test
    void khucDayNhat_vuaDung1400Byte_keCaTraceIdDai() {
        String id = "x".repeat(CastMessages.MAX_TRACE_ID_BYTES);
        int n = CastMessages.chunkSize(id);
        byte[] dg = FrameCodec.toBytes(new Frame(
                MessageType.CAST_DATA, (byte) 0, 1, new CastMessages.Data(1, id, 0, 1, new byte[n]).encode()));
        assertEquals(CastMessages.MAX_DATAGRAM, dg.length);
        assertTrue(CastMessages.chunkSize("dsa.sap-xep.noi-bot") <= 1400);
    }

    @Test
    void nenRoiGiaiNen_giuNguyenCacDong() throws Exception {
        var lines = java.util.List.of("{\"t\":\"say\",\"s\":\"5 > 1 nên đổi chỗ\"}", "", "{\"t\":\"end\"}");
        assertEquals(lines, CastMessages.decompress(CastMessages.compress(lines)));
    }

    @Test
    void boomNen_dungLaiOTran() {
        byte[] bomb = CastMessages.compress(java.util.List.of("0".repeat(CastMessages.MAX_TRACE_BYTES + 10)));
        assertTrue(bomb.length < 20_000, "4 MB số 0 nén còn vài KB");
        assertThrows(ProtocolException.class, () -> CastMessages.decompress(bomb));
        assertThrows(
                ProtocolException.class, () -> CastMessages.decompress(java.util.Arrays.copyOf(bomb, bomb.length / 2)));
    }

    @Test
    void traceIdQuaDai_biTuChoi() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CastMessages.chunkSize("ơ".repeat(CastMessages.MAX_TRACE_ID_BYTES)));
    }

    @Test
    void nackQuaRong_biTuChoi() {
        byte[] p = new CastMessages.Nack(1, 1, 1 + CastMessages.MAX_NACK_RANGE).encode();
        assertThrows(ProtocolException.class, () -> CastMessages.Nack.decode(p));
        byte[] nguoc = new CastMessages.Nack(1, 9, 3).encode();
        assertThrows(ProtocolException.class, () -> CastMessages.Nack.decode(nguoc));
    }
}
