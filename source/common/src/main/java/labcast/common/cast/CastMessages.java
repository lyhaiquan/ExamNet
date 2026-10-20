package labcast.common.cast;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.Frame;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.ProtocolException;
import labcast.common.trace.Trace;

/**
 * Payload các thông điệp chiếu (ĐG1, spec §10, kế hoạch giai đoạn 2 task 4.4). Server và app cùng
 * dùng lớp này nên hai bên không thể lệch thứ tự trường.
 *
 * <p>Mọi gói chiếu server phát (điều khiển, khúc dữ liệu, khung khoá) bắt đầu bằng {@code i32 castId}
 * — số ngẫu nhiên mỗi lần server khởi động, để app bỏ gói của lần chạy trước.
 */
public final class CastMessages {
    /** Datagram multicast không vượt cỡ này, để không bị phân mảnh IP (spec §10.2). */
    public static final int MAX_DATAGRAM = 1400;
    /** traceId dài nhất (UTF-8): đủ cho id bài, giữ khúc dữ liệu đủ lớn. */
    public static final int MAX_TRACE_ID_BYTES = 200;
    /** Trace sau giải nén không quá cỡ này (trần trace là 2 MB, spec §7.3; để dư gấp đôi). */
    public static final int MAX_TRACE_BYTES = 4 * 1024 * 1024;
    /** Một {@code CAST_NACK} xin tối đa ngần này gói. */
    public static final int MAX_NACK_RANGE = 256;
    /** Phần cố định của payload {@code CAST_DATA} ngoài traceId và khúc: castId, độ dài chuỗi, chỉ số, tổng, độ dài blob. */
    private static final int DATA_OVERHEAD = 4 + 2 + 4 + 4 + 4;

    public static final String LOAD = "LOAD";
    public static final String PLAY = "PLAY";
    public static final String PAUSE = "PAUSE";
    public static final String SEEK = "SEEK";
    public static final String SPEED = "SPEED";
    public static final String STOP = "STOP";

    private CastMessages() {}

    /** Số byte trace tối đa trong một khúc {@code CAST_DATA} để cả datagram vừa {@link #MAX_DATAGRAM}. */
    public static int chunkSize(String traceId) {
        int n = traceId.getBytes(StandardCharsets.UTF_8).length;
        if (n > MAX_TRACE_ID_BYTES) {
            throw new IllegalArgumentException("traceId quá " + MAX_TRACE_ID_BYTES + " byte: " + n);
        }
        return MAX_DATAGRAM - Frame.HEADER_SIZE - DATA_OVERHEAD - n;
    }

