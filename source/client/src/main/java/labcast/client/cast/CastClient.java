package labcast.client.cast;

import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import labcast.client.clock.Clock;
import labcast.client.net.LcpClient;
import labcast.common.cast.CastMessages;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.ProtocolException;

/**
 * Nối phần nhận chiếu vào kết nối của app: nghe multicast trên card của kết nối TCP, nhận gói sửa và
 * gói chuyển đường qua TCP, gửi {@code CAST_NACK}, báo {@code lastCastSeq} theo heartbeat. Mọi việc với
 * {@link CastSession} chạy trên {@code ui} (luồng giao diện trong app).
 */
public final class CastClient implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(CastClient.class.getName());

    private final LcpClient client;
    private final CastSession session;
    private final CastReceiver receiver;
    private final AtomicLong nacks = new AtomicLong();

    /** {@code castId}: số server báo trong {@code HELLO_ACK} lúc đăng nhập, để bỏ gói chiếu của lớp khác. */
    public CastClient(LcpClient client, int castId, Clock clock, Executor ui) {
        this(client, castId, clock, ui, CastReceiver.DEFAULT_GROUP);
    }

    public CastClient(LcpClient client, int castId, Clock clock, Executor ui, InetSocketAddress group) {
        this.client = client;
        this.session = new CastSession(clock, this::sendNack);
        this.receiver =
                new CastReceiver(nics(client), group, f -> ui.execute(() -> session.onFrame(f)), this::sendNack);
        receiver.expectCastId(castId);
    }

    /** Bắt đầu nghe; trả số card đã tham gia nhóm (0 thì vẫn nhận được qua TCP). */
    public int start() {
        for (MessageType t : List.of(
                MessageType.CAST_CONTROL, MessageType.CAST_DATA, MessageType.CAST_KEYFRAME, MessageType.CAST_REPAIR)) {
            client.on(t, receiver::offerTcp);
        }
        client.heartbeat().addField("lastCastSeq", receiver::lastSeq);
        // Nối lại sau khi server khởi động lại: HELLO_ACK mới mang castId mới
        client.on(MessageType.HELLO_ACK, f -> {
            try {
                PayloadReader r = f.reader();
                r.i64();
                r.str();
                receiver.expectCastId(r.hasRemaining() ? r.i32() : 0);
            } catch (ProtocolException e) {
                LOG.fine("HELLO_ACK hỏng: " + e.getMessage());
            }
        });
        return receiver.start();
    }

    public CastSession session() {
        return session;
    }

    public CastReceiver receiver() {
        return receiver;
    }

    /** Số {@code CAST_NACK} đã gửi: cả gói thiếu do {@link CastReceiver} thấy lẫn khúc thiếu do {@link CastSession} xin. */
    public long nacksSent() {
        return nacks.get();
    }

    private void sendNack(CastMessages.Nack n) {
        nacks.incrementAndGet();
        client.send(MessageType.CAST_NACK, n.encode());
    }

    private static List<NetworkInterface> nics(LcpClient client) {
        try {
            return LocalNic.forLocalAddress(client.localAddress());
        } catch (SocketException e) {
            LOG.warning("Không liệt kê được card mạng: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public void close() {
        receiver.close();
    }
}
