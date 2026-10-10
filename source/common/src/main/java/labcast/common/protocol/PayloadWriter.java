package labcast.common.protocol;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Ghi payload nhị phân theo ký hiệu ở docs/plans/README.md: số big-endian, {@code str} là
 * {@code [u16 độ dài][UTF-8]}, {@code blob} là {@code [i32 độ dài][byte]}.
 */
public final class PayloadWriter {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream(64);

    public PayloadWriter u8(int v) {
        if (v < 0 || v > 0xFF) {
            throw new IllegalArgumentException("u8 ngoài phạm vi: " + v);
        }
        out.write(v);
        return this;
    }

    public PayloadWriter u16(int v) {
        if (v < 0 || v > 0xFFFF) {
            throw new IllegalArgumentException("u16 ngoài phạm vi: " + v);
        }
        out.write(v >>> 8);
        out.write(v);
        return this;
    }

    public PayloadWriter i32(int v) {
        out.write(v >>> 24);
        out.write(v >>> 16);
        out.write(v >>> 8);
        out.write(v);
        return this;
    }

    public PayloadWriter i64(long v) {
        i32((int) (v >>> 32));
        return i32((int) v);
    }

    public PayloadWriter bool(boolean v) {
        out.write(v ? 1 : 0);
        return this;
    }

    /** Chuỗi UTF-8 tối đa 65 535 byte; dài hơn thì dùng {@link #blob} hoặc JSON. */
    public PayloadWriter str(String s) {
        byte[] b = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        if (b.length > 0xFFFF) {
            throw new IllegalArgumentException("Chuỗi quá 65 535 byte: " + b.length);
        }
        u16(b.length);
        out.writeBytes(b);
        return this;
    }

    public PayloadWriter blob(byte[] b) {
        i32(b.length);
        out.writeBytes(b);
        return this;
    }

    public byte[] toBytes() {
        return out.toByteArray();
    }
}
