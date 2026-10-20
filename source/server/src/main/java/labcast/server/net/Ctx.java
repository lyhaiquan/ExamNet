package labcast.server.net;

import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import labcast.server.session.Session;

/** Ngữ cảnh của một frame đang xử lý: kết nối gửi nó, phiên, thời điểm nhận. */
public final class Ctx {
    private final Connection connection;
    private final long receivedAt;

    public Ctx(Connection connection, long receivedAt) {
        this.connection = connection;
        this.receivedAt = receivedAt;
    }

    public Connection connection() {
        return connection;
    }

    /** Null trước khi đăng nhập. */
    public Session session() {
        return connection.session();
    }

    public void bindSession(Session s) {
        connection.bindSession(s);
    }

    /**
     * Thời điểm (ms, đồng hồ server) <b>luồng đọc</b> nhận frame — không phải lúc handler chạy. Đồng bộ
     * giờ (t2) và câu hỏi nhanh (arrivedAt) cần mốc này để không tính cả thời gian xếp hàng.
     */
    public long receivedAt() {
        return receivedAt;
    }

    public void reply(MessageType t, byte[] payload) {
        connection.send(Frame.of(t, payload), Priority.NORMAL);
    }

    public void reply(Frame f, Priority p) {
        connection.send(f, p);
    }

    public void error(ErrorCode c, String message, int retryAfterMs) {
        connection.send(new ErrorMessage(c, message, retryAfterMs).toFrame(), Priority.NORMAL);
    }
}
