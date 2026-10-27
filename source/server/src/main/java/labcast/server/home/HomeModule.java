package labcast.server.home;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.Role;
import labcast.server.ServerContext;
import labcast.server.ServerModule;
import labcast.server.net.Ctx;
import labcast.server.net.Priority;
import labcast.server.session.Session;

/**
 * Số liệu trang chủ ({@code HOME_REQ} → {@code HOME_DATA}, JSON), tính theo vai trò người hỏi:
 *
 * <ul>
 *   <li>Chung: {@code days} — 30 ngày gần nhất, mỗi ngày {@code date} (dd/MM), {@code runs}, {@code
 *       submits}; {@code submits30}, {@code submitsPrev30}, {@code runs30}.
 *   <li>Quản trị: {@code users} (total, admins, teachers, students, new30, newPrev30 và theo vai trò),
 *       {@code classesTotal}, {@code lessonsTotal}, {@code submitsTotal}, {@code classes}, {@code
 *       recent}.
 *   <li>Giáo viên: {@code classes} (lớp mình dạy: id, name, teacher, students, completion %), {@code
 *       recent} (bài nộp của học viên lớp mình).
 *   <li>Học viên: {@code streak} (số ngày liên tiếp có chạy/nộp), {@code activeDays30}, {@code
 *       leaderboard} (lớp đang chọn: name, mastered, xp, me, rank), {@code myRank}.
 * </ul>
 *
 * Phạm vi dữ liệu: quản trị cả hệ thống, giáo viên học viên các lớp mình dạy, học viên chính mình.
 */
public final class HomeModule implements ServerModule {
    static final int DAYS = 30;
    private static final long DAY_MS = 86_400_000L;
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");
    private ServerContext ctx;
    private final ZoneId zone;

    public HomeModule() {
        this(ZoneId.systemDefault());
    }

    HomeModule(ZoneId zone) {
        this.zone = zone;
    }

    @Override
    public void install(ServerContext c) {
        ctx = c;
        c.router().on(MessageType.HOME_REQ, this::home);
    }

    private void home(Ctx c, Frame f) throws Exception {
        Session s = c.session();
        long now = System.currentTimeMillis();
        Map<String, Object> out = ctx.db().read(db -> build(db, s, now));
        c.reply(JsonPayload.frame(MessageType.HOME_DATA, out), Priority.NORMAL);
    }

    /** Điều kiện lọc submissions theo vai trò; mọi nhánh nhận đúng một tham số userId. */
    private static String scope(Role role) {
        return switch (role) {
            case ADMIN -> "? > 0";
            case TEACHER -> "s.user_id IN (SELECT e.user_id FROM enrollments e JOIN classes c ON c.id=e.class_id"
                    + " WHERE c.teacher_id=?)";
            case STUDENT -> "s.user_id=?";
        };
    }

