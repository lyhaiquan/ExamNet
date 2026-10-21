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
import labcast.server.lesson.LessonRepository;
import labcast.server.presence.AppFocus;
import labcast.server.session.Session;

/**
 * Số liệu cho trang dashboard ({@code http://localhost:8080/}): học viên nào đang vào, mạng tốt hay chập
 * chờn, ai đang rời app, ai làm nhiều làm ít, lượt chạy code gần đây, nhật ký sự kiện và log server (mỗi
 * dòng kèm loại theo {@link LogCategory} để lọc). Chỉ đọc, không đổi gì.
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
    /** Mã bài bỏ đuôi ngôn ngữ: {@code dsa.cay.heap-them#cpp} → {@code dsa.cay.heap-them}. */
    private static final String BAI = "(CASE WHEN instr(s.lesson_id, '#') > 0"
            + " THEN substr(s.lesson_id, 1, instr(s.lesson_id, '#') - 1) ELSE s.lesson_id END)";

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
        root.put(
                "baiNopDuoc",
                ctx.find(LessonRepository.class)
                        .map(r -> r.all().stream()
                                .filter(l -> !l.loai().equals("giang-giai"))
                                .count())
                        .orElse(0L));
        Map<Long, ObjectNode> stats = stats();
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
                            n.set("stats", stats.getOrDefault(id, emptyStats()));
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
                            n.put("category", LogCategory.ofAction(rs.getString(3)));
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
                n.put("category", l.category());
                n.put("message", l.message());
            });
        }
        return root.toString();
    }

    /**
     * Học viên làm nhiều hay ít: số lượt chạy, lượt nộp, nộp sai, số bài đã giải (nộp đúng, mỗi bài tính
     * một lần), tổng điểm (điểm cao nhất của mỗi bài đã giải) và lúc chạy hoặc nộp gần nhất. Một câu
     * GROUP BY trên {@code submissions}, có index theo user_id.
     *
     * <p>Bản ngôn ngữ khác của một bài có mã {@code <bài>#cpp}, {@code <bài>#java}…: bỏ đuôi {@code #…}
     * trước khi đếm, giải một bài bằng hai ngôn ngữ vẫn là một bài.
     */
    private Map<Long, ObjectNode> stats() {
        Map<Long, ObjectNode> out = new HashMap<>();
        try {
            ctx.db().read(c -> {
                try (var st = c.prepareStatement("SELECT s.user_id,"
                        + " SUM(s.kind = 'RUN'), SUM(s.kind = 'SUBMIT'), SUM(s.kind = 'SUBMIT' AND s.verdict <> 'OK'),"
                        + " COUNT(DISTINCT CASE WHEN s.kind = 'SUBMIT' AND s.verdict = 'OK' THEN " + BAI + " END),"
                        + " MAX(s.created_at), COALESCE(b.diem, 0)"
                        + " FROM submissions s LEFT JOIN (SELECT user_id, SUM(cao) AS diem FROM (SELECT s.user_id,"
                        + " MAX(s.score) AS cao FROM submissions s WHERE s.kind = 'SUBMIT' AND s.verdict = 'OK'"
                        + " GROUP BY s.user_id, " + BAI + ") GROUP BY user_id) b ON b.user_id = s.user_id"
                        + " GROUP BY s.user_id")) {
                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next()) {
                            ObjectNode n = JsonPayload.MAPPER.createObjectNode();
                            n.put("chay", rs.getInt(2));
                            n.put("nop", rs.getInt(3));
                            n.put("nopSai", rs.getInt(4));
                            n.put("daGiai", rs.getInt(5));
                            n.put("lanCuoi", rs.getLong(6));
                            n.put("diem", rs.getInt(7));
                            out.put(rs.getLong(1), n);
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Dashboard không đọc được thống kê học viên", e);
        }
        return out;
    }

    private static ObjectNode emptyStats() {
        ObjectNode n = JsonPayload.MAPPER.createObjectNode();
        n.put("chay", 0);
        n.put("nop", 0);
        n.put("nopSai", 0);
        n.put("daGiai", 0);
        n.put("lanCuoi", 0L);
        n.put("diem", 0);
        return n;
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
