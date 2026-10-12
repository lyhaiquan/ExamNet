package labcast.client.net;

import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;

/**
 * Gửi {@code APP_FOCUS(bool inApp, i64 clientTs, str lessonId)} mỗi lần học viên rời app hoặc quay lại
 * (theo {@code LessonLock}). Mất kết nối rồi nối lại thì gửi lại trạng thái hiện tại, vì lúc mất kết nối
 * học viên có thể đã quay lại mà server chưa biết. Server bỏ qua tin trùng trạng thái nên gửi thừa
 * không sao.
 */
public final class AppFocusReporter {
    private final LcpClient client;
    private volatile boolean inApp = true;
    private volatile String lessonId = "";
    private volatile boolean wasDisconnected;

    public AppFocusReporter(LcpClient client) {
        this.client = client;
        client.onStateChange(s -> {
            if (s == LcpClient.State.RECONNECTING) {
                wasDisconnected = true;
            } else if (s == LcpClient.State.CONNECTED && wasDisconnected) {
                // LcpClient báo CONNECTED sau khi đã gửi RESUME_REQ trên kết nối mới, nên server gắn
                // phiên xong mới đọc tới tin này
                wasDisconnected = false;
                send();
            }
        });
    }

    /** Học viên vừa rời ({@code false}) hoặc quay lại ({@code true}) app, khi đang mở bài {@code lessonId}. */
    public void changed(boolean inApp, String lessonId) {
        this.inApp = inApp;
        this.lessonId = lessonId == null ? "" : lessonId;
        send();
    }

    public boolean inApp() {
        return inApp;
    }

    private void send() {
        client.send(
                MessageType.APP_FOCUS,
                new PayloadWriter()
                        .bool(inApp)
                        .i64(System.currentTimeMillis())
                        .str(lessonId)
                        .toBytes());
    }
}
