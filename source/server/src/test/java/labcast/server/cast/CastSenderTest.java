package labcast.server.cast;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import labcast.common.cast.CastMessages;
import labcast.common.protocol.Frame;
import labcast.common.protocol.FrameCodec;
import labcast.common.protocol.MessageType;
import org.junit.jupiter.api.Test;

class CastSenderTest {

    /** Ghi lại mọi datagram thay vì gửi ra mạng. */
    static final class Captured implements CastSender.Sink {
        final List<byte[]> datagrams = new ArrayList<>();

        @Override
        public synchronized void send(byte[] d) {
            datagrams.add(d);
        }

        synchronized List<Frame> frames() throws Exception {
            List<Frame> out = new ArrayList<>();
            for (byte[] d : datagrams) {
                out.add(FrameCodec.fromBytes(d, 0, d.length));
            }
            return out;
        }

        @Override
        public void close() {}
    }

    static CastSender sender(Captured c) {
        return new CastSender(c, new Pacer(Long.MAX_VALUE / 4, Long.MAX_VALUE / 4, System::nanoTime, n -> {}));
    }

    /** Khoảng 200 KB trace, chữ ngẫu nhiên nên nén không nhỏ đi bao nhiêu. */
    static List<String> bigTrace() {
        Random r = new Random(1);
        List<String> lines = new ArrayList<>();
        lines.add("{\"t\":\"hdr\",\"lesson\":\"x\",\"views\":[{\"v\":\"a\",\"kieu\":\"mang\"}]}");
        StringBuilder sb = new StringBuilder();
        while (sb.length() < 200_000) {
            String s = Long.toString(r.nextLong(), 36);
            sb.append(s);
            lines.add("{\"t\":\"say\",\"s\":\"" + s + "\"}");
        }
        lines.add("{\"t\":\"end\",\"truncated\":false}");
        return lines;
    }

    @Test
    void trace200KB_chiaDungSoKhuc_moiDatagramToiDa1400Byte_seqLienTiep() throws Exception {
        var cap = new Captured();
        CastSender s = sender(cap);
        CastState.Loaded l = CastState.prepare("dsa.sap-xep.noi-bot", bigTrace());
        assertTrue(l.totalChunks() > 50, "trace lớn phải ra nhiều khúc: " + l.totalChunks());
        for (int i = 0; i < l.totalChunks(); i++) {
            s.publish(MessageType.CAST_DATA, l.data(42, i).encode());
        }
        List<Frame> frames = cap.frames();
        assertEquals(l.totalChunks(), frames.size());
        int longest = cap.datagrams.stream().mapToInt(d -> d.length).max().orElseThrow();
        assertTrue(longest <= CastMessages.MAX_DATAGRAM, "datagram dài nhất " + longest);
        for (int i = 0; i < frames.size(); i++) {
            assertEquals(i + 1, frames.get(i).seq(), "SEQ bắt đầu từ 1 và liên tiếp");
            assertEquals(i, CastMessages.Data.decode(frames.get(i).payload()).chunkIndex());
        }
        assertEquals(frames.size(), s.lastSeq());
    }

    @Test
    void ghepLaiCacKhuc_raDungTraceBanDau() throws Exception {
        List<String> lines = bigTrace();
        CastState.Loaded l = CastState.prepare("x", lines);
        var all = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < l.totalChunks(); i++) {
            all.writeBytes(l.data(1, i).bytes());
        }
        assertEquals(lines, CastMessages.decompress(all.toByteArray()));
    }

    @Test
    void goiVuaGui_layLaiDuocDungByte() throws Exception {
        var cap = new Captured();
        CastSender s = sender(cap);
        Frame f = s.publish(MessageType.CAST_KEYFRAME, new byte[] {1, 2, 3});
        assertArrayEquals(cap.datagrams.get(0), s.recent(f.seq()).orElseThrow());
        assertTrue(s.recent(f.seq() + 1).isEmpty());
    }

    @Test
    void datagramQua1400Byte_biTuChoi_khongTieuSeq() {
        var cap = new Captured();
        CastSender s = sender(cap);
        assertThrows(IllegalArgumentException.class, () -> s.publish(MessageType.CAST_DATA, new byte[1400]));
        assertEquals(0, s.lastSeq());
        assertTrue(cap.datagrams.isEmpty());
    }
}
