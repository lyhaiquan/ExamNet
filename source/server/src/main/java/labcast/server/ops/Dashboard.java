package labcast.server.ops;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.Role;
import labcast.server.ServerContext;
import labcast.server.cast.CastModule;
import labcast.server.classroom.ClassEvents;
import labcast.server.presence.AppFocus;
import labcast.server.session.Session;

/**
 * Số liệu cho trang dashboard ({@code http://localhost:9000/}): học viên nào đang vào, mạng tốt hay chập
 * chờn, ai đang rời app, lượt chạy code gần đây, nhật ký sự kiện và log server. Chỉ đọc, không đổi gì.
 *
 * <p>KHÔNG đưa token phiên hay mật khẩu băm vào JSON. Mỗi bảng lấy tối đa vài chục dòng mới nhất để
 * trang hỏi 2 giây một lần cũng không làm nặng database (một kết nối SQLite dùng chung).
 */
public final class Dashboard {
    private static final Logger LOG = Logger.getLogger(Dashboard.class.getName());
    static final int RUNS = 40;
    static final int AUDIT = 60;
    static final int LOGS = 120;
    static final int STUDENTS = 500;

    private final ServerContext ctx;
    private final BuildInfo info;
    private final LogBuffer logs;
    private final int tcpPort;

    public Dashboard(ServerContext ctx, BuildInfo info, LogBuffer logs, int tcpPort) {
        this.ctx = ctx;
        this.info = info;
        this.logs = logs;
        this.tcpPort = tcpPort;
    }

    public String json() {
        long now = System.currentTimeMillis();
        ObjectNode root = JsonPayload.MAPPER.createObjectNode();
        ObjectNode server = root.putObject("server");
        server.put("version", info.version());
        server.put("commit", info.commit());
        server.put("startedAt", info.startedAt().toEpochMilli());
        server.put("now", now);
        server.put("lop", ctx.config().lop());
        server.put("mon", ctx.config().mon() == null ? "" : ctx.config().mon().toUpperCase());
        server.put("tcpPort", tcpPort);
        server.put("sandbox", ctx.config().khongSandbox() ? "Python trên máy server (--khong-sandbox)" : "Docker");

        ctx.find(CastModule.class).ifPresent(c -> root.set("cast", JsonPayload.MAPPER.valueToTree(c.status())));

        Map<Long, Session> sessions = new HashMap<>();
        for (Session s : ctx.sessions().all()) {
            sessions.put(s.userId(), s);
        }
        Optional<AppFocus> focus = ctx.find(AppFocus.class);
        ArrayNode students = root.putArray("students");
        try {
            ctx.db().read(c -> {
                try (var st = c.prepareStatement(
                        "SELECT id, username, full_name FROM users WHERE role = 'STUDENT' ORDER BY username LIMIT ?")) {
                    st.setInt(1, STUDENTS);
                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next()) {
                            long id = rs.getLong(1);
                            ObjectNode n = students.addObject();
                            n.put("userId", id);
                            n.put("username", rs.getString(2));
                            n.put("fullName", rs.getString(3));
                            student(n, id, sessions.get(id), focus, now);
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Dashboard không đọc được danh sách học viên", e);
        }
        ArrayNode teachers = root.putArray("teachersOnline");
        for (Session s : sessions.values()) {
            if (s.role() == Role.TEACHER && s.online()) {
                teachers.add(s.fullName());
            }
        }

        ArrayNode runs = root.putArray("runs");
        ArrayNode audit = root.putArray("audit");
        try {
            ctx.db().read(c -> {
                try (var st = c.prepareStatement("SELECT s.created_at, u.username, u.full_name, s.lesson_id, s.kind,"
                        + " s.verdict FROM submissions s JOIN users u ON u.id = s.user_id"
                        + " ORDER BY s.id DESC LIMIT ?")) {
                    st.setInt(1, RUNS);
                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next()) {
                            ObjectNode n = runs.addObject();
                            n.put("ts", rs.getLong(1));
                            n.put("username", rs.getString(2));
                            n.put("fullName", rs.getString(3));
                            n.put("lessonId", rs.getString(4));
                            n.put("kind", rs.getString(5));
                            n.put("verdict", rs.getString(6));
                        }
                    }
                }
                try (var st = c.prepareStatement(
                        "SELECT ts, actor, action, detail FROM audit_log ORDER BY id DESC LIMIT ?")) {
                    st.setInt(1, AUDIT);
                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next()) {
                            ObjectNode n = audit.addObject();
                            n.put("ts", rs.getLong(1));
                            n.put("actor", rs.getString(2));
                            n.put("action", rs.getString(3));
                            n.put("detail", rs.getString(4));
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Dashboard không đọc được lượt chạy / audit_log", e);
        }

        ArrayNode log = root.putArray("logs");
        if (logs != null) {
            logs.recent().stream().limit(LOGS).forEach(l -> {
                ObjectNode n = log.addObject();
                n.put("ts", l.ts());
                n.put("level", l.level());
                n.put("logger", l.logger());
                n.put("message", l.message());
            });
        }
        return root.toString();
    }

    private void student(ObjectNode n, long id, Session s, Optional<AppFocus> focus, long now) {
        n.put("loggedIn", s != null);
        n.put("online", s != null && s.online());
        n.put("presence", s == null ? "CHUA_VAO" : ctx.presence().state(id).name());
        long srtt = s == null ? -1 : ctx.presence().srtt(id);
        if (srtt >= 0) {
            n.put("srttMs", srtt);
        } else {
            n.putNull("srttMs");
        }
        Optional<AppFocus.State> f = focus.flatMap(a -> a.state(id));
        n.put("inApp", f.map(AppFocus.State::inApp).orElse(true));
        n.put("leaves", f.map(AppFocus.State::leaves).orElse(0));
        n.put("awayMs", f.map(st -> st.awayMs(now)).orElse(0L));
        n.put("lessonId", f.map(AppFocus.State::lessonId).orElse(""));
        if (f.isPresent() && !f.get().inApp()) {
            n.put("leftAt", f.get().since());
        }
        ArrayNode pastes = n.putArray("pasteFlags");
        ctx.find(ClassEvents.class).ifPresent(e -> {
            for (ClassEvents.PasteFlag p : e.pasteFlags(id)) {
                pastes.addObject()
                        .put("lessonId", p.lessonId())
                        .put("times", p.times())
                        .put("chars", p.chars())
                        .put("detail", p.detail())
                        .put("at", p.at());
            }
        });
    }
}