    /** Các dòng trace (JSON mỗi dòng) nối bằng {@code \n}, nén Deflate — phần thân chia khúc của {@code CAST_DATA}. */
    public static byte[] compress(List<String> lines) {
        byte[] raw = String.join("\n", lines).getBytes(StandardCharsets.UTF_8);
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            d.setInput(raw);
            d.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length / 4 + 64);
            byte[] buf = new byte[8192];
            while (!d.finished()) {
                out.write(buf, 0, d.deflate(buf));
            }
            return out.toByteArray();
        } finally {
            d.end();
        }
    }

    /**
     * Ngược của {@link #compress}. Dừng ở {@link #MAX_TRACE_BYTES} byte sau giải nén: vài KB dữ liệu nén
     * có thể bung ra hàng GB.
     */
    public static List<String> decompress(byte[] data) throws ProtocolException {
        Inflater inf = new Inflater();
        try {
            inf.setInput(data);
            ByteArrayOutputStream out = new ByteArrayOutputStream(data.length * 4);
            byte[] buf = new byte[8192];
            while (!inf.finished()) {
                int n = inf.inflate(buf);
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) {
                    throw new ProtocolException(ErrorCode.BAD_FRAME, "Dữ liệu trace chiếu bị cụt");
                }
                out.write(buf, 0, n);
                if (out.size() > MAX_TRACE_BYTES) {
                    throw new ProtocolException(ErrorCode.BAD_FRAME, "Trace chiếu vượt " + MAX_TRACE_BYTES + " byte");
                }
            }
            String s = out.toString(StandardCharsets.UTF_8);
            return s.isEmpty() ? List.of() : List.of(s.split("\n", -1));
        } catch (DataFormatException e) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "Dữ liệu trace chiếu hỏng: " + e.getMessage());
        } finally {
            inf.end();
        }
    }

    /**
     * Vị trí của buổi chiếu ở thời điểm {@code t} (đồng hồ server): từ {@code anchorAt} ở bước {@code
     * step}, nếu đang chạy thì tiến một bước mỗi {@code STEP_MS / tốc độ}, không vượt sự kiện cuối. Server
     * và mọi app dùng đúng hàm này, tính bằng số nguyên, nên cùng một thời điểm ra cùng một bước.
     */
    public static int positionAt(long t, int step, boolean playing, int speedMilli, long anchorAt, int size) {
        if (!playing || t <= anchorAt) {
            return Math.min(step, size - 1);
        }
        long advance = (t - anchorAt) * speedMilli / (1000L * Trace.STEP_MS);
        return (int) Math.min(size - 1L, step + advance);
    }

    /** castId ở đầu payload của mọi gói chiếu server phát. */
    public static int castId(Frame f) throws ProtocolException {
        return f.reader().i32();
    }

    /** Giáo viên → server, qua TCP: {@code str action, str traceId, i32 step, i32 speedMilli} (1000 = 1×). */
    public record Request(String action, String traceId, int step, int speedMilli) {
        public byte[] encode() {
            return new PayloadWriter()
                    .str(action)
                    .str(traceId)
                    .i32(step)
                    .i32(speedMilli)
                    .toBytes();
        }

        public static Request decode(byte[] p) throws ProtocolException {
            PayloadReader r = new PayloadReader(p);
            return new Request(r.str(), r.str(), r.i32(), r.i32());
        }
    }

    /**
     * Server → lớp: {@code i32 castId, str action, str traceId, i32 step, i32 speedMilli, i64 at, bool
     * playing}. Từ thời điểm {@code at} (đồng hồ server) mọi máy ở bước {@code step}; nếu {@code playing}
     * thì từ đó tiến đều theo tốc độ. Mỗi gói mang đủ trạng thái, nên lỡ một gói không làm lệch các gói sau.
     */
    public record Control(
            int castId, String action, String traceId, int step, int speedMilli, long at, boolean playing) {
        public byte[] encode() {
            return new PayloadWriter()
                    .i32(castId)
                    .str(action)
                    .str(traceId)
                    .i32(step)
                    .i32(speedMilli)
                    .i64(at)
                    .bool(playing)
                    .toBytes();
        }

        public static Control decode(byte[] p) throws ProtocolException {
            PayloadReader r = new PayloadReader(p);
            return new Control(r.i32(), r.str(), r.str(), r.i32(), r.i32(), r.i64(), r.bool());
        }
    }

    /**
     * Một khúc của trace đã nén Deflate: {@code i32 castId, str traceId, i32 chunkIndex, i32 totalChunks,
     * blob bytes}. Khúc {@code i} có {@code SEQ = firstDataSeq + i}.
     */
    public record Data(int castId, String traceId, int chunkIndex, int totalChunks, byte[] bytes) {
        public byte[] encode() {
            return new PayloadWriter()
                    .i32(castId)
                    .str(traceId)
                    .i32(chunkIndex)
                    .i32(totalChunks)
                    .blob(bytes)
                    .toBytes();
        }

        public static Data decode(byte[] p) throws ProtocolException {
            PayloadReader r = new PayloadReader(p);
            Data d = new Data(r.i32(), r.str(), r.i32(), r.i32(), r.blob());
            if (d.totalChunks <= 0 || d.chunkIndex < 0 || d.chunkIndex >= d.totalChunks) {
                throw new ProtocolException(
                        ErrorCode.BAD_FRAME, "Khúc " + d.chunkIndex + "/" + d.totalChunks + " không hợp lệ");
            }
            return d;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Data d
                    && castId == d.castId
                    && traceId.equals(d.traceId)
                    && chunkIndex == d.chunkIndex
                    && totalChunks == d.totalChunks
                    && Arrays.equals(bytes, d.bytes);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * traceId.hashCode() + chunkIndex) + Arrays.hashCode(bytes);
        }

        @Override
        public String toString() {
            return "Data[" + traceId + " " + chunkIndex + "/" + totalChunks + ", " + bytes.length + " byte]";
        }
    }

    /**
     * Khung khoá, phát mỗi 2 s khi đang chiếu: {@code i32 castId, str traceId, i32 totalChunks, i32
     * firstDataSeq, i32 step, bool playing, i32 speedMilli, i64 anchorAt, i32 lastSeq}. Máy vào muộn nhờ
     * nó biết phải xin khúc nào; {@code lastSeq} là SEQ gói cuối phát trước khung này, để máy lỡ gói cuối
     * cũng biết mình thiếu.
     */
    public record Keyframe(
            int castId,
            String traceId,
            int totalChunks,
            int firstDataSeq,
            int step,
            boolean playing,
            int speedMilli,
            long anchorAt,
            int lastSeq) {
        public byte[] encode() {
            return new PayloadWriter()
                    .i32(castId)
                    .str(traceId)
                    .i32(totalChunks)
                    .i32(firstDataSeq)
                    .i32(step)
                    .bool(playing)
                    .i32(speedMilli)
                    .i64(anchorAt)
                    .i32(lastSeq)
                    .toBytes();
        }

        public static Keyframe decode(byte[] p) throws ProtocolException {
            PayloadReader r = new PayloadReader(p);
            return new Keyframe(r.i32(), r.str(), r.i32(), r.i32(), r.i32(), r.bool(), r.i32(), r.i64(), r.i32());
        }
    }

    /** App → server, qua TCP: {@code i32 castId, i32 fromSeq, i32 toSeq}, tối đa {@link #MAX_NACK_RANGE} gói. */
    public record Nack(int castId, int from, int to) {
        public byte[] encode() {
            return new PayloadWriter().i32(castId).i32(from).i32(to).toBytes();
        }

        public static Nack decode(byte[] p) throws ProtocolException {
            PayloadReader r = new PayloadReader(p);
            Nack n = new Nack(r.i32(), r.i32(), r.i32());
            if (n.from <= 0 || n.to < n.from || n.to - n.from >= MAX_NACK_RANGE) {
                throw new ProtocolException(ErrorCode.BAD_FRAME, "CAST_NACK sai dải " + n.from + ".." + n.to);
            }
            return n;
        }
    }
}
