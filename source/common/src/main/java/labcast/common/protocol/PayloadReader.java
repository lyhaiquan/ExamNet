package labcast.common.protocol;

import java.nio.charset.StandardCharsets;

/**
 * Đọc payload nhị phân, đúng thứ tự đã ghi bằng {@link PayloadWriter}. Thiếu byte thì ném {@link
 * ProtocolException} (BAD_FRAME) — payload đến từ mạng, không bao giờ được làm nổ {@code
 * ArrayIndexOutOfBoundsException}.
 */
public final class PayloadReader {
    private final byte[] b;
    private int pos;

    public PayloadReader(byte[] payload) {
        this.b = payload;
    }

    public int u8() throws ProtocolException {
        need(1);
        return b[pos++] & 0xFF;
    }

    public int u16() throws ProtocolException {
        need(2);
        int v = ((b[pos] & 0xFF) << 8) | (b[pos + 1] & 0xFF);
        pos += 2;
        return v;
    }

    public int i32() throws ProtocolException {
        need(4);
        int v = ((b[pos] & 0xFF) << 24)
                | ((b[pos + 1] & 0xFF) << 16)
                | ((b[pos + 2] & 0xFF) << 8)
                | (b[pos + 3] & 0xFF);
        pos += 4;
        return v;
    }

    public long i64() throws ProtocolException {
        long hi = i32();
        long lo = i32() & 0xFFFF_FFFFL;
        return (hi << 32) | lo;
    }

    public boolean bool() throws ProtocolException {
        int v = u8();
        if (v > 1) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "bool phải là 0 hoặc 1: " + v);
        }
        return v == 1;
    }

    public String str() throws ProtocolException {
        int len = u16();
        need(len);
        String s = new String(b, pos, len, StandardCharsets.UTF_8);
        pos += len;
        return s;
    }

    public byte[] blob() throws ProtocolException {
        int len = i32();
        if (len < 0) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "Độ dài blob âm: " + len);
        }
        need(len);
        byte[] out = new byte[len];
        System.arraycopy(b, pos, out, 0, len);
        pos += len;
        return out;
    }

    public boolean hasRemaining() {
        return pos < b.length;
    }

    private void need(int n) throws ProtocolException {
        if (n > b.length - pos) {
            throw new ProtocolException(
                    ErrorCode.BAD_FRAME, "Payload thiếu byte: cần " + n + ", còn " + (b.length - pos));
        }
    }
}
