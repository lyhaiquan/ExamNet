package labcast.server.account;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import labcast.common.protocol.Role;
import labcast.server.store.Database;

/** Bảng {@code users}. Mọi câu có dữ liệu người dùng đi qua PreparedStatement. */
public final class UserRepository {
    private static final String COLS = "id, username, full_name, role, pwd_hash, salt, iterations";

    private final Database db;

    public UserRepository(Database db) {
        this.db = db;
    }

    public Optional<User> byUsername(String username) throws SQLException {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLS + " FROM users WHERE username = ?")) {
                ps.setString(1, username);
                return one(ps);
            }
        });
    }

    public Optional<User> byId(long id) throws SQLException {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLS + " FROM users WHERE id = ?")) {
                ps.setLong(1, id);
                return one(ps);
            }
        });
    }

    /** Tạo tài khoản; trả id. Tên đăng nhập đã có thì ném SQLException (ràng buộc UNIQUE). */
    public long create(String username, String fullName, Role role, String password) throws SQLException {
        PasswordHasher.Hashed h = PasswordHasher.hash(password); // băm ngoài khoá database
        return db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO users (username, full_name, role, pwd_hash, salt, iterations, created_at)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, fullName);
                ps.setString(3, role.name());
                ps.setBytes(4, h.hash());
                ps.setBytes(5, h.salt());
                ps.setInt(6, h.iterations());
                ps.setLong(7, System.currentTimeMillis());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    /** Đặt mật khẩu mới (băm lại với salt mới). Trả false nếu không có tài khoản này. */
    public boolean setPassword(long userId, String password) throws SQLException {
        PasswordHasher.Hashed h = PasswordHasher.hash(password); // băm ngoài khoá database
        return db.tx(c -> {
            try (PreparedStatement ps =
                    c.prepareStatement("UPDATE users SET pwd_hash = ?, salt = ?, iterations = ? WHERE id = ?")) {
                ps.setBytes(1, h.hash());
                ps.setBytes(2, h.salt());
                ps.setInt(3, h.iterations());
                ps.setLong(4, userId);
                return ps.executeUpdate() == 1;
            }
        });
    }

    /** Đã có tên đăng nhập này chưa, không phân biệt hoa thường ({@code SV01} và {@code sv01} là một). */
    public boolean existsIgnoreCase(String username) throws SQLException {
        return db.read(c -> {
            try (PreparedStatement ps =
                    c.prepareStatement("SELECT 1 FROM users WHERE lower(username) = lower(?) LIMIT 1")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    public void setFullName(long userId, String fullName) throws SQLException {
        db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE users SET full_name = ? WHERE id = ?")) {
                ps.setString(1, fullName);
                ps.setLong(2, userId);
                return ps.executeUpdate();
            }
        });
    }

    /** Bị quản trị khoá: không đăng nhập được. */
    public boolean isDisabled(long userId) throws SQLException {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT disabled FROM users WHERE id = ?")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) != 0;
                }
            }
        });
    }

    public void setDisabled(long userId, boolean disabled) throws SQLException {
        db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE users SET disabled = ? WHERE id = ?")) {
                ps.setInt(1, disabled ? 1 : 0);
                ps.setLong(2, userId);
                return ps.executeUpdate();
            }
        });
    }

    /** Lần đăng nhập sau phải tự đặt mật khẩu mới không (đang dùng mật khẩu tạm). */
    public boolean mustChangePassword(long userId) throws SQLException {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT must_change_pwd FROM users WHERE id = ?")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) != 0;
                }
            }
        });
    }

    /** Bật / tắt cờ phải đổi mật khẩu; mật khẩu tạm đang giữ (nếu có) bị xoá. Có mật khẩu tạm: {@link #setTemporaryPassword}. */
    public void setMustChangePassword(long userId, boolean must) throws SQLException {
        db.tx(c -> {
            try (PreparedStatement ps =
                    c.prepareStatement("UPDATE users SET must_change_pwd = ?, temp_password = NULL WHERE id = ?")) {
                ps.setInt(1, must ? 1 : 0);
                ps.setLong(2, userId);
                return ps.executeUpdate();
            }
        });
    }

    /**
     * Mật khẩu vừa đặt là mật khẩu tạm (nhập lớp, quản trị tạo, đặt lại): lần đăng nhập sau phải tự đặt mật khẩu
     * riêng; giữ dạng chữ tới lúc đó để quản trị xuất file tài khoản gửi giáo viên.
     */
    public void setTemporaryPassword(long userId, String password) throws SQLException {
        db.tx(c -> {
            try (PreparedStatement ps =
                    c.prepareStatement("UPDATE users SET must_change_pwd = 1, temp_password = ? WHERE id = ?")) {
                ps.setString(1, password);
                ps.setLong(2, userId);
                return ps.executeUpdate();
            }
        });
    }

    /** Mật khẩu tạm đang giữ; {@code null}: người dùng đã tự đặt mật khẩu, hay tạo trước khi app lưu mật khẩu tạm. */
    public String temporaryPassword(long userId) throws SQLException {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT temp_password FROM users WHERE id = ?")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        });
    }

    /** Ghi yêu cầu quên mật khẩu; đã có yêu cầu đang chờ thì giữ nguyên. */
    public void requestPasswordReset(long userId, long now) throws SQLException {
        db.tx(c -> {
            try (PreparedStatement ps =
                    c.prepareStatement("INSERT OR IGNORE INTO password_requests (user_id, created_at) VALUES (?, ?)")) {
                ps.setLong(1, userId);
                ps.setLong(2, now);
                return ps.executeUpdate();
            }
        });
    }

    public void clearPasswordRequest(long userId) throws SQLException {
        db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM password_requests WHERE user_id = ?")) {
                ps.setLong(1, userId);
                return ps.executeUpdate();
            }
        });
    }

    /** Một yêu cầu quên mật khẩu đang chờ. */
    public record PasswordRequest(String username, String fullName, Role role, long at) {}

    /**
     * Yêu cầu đang chờ mà người xem xử lý được: quản trị thấy tất cả (cả giáo viên quên mật khẩu), giáo viên chỉ
     * thấy học viên ghi danh trong lớp mình dạy. Cũ nhất trước.
     */
    public List<PasswordRequest> passwordRequests(long viewerId, boolean admin) throws SQLException {
        return db.read(c -> {
            String sql = "SELECT u.username, u.full_name, u.role, r.created_at FROM password_requests r"
                    + " JOIN users u ON u.id = r.user_id"
                    + (admin
                            ? " WHERE ? > 0"
                            : " WHERE u.role = 'STUDENT' AND EXISTS (SELECT 1 FROM enrollments e JOIN classes c"
                                    + " ON c.id = e.class_id WHERE e.user_id = u.id AND c.teacher_id = ?)")
                    + " ORDER BY r.created_at, u.username";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, admin ? 1 : viewerId);
                List<PasswordRequest> out = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(new PasswordRequest(
                                rs.getString(1), rs.getString(2), Role.valueOf(rs.getString(3)), rs.getLong(4)));
                    }
                }
                return out;
            }
        });
    }

    /** Học viên {@code studentId} có ghi danh vào lớp nào do giáo viên {@code teacherId} dạy không. */
    public boolean isStudentOf(long studentId, long teacherId) throws SQLException {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM enrollments e JOIN classes c"
                    + " ON c.id = e.class_id WHERE e.user_id = ? AND c.teacher_id = ? LIMIT 1")) {
                ps.setLong(1, studentId);
                ps.setLong(2, teacherId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    /** Giáo viên tạo sớm nhất (lớp mặc định thuộc về người này). */
    public Optional<User> firstTeacher() throws SQLException {
        return db.read(c -> {
            try (PreparedStatement ps =
                    c.prepareStatement("SELECT " + COLS + " FROM users WHERE role = 'TEACHER' ORDER BY id LIMIT 1")) {
                return one(ps);
            }
        });
    }

    private static Optional<User> one(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(new User(
                    rs.getLong(1),
                    rs.getString(2),
                    rs.getString(3),
                    Role.valueOf(rs.getString(4)),
                    new PasswordHasher.Hashed(rs.getBytes(5), rs.getBytes(6), rs.getInt(7))));
        }
    }
}
