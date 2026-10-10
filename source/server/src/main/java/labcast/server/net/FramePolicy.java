package labcast.server.net;

import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.MessageType;
import labcast.server.session.Session;

/**
 * Luật chặn thông điệp, chạy trên <b>luồng đọc</b> trước khi frame vào pool nghiệp vụ (spec §17.3).
 * Người 2 giữ giao diện và chỗ gọi; Người 3 cài luật giới hạn tốc độ ở giai đoạn 2.
 */
public interface FramePolicy {

    record Decision(boolean allowed, ErrorCode code, int retryAfterMs) {
        public static final Decision ALLOW = new Decision(true, null, 0);

        public static Decision deny(ErrorCode code, int retryAfterMs) {
            return new Decision(false, code, retryAfterMs);
        }
    }

    /** {@code s} là null khi kết nối chưa đăng nhập. */
    Decision check(Session s, MessageType t);

    FramePolicy ALLOW_ALL = (s, t) -> Decision.ALLOW;
}
