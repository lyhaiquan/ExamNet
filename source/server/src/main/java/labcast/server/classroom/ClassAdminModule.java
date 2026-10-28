package labcast.server.classroom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.ProtocolException;
import labcast.common.protocol.Role;
import labcast.server.ServerContext;
import labcast.server.ServerModule;
import labcast.server.net.Ctx;
import labcast.server.net.Priority;
import labcast.server.session.Session;

/**
 * Quản lý lớp, chỉ quản trị: xem mọi lớp, tạo lớp và giao cho một giáo viên, đổi tên, đổi giáo viên, xoá lớp.
 * Khác "Tạo lớp thực hành" của giáo viên (lớp luôn thuộc người bấm): quản trị chọn giáo viên phụ trách.
 *
 * <p>Chỉ xoá được lớp chưa có học viên, thảo luận hay câu hỏi nhanh nào — lớp đã dùng thì bài làm, thảo luận gắn
 * với lớp sẽ mất chỗ dựa. Mã mời và phòng thực hành rỗng của lớp xoá theo.
 */
public final class ClassAdminModule implements ServerModule {
    static final int MAX_NAME = 100;
    private static final Set<Role> ADMIN_ONLY = Set.of(Role.ADMIN);

    private ServerContext ctx;

    @Override
    public void install(ServerContext ctx) {
        this.ctx = ctx;
        ctx.router()
                .on(MessageType.ADMIN_CLASS_LIST, ADMIN_ONLY, this::list)
                .on(MessageType.ADMIN_CLASS_SAVE, ADMIN_ONLY, this::save)
                .on(MessageType.ADMIN_CLASS_DELETE, ADMIN_ONLY, this::delete);
    }

