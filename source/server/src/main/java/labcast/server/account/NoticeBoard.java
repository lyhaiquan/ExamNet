package labcast.server.account;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.Role;
import labcast.server.net.Priority;
import labcast.server.session.Session;
import labcast.server.session.SessionManager;
import labcast.server.store.Database;

/**
 * Thông báo bảo mật cho quản trị: yêu cầu quên mật khẩu đang chờ và các sự kiện mật khẩu / khoá tài khoản lấy từ
 * {@code audit_log}. Không bao giờ trả mật khẩu, kể cả mật khẩu tạm.
 */
final class NoticeBoard {
    /** Hành động của {@code audit_log} hiện ở trang thông báo. */
    static final List<String> ACTIONS = List.of(
            "PASSWORD_FORGOT",
            "PASSWORD_FORGOT_UNKNOWN",
            "PASSWORD_CHANGE",
            "PASSWORD_CHANGE_FAIL",
            "PASSWORD_RESET",
            "AUTH_LOCKED");

    /** Đã có yêu cầu quên mật khẩu hiện ở danh sách "đang chờ", không cộng thêm vào số đỏ lần nữa. */
    private static final List<String> NOT_COUNTED = List.of("PASSWORD_FORGOT");

    private static final int LIMIT = 200;

    private final Database db;
    private final UserRepository users;
    private final SessionManager sessions;

    NoticeBoard(Database db, UserRepository users, SessionManager sessions) {
        this.db = db;
        this.users = users;
        this.sessions = sessions;
    }

    /** Đẩy cho mọi quản trị đang online rằng có sự kiện mới. */
    void push(String action, String actor) {
        ObjectNode n =
                JsonNodeFactory.instance.objectNode().put("action", action).put("actor", actor);
        for (Session s : sessions.all()) {
            if (s.role() == Role.ADMIN) {
                s.send(JsonPayload.frame(MessageType.NOTICE_NEW, n), Priority.NORMAL);
            }
        }
    }

    /** Trả JSON cho {@code NOTICES_DATA}; {@code markRead}: dời mốc đã đọc của {@code adminId} tới sự kiện mới nhất. */
    ObjectNode snapshot(long adminId, boolean markRead) throws SQLException {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        ArrayNode pending = root.putArray("pending");
        for (UserRepository.PasswordRequest r : users.passwordRequests(adminId, true)) {
            pending.addObject()
                    .put("username", r.username())
                    .put("fullName", r.fullName())
                    .put("role", r.role().name())
                    .put("at", r.at());
        }
        long lastRead = db.read(c -> {
            try (PreparedStatement ps =
                    c.prepareStatement("SELECT last_audit_id FROM notice_reads WHERE user_id = ?")) {
                ps.setLong(1, adminId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        });
        String in = "?,".repeat(ACTIONS.size());
        in = in.substring(0, in.length() - 1);
        String sql = "SELECT id, ts, actor, action, detail FROM audit_log WHERE action IN (" + in
                + ") ORDER BY id DESC LIMIT " + LIMIT;
        ArrayNode events = root.putArray("events");
        long[] newest = {lastRead};
        int[] unread = {0};
        db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < ACTIONS.size(); i++) {
                    ps.setString(i + 1, ACTIONS.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long id = rs.getLong(1);
                        String action = rs.getString(4);
                        events.addObject()
                                .put("id", id)
                                .put("ts", rs.getLong(2))
                                .put("actor", rs.getString(3))
                                .put("action", action)
                                .put("detail", rs.getString(5));
                        newest[0] = Math.max(newest[0], id);
                        if (id > lastRead && !NOT_COUNTED.contains(action)) unread[0]++;
                    }
                }
            }
            return null;
        });
        if (markRead && newest[0] > lastRead) {
            db.tx(c -> {
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO notice_reads (user_id, last_audit_id)"
                        + " VALUES (?, ?) ON CONFLICT(user_id) DO UPDATE SET last_audit_id = excluded.last_audit_id")) {
                    ps.setLong(1, adminId);
                    ps.setLong(2, newest[0]);
                    return ps.executeUpdate();
                }
            });
            unread[0] = 0;
        }
        root.put("badge", pending.size() + unread[0]);
        return root;
    }
}
