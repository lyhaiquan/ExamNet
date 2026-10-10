package labcast.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import labcast.common.protocol.Frame;
import labcast.common.protocol.FrameCodec;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;

/** Client TCP tối giản cho test phía server: gửi frame, đọc frame, có hết giờ. */
public final class TestClient implements AutoCloseable {
    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;
    private int seq;

    private TestClient(Socket socket) throws IOException {
        this.socket = socket;
        socket.setSoTimeout(5_000);
        this.in = new BufferedInputStream(socket.getInputStream());
        this.out = socket.getOutputStream();
    }

    public static TestClient connect(int port) throws IOException {
        return new TestClient(new Socket(InetAddress.getLoopbackAddress(), port));
    }

    public void send(Frame f) throws IOException {
        FrameCodec.write(out, f.seq() == 0 ? f.withSeq(++seq) : f);
        out.flush();
    }

    public void send(MessageType t, byte[] payload) throws IOException {
        send(Frame.of(t, payload));
    }

    public void sendRaw(byte[] bytes) throws IOException {
        out.write(bytes);
        out.flush();
    }

    /** Frame kế tiếp; null nếu server đóng sạch. */
    public Frame read() throws IOException {
        return FrameCodec.read(in);
    }

    /** Bỏ qua các frame khác (ví dụ HEARTBEAT) cho tới khi gặp một trong các loại cần. */
    public Frame readUntil(MessageType... types) throws IOException {
        while (true) {
            Frame f = read();
            if (f == null) {
                throw new IOException("Server đóng kết nối trước khi gửi " + java.util.Arrays.toString(types));
            }
            for (MessageType t : types) {
                if (f.type() == t) {
                    return f;
                }
            }
        }
    }

    public int readRawByte() throws IOException {
        return in.read();
    }

    /** HELLO rồi AUTH; trả frame AUTH_OK hoặc AUTH_FAIL. */
    public Frame login(String user, String pass) throws IOException {
        send(MessageType.HELLO, new PayloadWriter().str("test").str("may-test").toBytes());
        readUntil(MessageType.HELLO_ACK);
        send(MessageType.AUTH, new PayloadWriter().str(user).str(pass).toBytes());
        return readUntil(MessageType.AUTH_OK, MessageType.AUTH_FAIL);
    }

    public Socket socket() {
        return socket;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
