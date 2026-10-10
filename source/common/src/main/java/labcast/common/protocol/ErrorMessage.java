package labcast.common.protocol;

/** Thông điệp {@code ERROR}: {@code u16 code, str message, i32 retryAfterMs}. */
public record ErrorMessage(ErrorCode code, String message, int retryAfterMs) {

    public Frame toFrame() {
        return Frame.of(
                MessageType.ERROR,
                new PayloadWriter()
                        .u16(code.code())
                        .str(message)
                        .i32(retryAfterMs)
                        .toBytes());
    }

    public static ErrorMessage from(Frame f) throws ProtocolException {
        PayloadReader r = f.reader();
        return new ErrorMessage(ErrorCode.fromCode(r.u16()), r.str(), r.i32());
    }

    /** {@code BYE(reason)}. */
    public static Frame bye(ByeReason reason) {
        return Frame.of(MessageType.BYE, new PayloadWriter().str(reason.name()).toBytes());
    }
}
