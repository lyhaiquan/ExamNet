package labcast.server.classroom;

import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.Role;
import labcast.server.ServerContext;
import labcast.server.ServerModule;
import labcast.server.net.Ctx;
import labcast.server.net.Priority;
import labcast.server.session.Session;

/** Lớp, phiên thực hành và bảng giám sát: lấy dữ liệu từ SQLite/phiên online thật. */
public final class ClassroomWorkspaceModule implements ServerModule {
    private static final SecureRandom RANDOM = new SecureRandom();
    private ServerContext ctx;

    @Override
    public void install(ServerContext c) {
        ctx = c;
        try {
            c.db().read(db -> {
                try (Statement st = db.createStatement()) {
                    st.execute("CREATE TABLE IF NOT EXISTS class_invites ("
                            + "class_id INTEGER PRIMARY KEY REFERENCES classes(id), join_code TEXT UNIQUE NOT NULL)");
                    st.execute("CREATE TABLE IF NOT EXISTS practice_rooms ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT, class_id INTEGER NOT NULL REFERENCES classes(id), "
                            + "title TEXT NOT NULL, join_code TEXT NOT NULL UNIQUE, teacher_id INTEGER NOT NULL REFERENCES users(id), "
                            + "created_at INTEGER NOT NULL, active INTEGER NOT NULL DEFAULT 1)");
                    st.execute("CREATE TABLE IF NOT EXISTS practice_members ("
                            + "room_id INTEGER NOT NULL REFERENCES practice_rooms(id), user_id INTEGER NOT NULL REFERENCES users(id), "
                            + "joined_at INTEGER NOT NULL, PRIMARY KEY(room_id,user_id))");
                    st.execute("CREATE TABLE IF NOT EXISTS practice_room_lessons ("
                            + "room_id INTEGER NOT NULL REFERENCES practice_rooms(id) ON DELETE CASCADE, "
                            + "lesson_id TEXT NOT NULL, added_at INTEGER NOT NULL, "
                            + "PRIMARY KEY(room_id, lesson_id))");
                    st.execute("CREATE TABLE IF NOT EXISTS student_activity ("
                            + "user_id INTEGER PRIMARY KEY REFERENCES users(id), class_id INTEGER NOT NULL, "
                            + "screen TEXT NOT NULL, lesson_id TEXT, updated_at INTEGER NOT NULL)");
                }
                return null;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("Không tạo được bảng phiên thực hành", e);
        }
        c.router()
                .on(MessageType.CLASS_LIST_REQ, this::classes)
                .on(MessageType.CLASS_SELECT_REQ, this::selectClass)
                .on(MessageType.PRACTICE_LIST_REQ, this::rooms)
                .on(MessageType.PRACTICE_CREATE_REQ, Set.of(Role.TEACHER), this::createRoom)
                .on(MessageType.PRACTICE_JOIN_REQ, Set.of(Role.STUDENT), this::joinRoom)
                .on(MessageType.STUDENT_OVERVIEW_REQ, Set.of(Role.TEACHER), this::students)
                .on(MessageType.STUDENT_ACTIVITY, Set.of(Role.STUDENT), this::activity)
                .on(MessageType.CLASS_CREATE_REQ, Set.of(Role.TEACHER), this::createClass)
                .on(MessageType.CLASS_JOIN_REQ, Set.of(Role.STUDENT), this::joinClass)
                .on(MessageType.PRACTICE_ASSIGN_REQ, Set.of(Role.TEACHER), this::assignLesson)
                .on(MessageType.PRACTICE_UNASSIGN_REQ, Set.of(Role.TEACHER), this::unassignLesson)
                .on(MessageType.PRACTICE_LESSONS_REQ, this::roomLessons);
    }

    private void classes(Ctx c, Frame f) throws Exception {
        Session s = c.session();
        List<Map<String, Object>> data = ctx.db().read(db -> {
            List<Map<String, Object>> res = new ArrayList<>();
            String sql = s.role() == Role.TEACHER
                    ? "SELECT c.id,c.name,COALESCE(i.join_code,'') FROM classes c LEFT JOIN class_invites i ON i.class_id=c.id WHERE c.teacher_id=? ORDER BY c.created_at DESC"
                    : "SELECT c.id,c.name FROM classes c JOIN enrollments e ON e.class_id=c.id WHERE e.user_id=? ORDER BY c.name";
            try (PreparedStatement ps = db.prepareStatement(sql)) {
                ps.setLong(1, s.userId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", rs.getLong(1));
                        row.put("name", rs.getString(2));
                        if (s.role() == Role.TEACHER) row.put("joinCode", rs.getString(3));
                        res.add(row);
                    }
                }
            }
            return res;
        });
        c.reply(
                JsonPayload.frame(MessageType.CLASS_LIST_DATA, Map.of("classes", data, "selectedId", s.classId())),
                Priority.NORMAL);
    }

    /** Giáo viên tạo lớp thực hành thực sự (classes + mã mời). */
    private void createClass(Ctx c, Frame f) throws Exception {
        Session teacher = c.session();
        String title = f.reader().str().strip();
        if (title.isEmpty() || title.length() > 100) {
            c.error(ErrorCode.BAD_FRAME, "Tên lớp phải từ 1 đến 100 ký tự", 0);
            return;
        }
        long id = new ClassRepository(ctx.db()).ensure(title, teacher.userId());
        String code = inviteFor(id);
        teacher.setClassId(id);
        ctx.audit().record(teacher.username(), "CLASS_CREATE", title);
        c.reply(
                JsonPayload.frame(MessageType.CLASS_CREATE_RESULT, Map.of("id", id, "name", title, "joinCode", code)),
                Priority.NORMAL);
    }

    private String inviteFor(long classId) throws SQLException {
        String found = ctx.db().read(db -> {
            try (PreparedStatement ps = db.prepareStatement("SELECT join_code FROM class_invites WHERE class_id=?")) {
                ps.setLong(1, classId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        });
        if (found != null) return found;
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = String.format("%06d", RANDOM.nextInt(1_000_000));
            try {
                ctx.db().read(db -> {
                    try (PreparedStatement ps = db.prepareStatement(
                            "INSERT OR IGNORE INTO class_invites(class_id,join_code) VALUES(?,?)")) {
                        ps.setLong(1, classId);
                        ps.setString(2, code);
                        ps.executeUpdate();
                    }
                    return null;
                });
                found = ctx.db().read(db -> {
                    try (PreparedStatement ps =
                            db.prepareStatement("SELECT join_code FROM class_invites WHERE class_id=?")) {
                        ps.setLong(1, classId);
                        try (ResultSet rs = ps.executeQuery()) {
                            return rs.next() ? rs.getString(1) : null;
                        }
                    }
                });
                if (found != null) return found;
            } catch (SQLException e) {
                if (attempt == 4) throw e;
            }
        }
        throw new SQLException("Không cấp được mã tham gia lớp");
    }

    private void joinClass(Ctx c, Frame f) throws Exception {
        Session student = c.session();
        String code = f.reader().str().strip();
        if (!code.matches("[0-9]{6}")) {
            c.error(ErrorCode.BAD_FRAME, "Mã lớp gồm 6 chữ số", 0);
            return;
        }
        long id = ctx.db().read(db -> {
            try (PreparedStatement ps = db.prepareStatement("SELECT class_id FROM class_invites WHERE join_code=?")) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        });
        if (id == 0) {
            c.error(ErrorCode.NOT_FOUND, "Không tìm thấy lớp có mã này", 0);
            return;
        }
        new ClassRepository(ctx.db()).enroll(id, student.userId());
        student.setClassId(id);
        String name = ctx.db().read(db -> {
            try (PreparedStatement ps = db.prepareStatement("SELECT name FROM classes WHERE id=?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : "";
                }
            }
        });
        ctx.audit().record(student.username(), "CLASS_JOIN", name);
        c.reply(JsonPayload.frame(MessageType.CLASS_JOIN_RESULT, Map.of("id", id, "name", name)), Priority.NORMAL);
    }

    private boolean allowed(Session s, long classId) throws SQLException {
        return ctx.db().read(db -> {
            String sql = s.role() == Role.TEACHER
                    ? "SELECT 1 FROM classes WHERE id=? AND teacher_id=?"
                    : "SELECT 1 FROM enrollments WHERE class_id=? AND user_id=?";
            try (PreparedStatement ps = db.prepareStatement(sql)) {
                ps.setLong(1, classId);
                ps.setLong(2, s.userId());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    private void selectClass(Ctx c, Frame f) throws Exception {
        long id = f.reader().i64();
        if (!allowed(c.session(), id)) {
            c.error(ErrorCode.FORBIDDEN, "Bạn không thuộc lớp này", 0);
            return;
        }
        String name = ctx.db().read(db -> {
            try (PreparedStatement ps = db.prepareStatement("SELECT name FROM classes WHERE id=?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : "";
                }
            }
        });
        c.session().setClassId(id);
        c.reply(JsonPayload.frame(MessageType.CLASS_SELECT_RESULT, Map.of("id", id, "name", name)), Priority.NORMAL);
    }

    private void rooms(Ctx c, Frame f) throws Exception {
        Session s = c.session();
        if (s.classId() <= 0 || !allowed(s, s.classId())) {
            c.error(ErrorCode.FORBIDDEN, "Chưa chọn lớp hợp lệ", 0);
            return;
        }
        List<Map<String, Object>> data = ctx.db().read(db -> {
            List<Map<String, Object>> result = new ArrayList<>();
            try (PreparedStatement ps = db.prepareStatement("SELECT r.id,r.title,r.join_code,r.created_at,"
                    + "EXISTS(SELECT 1 FROM practice_members m WHERE m.room_id=r.id AND m.user_id=?) "
                    + "FROM practice_rooms r WHERE r.class_id=? AND r.active=1 ORDER BY r.created_at DESC")) {
                ps.setLong(1, s.userId());
                ps.setLong(2, s.classId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", rs.getLong(1));
                        item.put("title", rs.getString(2));
                        if (s.role() == Role.TEACHER) item.put("joinCode", rs.getString(3));
                        item.put("createdAt", rs.getLong(4));
                        item.put("joined", rs.getInt(5) == 1);
                        result.add(item);
                    }
                }
            }
            return result;
        });
        c.reply(JsonPayload.frame(MessageType.PRACTICE_LIST_DATA, Map.of("rooms", data)), Priority.NORMAL);
    }

    private void createRoom(Ctx c, Frame f) throws Exception {
        Session s = c.session();
        String name = f.reader().str().strip();
        if (name.isBlank() || name.length() > 100) {
            c.error(ErrorCode.BAD_FRAME, "Tên phiên thực hành phải từ 1 đến 100 ký tự", 0);
            return;
        }
        if (!allowed(s, s.classId())) {
            c.error(ErrorCode.FORBIDDEN, "Lớp không thuộc giáo viên", 0);
            return;
        }
        String code = "";
        long id = 0;
        // Mã ngẫu nhiên, UNIQUE ở SQLite để chống trùng.
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = String.format("%06d", RANDOM.nextInt(1_000_000));
            try {
                id = ctx.db().tx(db -> {
                    try (PreparedStatement ps = db.prepareStatement(
                            "INSERT INTO practice_rooms(class_id,title,join_code,teacher_id,created_at) VALUES(?,?,?,?,?)",
                            Statement.RETURN_GENERATED_KEYS)) {
                        ps.setLong(1, s.classId());
                        ps.setString(2, name);
                        ps.setString(3, candidate);
                        ps.setLong(4, s.userId());
                        ps.setLong(5, System.currentTimeMillis());
                        ps.executeUpdate();
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) return rs.getLong(1);
                        }
                        throw new SQLException("Không lấy được ID phiên");
                    }
                });
                code = candidate;
                break;
            } catch (SQLException e) {
                if (attempt == 4) throw e;
            }
        }
        ctx.audit().record(s.username(), "PRACTICE_CREATE", name);
        c.reply(
                JsonPayload.frame(
                        MessageType.PRACTICE_ACTION_RESULT,
                        Map.of("action", "created", "id", id, "title", name, "joinCode", code)),
                Priority.NORMAL);
    }

    private void joinRoom(Ctx c, Frame f) throws Exception {
        Session s = c.session();
        long id = f.reader().i64();
        if (!allowed(s, s.classId())) {
            c.error(ErrorCode.FORBIDDEN, "Bạn chưa thuộc lớp", 0);
            return;
        }
        String title = ctx.db().read(db -> {
            try (PreparedStatement ps =
                    db.prepareStatement("SELECT title FROM practice_rooms WHERE id=? AND class_id=? AND active=1")) {
                ps.setLong(1, id);
                ps.setLong(2, s.classId());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        });
        if (title == null) {
            c.error(ErrorCode.NOT_FOUND, "Không tìm thấy phiên thực hành của lớp", 0);
            return;
        }
        ctx.db().tx(db -> {
            try (PreparedStatement ps = db.prepareStatement(
                    "INSERT OR IGNORE INTO practice_members(room_id,user_id,joined_at) VALUES(?,?,?)")) {
                ps.setLong(1, id);
                ps.setLong(2, s.userId());
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            }
            return null;
        });
        writeActivity(s, "Đang tham gia thực hành", null);
        c.reply(
                JsonPayload.frame(
                        MessageType.PRACTICE_ACTION_RESULT, Map.of("action", "joined", "id", id, "title", title)),
                Priority.NORMAL);
    }

    private void activity(Ctx c, Frame f) throws Exception {
        PayloadReader reader = f.reader();
        String screen = reader.str().strip();
        String lesson = reader.hasRemaining() ? reader.str().strip() : "";
        if (screen.length() > 90 || lesson.length() > 180) {
            c.error(ErrorCode.BAD_FRAME, "Trạng thái hoạt động quá dài", 0);
            return;
        }
        Session s = c.session();
        if (s.classId() > 0 && allowed(s, s.classId())) writeActivity(s, screen, lesson.isBlank() ? null : lesson);
    }

    private void writeActivity(Session s, String screen, String lesson) throws SQLException {
        ctx.db().read(db -> {
            try (PreparedStatement ps = db.prepareStatement(
                    "INSERT INTO student_activity(user_id,class_id,screen,lesson_id,updated_at) VALUES(?,?,?,?,?) "
                            + "ON CONFLICT(user_id) DO UPDATE SET class_id=excluded.class_id,screen=excluded.screen,lesson_id=excluded.lesson_id,updated_at=excluded.updated_at")) {
                ps.setLong(1, s.userId());
                ps.setLong(2, s.classId());
                ps.setString(3, screen);
                ps.setString(4, lesson);
                ps.setLong(5, System.currentTimeMillis());
                ps.executeUpdate();
            }
            return null;
        });
    }

    /** Tải bài của một phiên; chỉ người thuộc lớp mới có quyền xem. */
    private void roomLessons(Ctx c, Frame f) throws Exception {
        long roomId = f.reader().i64();
        Session user = c.session();
        if (!roomInSelectedClass(roomId, user)) {
            c.error(ErrorCode.FORBIDDEN, "Phiên thực hành không thuộc lớp đang chọn", 0);
            return;
        }
        List<Map<String, Object>> lessons = ctx.db().read(db -> {
            List<Map<String, Object>> result = new ArrayList<>();
            try (PreparedStatement ps = db.prepareStatement(
                    "SELECT lesson_id FROM practice_room_lessons WHERE room_id=? ORDER BY added_at,lesson_id")) {
                ps.setLong(1, roomId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) result.add(Map.of("id", rs.getString(1)));
                }
            }
            return result;
        });
        c.reply(
                JsonPayload.frame(MessageType.PRACTICE_LESSONS_DATA, Map.of("roomId", roomId, "lessons", lessons)),
                Priority.NORMAL);
    }

    private boolean roomInSelectedClass(long id, Session user) throws SQLException {
        if (user.classId() <= 0 || !allowed(user, user.classId())) return false;
        return ctx.db().read(db -> {
            try (PreparedStatement ps =
                    db.prepareStatement("SELECT 1 FROM practice_rooms WHERE id=? AND class_id=? AND active=1")) {
                ps.setLong(1, id);
                ps.setLong(2, user.classId());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    private void assignLesson(Ctx c, Frame f) throws Exception {
        updateRoomLesson(c, f, true);
    }

    private void unassignLesson(Ctx c, Frame f) throws Exception {
        updateRoomLesson(c, f, false);
    }

    private void updateRoomLesson(Ctx c, Frame f, boolean adding) throws Exception {
        var reader = f.reader();
        long roomId = reader.i64();
        String lessonId = reader.str().strip();
        Session teacher = c.session();
        if (!roomInSelectedClass(roomId, teacher)) {
            c.error(ErrorCode.FORBIDDEN, "Không có quyền chỉnh sửa phiên này", 0);
            return;
        }
        if (lessonId.isEmpty()
                || lessonId.length() > 180
                || ctx.require(labcast.server.lesson.LessonRepository.class)
                        .get(lessonId)
                        .isEmpty()) {
            c.error(ErrorCode.NOT_FOUND, "Bài tập không tồn tại trên Server", 0);
            return;
        }
        ctx.db().tx(db -> {
            try (PreparedStatement ps = db.prepareStatement(
                    adding
                            ? "INSERT OR IGNORE INTO practice_room_lessons(room_id,lesson_id,added_at) VALUES(?,?,?)"
                            : "DELETE FROM practice_room_lessons WHERE room_id=? AND lesson_id=?")) {
                ps.setLong(1, roomId);
                ps.setString(2, lessonId);
                if (adding) ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            }
            return null;
        });
        ctx.audit().record(teacher.username(), adding ? "PRACTICE_ASSIGN" : "PRACTICE_UNASSIGN", lessonId);
        c.reply(
                JsonPayload.frame(
                        MessageType.PRACTICE_ACTION_RESULT,
                        Map.of("action", adding ? "assigned" : "unassigned", "id", roomId, "lessonId", lessonId)),
                Priority.NORMAL);
    }

    private void students(Ctx c, Frame f) throws Exception {
        Session teacher = c.session();
        if (!allowed(teacher, teacher.classId())) {
            c.error(ErrorCode.FORBIDDEN, "Lớp không thuộc giáo viên", 0);
            return;
        }
        List<Map<String, Object>> rows = ctx.db().read(db -> {
            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "SELECT u.id,u.username,u.full_name,"
                    + "COALESCE(a.screen,'Chưa có hoạt động'),COALESCE(a.lesson_id,''),COALESCE(a.updated_at,0),"
                    + "(SELECT COUNT(*) FROM progress p WHERE p.user_id=u.id AND p.status='MASTERED'),"
                    + "COALESCE((SELECT r.title FROM practice_members m JOIN practice_rooms r ON r.id=m.room_id "
                    + "WHERE m.user_id=u.id AND r.class_id=? ORDER BY m.joined_at DESC LIMIT 1),'—') "
                    + "FROM enrollments e JOIN users u ON u.id=e.user_id "
                    + "LEFT JOIN student_activity a ON a.user_id=u.id AND a.class_id=e.class_id "
                    + "WHERE e.class_id=? AND u.role='STUDENT' ORDER BY u.full_name";
            try (PreparedStatement ps = db.prepareStatement(sql)) {
                ps.setLong(1, teacher.classId());
                ps.setLong(2, teacher.classId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long uid = rs.getLong(1);
                        Session live = ctx.sessions().byUser(uid).orElse(null);
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("userId", uid);
                        row.put("username", rs.getString(2));
                        row.put("fullName", rs.getString(3));
                        row.put("activity", rs.getString(4));
                        row.put("lessonId", rs.getString(5));
                        row.put("updatedAt", rs.getLong(6));
                        row.put("completed", rs.getInt(7));
                        row.put("practice", rs.getString(8));
                        row.put("online", live != null && live.online());
                        list.add(row);
                    }
                }
            }
            return list;
        });
        c.reply(JsonPayload.frame(MessageType.STUDENT_OVERVIEW_DATA, Map.of("students", rows)), Priority.NORMAL);
    }
}
