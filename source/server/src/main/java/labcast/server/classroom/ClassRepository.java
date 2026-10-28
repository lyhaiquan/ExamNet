package labcast.server.classroom;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import labcast.server.store.Database;

/** Bảng {@code classes} và {@code enrollments}. Giai đoạn 1 mỗi server một lớp. */
public final class ClassRepository {
    private final Database db;

    public ClassRepository(Database db) {
        this.db = db;
    }

    /** Lớp tên {@code name} của giáo viên; chưa có thì tạo. Trả id lớp. */
    public long ensure(String name, long teacherId) throws SQLException {
        return db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id FROM classes WHERE name = ? AND teacher_id = ? ORDER BY id LIMIT 1")) {
                ps.setString(1, name);
                ps.setLong(2, teacherId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO classes (name, teacher_id, created_at) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setLong(2, teacherId);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public void enroll(long classId, long userId) throws SQLException {
        db.tx(c -> {
            try (PreparedStatement ps =
                    c.prepareStatement("INSERT OR IGNORE INTO enrollments (class_id, user_id) VALUES (?, ?)")) {
                ps.setLong(1, classId);
                ps.setLong(2, userId);
                return ps.executeUpdate();
            }
        });
    }

    /** Chuyển học viên sang đúng một lớp: bỏ mọi lớp đang ghi danh rồi ghi danh lớp mới. */
    public void moveStudent(long userId, long classId) throws SQLException {
        db.tx(c -> {
            try (PreparedStatement del = c.prepareStatement("DELETE FROM enrollments WHERE user_id = ?");
                    PreparedStatement ins =
                            c.prepareStatement("INSERT INTO enrollments (class_id, user_id) VALUES (?, ?)")) {
                del.setLong(1, userId);
                del.executeUpdate();
                ins.setLong(1, classId);
                ins.setLong(2, userId);
                return ins.executeUpdate();
            }
        });
    }

    /** Lớp của một người: giáo viên là lớp mình dạy, học viên là lớp được ghi danh. 0 nếu chưa có. */
    /** Một lớp kèm tên giáo viên phụ trách, để quảng bá khi máy học viên dò lớp. */
    public record Listed(long id, String name, String teacher) {}

    /** Mọi lớp trên server này, theo tên. */
    public java.util.List<Listed> listWithTeachers() throws SQLException {
        return db.read(c -> {
            java.util.List<Listed> out = new java.util.ArrayList<>();
            try (PreparedStatement ps =
                            c.prepareStatement("SELECT c.id, c.name, COALESCE(u.full_name, '') FROM classes c"
                                    + " LEFT JOIN users u ON u.id = c.teacher_id ORDER BY c.name, c.id");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Listed(rs.getLong(1), rs.getString(2), rs.getString(3)));
            }
            return out;
        });
    }

    public long classOf(long userId) throws SQLException {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT class_id FROM enrollments WHERE user_id = ?"
                    + " UNION ALL SELECT id FROM classes WHERE teacher_id = ? LIMIT 1")) {
                ps.setLong(1, userId);
                ps.setLong(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        });
    }
}