    Map<String, Object> build(Connection db, Session s, long now) throws SQLException {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("role", s.role().name());
        long t30 = now - DAYS * DAY_MS;
        long t60 = now - 2 * DAYS * DAY_MS;

        List<long[]> events = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement("SELECT s.created_at, s.kind='SUBMIT' FROM submissions s WHERE "
                + scope(s.role()) + " AND s.created_at >= ? AND s.kind IN ('RUN','SUBMIT')")) {
            ps.setLong(1, s.userId());
            ps.setLong(2, t60);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) events.add(new long[] {rs.getLong(1), rs.getInt(2)});
            }
        }
        out.put("days", days(events, now, zone));
        out.put(
                "submits30",
                events.stream().filter(e -> e[1] == 1 && e[0] >= t30).count());
        out.put(
                "submitsPrev30",
                events.stream().filter(e -> e[1] == 1 && e[0] < t30).count());
        out.put("runs30", events.stream().filter(e -> e[1] == 0 && e[0] >= t30).count());
        int lessons = count(db, "SELECT COUNT(*) FROM lessons");
        out.put("lessonsTotal", lessons);

        switch (s.role()) {
            case ADMIN -> {
                out.put("users", users(db, t30, t60));
                out.put("classesTotal", count(db, "SELECT COUNT(*) FROM classes"));
                out.put("classesNew30", count(db, "SELECT COUNT(*) FROM classes WHERE created_at >= " + t30));
                out.put(
                        "classesNewPrev30",
                        count(
                                db,
                                "SELECT COUNT(*) FROM classes WHERE created_at >= " + t60 + " AND created_at < "
                                        + t30));
                out.put("submitsTotal", count(db, "SELECT COUNT(*) FROM submissions WHERE kind='SUBMIT'"));
                out.put("classes", classes(db, s, lessons));
                out.put("recent", recent(db, s, true));
            }
            case TEACHER -> {
                out.put("classes", classes(db, s, lessons));
                out.put("recent", recent(db, s, false));
            }
            case STUDENT -> {
                Set<LocalDate> active = new HashSet<>();
                for (long[] e : events) active.add(day(e[0], zone));
                LocalDate today = day(now, zone);
                out.put("streak", streak(active, today));
                out.put(
                        "activeDays30",
                        active.stream()
                                .filter(d -> d.isAfter(today.minusDays(DAYS)))
                                .count());
                leaderboard(db, s, out);
            }
        }
        return out;
    }

    private static LocalDate day(long ms, ZoneId zone) {
        return Instant.ofEpochMilli(ms).atZone(zone).toLocalDate();
    }

    /** 30 ngày gần nhất, cũ trước; mỗi ngày đếm lượt chạy và lượt nộp. */
    static List<Map<String, Object>> days(List<long[]> events, long now, ZoneId zone) {
        LocalDate today = day(now, zone);
        Map<LocalDate, int[]> byDay = new LinkedHashMap<>();
        for (int i = DAYS - 1; i >= 0; i--) byDay.put(today.minusDays(i), new int[2]);
        for (long[] e : events) {
            int[] c = byDay.get(day(e[0], zone));
            if (c != null) c[e[1] == 1 ? 1 : 0]++;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        byDay.forEach((d, c) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", d.format(DAY_LABEL));
            m.put("runs", c[0]);
            m.put("submits", c[1]);
            out.add(m);
        });
        return out;
    }

    /** Số ngày liên tiếp có hoạt động, tính lùi từ hôm nay; hôm nay chưa làm thì tính từ hôm qua. */
    static int streak(Set<LocalDate> active, LocalDate today) {
        LocalDate d = active.contains(today) ? today : today.minusDays(1);
        int n = 0;
        while (active.contains(d)) {
            n++;
            d = d.minusDays(1);
        }
        return n;
    }

    private static int count(Connection db, String sql) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static Map<String, Object> users(Connection db, long t30, long t60) throws SQLException {
        Map<String, Object> u = new LinkedHashMap<>();
        int total = 0;
        int n30 = 0;
        int nPrev = 0;
        for (Role r : Role.values()) {
            u.put(key(r), 0);
            u.put(key(r) + "New30", 0);
            u.put(key(r) + "NewPrev30", 0);
        }
        try (PreparedStatement ps = db.prepareStatement("SELECT role, COUNT(*), SUM(created_at >= ?),"
                + " SUM(created_at >= ? AND created_at < ?) FROM users GROUP BY role")) {
            ps.setLong(1, t30);
            ps.setLong(2, t60);
            ps.setLong(3, t30);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String k = key(Role.valueOf(rs.getString(1)));
                    u.put(k, rs.getInt(2));
                    u.put(k + "New30", rs.getInt(3));
                    u.put(k + "NewPrev30", rs.getInt(4));
                    total += rs.getInt(2);
                    n30 += rs.getInt(3);
                    nPrev += rs.getInt(4);
                }
            }
        }
        u.put("total", total);
        u.put("new30", n30);
        u.put("newPrev30", nPrev);
        return u;
    }

    private static String key(Role r) {
        return switch (r) {
            case ADMIN -> "admins";
            case TEACHER -> "teachers";
            case STUDENT -> "students";
        };
    }

    /** Lớp quản trị thấy (mọi lớp) hoặc giáo viên dạy, kèm sĩ số và tỉ lệ bài đạt. */
    private static List<Map<String, Object>> classes(Connection db, Session s, int lessons) throws SQLException {
        String where = s.role() == Role.ADMIN ? "? > 0" : "c.teacher_id = ?";
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement("SELECT c.id, c.name, u.full_name,"
                + " (SELECT COUNT(*) FROM enrollments e JOIN users st ON st.id=e.user_id"
                + "  WHERE e.class_id=c.id AND st.role='STUDENT'),"
                + " (SELECT COUNT(*) FROM progress p JOIN enrollments e ON e.user_id=p.user_id"
                + "  WHERE e.class_id=c.id AND p.status='MASTERED')"
                + " FROM classes c JOIN users u ON u.id=c.teacher_id WHERE " + where
                + " ORDER BY c.created_at DESC")) {
            ps.setLong(1, s.userId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int students = rs.getInt(4);
                    int mastered = rs.getInt(5);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong(1));
                    m.put("name", rs.getString(2));
                    m.put("teacher", rs.getString(3));
                    m.put("students", students);
                    m.put(
                            "completion",
                            students == 0 || lessons == 0
                                    ? 0
                                    : Math.round(mastered * 100.0 / ((long) students * lessons)));
                    list.add(m);
                }
            }
        }
        return list;
    }

    /** Hoạt động gần đây: bài nộp trong phạm vi; quản trị thêm tài khoản mới và lớp mới. */
    private static List<Map<String, Object>> recent(Connection db, Session s, boolean admin) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement("SELECT u.full_name, l.ten, s.verdict, s.score, s.created_at"
                + " FROM submissions s JOIN users u ON u.id=s.user_id JOIN lessons l ON l.id=s.lesson_id WHERE "
                + scope(s.role()) + " AND s.kind='SUBMIT' ORDER BY s.created_at DESC LIMIT 8")) {
            ps.setLong(1, s.userId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    boolean ok = "OK".equals(rs.getString(3));
                    list.add(event(
                            ok ? "submit-ok" : "submit-wrong",
                            rs.getString(1),
                            (ok ? "nộp đạt bài " : "nộp chưa đạt bài ") + rs.getString(2),
                            rs.getLong(5)));
                }
            }
        }
        if (admin) {
            try (PreparedStatement ps = db.prepareStatement(
                            "SELECT full_name, role, created_at FROM users ORDER BY created_at DESC LIMIT 5");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String role =
                            switch (Role.valueOf(rs.getString(2))) {
                                case ADMIN -> "quản trị";
                                case TEACHER -> "giáo viên";
                                case STUDENT -> "học viên";
                            };
                    list.add(event("user", rs.getString(1), "có tài khoản " + role, rs.getLong(3)));
                }
            }
            try (PreparedStatement ps = db.prepareStatement("SELECT c.name, u.full_name, c.created_at FROM classes c"
                            + " JOIN users u ON u.id=c.teacher_id ORDER BY c.created_at DESC LIMIT 5");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(event("class", rs.getString(2), "phụ trách lớp mới " + rs.getString(1), rs.getLong(3)));
                }
            }
        }
        list.sort(Comparator.comparingLong((Map<String, Object> m) -> (Long) m.get("at"))
                .reversed());
        return list.subList(0, Math.min(8, list.size()));
    }

    private static Map<String, Object> event(String kind, String who, String text, long at) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kind", kind);
        m.put("who", who);
        m.put("text", text);
        m.put("at", at);
        return m;
    }

    /** Bảng xếp hạng lớp đang chọn: nhiều bài đạt trước, bằng nhau thì tổng điểm. */
    private static void leaderboard(Connection db, Session s, Map<String, Object> out) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement("SELECT u.id, u.full_name,"
                + " (SELECT COUNT(*) FROM progress p WHERE p.user_id=u.id AND p.status='MASTERED') AS m,"
                + " (SELECT COALESCE(SUM(p.score),0) FROM progress p WHERE p.user_id=u.id AND p.status='MASTERED') AS x"
                + " FROM enrollments e JOIN users u ON u.id=e.user_id WHERE e.class_id=? AND u.role='STUDENT'"
                + " ORDER BY m DESC, x DESC, u.full_name")) {
            ps.setLong(1, s.classId());
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 0;
                while (rs.next()) {
                    rank++;
                    boolean me = rs.getLong(1) == s.userId();
                    if (me) out.put("myRank", rank);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("rank", rank);
                    m.put("name", rs.getString(2));
                    m.put("mastered", rs.getInt(3));
                    m.put("xp", rs.getInt(4));
                    m.put("me", me);
                    rows.add(m);
                }
            }
        }
        out.put("classSize", rows.size());
        out.put("leaderboard", rows.subList(0, Math.min(5, rows.size())));
        out.putIfAbsent("myRank", 0);
    }
}
