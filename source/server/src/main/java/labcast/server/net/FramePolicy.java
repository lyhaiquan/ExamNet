package labcast.server.net;

import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.MessageType;
import labcast.server.session.Session;

/**
 * Luật chặn thông điệp, chạy trên <b>luồng đọc</b> trước khi frame vào pool nghiệp vụ (spec §17.3).
 * Người 2 giữ giao diện và chỗ gọi; Người 3 cài luật giới hạn tốc độ ở giai đoạn 2.
 *
 * <p>Chạy trên luồng đọc của mọi kết nối cùng lúc: phải nhanh, an toàn đa luồng, không đọc database.
 */
public interface FramePolicy {

    /**
     * @param code null = bỏ frame mà không trả {@code ERROR} (đã báo một lần rồi, báo nữa chỉ làm dày mạng)
     * @param message lời báo cho học viên; null thì {@link Connection} tự ghi "Bị chặn: loại"
     */
    record Decision(boolean allowed, ErrorCode code, int retryAfterMs, String message) {
        public static final Decision ALLOW = new Decision(true, null, 0, null);
        public static final Decision DROP = new Decision(false, null, 0, null);

        public static Decision deny(ErrorCode code, int retryAfterMs) {
            return new Decision(false, code, retryAfterMs, null);
        }

        public static Decision deny(ErrorCode code, int retryAfterMs, String message) {
            return new Decision(false, code, retryAfterMs, message);
        }
    }

    /** {@code s} là null khi kết nối chưa đăng nhập. */
    Decision check(Session s, MessageType t);

    /**
     * Chỗ {@link Connection} gọi. Luật cần đếm theo từng kết nối (kể cả trước khi đăng nhập) thì ghi đè hàm
     * này và giữ số đếm trong {@link Connection#attr(String)}: kết nối đóng thì số đếm mất theo.
     */
    default Decision check(Connection c, MessageType t) {
        return check(c.session(), t);
    }

    FramePolicy ALLOW_ALL = (s, t) -> Decision.ALLOW;
}
