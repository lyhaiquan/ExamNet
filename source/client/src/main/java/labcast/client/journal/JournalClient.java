package labcast.client.journal;

import java.util.ArrayList;
import java.util.List;
import labcast.client.net.LcpClient;
import labcast.common.protocol.CodeDelta;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.ProtocolException;

/**
 * Gửi delta lên server và giữ lại tới khi có {@code CODE_ACK} (ĐG2). Mất mạng rồi nối lại thì gửi lại
 * mọi delta chưa được xác nhận — server bỏ trùng theo {@code seq}, nên gửi thừa không sao.
 *
 * <p>Giai đoạn 1 giữ danh sách chưa ACK trong bộ nhớ; giai đoạn 2 thay bằng nhật ký trên đĩa (ghi trước
 * khi gửi) để tắt app đột ngột cũng không mất.
 */
public final class JournalClient {
    private final LcpClient client;
    private final List<CodeDelta> unacked = new ArrayList<>();
    private volatile boolean wasDisconnected;

    public JournalClient(LcpClient client) {
        this.client = client;
        // CODE_ACK(str lessonId, i64 lastSeq)
        client.on(MessageType.CODE_ACK, f -> {
            try {
                PayloadReader r = f.reader();
                ack(r.str(), r.i64());
            } catch (ProtocolException ignored) {
                // ACK hỏng: lần sau server ACK lại
            }
        });
        client.onStateChange(s -> {
            if (s == LcpClient.State.RECONNECTING) {
                wasDisconnected = true;
            } else if (s == LcpClient.State.CONNECTED && wasDisconnected) {
                wasDisconnected = false;
                resendAll();
            }
        });
    }

    public void submit(CodeDelta d) {
        synchronized (unacked) {
            unacked.add(d);
        }
        client.send(d.toFrame());
    }

    public int unacked() {
        synchronized (unacked) {
            return unacked.size();
        }
    }

    private void ack(String lessonId, long lastSeq) {
        synchronized (unacked) {
            unacked.removeIf(d -> d.lessonId().equals(lessonId) && d.seq() <= lastSeq);
        }
    }

    private void resendAll() {
        List<CodeDelta> copy;
        synchronized (unacked) {
            copy = List.copyOf(unacked);
        }
        for (CodeDelta d : copy) {
            client.send(d.toFrame());
        }
    }
}
