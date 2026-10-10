package labcast.server.discovery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import labcast.common.protocol.Frame;
import labcast.common.protocol.FrameCodec;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.ProtocolException;

/**
 * Trả lời {@code DISCOVER} gửi broadcast tới cổng UDP 7002, để app học viên tự thấy lớp mà không phải
 * gõ IP. Datagram rác thì bỏ qua — cổng UDP mở ra mạng LAN, ai cũng gửi được.
 */
public final class DiscoveryResponder implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(DiscoveryResponder.class.getName());

    private final DatagramSocket socket;
    private final Supplier<byte[]> replyPayload;
    private volatile boolean closed;

    /** {@code port} 0 khi test. {@code replyPayload} dựng payload {@code DISCOVER_REPLY} mỗi lần trả lời. */
    public DiscoveryResponder(int port, Supplier<byte[]> replyPayload) throws SocketException {
        this.socket = new DatagramSocket(null);
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(port));
        this.replyPayload = replyPayload;
    }

    public void start() {
        Thread t = new Thread(this::loop, "tim-server-" + port());
        t.setDaemon(true);
        t.start();
    }

    public int port() {
        return socket.getLocalPort();
    }

    private void loop() {
        byte[] buf = new byte[1500];
        while (!closed) {
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(p);
            } catch (IOException e) {
                if (!closed) {
                    LOG.log(Level.WARNING, "Nhận DISCOVER lỗi", e);
                }
                continue;
            }
            try {
                Frame f = FrameCodec.fromBytes(p.getData(), p.getOffset(), p.getLength());
                if (f.type() != MessageType.DISCOVER) {
                    continue;
                }
                byte[] out = FrameCodec.toBytes(Frame.of(MessageType.DISCOVER_REPLY, replyPayload.get()));
                socket.send(new DatagramPacket(out, out.length, p.getSocketAddress()));
            } catch (ProtocolException e) {
                // datagram rác: bỏ qua
            } catch (IOException | RuntimeException e) {
                LOG.log(Level.WARNING, "Trả lời DISCOVER lỗi", e);
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        socket.close();
    }
}
