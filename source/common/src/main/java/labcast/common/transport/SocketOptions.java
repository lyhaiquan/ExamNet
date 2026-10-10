package labcast.common.transport;

import java.net.Socket;
import java.net.SocketException;

/** Tuỳ chọn socket dùng chung cho server và app. */
public final class SocketOptions {
    /** Không nhận byte nào trong khoảng này thì coi kết nối đã chết (heartbeat 2 s nên không oan). */
    public static final int READ_TIMEOUT_MS = 15_000;

    private SocketOptions() {}

    public static void applyTcp(Socket s) throws SocketException {
        s.setTcpNoDelay(true); // frame nhỏ (heartbeat, delta) phải đi ngay, không chờ gộp Nagle
        s.setKeepAlive(true);
        s.setSoTimeout(READ_TIMEOUT_MS);
    }
}
