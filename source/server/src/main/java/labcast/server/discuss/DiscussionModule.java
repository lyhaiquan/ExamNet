package labcast.server.discuss;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.ProtocolException;
import labcast.common.protocol.Role;
import labcast.server.ServerContext;
import labcast.server.ServerModule;
import labcast.server.net.Ctx;
import labcast.server.net.Handler;
import labcast.server.net.Priority;
import labcast.server.session.Session;

/**
 * Thảo luận ở từng bài (giai đoạn 6.D), riêng cho từng lớp:
 *
 * <ul>
 *   <li>{@code DISCUSS_REQ(str lessonId)} → {@code DISCUSS_DATA JSON{lessonId, items:[…]}}, mỗi mục {@code
 *       {id, parentId, author, role, text, at, hidden, mine}}; học viên không nhận mục đã ẩn.
 *   <li>{@code DISCUSS_POST JSON{lessonId, parentId?, text}}: tối đa {@value #MAX_LEN} ký tự, trả lời lồng
 *       một cấp, mỗi người cách nhau ít nhất {@value #MIN_GAP_MS} ms.
 *   <li>{@code DISCUSS_HIDE(i64 id, bool hidden)}: giáo viên của lớp hoặc quản trị; ghi audit_log.
 *   <li>{@code DISCUSS_NEW JSON{lessonId, item}}: phát cho mọi người online cùng lớp khi có bình luận mới
 *       hoặc vừa ẩn/hiện; app tự lọc theo bài đang mở.
 * </ul>
 */
public final class DiscussionModule implements ServerModule {
    static final int MAX_LEN = 2000;
    static final long MIN_GAP_MS = 2000;
    private ServerContext ctx;
    private final Map<Long, Long> lastPost = new ConcurrentHashMap<>();

    /** Từ chối từ bên trong giao dịch SQL (SqlWork không ném được ProtocolException); gỡ ra ở {@link #run}. */
    private static final class Refused extends RuntimeException {
        final ProtocolException cause;

        Refused(ErrorCode code, String message) {
            super(message, null, false, false);
            cause = new ProtocolException(code, message);
        }
    }

    private static Refused refuse(ErrorCode code, String message) {
        return new Refused(code, message);
    }

    private static Handler run(Handler h) {
        return (c, f) -> {
            try {
                h.handle(c, f);
            } catch (Refused r) {
                throw r.cause;
            }
        };
    }

    @Override
    public void install(ServerContext c) {
        ctx = c;
        c.router()
                .on(MessageType.DISCUSS_REQ, run(this::list))
                .on(MessageType.DISCUSS_POST, run(this::post))
                .on(MessageType.DISCUSS_HIDE, Role.STAFF, run(this::hide));
    }

