package labcast.client.ui;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import labcast.client.net.DiscoveryClient;
import labcast.client.net.LcpClient;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;

/** Logic của màn đăng nhập, tách khỏi giao diện: tìm lớp trong LAN, nối, chào, đăng nhập. */
public final class LoginController {

    /** Phiên đã đăng nhập. {@code castId}: để chỉ nhận gói chiếu của server này (0: server không chiếu). */
    public record LoggedIn(
            LcpClient client,
            String token,
            Role role,
            long userId,
            String fullName,
            long classId,
            String className,
            int castId,
            boolean mustChangePassword) {}

    /** Đăng nhập không thành: sai mật khẩu, không nối được server… {@code getMessage()} hiện cho học viên. */
    public static final class LoginFailed extends Exception {
        public LoginFailed(String message) {
            super(message);
        }
    }

    private LoginController() {}

    /** Tìm lớp trong mạng LAN (UDP broadcast tới cổng 7002), gom trả lời trong 1,5 s. */
    public static CompletableFuture<List<DiscoveryClient.ServerInfo>> discover() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return DiscoveryClient.find(DiscoveryClient.DEFAULT_PORT, Duration.ofMillis(1_500));
            } catch (IOException e) {
                return List.of();
            }
        });
    }

    /**
     * Quên mật khẩu: nối, HELLO, gửi yêu cầu cho giáo viên rồi đóng. Trả câu server báo (giống nhau dù tài khoản có
     * hay không).
     */
    public static String forgot(String host, int port, String username) throws LoginFailed {
        try (LcpClient client = new LcpClient()) {
            CompletableFuture<Frame> hello = client.expect(MessageType.HELLO_ACK);
            try {
                client.connect(host, port);
            } catch (IOException e) {
                throw new LoginFailed("Không nối được tới " + host + ":" + port + " (" + e.getMessage() + ")");
            }
            hello.get(5, TimeUnit.SECONDS);
            CompletableFuture<Frame> reply = client.expect(MessageType.ADMIN_OK, MessageType.ERROR);
            client.send(
                    MessageType.PASSWORD_FORGOT,
                    new PayloadWriter().str(username).toBytes());
            Frame f = reply.get(10, TimeUnit.SECONDS);
            if (f.type() == MessageType.ERROR) {
                // server cũ chưa có chức năng này
                throw new LoginFailed(ErrorMessage.from(f).message());
            }
            return f.reader().str();
        } catch (LoginFailed e) {
            throw e;
        } catch (Exception e) {
            throw new LoginFailed("Server không trả lời: " + e.getMessage());
        }
    }

    /** Nối, HELLO, AUTH. Thành công thì client đã nhớ token để tự khôi phục phiên khi mất mạng. */
    public static LoggedIn login(String host, int port, String username, String password) throws LoginFailed {
        LcpClient client = new LcpClient();
        try {
            CompletableFuture<Frame> hello = client.expect(MessageType.HELLO_ACK);
            try {
                client.connect(host, port);
            } catch (IOException e) {
                throw new LoginFailed("Không nối được tới " + host + ":" + port + " (" + e.getMessage() + ")");
            }
            PayloadReader h = hello.get(5, TimeUnit.SECONDS).reader();
            h.i64();
            String className = h.str();
            int castId = h.hasRemaining() ? h.i32() : 0;

            CompletableFuture<Frame> auth =
                    client.expect(MessageType.AUTH_OK, MessageType.AUTH_FAIL, MessageType.ERROR);
            client.send(
                    MessageType.AUTH,
                    new PayloadWriter().str(username).str(password).toBytes());
            Frame f = auth.get(10, TimeUnit.SECONDS);
            if (f.type() == MessageType.AUTH_FAIL) {
                throw new LoginFailed(f.reader().str());
            }
            if (f.type() == MessageType.ERROR) {
                throw new LoginFailed(ErrorMessage.from(f).message());
            }
            PayloadReader r = f.reader();
            String token = r.str();
            Role role = Role.valueOf(r.str());
            long userId = r.i64();
            String fullName = r.str();
            long classId = r.i64();
            // server cũ không gửi cờ này
            boolean mustChange = r.hasRemaining() && r.bool();
            client.setToken(token);
            return new LoggedIn(client, token, role, userId, fullName, classId, className, castId, mustChange);
        } catch (LoginFailed e) {
            client.close();
            throw e;
        } catch (Exception e) {
            client.close();
            throw new LoginFailed("Server không trả lời: " + e.getMessage());
        }
    }
}
