package labcast.common.protocol;

/** Mã lỗi trong thông điệp {@code ERROR(code, message, retryAfterMs)} (spec §10.3). */
public enum ErrorCode {
    BAD_FRAME(1),
    AUTH_REQUIRED(2),
    /** Sai vai trò, hoặc đụng vào dữ liệu của người khác. */
    FORBIDDEN(3),
    /** Bài hoặc bậc chưa mở. */
    LOCKED(4),
    COOLDOWN(5),
    RATE_LIMITED(6),
    NONCE_INVALID(7),
    SQL_NOT_ALLOWED(8),
    SANDBOX_DOWN(9),
    /** Không có bài, mốc lưu, học viên… được hỏi tới. */
    NOT_FOUND(10),
    INTERNAL(99);

    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    /** Mã lạ (app cũ gặp server mới) coi là INTERNAL, không làm hỏng kết nối. */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode c : values()) {
            if (c.code == code) {
                return c;
            }
        }
        return INTERNAL;
    }
}
