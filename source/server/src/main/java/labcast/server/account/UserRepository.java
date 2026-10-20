package labcast.server.account;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
