package labcast.server.session;

import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.function.LongUnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import labcast.common.protocol.ByeReason;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Role;
import labcast.server.account.User;
import labcast.server.net.Connection;
import labcast.server.store.AuditLog;
import labcast.server.store.Database;

/**
 * Giữ các phiên đăng nhập. Mỗi tài khoản một phiên: đăng nhập ở máy thứ hai thì máy thứ nhất nhận
 * {@code BYE(LOGGED_IN_ELSEWHERE)} — mỗi bài chỉ có một nơi ghi code (spec §12).
 *
 * <p>Token lưu cả trong bảng {@code sessions}, nên server khởi động lại thì app vẫn {@code RESUME}
 * được bằng token cũ.
 */
public final class SessionManager {
    private static final Logger LOG = Logger.getLogger(SessionManager.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Database db;
    private final AuditLog audit;
    private final LongSupplier clock;
    private final Map<String, Session> byToken = new ConcurrentHashMap<>();
    private final Map<Long, Session> byUser = new ConcurrentHashMap<>();
    private volatile CodeSeqLookup codeSeqLookup = CodeSeqLookup.NONE;
    private volatile LongUnaryOperator classLookup = u -> 0;

    public SessionManager(Database db, AuditLog audit, LongSupplier clock) {
        this.db = db;
        this.audit = audit;
        this.clock = clock;
    }

    /** Đăng nhập thành công: tạo phiên mới, đóng phiên cũ của cùng tài khoản nếu có. */
    public synchronized Session login(User u, long classId, Connection c, String device) throws SQLException {
        Session old = byUser.remove(u.id());
        if (old != null) {
            byToken.remove(old.token());
            Connection oc = old.connection();
            if (oc != null && oc != c) {
                oc.sendAndClose(ErrorMessage.bye(ByeReason.LOGGED_IN_ELSEWHERE));
                audit.record(u.username(), "LOGGED_IN_ELSEWHERE", "phiên cũ ở " + oc.remoteAddress());
            }
        }
        String token = newToken();
        long now = clock.getAsLong();
        db.tx(conn -> {
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM sessions WHERE user_id = ?");
                    PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO sessions (token, user_id, device, created_at, last_seen) VALUES (?, ?, ?, ?, ?)")) {
                del.setLong(1, u.id());
                del.executeUpdate();
                ins.setString(1, token);
                ins.setLong(2, u.id());
                ins.setString(3, device);
                ins.setLong(4, now);
                ins.setLong(5, now);
                return ins.executeUpdate();
            }
        });
        Session s = new Session(u.id(), u.username(), u.fullName(), u.role(), token, classId);
        s.attach(c);
        byToken.put(token, s);
        byUser.put(u.id(), s);
        return s;
    }

    /** Gắn kết nối mới vào phiên có sẵn. Token lạ hoặc đã bị thay bởi lần đăng nhập khác → rỗng. */
    public synchronized Optional<Session> resume(String token, Connection c) throws SQLException {
        Session s = byToken.get(token);
        if (s == null) {
            s = loadFromDb(token).orElse(null);
            if (s == null) {
                return Optional.empty();
            }
            byToken.put(token, s);
            byUser.put(s.userId(), s);
        }
        Connection old = s.attach(c);
        if (old != null) {
            old.close(); // kết nối cũ của chính máy này, thường đã chết mà chưa ai biết
        }
        touch(token);
        return Optional.of(s);
    }

    public Optional<Session> byUser(long userId) {
        return Optional.ofNullable(byUser.get(userId));
    }

    /**
     * Đóng phiên của một người (trang quản trị xoá tài khoản, đổi mật khẩu, đổi lớp). Token cũ hết hiệu lực
     * nên app không {@code RESUME} được mà phải đăng nhập lại, lúc đó nhận lớp mới.
     */
    public synchronized boolean kick(long userId) throws SQLException {
        Session s = byUser.remove(userId);
        if (s != null) {
            byToken.remove(s.token());
        }
        db.tx(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sessions WHERE user_id = ?")) {
                ps.setLong(1, userId);
                return ps.executeUpdate();
            }
        });
        Connection c = s == null ? null : s.connection();
        if (c != null) {
            c.close();
        }
        return s != null;
    }

    public Collection<Session> all() {
        return List.copyOf(byUser.values());
    }

    /** Kết nối đóng: gỡ khỏi phiên nhưng GIỮ phiên để chờ {@code RESUME}. */
    public void onDisconnect(Connection c) {
        Session s = c.session();
        if (s != null) {
            s.detach(c);
        }
    }

    /** Người 1 gắn ở giai đoạn 2 để {@code RESUME_STATE} trả đúng seq code trên server. */
    public void setCodeSeqLookup(CodeSeqLookup l) {
        this.codeSeqLookup = l == null ? CodeSeqLookup.NONE : l;
    }

    public CodeSeqLookup codeSeqLookup() {
        return codeSeqLookup;
    }

    /** Lớp của một người, dùng khi dựng lại phiên từ database. */
    public void setClassLookup(LongUnaryOperator l) {
        this.classLookup = l;
    }

    private Optional<Session> loadFromDb(String token) throws SQLException {
        return db.read(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT u.id, u.username, u.full_name, u.role"
                    + " FROM sessions s JOIN users u ON u.id = s.user_id WHERE s.token = ?")) {
                ps.setString(1, token);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    long userId = rs.getLong(1);
                    return Optional.of(new Session(
                            userId,
                            rs.getString(2),
                            rs.getString(3),
                            Role.valueOf(rs.getString(4)),
                            token,
                            classLookup.applyAsLong(userId)));
                }
            }
        });
    }

    private void touch(String token) {
        try {
            db.read(conn -> {
                try (PreparedStatement ps =
                        conn.prepareStatement("UPDATE sessions SET last_seen = ? WHERE token = ?")) {
                    ps.setLong(1, clock.getAsLong());
                    ps.setString(2, token);
                    return ps.executeUpdate();
                }
            });
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Không cập nhật được last_seen", e);
        }
    }

    private static String newToken() {
        byte[] b = new byte[16];
        RANDOM.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }
}
