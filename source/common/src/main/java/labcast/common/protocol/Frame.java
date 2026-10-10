package labcast.common.protocol;

/**
 * Một thông điệp LCP/1.0 (spec §10.1).
 *
 * <pre>
 * | MAGIC 'L' 'C' | VER 1 | TYPE 1 | FLAGS 1 | SEQ 4 (BE) | LEN 4 (BE) | PAYLOAD LEN byte |
 * </pre>
 */
public record Frame(MessageType type, byte flags, int seq, byte[] payload) {
    public static final byte MAGIC_0 = 'L';
    public static final byte MAGIC_1 = 'C';
    public static final byte VERSION = 1;
    public static final int HEADER_SIZE = 13;
    /** Trần payload; kiểm TRƯỚC khi cấp phát mảng để một LEN giả không làm tràn bộ nhớ. */
    public static final int MAX_PAYLOAD = 8 * 1024 * 1024;

    public static final byte FLAG_COMPRESSED = 1;
    public static final byte FLAG_JSON = 2;
    public static final byte FLAG_REQUIRES_ACK = 4;
    /** Gói chiếu đi đường TCP thay cho multicast (máy không nhận được multicast). */
    public static final byte FLAG_VIA_TCP = 8;

    public Frame {
        if (type == null) {
            throw new IllegalArgumentException("type null");
        }
        if (payload == null) {
            payload = new byte[0];
        }
        if (payload.length > MAX_PAYLOAD) {
            throw new IllegalArgumentException("Payload quá " + MAX_PAYLOAD + " byte: " + payload.length);
        }
    }

    /** Frame không cờ, SEQ 0 — kết nối gán SEQ khi gửi. */
    public static Frame of(MessageType type, byte[] payload) {
        return new Frame(type, (byte) 0, 0, payload);
    }

    public boolean has(byte flag) {
        return (flags & flag) != 0;
    }

    /** Bộ đọc payload nhị phân, đọc từ đầu. */
    public PayloadReader reader() {
        return new PayloadReader(payload);
    }

    public Frame withSeq(int seq) {
        return new Frame(type, flags, seq, payload);
    }

    @Override
    public String toString() {
        return type + "(flags=" + flags + ", seq=" + seq + ", " + payload.length + " byte)";
    }
}
