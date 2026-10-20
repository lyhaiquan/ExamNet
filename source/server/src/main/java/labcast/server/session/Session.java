package labcast.server.session;

import labcast.common.protocol.Frame;
import labcast.common.protocol.Role;
import labcast.server.net.Connection;
import labcast.server.net.Priority;

/**
 * Một phiên đăng nhập. Phiên sống lâu hơn kết nối: mất mạng thì kết nối chết nhưng phiên còn, app
 * nối lại bằng {@code RESUME_REQ(token)} để gắn kết nối mới vào đúng phiên cũ.
 */
public final class Session {
    private final long userId;
    private final String username;
    private final String fullName;
    private final Role role;
    private final String token;
    private final long classId;
    private volatile Connection connection;

    public Session(long userId, String username, String fullName, Role role, String token, long classId) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.token = token;
        this.classId = classId;
    }

    public long userId() {
        return userId;
    }

    public String username() {
        return username;
    }

    public String fullName() {
        return fullName;
    }

    public Role role() {
        return role;
    }

    public String token() {
        return token;
    }

    public long classId() {
        return classId;
    }

    /** Kết nối đang gắn, null khi mất mạng và chưa nối lại. */
    public Connection connection() {
        return connection;
    }

    public boolean online() {
        Connection c = connection;
        return c != null && !c.isClosed();
    }

    /** Gắn kết nối mới; trả kết nối cũ (nếu có) để bên gọi đóng nó. */
    public synchronized Connection attach(Connection c) {
        Connection old = connection;
        connection = c;
        c.bindSession(this);
        return old == c ? null : old;
    }

    /** Gỡ kết nối {@code c} nếu nó đang gắn; kết nối khác thì không đụng tới. */
    public synchronized boolean detach(Connection c) {
        if (connection == c) {
            connection = null;
            return true;
        }
        return false;
    }

    /** Gửi tới máy đang gắn; trả false khi mất mạng hoặc hàng đợi từ chối. */
    public boolean send(Frame f, Priority p) {
        return send(f, p, null);
    }

    public boolean send(Frame f, Priority p, String conflateKey) {
        Connection c = connection;
        return c != null && c.send(f, p, conflateKey);
    }

    @Override
    public String toString() {
        return "Session(" + username + ", " + role + ")";
    }
}
