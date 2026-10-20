package labcast.common.protocol;

/**
 * Một thay đổi văn bản trong ô soạn code (ĐG2): tại {@code pos} xoá {@code delLen} ký tự rồi chèn
 * {@code insText}. Vị trí đếm theo {@code char} của Java ở cả hai phía. {@code seq} tăng 1 mỗi delta
 * của một (học viên, bài), là khoá để server bỏ trùng khi app gửi lại.
 *
 * <p>Payload {@code CODE_DELTA}: {@code str lessonId, i64 seq, i32 pos, i32 delLen, str insText, i64
 * clientTs}.
 */
public record CodeDelta(String lessonId, long seq, int pos, int delLen, String insText, long clientTs) {

    public byte[] toPayload() {
        return new PayloadWriter()
                .str(lessonId)
                .i64(seq)
                .i32(pos)
                .i32(delLen)
                .str(insText)
                .i64(clientTs)
                .toBytes();
    }

    public Frame toFrame() {
        return Frame.of(MessageType.CODE_DELTA, toPayload());
    }

    public static CodeDelta from(Frame f) throws ProtocolException {
        PayloadReader r = f.reader();
        CodeDelta d = new CodeDelta(r.str(), r.i64(), r.i32(), r.i32(), r.str(), r.i64());
        if (d.seq < 1 || d.pos < 0 || d.delLen < 0) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "CODE_DELTA sai: " + d);
        }
        return d;
    }
}