    /** Lớp đang chọn của phiên, kiểm người này thuộc lớp (học viên ghi danh, giáo viên dạy, quản trị). */
    private long classOf(Connection db, Session s) throws SQLException {
        long classId = s.classId();
        if (classId <= 0) throw refuse(ErrorCode.FORBIDDEN, "Chọn lớp trước khi thảo luận");
        String sql =
                switch (s.role()) {
                    case ADMIN -> "SELECT 1 FROM classes WHERE id=? AND ? > 0";
                    case TEACHER -> "SELECT 1 FROM classes WHERE id=? AND teacher_id=?";
                    case STUDENT -> "SELECT 1 FROM enrollments WHERE class_id=? AND user_id=?";
                };
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setLong(1, classId);
            ps.setLong(2, s.userId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw refuse(ErrorCode.FORBIDDEN, "Không thuộc lớp này");
            }
        }
        return classId;
    }

    private void list(Ctx c, Frame f) throws Exception {
        Session s = c.session();
        String lessonId = f.reader().str();
        List<Map<String, Object>> items = ctx.db().read(db -> {
            long classId = classOf(db, s);
            List<Map<String, Object>> out = new ArrayList<>();
            try (PreparedStatement ps = db.prepareStatement(SELECT + " WHERE d.class_id=? AND d.lesson_id=?"
                    + (s.role().isStaff() ? "" : " AND d.hidden=0") + " ORDER BY d.id")) {
                ps.setLong(1, classId);
                ps.setString(2, lessonId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(item(rs, s.userId()));
                }
            }
            return out;
        });
        c.reply(
                JsonPayload.frame(MessageType.DISCUSS_DATA, Map.of("lessonId", lessonId, "items", items)),
                Priority.NORMAL);
    }

    private static final String SELECT = "SELECT d.id, d.parent_id, u.full_name, u.role, d.text, d.created_at,"
            + " d.hidden, d.author_id, d.lesson_id FROM discussions d JOIN users u ON u.id=d.author_id";

    private static Map<String, Object> item(ResultSet rs, long me) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getLong(1));
        long parent = rs.getLong(2);
        m.put("parentId", rs.wasNull() ? 0 : parent);
        m.put("author", rs.getString(3));
        m.put("role", rs.getString(4));
        m.put("text", rs.getString(5));
        m.put("at", rs.getLong(6));
        m.put("hidden", rs.getInt(7) == 1);
        m.put("mine", rs.getLong(8) == me);
        return m;
    }

    private void post(Ctx c, Frame f) throws Exception {
        Session s = c.session();
        JsonNode in = JsonPayload.tree(f);
        String lessonId = in.path("lessonId").asText("");
        String text = in.path("text").asText("").strip();
        long parentId = in.path("parentId").asLong(0);
        if (text.isEmpty()) throw refuse(ErrorCode.BAD_FRAME, "Bình luận trống");
        if (text.length() > MAX_LEN) {
            throw refuse(ErrorCode.BAD_FRAME, "Bình luận dài quá " + MAX_LEN + " ký tự");
        }
        long now = System.currentTimeMillis();
        Long last = lastPost.get(s.userId());
        if (last != null && now - last < MIN_GAP_MS) {
            throw refuse(ErrorCode.RATE_LIMITED, "Gửi chậm lại một chút");
        }
        long[] saved = new long[2];
        Map<String, Object> item = ctx.db().tx(db -> {
            long classId = classOf(db, s);
            try (PreparedStatement ps = db.prepareStatement("SELECT 1 FROM lessons WHERE id=?")) {
                ps.setString(1, lessonId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw refuse(ErrorCode.NOT_FOUND, "Không có bài " + lessonId);
                }
            }
            if (parentId > 0) {
                // trả lời lồng một cấp: cha phải là bình luận gốc của cùng bài, cùng lớp
                try (PreparedStatement ps = db.prepareStatement(
                        "SELECT parent_id FROM discussions WHERE id=? AND class_id=? AND lesson_id=?")) {
                    ps.setLong(1, parentId);
                    ps.setLong(2, classId);
                    ps.setString(3, lessonId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw refuse(ErrorCode.NOT_FOUND, "Không có bình luận gốc");
                        rs.getLong(1);
                        if (!rs.wasNull()) {
                            throw refuse(ErrorCode.BAD_FRAME, "Chỉ trả lời được bình luận gốc");
                        }
                    }
                }
            }
            try (PreparedStatement ps = db.prepareStatement(
                    "INSERT INTO discussions (class_id, lesson_id, parent_id, author_id, text, created_at, hidden)"
                            + " VALUES (?, ?, ?, ?, ?, ?, 0)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, classId);
                ps.setString(2, lessonId);
                if (parentId > 0) ps.setLong(3, parentId);
                else ps.setNull(3, java.sql.Types.INTEGER);
                ps.setLong(4, s.userId());
                ps.setString(5, text);
                ps.setLong(6, now);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    saved[0] = keys.getLong(1);
                }
            }
            saved[1] = classId;
            return load(db, saved[0], s.userId());
        });
        lastPost.put(s.userId(), now);
        broadcast(saved[1], lessonId, saved[0], s);
        c.reply(
                JsonPayload.frame(MessageType.DISCUSS_NEW, Map.of("lessonId", lessonId, "item", item)),
                Priority.NORMAL);
    }

    private static Map<String, Object> load(Connection db, long id, long me) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(SELECT + " WHERE d.id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? item(rs, me) : Map.of();
            }
        }
    }

    private void hide(Ctx c, Frame f) throws Exception {
        Session s = c.session();
        var r = f.reader();
        long id = r.i64();
        boolean hidden = r.bool();
        String[] lesson = new String[1];
        long[] classId = new long[1];
        ctx.db().tx(db -> {
            long cls = classOf(db, s);
            try (PreparedStatement ps =
                    db.prepareStatement("UPDATE discussions SET hidden=? WHERE id=? AND class_id=?")) {
                ps.setInt(1, hidden ? 1 : 0);
                ps.setLong(2, id);
                ps.setLong(3, cls);
                if (ps.executeUpdate() == 0) throw refuse(ErrorCode.NOT_FOUND, "Không có bình luận");
            }
            try (PreparedStatement ps = db.prepareStatement("SELECT lesson_id FROM discussions WHERE id=?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    lesson[0] = rs.getString(1);
                }
            }
            classId[0] = cls;
            return null;
        });
        ctx.audit().record(s.username(), hidden ? "DISCUSS_HIDE" : "DISCUSS_SHOW", "id=" + id);
        broadcast(classId[0], lesson[0], id, null);
        c.reply(
                MessageType.ADMIN_OK,
                new labcast.common.protocol.PayloadWriter()
                        .str(hidden ? "Đã ẩn" : "Đã hiện")
                        .toBytes());
    }

    /**
     * Phát mục vừa đổi cho mọi người online cùng lớp (trừ phiên gửi, đã nhận qua reply). Học viên nhận mục đã
     * ẩn dưới dạng {@code hidden: true, text: ""} để gỡ khỏi màn, không thấy nội dung.
     */
    private void broadcast(long classId, String lessonId, long id, Session skip) throws SQLException {
        for (Session other : ctx.sessions().all()) {
            if (other == skip || !other.online() || other.classId() != classId) continue;
            Map<String, Object> item = ctx.db().read(db -> load(db, id, other.userId()));
            if (!other.role().isStaff() && Boolean.TRUE.equals(item.get("hidden"))) {
                item = new LinkedHashMap<>(item);
                item.put("text", "");
            }
            other.send(
                    JsonPayload.frame(MessageType.DISCUSS_NEW, Map.of("lessonId", lessonId, "item", item)),
                    Priority.NORMAL);
        }
    }

    /** Số bình luận (không tính đã ẩn) theo bài của một lớp, cho danh sách bài. */
    public static Map<String, int[]> counts(Connection db, long classId, long since) throws SQLException {
        Map<String, int[]> out = new LinkedHashMap<>();
        try (PreparedStatement ps = db.prepareStatement("SELECT lesson_id, COUNT(*), SUM(created_at >= ?)"
                + " FROM discussions WHERE class_id=? AND hidden=0 GROUP BY lesson_id")) {
            ps.setLong(1, since);
            ps.setLong(2, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString(1), new int[] {rs.getInt(2), rs.getInt(3)});
            }
        }
        return out;
    }
}
