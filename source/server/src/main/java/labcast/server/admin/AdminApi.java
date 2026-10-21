package labcast.server.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.Role;
import labcast.server.ServerContext;
import labcast.server.account.RosterImporter;
import labcast.server.account.UserRepository;
import labcast.server.classroom.ClassDirectory.ClassInfo;
import labcast.server.classroom.ClassEvents;
import labcast.server.classroom.ClassRepository;
import labcast.server.presence.AppFocus;
import labcast.server.presence.PresenceState;
import labcast.server.session.Session;

/**
 * API JSON của trang quản trị ({@code http://localhost:8080/admin}): ai đang online theo lớp, quản lý lớp,
 * học viên, giáo viên. Chủ: A.
 *
 * <p>Không biết gì về HTTP: {@link labcast.server.ops.StatusEndpoint} lo cổng, chỉ cho máy server vào,
 * rồi chuyển {@code method, path, query, body} vào {@link #handle}. Nhờ vậy test gọi thẳng được.
 *
 * <p>KHÔNG trả mật khẩu băm hay token phiên. Mỗi thao tác sửa ghi một dòng {@code audit_log} loại
 * {@code ADMIN_*}, actor {@code localhost}.
 */
public final class AdminApi {
    private static final Logger LOG = Logger.getLogger(AdminApi.class.getName());
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._@-]{1,64}");
    static final int MIN_PASSWORD = 6;
    static final int MAX_NAME = 100;
    static final String ACTOR = "localhost";

    /** Kết quả trả cho trình duyệt. */
    public record Reply(int status, String json) {
        static Reply ok(JsonNode n) {
            return new Reply(200, n.toString());
        }

        static Reply error(int status, String message) {
            return new Reply(
                    status,
                    JsonPayload.MAPPER.createObjectNode().put("loi", message).toString());
        }
    }

    /** Lỗi do dữ liệu gửi lên; trả 400 kèm thông báo cho người dùng. */
    static final class BadRequest extends RuntimeException {
        final int status;

        BadRequest(String message) {
            this(400, message);
        }

        BadRequest(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    private final ServerContext ctx;
    private final UserRepository users;
    private final ClassRepository classes;
    private final LongSupplier clock;
    private final ZoneId zone;

    public AdminApi(ServerContext ctx, LongSupplier clock, ZoneId zone) {
        this.ctx = ctx;
        this.users = ctx.require(UserRepository.class);
        this.classes = ctx.require(ClassRepository.class);
        this.clock = clock;
        this.zone = zone;
    }

    public AdminApi(ServerContext ctx) {
        this(ctx, System::currentTimeMillis, ZoneId.systemDefault());
    }

    /**
     * @param path phần sau {@code /api/admin/}, ví dụ {@code users} hoặc {@code classes/delete}
     * @param query tham số trên URL đã giải mã
     * @param body thân yêu cầu (JSON) với POST, rỗng với GET
     */
    public Reply handle(String method, String path, Map<String, String> query, String body) {
        try {
            boolean get = method.equals("GET") || method.equals("HEAD");
            boolean post = method.equals("POST");
            JsonNode in = post ? parse(body) : null;
            return switch (path) {
                case "overview" -> get ? Reply.ok(overview()) : notAllowed();
                case "classes" -> get ? Reply.ok(classList()) : post ? Reply.ok(saveClass(in)) : notAllowed();
                case "classes/delete" -> post ? Reply.ok(deleteClass(in)) : notAllowed();
                case "users" -> get ? Reply.ok(userList(query)) : post ? Reply.ok(saveUser(in)) : notAllowed();
                case "users/delete" -> post ? Reply.ok(deleteUser(in)) : notAllowed();
                case "users/kick" -> post ? Reply.ok(kick(in)) : notAllowed();
                case "import" -> post ? Reply.ok(importCsv(in)) : notAllowed();
                default -> Reply.error(404, "Không có API " + path);
            };
        } catch (BadRequest e) {
            return Reply.error(e.status, e.getMessage());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Lỗi API quản trị " + method + " " + path, e);
            return Reply.error(500, "Lỗi server: " + e.getMessage());
        }
    }

    private static Reply notAllowed() {
        return Reply.error(405, "Sai phương thức");
    }

    // ---------------------------------------------------------------- tổng quan

    ObjectNode overview() throws SQLException {
        long now = clock.getAsLong();
        Map<Long, Session> sessions = sessionsByUser();
        List<UserRepository.Row> students = users.list(Role.STUDENT);
        Map<Long, String> names = new HashMap<>();
        users.list(null).forEach(u -> names.put(u.id(), u.fullName()));
        Map<Long, List<String>> classNamesOf = classNamesByStudent();

        ObjectNode root = JsonPayload.MAPPER.createObjectNode();
        root.put("now", now);
        root.put("mon", ctx.config().mon() == null ? "" : ctx.config().mon().toUpperCase());
        root.put("tong", students.size());
        root.put(
                "online",
                students.stream().filter(u -> online(sessions.get(u.id()))).count());
        root.put(
                "chuaXepLop",
                students.stream().filter(u -> !classNamesOf.containsKey(u.id())).count());
        root.put("soGiaoVien", names.size() - students.size());
        ArrayNode gvOnline = root.putArray("giaoVienOnline");
        sessions.values().stream()
                .filter(s -> s.role() == Role.TEACHER && s.online())
                .forEach(s -> gvOnline.add(s.fullName()));

        Map<Long, String> caHomNay = todaySchedule(now);
        Map<Long, int[]> giao = openAssignments(now);
        ArrayNode lop = root.putArray("lop");
        int baiDangMo = 0;
        int kiemTraDangMo = 0;
        for (ClassInfo c : classes.all()) {
            ObjectNode n = lop.addObject();
            n.put("id", c.id());
            n.put("ten", c.ten());
            n.put("siSo", c.siSo());
            n.put(
                    "online",
                    classes.studentsOf(c.id()).stream()
                            .filter(id -> online(sessions.get(id)))
                            .count());
            ArrayNode gv = n.putArray("giaoVien");
            classes.teachersOf(c.id()).forEach(id -> gv.add(names.getOrDefault(id, "#" + id)));
            n.put("ca", caHomNay.getOrDefault(c.id(), ""));
            int[] g = giao.getOrDefault(c.id(), new int[2]);
            n.put("thucHanh", g[0]);
            n.put("kiemTra", g[1]);
            baiDangMo += g[0] + g[1];
            kiemTraDangMo += g[1];
        }
        root.put("baiDangMo", baiDangMo);
        root.put("kiemTraDangMo", kiemTraDangMo);
        root.put("luotNopHomNay", submitsToday(now));
        root.set("canChuY", attention(students, sessions, classNamesOf, now));
        return root;
    }

    /** Học viên đang online mà rời app, mạng chập chờn hoặc bị đánh dấu dán khối lớn. */
    private ArrayNode attention(
            List<UserRepository.Row> students,
            Map<Long, Session> sessions,
            Map<Long, List<String>> classNamesOf,
            long now) {
        ArrayNode out = JsonPayload.MAPPER.createArrayNode();
        Optional<AppFocus> focus = ctx.find(AppFocus.class);
        Optional<ClassEvents> events = ctx.find(ClassEvents.class);
        for (UserRepository.Row u : students) {
            Session s = sessions.get(u.id());
            if (s == null) {
                continue;
            }
            Optional<AppFocus.State> f = focus.flatMap(a -> a.state(u.id()));
            List<ClassEvents.PasteFlag> pastes =
                    events.map(e -> e.pasteFlags(u.id())).orElse(List.of());
            String state;
            String detail;
            if (f.isPresent() && !f.get().inApp()) {
                state = "ROI_APP";
                detail = f.get().leaves() + " lần · " + (f.get().awayMs(now) / 1000) + " s";
            } else if (s.online() && ctx.presence().state(u.id()) == PresenceState.SUSPECT) {
                state = "CHAP_CHON";
                long srtt = ctx.presence().srtt(u.id());
                detail = srtt >= 0 ? "RTT " + srtt + " ms" : "";
            } else if (!pastes.isEmpty()) {
                state = "DAN_KHOI";
                detail = pastes.get(0).times() + " lần · " + pastes.get(0).chars() + " ký tự";
            } else {
                continue;
            }
            ObjectNode n = out.addObject();
            n.put("id", u.id());
            n.put("username", u.username());
            n.put("ten", u.fullName());
            n.put("lop", String.join(", ", classNamesOf.getOrDefault(u.id(), List.of())));
            n.put("trangThai", state);
            n.put("chiTiet", detail);
            n.put("lessonId", f.map(AppFocus.State::lessonId).orElse(""));
        }
        return out;
    }

    /** Ca học hôm nay của từng lớp, dạng "07:00–09:30, 13:30–16:00". */
    private Map<Long, String> todaySchedule(long now) throws SQLException {
        int weekday = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), zone)
                .getDayOfWeek()
                .getValue();
        Map<Long, String> out = new HashMap<>();
        ctx.db().read(c -> {
            try (var ps = c.prepareStatement("SELECT class_id, start_min, end_min FROM class_schedule"
                    + " WHERE weekday = ? ORDER BY class_id, start_min")) {
                ps.setInt(1, weekday);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String ca = hhmm(rs.getInt(2)) + "–" + hhmm(rs.getInt(3));
                        out.merge(rs.getLong(1), ca, (a, b) -> a + ", " + b);
                    }
                }
            }
            return null;
        });
        return out;
    }

    static String hhmm(int minutes) {
        return "%02d:%02d".formatted(minutes / 60, minutes % 60);
    }

    /** Số bài giao đang mở của từng lớp: [thực hành, kiểm tra]. */
    private Map<Long, int[]> openAssignments(long now) throws SQLException {
        Map<Long, int[]> out = new HashMap<>();
        ctx.db().read(c -> {
            try (var ps = c.prepareStatement("SELECT class_id, loai, count(*) FROM assignments"
                    + " WHERE active = 1 AND opens_at <= ? AND closes_at > ? GROUP BY class_id, loai")) {
                ps.setLong(1, now);
                ps.setLong(2, now);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int[] g = out.computeIfAbsent(rs.getLong(1), k -> new int[2]);
                        g["kiem-tra".equals(rs.getString(2)) ? 1 : 0] += rs.getInt(3);
                    }
                }
            }
            return null;
        });
        return out;
    }

    private long submitsToday(long now) throws SQLException {
        long start = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), zone)
                .toLocalDate()
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli();
        return ctx.db().read(c -> {
            try (var ps =
                    c.prepareStatement("SELECT count(*) FROM submissions WHERE kind = 'SUBMIT' AND created_at >= ?")) {
                ps.setLong(1, start);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0L;
                }
            }
        });
    }

    // ---------------------------------------------------------------- lớp

    ArrayNode classList() throws SQLException {
        Map<Long, Session> sessions = sessionsByUser();
        Map<Long, UserRepository.Row> byId = new HashMap<>();
        users.list(null).forEach(u -> byId.put(u.id(), u));
        ArrayNode out = JsonPayload.MAPPER.createArrayNode();
        for (ClassInfo c : classes.all()) {
            ObjectNode n = out.addObject();
            n.put("id", c.id());
            n.put("ten", c.ten());
            n.put("siSo", c.siSo());
            n.put(
                    "online",
                    classes.studentsOf(c.id()).stream()
                            .filter(id -> online(sessions.get(id)))
                            .count());
            ArrayNode gv = n.putArray("giaoVien");
            for (long id : classes.teachersOf(c.id())) {
                UserRepository.Row u = byId.get(id);
                gv.addObject().put("id", id).put("ten", u == null ? "#" + id : u.fullName());
            }
        }
        return out;
    }

    /** {@code {"id":null|id,"ten","giaoVien":[id…]}}. */
    ObjectNode saveClass(JsonNode in) throws SQLException {
        long id = in.path("id").asLong(0);
        String ten = text(in, "ten", 40, "Tên lớp");
        if (classes.nameTaken(ten, id)) {
            throw new BadRequest(409, "Đã có lớp tên " + ten);
        }
        List<Long> gv = idList(in.path("giaoVien"));
        for (long t : gv) {
            requireRole(t, Role.TEACHER);
        }
        if (id == 0) {
            long creator = gv.isEmpty()
                    ? users.firstTeacher()
                            .orElseThrow(() -> new BadRequest("Chưa có giáo viên nào: tạo giáo viên trước"))
                            .id()
                    : gv.get(0);
            id = classes.create(ten, creator, gv);
            ctx.audit().record(ACTOR, "ADMIN_CLASS_CREATE", ten + " (id " + id + ")");
        } else {
            if (classes.find(id).isEmpty()) {
                throw new BadRequest(404, "Không có lớp " + id);
            }
            classes.update(id, ten, gv);
            ctx.audit().record(ACTOR, "ADMIN_CLASS_UPDATE", ten + " (id " + id + ")");
        }
        return JsonPayload.MAPPER.createObjectNode().put("ok", true).put("id", id);
    }

    ObjectNode deleteClass(JsonNode in) throws SQLException {
        long id = in.path("id").asLong(0);
        ClassInfo c = classes.find(id).orElseThrow(() -> new BadRequest(404, "Không có lớp " + id));
        try {
            classes.delete(id);
        } catch (SQLException e) {
            throw new BadRequest(409, "Lớp " + c.ten() + " đã có câu hỏi nhanh, không xoá được");
        }
        ctx.audit().record(ACTOR, "ADMIN_CLASS_DELETE", c.ten() + " (id " + id + ", " + c.siSo() + " học viên)");
        return JsonPayload.MAPPER.createObjectNode().put("ok", true);
    }

    // ---------------------------------------------------------------- người dùng

    /** {@code ?vaiTro=STUDENT|TEACHER&lop=id}; lop=0 là học viên chưa xếp lớp. */
    ArrayNode userList(Map<String, String> query) throws SQLException {
        Role role = parseRole(query.get("vaiTro"), true);
        String lopParam = query.get("lop");
        Long lop = lopParam == null || lopParam.isBlank() ? null : parseLong(lopParam, "lop");
        Map<Long, Session> sessions = sessionsByUser();
        Map<Long, Long> lastSubmit = lastSubmissions();
        ArrayNode out = JsonPayload.MAPPER.createArrayNode();
        for (UserRepository.Row u : users.list(role)) {
            List<ClassInfo> cls = classes.classesOf(u.id());
            if (lop != null && (lop == 0 ? !cls.isEmpty() : cls.stream().noneMatch(c -> c.id() == lop))) {
                continue;
            }
            ObjectNode n = out.addObject();
            n.put("id", u.id());
            n.put("username", u.username());
            n.put("ten", u.fullName());
            n.put("vaiTro", u.role().name());
            ArrayNode l = n.putArray("lop");
            cls.forEach(c -> l.addObject().put("id", c.id()).put("ten", c.ten()));
            n.put("online", online(sessions.get(u.id())));
            Long last = lastSubmit.get(u.id());
            if (last == null) {
                n.putNull("lanCuoi");
            } else {
                n.put("lanCuoi", last);
            }
            n.put("taoLuc", u.createdAt());
        }
        return out;
    }

    /**
     * {@code {"id":null|id,"username","ten","vaiTro","matKhau":null|"…","lop":[id…]}}. Tạo mới phải có mật
     * khẩu; sửa thì mật khẩu rỗng là giữ nguyên. Vai trò không đổi được sau khi tạo. Đổi mật khẩu hoặc lớp
     * thì phiên đang mở bị đóng để đăng nhập lại nhận lớp mới.
     */
    ObjectNode saveUser(JsonNode in) throws SQLException {
        long id = in.path("id").asLong(0);
        String username = in.path("username").asText("").strip();
        if (!USERNAME.matcher(username).matches()) {
            throw new BadRequest("Tên đăng nhập chỉ gồm chữ không dấu, số và . _ @ -, tối đa 64 ký tự");
        }
        String ten = text(in, "ten", MAX_NAME, "Họ tên");
        String matKhau = in.path("matKhau").asText("");
        if (!matKhau.isEmpty() && matKhau.length() < MIN_PASSWORD) {
            throw new BadRequest("Mật khẩu ít nhất " + MIN_PASSWORD + " ký tự");
        }
        List<Long> lop = idList(in.path("lop"));
        for (long c : lop) {
            if (classes.find(c).isEmpty()) {
                throw new BadRequest(404, "Không có lớp " + c);
            }
        }
        Optional<labcast.server.account.User> sameName = users.byUsername(username);
        if (sameName.isPresent() && sameName.get().id() != id) {
            throw new BadRequest(409, "Tên đăng nhập " + username + " đã có người dùng");
        }

        Role role;
        boolean kick;
        if (id == 0) {
            role = parseRole(in.path("vaiTro").asText(null), false);
            if (matKhau.isEmpty()) {
                throw new BadRequest("Tài khoản mới phải có mật khẩu");
            }
            id = users.create(username, ten, role, matKhau);
            kick = false;
            ctx.audit().record(ACTOR, "ADMIN_USER_CREATE", username + " (" + role + ")");
        } else {
            labcast.server.account.User old =
                    users.byId(id).orElseThrow(() -> new BadRequest(404, "Không có người dùng này"));
            role = old.role();
            List<Long> oldClasses =
                    classes.classesOf(id).stream().map(ClassInfo::id).toList();
            kick = !matKhau.isEmpty()
                    || !old.username().equals(username)
                    || !new java.util.HashSet<>(oldClasses).equals(new java.util.HashSet<>(lop));
            users.update(id, username, ten);
            if (!matKhau.isEmpty()) {
                users.setPassword(id, matKhau);
            }
            ctx.audit().record(ACTOR, "ADMIN_USER_UPDATE", username + (matKhau.isEmpty() ? "" : ", đổi mật khẩu"));
        }
        if (role == Role.STUDENT) {
            classes.setStudentClasses(id, lop);
        } else {
            classes.setTeacherClasses(id, lop);
        }
        boolean kicked = kick && ctx.sessions().kick(id);
        return JsonPayload.MAPPER
                .createObjectNode()
                .put("ok", true)
                .put("id", id)
                .put("dangXuat", kicked);
    }

    ObjectNode deleteUser(JsonNode in) throws SQLException {
        long id = in.path("id").asLong(0);
        labcast.server.account.User u =
                users.byId(id).orElseThrow(() -> new BadRequest(404, "Không có người dùng này"));
        ctx.sessions().kick(id);
        try {
            users.delete(id);
        } catch (SQLException e) {
            throw new BadRequest(
                    409,
                    u.fullName() + " đã có bài làm hoặc là giáo viên duy nhất, không xoá được."
                            + " Muốn chặn đăng nhập thì đổi mật khẩu.");
        }
        ctx.audit().record(ACTOR, "ADMIN_USER_DELETE", u.username());
        return JsonPayload.MAPPER.createObjectNode().put("ok", true);
    }

    ObjectNode kick(JsonNode in) throws SQLException {
        long id = in.path("id").asLong(0);
        labcast.server.account.User u =
                users.byId(id).orElseThrow(() -> new BadRequest(404, "Không có người dùng này"));
        boolean kicked = ctx.sessions().kick(id);
        ctx.audit().record(ACTOR, "ADMIN_USER_KICK", u.username());
        return JsonPayload.MAPPER.createObjectNode().put("ok", true).put("dangXuat", kicked);
    }

    /** {@code {"lop":id|0,"csv":"username,họ tên,mật khẩu\n…"}}: tạo học viên hàng loạt. */
    ObjectNode importCsv(JsonNode in) throws SQLException {
        long lop = in.path("lop").asLong(0);
        if (lop != 0 && classes.find(lop).isEmpty()) {
            throw new BadRequest(404, "Không có lớp " + lop);
        }
        String csv = in.path("csv").asText("");
        if (csv.isBlank()) {
            throw new BadRequest("File CSV rỗng");
        }
        RosterImporter.Result r = new RosterImporter(users, classes).importCsv(csv, lop);
        ctx.audit().record(ACTOR, "ADMIN_ROSTER_IMPORT", r.message());
        ObjectNode out = JsonPayload.MAPPER.createObjectNode().put("ok", true).put("taoMoi", r.created());
        ArrayNode skipped = out.putArray("boQua");
        r.skipped().forEach(skipped::add);
        out.put("thongBao", r.message());
        return out;
    }

    // ---------------------------------------------------------------- tiện ích

    private Map<Long, Session> sessionsByUser() {
        Map<Long, Session> out = new HashMap<>();
        for (Session s : ctx.sessions().all()) {
            out.put(s.userId(), s);
        }
        return out;
    }

    private static boolean online(Session s) {
        return s != null && s.online();
    }

    private Map<Long, List<String>> classNamesByStudent() throws SQLException {
        Map<Long, List<String>> out = new HashMap<>();
        ctx.db().read(c -> {
            try (var ps = c.prepareStatement(
                            "SELECT e.user_id, c.name FROM enrollments e JOIN classes c ON c.id = e.class_id"
                                    + " ORDER BY c.name");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>()).add(rs.getString(2));
                }
            }
            return null;
        });
        return out;
    }

    private Map<Long, Long> lastSubmissions() throws SQLException {
        Map<Long, Long> out = new HashMap<>();
        ctx.db().read(c -> {
            try (var ps = c.prepareStatement("SELECT user_id, max(created_at) FROM submissions GROUP BY user_id");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getLong(1), rs.getLong(2));
                }
            }
            return null;
        });
        return out;
    }

    private void requireRole(long userId, Role role) throws SQLException {
        labcast.server.account.User u =
                users.byId(userId).orElseThrow(() -> new BadRequest(404, "Không có người dùng " + userId));
        if (u.role() != role) {
            throw new BadRequest(u.fullName() + " không phải " + (role == Role.TEACHER ? "giáo viên" : "học viên"));
        }
    }

    private static JsonNode parse(String body) {
        if (body == null || body.isBlank()) {
            throw new BadRequest("Thiếu dữ liệu JSON");
        }
        try {
            JsonNode n = JsonPayload.MAPPER.readTree(body);
            if (n == null || !n.isObject()) {
                throw new BadRequest("Dữ liệu phải là một object JSON");
            }
            return n;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new BadRequest("JSON hỏng");
        }
    }

    private static String text(JsonNode in, String field, int max, String label) {
        String s = in.path(field).asText("").strip();
        if (s.isEmpty()) {
            throw new BadRequest(label + " không được trống");
        }
        if (s.length() > max) {
            throw new BadRequest(label + " dài quá " + max + " ký tự");
        }
        return s;
    }

    private static List<Long> idList(JsonNode arr) {
        List<Long> out = new ArrayList<>();
        if (arr == null || arr.isMissingNode() || arr.isNull()) {
            return out;
        }
        if (!arr.isArray()) {
            throw new BadRequest("Danh sách id phải là mảng");
        }
        for (JsonNode n : arr) {
            if (!n.canConvertToLong()) {
                throw new BadRequest("id phải là số");
            }
            out.add(n.asLong());
        }
        return out;
    }

    private static Role parseRole(String s, boolean nullable) {
        if (s == null || s.isBlank()) {
            if (nullable) {
                return null;
            }
            throw new BadRequest("Thiếu vai trò");
        }
        try {
            return Role.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw new BadRequest("Vai trò phải là TEACHER hoặc STUDENT");
        }
    }

    private static long parseLong(String s, String name) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new BadRequest(name + " phải là số");
        }
    }
}
