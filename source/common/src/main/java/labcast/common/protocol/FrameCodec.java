package labcast.common.protocol;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/** Đọc, ghi frame LCP/1.0 trên luồng TCP và trên datagram UDP. */
public final class FrameCodec {
    private FrameCodec() {}

    /** Ghi một frame. Không tự flush: luồng ghi flush khi hàng đợi rỗng để gộp nhiều frame. */
    public static void write(OutputStream out, Frame f) throws IOException {
        out.write(header(f));
        out.write(f.payload());
    }

    /**
     * Đọc một frame. Trả {@code null} khi bên kia đóng sạch ở ranh giới frame. TCP được phép trả về
     * từng mẩu nhỏ, nên header và payload đều đọc lặp cho đủ.
     *
     * @throws ProtocolException MAGIC, VER, TYPE hay LEN sai
     * @throws EOFException kết nối đóng giữa chừng một frame
     */
    public static Frame read(InputStream in) throws IOException {
        byte[] h = new byte[Frame.HEADER_SIZE];
        int first = in.read();
        if (first == -1) {
            return null;
        }
        h[0] = (byte) first;
        readFully(in, h, 1, h.length - 1);
        Header info = parseHeader(h, 0);
        byte[] payload = new byte[info.len];
        readFully(in, payload, 0, info.len);
        return new Frame(info.type, info.flags, info.seq, payload);
    }

    /** Cả frame thành một mảng byte, cho datagram UDP. */
    public static byte[] toBytes(Frame f) {
        byte[] p = f.payload();
        return ByteBuffer.allocate(Frame.HEADER_SIZE + p.length)
                .put(header(f))
                .put(p)
                .array();
    }

    /** Đọc frame từ một datagram. Datagram phải chứa đúng một frame đầy đủ. */
    public static Frame fromBytes(byte[] buf, int off, int len) throws ProtocolException {
        if (len < Frame.HEADER_SIZE) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "Datagram ngắn hơn header: " + len + " byte");
        }
        Header info = parseHeader(buf, off);
        if (Frame.HEADER_SIZE + info.len != len) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "LEN " + info.len + " không khớp độ dài datagram " + len);
        }
        byte[] payload = new byte[info.len];
        System.arraycopy(buf, off + Frame.HEADER_SIZE, payload, 0, info.len);
        return new Frame(info.type, info.flags, info.seq, payload);
    }

    private static byte[] header(Frame f) {
        return ByteBuffer.allocate(Frame.HEADER_SIZE)
                .put(Frame.MAGIC_0)
                .put(Frame.MAGIC_1)
                .put(Frame.VERSION)
                .put((byte) f.type().code())
                .put(f.flags())
                .putInt(f.seq())
                .putInt(f.payload().length)
                .array();
    }

    private record Header(MessageType type, byte flags, int seq, int len) {}

    private static Header parseHeader(byte[] h, int off) throws ProtocolException {
        if (h[off] != Frame.MAGIC_0 || h[off + 1] != Frame.MAGIC_1) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "Sai MAGIC");
        }
        if (h[off + 2] != Frame.VERSION) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "Sai VER " + h[off + 2]);
        }
        MessageType type = MessageType.fromCode(h[off + 3] & 0xFF);
        ByteBuffer b = ByteBuffer.wrap(h, off + 5, 8);
        int seq = b.getInt();
        int len = b.getInt();
        if (len < 0 || len > Frame.MAX_PAYLOAD) { // kiểm TRƯỚC khi new byte[len]
            throw new ProtocolException(ErrorCode.BAD_FRAME, "LEN không hợp lệ: " + len);
        }
        return new Header(type, h[off + 4], seq, len);
    }

    private static void readFully(InputStream in, byte[] b, int off, int len) throws IOException {
        while (len > 0) {
            int n = in.read(b, off, len);
            if (n < 0) {
                throw new EOFException("Kết nối đóng giữa frame");
            }
            off += n;
            len -= n;
        }
    }
}
