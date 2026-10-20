package labcast.common.protocol;

import java.io.IOException;

/** Dữ liệu nhận được không đúng LCP/1.0. Luồng đọc gặp lỗi này thì trả ERROR rồi đóng kết nối. */
public final class ProtocolException extends IOException {
    private final ErrorCode code;

    public ProtocolException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
