package labcast.server.classroom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import labcast.server.store.Database;

/**
 * Bảng {@code classes}, {@code enrollments}, {@code class_teachers}. Đợt 2: một server nhiều lớp (DSA1,
 * DSA2…), trang quản trị tạo lớp và xếp người vào lớp.
 *
 * <p>Giáo viên phụ trách lớp chỉ lấy từ {@code class_teachers}; {@code classes.teacher_id} là người tạo lớp
 * (cột NOT NULL có từ giai đoạn 1).
 */
public final class ClassRepository implements ClassDirectory {
    private static final String SI_SO = "(SELECT count(*) FROM enrollments e WHERE e.class_id = c.id)";

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
                long id;
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    id = rs.getLong(1);
                }
                exec(c, "INSERT OR IGNORE INTO class_teachers (class_id, user_id) VALUES (?, ?)", id, teacherId);
                return id;
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

    /** Lớp của một người: giáo viên là lớp mình dạy, học viên là lớp được ghi danh. 0 nếu chưa có. */
    public long classOf(long userId) throws SQLException {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT class_id FROM enrollments WHERE user_id = ?"
                    + " UNION ALL SELECT class_id FROM class_teachers WHERE user_id = ? LIMIT 1")) {
                ps.setLong(1, userId);
                ps.setLong(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        });
    }

    // ---------------------------------------------------------------- ClassDirectory

    @Override
    public List<ClassInfo> all() {
        return infos("SELECT c.id, c.name, " + SI_SO + " FROM classes c ORDER BY c.name, c.id");
    }

    @Override
    public Optional<ClassInfo> find(long classId) {
        return infos("SELECT c.id, c.name, " + SI_SO + " FROM classes c WHERE c.id = ?", classId).stream()
                .findFirst();
    }

    @Override
    public List<ClassInfo> classesOf(long userId) {
        return infos(
                "SELECT c.id, c.name, " + SI_SO + " FROM classes c WHERE c.id IN ("
                        + "SELECT class_id FROM enrollments WHERE user_id = ?"
                        + " UNION SELECT class_id FROM class_teachers WHERE user_id = ?) ORDER BY c.name, c.id",
                userId,
                userId);
    }

    @Override
    public List<Long> studentsOf(long classId) {
        return ids("SELECT user_id FROM enrollments WHERE class_id = ? ORDER BY user_id", classId);
    }

    @Override
    public List<Long> teachersOf(long classId) {
        return ids("SELECT user_id FROM class_teachers WHERE class_id = ? ORDER BY user_id", classId);
    }

    // ---------------------------------------------------------------- quản trị

    /** Lớp trùng tên (không phân biệt hoa thường), trừ lớp {@code exceptId}. */
    public boolean nameTaken(String name, long exceptId) throws SQLException {
        return db.read(c -> {
            try (PreparedStatement ps =
                    c.prepareStatement("SELECT 1 FROM classes WHERE lower(name) = lower(?) AND id <> ? LIMIT 1")) {
                ps.setString(1, name);
                ps.setLong(2, exceptId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    /** Tạo lớp. {@code creatorId} ghi vào {@code classes.teacher_id}; {@code teacherIds} có thể rỗng. */
    public long create(String name, long creatorId, List<Long> teacherIds) throws SQLException {
        return db.tx(c -> {
            long id;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO classes (name, teacher_id, created_at) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setLong(2, creatorId);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    id = rs.getLong(1);
                }
            }
            writeTeachers(c, id, teacherIds);
            return id;
        });
    }

    /** Đổi tên và thay toàn bộ giáo viên phụ trách ({@code teacherIds} có thể rỗng). */
    public void update(long classId, String name, List<Long> teacherIds) throws SQLException {
        db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE classes SET name = ? WHERE id = ?")) {
                ps.setString(1, name);
                ps.setLong(2, classId);
                ps.executeUpdate();
            }
            writeTeachers(c, classId, teacherIds);
            return null;
        });
    }

    /**
     * Xoá lớp cùng danh sách học viên, giáo viên, lịch và bài giao của lớp. Lớp đã có câu hỏi nhanh thì
     * ném SQLException (khoá ngoại), không xoá gì.
     */
    public void delete(long classId) throws SQLException {
        db.tx(c -> {
            exec(
                    c,
                    "DELETE FROM assignment_starts WHERE assignment_id IN"
                            + " (SELECT id FROM assignments WHERE class_id = ?)",
                    classId);
            exec(c, "DELETE FROM assignments WHERE class_id = ?", classId);
            exec(c, "DELETE FROM class_schedule WHERE class_id = ?", classId);
            exec(c, "DELETE FROM class_teachers WHERE class_id = ?", classId);
            exec(c, "DELETE FROM enrollments WHERE class_id = ?", classId);
            exec(c, "DELETE FROM classes WHERE id = ?", classId);
            return null;
        });
    }

    /** Thay toàn bộ lớp học viên này được ghi danh. */
    public void setStudentClasses(long userId, List<Long> classIds) throws SQLException {
        db.tx(c -> {
            exec(c, "DELETE FROM enrollments WHERE user_id = ?", userId);
            for (long id : new LinkedHashSet<>(classIds)) {
                exec(c, "INSERT INTO enrollments (class_id, user_id) VALUES (?, ?)", id, userId);
            }
            return null;
        });
    }

    /** Thay toàn bộ lớp giáo viên này phụ trách. */
    public void setTeacherClasses(long userId, List<Long> classIds) throws SQLException {
        db.tx(c -> {
            exec(c, "DELETE FROM class_teachers WHERE user_id = ?", userId);
            for (long id : new LinkedHashSet<>(classIds)) {
                exec(c, "INSERT OR IGNORE INTO class_teachers (class_id, user_id) VALUES (?, ?)", id, userId);
            }
            return null;
        });
    }

    private static void writeTeachers(Connection c, long classId, List<Long> teacherIds) throws SQLException {
        exec(c, "DELETE FROM class_teachers WHERE class_id = ?", classId);
        for (long t : new LinkedHashSet<>(teacherIds)) {
            exec(c, "INSERT INTO class_teachers (class_id, user_id) VALUES (?, ?)", classId, t);
        }
    }

    private static int exec(Connection c, String sql, long... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setLong(i + 1, args[i]);
            }
            return ps.executeUpdate();
        }
    }

    private List<ClassInfo> infos(String sql, long... args) {
        try {
            return db.read(c -> {
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    for (int i = 0; i < args.length; i++) {
                        ps.setLong(i + 1, args[i]);
                    }
                    List<ClassInfo> out = new ArrayList<>();
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            out.add(new ClassInfo(rs.getLong(1), rs.getString(2), rs.getInt(3)));
                        }
                    }
                    return out;
                }
            });
        } catch (SQLException e) {
            throw new IllegalStateException("Không đọc được bảng lớp", e);
        }
    }

    private List<Long> ids(String sql, long... args) {
        try {
            return db.read(c -> {
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    for (int i = 0; i < args.length; i++) {
                        ps.setLong(i + 1, args[i]);
                    }
                    List<Long> out = new ArrayList<>();
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            out.add(rs.getLong(1));
                        }
                    }
                    return out;
                }
            });
        } catch (SQLException e) {
            throw new IllegalStateException("Không đọc được bảng lớp", e);
        }
    }
}