    /**
     * {@code ADMIN_CLASS_LIST} → {@code ADMIN_CLASS_DATA} JSON {classes:[{id, name, teacherId, teacher,
     * teacherUsername, students, joinCode, createdAt}], teachers:[{id, fullName, username, locked}]}.
     */
    private void list(Ctx c, Frame f) throws Exception {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        ArrayNode classes = root.putArray("classes");
        ArrayNode teachers = root.putArray("teachers");
        ctx.db().read(db -> {
            try (PreparedStatement ps =
                            db.prepareStatement("SELECT c.id, c.name, c.teacher_id, u.full_name, u.username,"
                                    + " (SELECT count(*) FROM enrollments e WHERE e.class_id = c.id),"
                                    + " COALESCE(i.join_code, ''), c.created_at FROM classes c JOIN users u ON u.id = c.teacher_id"
                                    + " LEFT JOIN class_invites i ON i.class_id = c.id ORDER BY c.name");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    classes.addObject()
                            .put("id", rs.getLong(1))
                            .put("name", rs.getString(2))
                            .put("teacherId", rs.getLong(3))
                            .put("teacher", rs.getString(4))
                            .put("teacherUsername", rs.getString(5))
                            .put("students", rs.getInt(6))
                            .put("joinCode", rs.getString(7))
                            .put("createdAt", rs.getLong(8));
                }
            }
            try (PreparedStatement ps = db.prepareStatement("SELECT id, full_name, username, disabled FROM users"
                            + " WHERE role = 'TEACHER' ORDER BY full_name, username");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    teachers.addObject()
                            .put("id", rs.getLong(1))
                            .put("fullName", rs.getString(2))
                            .put("username", rs.getString(3))
                            .put("locked", rs.getInt(4) != 0);
                }
            }
            return null;
        });
        c.reply(JsonPayload.frame(MessageType.ADMIN_CLASS_DATA, root), Priority.NORMAL);
    }

    /**
     * {@code ADMIN_CLASS_SAVE} JSON {id, name, teacherId} → {@code ADMIN_OK}. {@code id} 0: tạo lớp mới cho giáo viên
     * đó; khác 0: đổi tên và / hoặc đổi giáo viên phụ trách. Hai lớp không được trùng tên.
     */
    private void save(Ctx c, Frame f) throws Exception {
        JsonNode in = JsonPayload.tree(f);
        long id = in.path("id").asLong(0);
        String name = in.path("name").asText("").strip();
        long teacherId = in.path("teacherId").asLong(0);
        if (name.isEmpty() || name.length() > MAX_NAME) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "Tên lớp phải từ 1 đến " + MAX_NAME + " ký tự");
        }
        String[] teacher = ctx.db().read(db -> teacher(db, teacherId));
        if (teacher == null) throw new ProtocolException(ErrorCode.BAD_FRAME, "Chọn giáo viên phụ trách lớp");
        if (teacher[1] != null) {
            throw new ProtocolException(
                    ErrorCode.BAD_FRAME, "Giáo viên " + teacher[0] + " đang bị khoá: mở khoá trước");
        }
        boolean taken = ctx.db().read(db -> {
            try (PreparedStatement ps =
                    db.prepareStatement("SELECT 1 FROM classes WHERE lower(name) = lower(?) AND id <> ?")) {
                ps.setString(1, name);
                ps.setLong(2, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
        if (taken) throw new ProtocolException(ErrorCode.BAD_FRAME, "Đã có lớp tên \"" + name + "\"");
        int changed = ctx.db().tx(db -> {
            try (PreparedStatement ps = db.prepareStatement(
                    id == 0
                            ? "INSERT INTO classes (name, teacher_id, created_at) VALUES (?, ?, ?)"
                            : "UPDATE classes SET name = ?, teacher_id = ? WHERE id = ?")) {
                ps.setString(1, name);
                ps.setLong(2, teacherId);
                ps.setLong(3, id == 0 ? System.currentTimeMillis() : id);
                return ps.executeUpdate();
            }
        });
        if (changed == 0) throw new ProtocolException(ErrorCode.NOT_FOUND, "Không có lớp này");
        String message = (id == 0 ? "Đã tạo lớp " : "Đã lưu lớp ") + name + ", giáo viên " + teacher[0];
        ctx.audit().record(c.session().username(), id == 0 ? "CLASS_CREATE" : "CLASS_EDIT", name);
        c.reply(MessageType.ADMIN_OK, new PayloadWriter().str(message).toBytes());
    }

    /** {@code ADMIN_CLASS_DELETE(i64 id)} → {@code ADMIN_OK}; lớp đã có học viên, thảo luận hay câu hỏi nhanh thì từ chối. */
    private void delete(Ctx c, Frame f) throws Exception {
        long id = f.reader().i64();
        String name = ctx.db().read(db -> {
            try (PreparedStatement ps = db.prepareStatement("SELECT name FROM classes WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        });
        if (name == null) throw new ProtocolException(ErrorCode.NOT_FOUND, "Không có lớp này");
        int[] used = ctx.db().read(db -> new int[] {
            count(db, "SELECT count(*) FROM enrollments WHERE class_id = ?", id),
            count(db, "SELECT count(*) FROM discussions WHERE class_id = ?", id)
                    + count(db, "SELECT count(*) FROM quizzes WHERE class_id = ?", id)
        });
        if (used[0] > 0) {
            throw new ProtocolException(
                    ErrorCode.BAD_FRAME,
                    "Lớp " + name + " còn " + used[0] + " học viên: chuyển học viên sang lớp khác ở màn Tài khoản rồi"
                            + " mới xoá");
        }
        if (used[1] > 0) {
            throw new ProtocolException(
                    ErrorCode.BAD_FRAME, "Lớp " + name + " đã có thảo luận hoặc câu hỏi: giữ lại để không mất dữ liệu");
        }
        ctx.db().tx(db -> {
            for (String sql : new String[] {
                "DELETE FROM practice_room_lessons WHERE room_id IN (SELECT id FROM practice_rooms WHERE class_id = ?)",
                "DELETE FROM practice_members WHERE room_id IN (SELECT id FROM practice_rooms WHERE class_id = ?)",
                "DELETE FROM practice_rooms WHERE class_id = ?",
                "DELETE FROM class_invites WHERE class_id = ?",
                "DELETE FROM student_activity WHERE class_id = ?",
                "DELETE FROM classes WHERE id = ?"
            }) {
                try (PreparedStatement ps = db.prepareStatement(sql)) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }
            }
            return null;
        });
        // giáo viên / quản trị đang chọn lớp này: về "chưa chọn lớp" thay vì trỏ vào lớp không còn
        for (Session s : ctx.sessions().all()) {
            if (s.classId() == id) s.setClassId(0);
        }
        ctx.audit().record(c.session().username(), "CLASS_DELETE", name);
        c.reply(
                MessageType.ADMIN_OK,
                new PayloadWriter().str("Đã xoá lớp " + name).toBytes());
    }

    /** {họ tên, null} của giáo viên {@code id}; {họ tên, "khoá"} nếu đang bị khoá; null nếu không phải giáo viên. */
    private static String[] teacher(Connection db, long id) throws SQLException {
        try (PreparedStatement ps =
                db.prepareStatement("SELECT full_name, disabled FROM users WHERE id = ? AND role = 'TEACHER'")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new String[] {rs.getString(1), rs.getInt(2) != 0 ? "khoá" : null} : null;
            }
        }
    }

    private static int count(Connection db, String sql, long id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
