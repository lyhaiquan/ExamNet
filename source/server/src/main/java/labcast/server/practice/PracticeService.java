package labcast.server.practice;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import labcast.server.store.Database;

/**
 * Nguồn sự thật cho một đợt thực hành có các bài được giáo viên chọn và một cửa
 * sổ thời gian.
 */
public final class PracticeService {
    public record StudentProgress(String fullName, String username, int completed, int total, boolean active,
            Set<String> completedLessons) {
    }

    public record Session(long id, long classId, String name, long startsAt, long closesAt, List<String> lessons) {
        public boolean open(long now) {
            return now >= startsAt && now < closesAt;
        }

        public boolean visible(long now) {
            return now >= startsAt;
        }
    }

    private final Database db;
    private final Map<Long, Long> activeStudents = new ConcurrentHashMap<>();
    private static final long ACTIVE_WINDOW_MS = 6_000;

    public PracticeService(Database db) {
        this.db = db;
    }

    public long create(long teacherId, long classId, String name, List<String> lessonIds, long startsAt,
            long durationMs,
            long now)
            throws SQLException {
        if (classId <= 0) {
            throw new IllegalArgumentException("Phải chọn lớp thực hành");
        }
        if (name == null || name.isBlank() || name.length() > 120) {
            throw new IllegalArgumentException("Tên đợt thực hành không hợp lệ");
        }
        if (lessonIds == null || lessonIds.isEmpty() || lessonIds.stream().distinct().count() != lessonIds.size()) {
            throw new IllegalArgumentException("Đợt thực hành phải có ít nhất một bài khác nhau");
        }
        if (durationMs < 60_000 || durationMs > 24L * 60L * 60L * 1000L) {
            throw new IllegalArgumentException("Thời gian thực hành phải từ 1 phút đến 24 giờ");
        }
        if (startsAt < now - 60_000) {
            throw new IllegalArgumentException("Giờ bắt đầu đã ở trong quá khứ");
        }
        return db.tx(c -> {
            try (PreparedStatement owner = c.prepareStatement(
                    "SELECT 1 FROM classes WHERE id = ? AND teacher_id = ?")) {
                owner.setLong(1, classId);
                owner.setLong(2, teacherId);
                try (ResultSet rs = owner.executeQuery()) {
                    if (!rs.next())
                        throw new IllegalArgumentException("Lớp thực hành không thuộc giáo viên");
                }
            }
            long id;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO practice_sessions(teacher_id, class_id, name, starts_at, closes_at, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, teacherId);
                ps.setLong(2, classId);
                ps.setString(3, name.strip());
                ps.setLong(4, startsAt);
                ps.setLong(5, startsAt + durationMs);
                ps.setLong(6, now);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getLong(1);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO practice_items(practice_id, position, lesson_id) VALUES (?, ?, ?)")) {
                for (int i = 0; i < lessonIds.size(); i++) {
                    ps.setLong(1, id);
                    ps.setInt(2, i);
                    ps.setString(3, lessonIds.get(i));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return id;
        });
    }

    public List<Session> list() throws SQLException {
        return db.read(c -> {
            List<Session> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, class_id, name, starts_at, closes_at FROM practice_sessions ORDER BY starts_at DESC");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    out.add(load(c, rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getLong(4), rs.getLong(5)));
            }
            return out;
        });
    }

