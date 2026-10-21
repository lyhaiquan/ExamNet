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

    /** Một dòng trong danh sách người dùng của trang quản trị (không có mật khẩu băm). */
    public record Row(long id, String username, String fullName, Role role, long createdAt) {}

    /** Mọi người dùng có vai trò {@code role} ({@code null}: mọi vai trò), theo tên đăng nhập. */
    public List<Row> list(Role role) throws SQLException {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, username, full_name, role, created_at FROM users WHERE ? IS NULL OR role = ?"
                            + " ORDER BY username")) {
                ps.setString(1, role == null ? null : role.name());
                ps.setString(2, role == null ? null : role.name());
                List<Row> out = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(new Row(
                                rs.getLong(1),
                                rs.getString(2),
                                rs.getString(3),
                                Role.valueOf(rs.getString(4)),
                                rs.getLong(5)));
                    }
                }
                return out;
            }
        });
    }

    /** Đổi tên đăng nhập và họ tên. Tên đăng nhập trùng người khác thì ném SQLException. */
    public void update(long id, String username, String fullName) throws SQLException {
        db.tx(c -> {
            try (PreparedStatement ps =
                    c.prepareStatement("UPDATE users SET username = ?, full_name = ? WHERE id = ?")) {
                ps.setString(1, username);
                ps.setString(2, fullName);
                ps.setLong(3, id);
                return ps.executeUpdate();
            }
        });
    }

    public void setPassword(long id, String password) throws SQLException {
        PasswordHasher.Hashed h = PasswordHasher.hash(password);
        db.tx(c -> {
            try (PreparedStatement ps =
                    c.prepareStatement("UPDATE users SET pwd_hash = ?, salt = ?, iterations = ? WHERE id = ?")) {
                ps.setBytes(1, h.hash());
                ps.setBytes(2, h.salt());
                ps.setInt(3, h.iterations());
                ps.setLong(4, id);
                return ps.executeUpdate();
            }
        });
    }

    /**
     * Xoá tài khoản cùng phiên đăng nhập và các dòng xếp lớp. Người đã có bài làm, code, câu trả lời…
     * thì ném SQLException (khoá ngoại) và không xoá gì. Lớp do người này tạo chuyển tên người tạo sang
     * một giáo viên khác.
     */
    public void delete(long id) throws SQLException {
        db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE classes SET teacher_id ="
                    + " (SELECT u.id FROM users u WHERE u.role = 'TEACHER' AND u.id <> ? ORDER BY u.id LIMIT 1)"
                    + " WHERE teacher_id = ? AND EXISTS"
                    + " (SELECT 1 FROM users u WHERE u.role = 'TEACHER' AND u.id <> ?)")) {
                ps.setLong(1, id);
                ps.setLong(2, id);
                ps.setLong(3, id);
                ps.executeUpdate();
            }
            for (String sql : List.of(
                    "DELETE FROM sessions WHERE user_id = ?",
                    "DELETE FROM enrollments WHERE user_id = ?",
                    "DELETE FROM class_teachers WHERE user_id = ?",
                    "DELETE FROM assignment_starts WHERE user_id = ?",
                    "DELETE FROM users WHERE id = ?")) {
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }
            }
            return null;
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