    /**
     * Lớp của đợt thực hành được liên kết với học sinh qua enrollments, không
     * qua classId đã lưu trong phiên đăng nhập. Điều này cho phép CSV vừa được
     * nhập có hiệu lực ngay mà không cần học sinh đăng nhập lại.
     */
    public boolean studentEnrolled(long practiceId, long userId) throws SQLException {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT EXISTS("
                            + "SELECT 1 FROM practice_sessions ps "
                            + "JOIN enrollments e ON e.class_id = ps.class_id "
                            + "WHERE ps.id = ? AND e.user_id = ?"
                            + ")")) {
                ps.setLong(1, practiceId);
                ps.setLong(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) != 0;
                }
            }
        });
    }

    public Session get(long id) throws SQLException {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT class_id, name, starts_at, closes_at FROM practice_sessions WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? load(c, id, rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getLong(4)) : null;
                }
            }
        });
    }

    public void delete(long practiceId, long teacherId) throws SQLException {
        db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM practice_sessions WHERE id = ? AND teacher_id = ?")) {
                ps.setLong(1, practiceId);
                ps.setLong(2, teacherId);
                if (ps.executeUpdate() == 0)
                    throw new IllegalArgumentException("Không tìm thấy đợt thực hành hoặc bạn không có quyền xóa");
            }
            return null;
        });
    }

    public List<StudentProgress> studentProgress(long practiceId, long teacherId) throws SQLException {
        return db.read(c -> {
            List<StudentProgress> out = new ArrayList<>();
            long classId;
            try (PreparedStatement classQuery = c.prepareStatement(
                    "SELECT COALESCE(ps.class_id, ("
                            + "SELECT id FROM classes WHERE teacher_id = ps.teacher_id ORDER BY id LIMIT 1"
                            + ")) FROM practice_sessions ps WHERE ps.id = ? AND ps.teacher_id = ?")) {
                classQuery.setLong(1, practiceId);
                classQuery.setLong(2, teacherId);
                try (ResultSet classRows = classQuery.executeQuery()) {
                    if (!classRows.next())
                        return out;
                    classId = classRows.getLong(1);
                }
            }
            if (classId <= 0)
                return out;
            String sql = "SELECT u.full_name, u.username, "
                    + "COUNT(DISTINCT CASE WHEN sub.kind = 'SUBMIT' AND sub.verdict = 'OK' THEN sub.lesson_id END), "
                    + "(SELECT COUNT(*) FROM practice_items WHERE practice_id = ?) "
                    + ", u.id "
                    + "FROM classes cl JOIN enrollments e ON e.class_id = cl.id "
                    + "JOIN users u ON u.id = e.user_id "
                    + "LEFT JOIN submissions sub ON sub.user_id = u.id "
                    + "AND sub.lesson_id IN (SELECT lesson_id FROM practice_items WHERE practice_id = ?) "
                    + "AND sub.created_at >= (SELECT starts_at FROM practice_sessions WHERE id = ?) "
                    + "WHERE cl.id = ? "
                    + "AND cl.teacher_id = ? AND u.role = 'STUDENT' "
                    + "GROUP BY u.id, u.full_name, u.username ORDER BY u.full_name, u.username";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, practiceId);
                ps.setLong(2, practiceId);
                ps.setLong(3, practiceId);
                ps.setLong(4, classId);
                ps.setLong(5, teacherId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long userId = rs.getLong(5);
                        boolean active = activeStudents.getOrDefault(activeKey(practiceId, userId),
                                0L) >= System.currentTimeMillis() - ACTIVE_WINDOW_MS;
                        Set<String> completedLessons = new HashSet<>();
                        try (PreparedStatement completed = c.prepareStatement(
                                "SELECT DISTINCT sub.lesson_id FROM submissions sub "
                                        + "WHERE sub.user_id = ? AND sub.kind = 'SUBMIT' AND sub.verdict = 'OK' "
                                        + "AND sub.created_at >= (SELECT starts_at FROM practice_sessions WHERE id = ?) "
                                        + "AND sub.lesson_id IN (SELECT lesson_id FROM practice_items WHERE practice_id = ?)")) {
                            completed.setLong(1, userId);
                            completed.setLong(2, practiceId);
                            completed.setLong(3, practiceId);
                            try (ResultSet completedRows = completed.executeQuery()) {
                                while (completedRows.next())
                                    completedLessons.add(completedRows.getString(1));
                            }
                        }
                        out.add(new StudentProgress(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getInt(4),
                                active, completedLessons));
                    }
                }
            }
            return out;
        });
    }

    public void touch(long practiceId, long userId) {
        activeStudents.put(activeKey(practiceId, userId), System.currentTimeMillis());
    }

    public void clearActive(long practiceId, long userId) {
        activeStudents.remove(activeKey(practiceId, userId));
    }

    private static long activeKey(long practiceId, long userId) {
        return (practiceId << 32) ^ (userId & 0xffffffffL);
    }

    public void finish(long practiceId, long userId, long now) throws SQLException {
        db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT OR REPLACE INTO practice_submissions"
                            + "(practice_id, user_id, lesson_id, submitted_at) "
                            + "SELECT ?, ?, lesson_id, ? FROM practice_items WHERE practice_id = ?")) {
                ps.setLong(1, practiceId);
                ps.setLong(2, userId);
                ps.setLong(3, now);
                ps.setLong(4, practiceId);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public boolean finished(long practiceId, long userId) throws SQLException {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT EXISTS(SELECT 1 FROM practice_submissions "
                            + "WHERE practice_id = ? AND user_id = ?)")) {
                ps.setLong(1, practiceId);
                ps.setLong(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1) != 0;
                }
            }
        });
    }

    /** Trả false khi bài thuộc một đợt đã lên lịch nhưng chưa mở hoặc đã đóng. */
    public boolean studentCanAccess(String lessonId, long now) throws SQLException {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT s.starts_at, s.closes_at FROM practice_sessions s "
                            + "JOIN practice_items i ON i.practice_id = s.id WHERE i.lesson_id = ?")) {
                ps.setString(1, lessonId);
                boolean found = false;
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        found = true;
                        if (now >= rs.getLong(1) && now < rs.getLong(2))
                            return true;
                    }
                }
                return !found;
            }
        });
    }

    private static Session load(java.sql.Connection c, long id, long classId, String name, long startsAt, long closesAt)
            throws SQLException {
        List<String> lessons = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT lesson_id FROM practice_items WHERE practice_id = ? ORDER BY position")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    lessons.add(rs.getString(1));
            }
        }
        return new Session(id, classId, name, startsAt, closesAt, List.copyOf(lessons));
    }
}