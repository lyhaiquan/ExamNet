package labcast.server.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.ProtocolException;
import labcast.common.protocol.Role;
import labcast.server.ServerContext;
import labcast.server.ServerModule;
import labcast.server.classroom.ClassRepository;
import labcast.server.net.Ctx;
import labcast.server.net.Priority;
import labcast.server.session.Session;

/**
 * Quản lý tài khoản, chỉ quản trị: xem mọi tài khoản, tạo giáo viên và học viên, sửa họ tên, chuyển lớp, khoá
 * hoặc mở khoá. Tài khoản quản trị vẫn chỉ tạo bằng {@code tao-admin} trong file cấu hình server.
 */
public final class AccountAdminModule implements ServerModule {
    /** Không có {@code :} (làm hỏng {@code --dat-lai-mat-khau user:mk}) hay {@code ,} (làm hỏng CSV). */
    static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._-]{2,32}");

    static final int MAX_NAME = 80;
    private static final Set<Role> ADMIN_ONLY = Set.of(Role.ADMIN);

    private ServerContext ctx;
    private UserRepository users;
    private ClassRepository classes;

    @Override
    public void install(ServerContext ctx) {
        this.ctx = ctx;
        this.users = ctx.require(UserRepository.class);
        this.classes = ctx.require(ClassRepository.class);
        ctx.router()
                .on(MessageType.ACCOUNT_LIST_REQ, ADMIN_ONLY, this::list)
                .on(MessageType.ACCOUNT_SAVE, ADMIN_ONLY, this::save)
                .on(MessageType.ACCOUNT_LOCK, ADMIN_ONLY, this::lock);
    }

    /**
     * {@code ACCOUNT_LIST_REQ} → {@code ACCOUNT_LIST_DATA} JSON {accounts:[{id, username, fullName, role, locked,
     * mustChange, online, classes:[tên lớp], classId}], classes:[{id, name, teacher}]}. Không bao giờ có mật khẩu băm
     * hay token.
     */
    private void list(Ctx c, Frame f) throws Exception {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        ArrayNode accounts = root.putArray("accounts");
        ArrayNode classList = root.putArray("classes");
        Map<Long, ObjectNode> byId = new LinkedHashMap<>();
        ctx.db().read(db -> {
            try (PreparedStatement ps = db.prepareStatement("SELECT id, username, full_name, role, disabled,"
                            + " must_change_pwd, created_at FROM users ORDER BY role, full_name, username");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong(1);
                    ObjectNode a = accounts.addObject();
                    a.put("id", id)
                            .put("username", rs.getString(2))
                            .put("fullName", rs.getString(3))
                            .put("role", rs.getString(4))
                            .put("locked", rs.getInt(5) != 0)
                            .put("mustChange", rs.getInt(6) != 0)
                            .put("createdAt", rs.getLong(7))
                            .put(
                                    "online",
                                    ctx.sessions()
                                            .byUser(id)
                                            .map(Session::online)
                                            .orElse(false))
                            .put("classId", 0L);
                    a.putArray("classes");
                    byId.put(id, a);
                }
            }
            // học viên: lớp ghi danh; giáo viên: lớp mình dạy
            try (PreparedStatement ps = db.prepareStatement("SELECT e.user_id, c.id, c.name FROM enrollments e"
                            + " JOIN classes c ON c.id = e.class_id UNION ALL"
                            + " SELECT c.teacher_id, c.id, c.name FROM classes c ORDER BY 2");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ObjectNode a = byId.get(rs.getLong(1));
                    if (a == null) continue;
                    ((ArrayNode) a.get("classes")).add(rs.getString(3));
                    if (a.get("classId").asLong() == 0) a.put("classId", rs.getLong(2));
                }
            }
            try (PreparedStatement ps = db.prepareStatement("SELECT c.id, c.name, u.full_name FROM classes c"
                            + " JOIN users u ON u.id = c.teacher_id ORDER BY c.name");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    classList
                            .addObject()
                            .put("id", rs.getLong(1))
                            .put("name", rs.getString(2))
                            .put("teacher", rs.getString(3));
                }
            }
            return null;
        });
        c.reply(JsonPayload.frame(MessageType.ACCOUNT_LIST_DATA, root), Priority.NORMAL);
    }

    /**
     * {@code ACCOUNT_SAVE} JSON {id, username, fullName, role, password, classId, newClassName} → {@code ADMIN_OK}.
     * {@code id} 0: tạo mới (giáo viên hoặc học viên, mật khẩu là mật khẩu tạm); khác 0: sửa họ tên, chuyển lớp
     * học viên, thêm lớp cho giáo viên.
     */
    private void save(Ctx c, Frame f) throws Exception {
        JsonNode in = JsonPayload.tree(f);
        long id = in.path("id").asLong(0);
        String fullName = in.path("fullName").asText("").strip();
        long classId = in.path("classId").asLong(0);
        String newClass = in.path("newClassName").asText("").strip();
        if (fullName.isEmpty() || fullName.length() > MAX_NAME) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "Họ tên phải từ 1 đến " + MAX_NAME + " ký tự");
        }
        if (newClass.length() > 100) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "Tên lớp dài quá 100 ký tự");
        }
        Session admin = c.session();
        String message;
        if (id == 0) {
            String username = in.path("username").asText("").strip();
            Role role = role(in.path("role").asText(""));
            if (!USERNAME.matcher(username).matches()) {
                throw new ProtocolException(
                        ErrorCode.BAD_FRAME, "Tên đăng nhập 2–32 ký tự, chỉ gồm chữ không dấu, số và . _ -");
            }
            if (users.existsIgnoreCase(username)) {
                throw new ProtocolException(ErrorCode.BAD_FRAME, "Đã có tài khoản " + username);
            }
            String bad = AccountModule.checkNewPassword(in.path("password").asText(""));
            if (bad != null) throw new ProtocolException(ErrorCode.BAD_FRAME, bad);
            if (role == Role.STUDENT && classId <= 0) {
                throw new ProtocolException(ErrorCode.BAD_FRAME, "Chọn lớp cho học viên");
            }
            if (role == Role.TEACHER && newClass.isEmpty()) {
                throw new ProtocolException(ErrorCode.BAD_FRAME, "Nhập tên lớp giáo viên này dạy");
            }
            requireClass(classId, role);
            id = users.create(username, fullName, role, in.path("password").asText(""));
            users.setMustChangePassword(id, true); // mật khẩu tạm: lần đăng nhập đầu tự đặt mật khẩu riêng
            if (role == Role.STUDENT) classes.enroll(classId, id);
            else classes.ensure(newClass, id);
            ctx.audit().record(admin.username(), "ACCOUNT_CREATE", role + " " + username);
            message =
                    "Đã tạo " + (role == Role.TEACHER ? "giáo viên " : "học viên ") + fullName + " (" + username + ")";
        } else {
            User u = users.byId(id).orElseThrow(() -> new ProtocolException(ErrorCode.NOT_FOUND, "Không có tài khoản"));
            if (u.role() == Role.ADMIN && u.id() != admin.userId()) {
                throw new ProtocolException(ErrorCode.FORBIDDEN, "Không sửa được tài khoản quản trị khác");
            }
            users.setFullName(id, fullName);
            if (u.role() == Role.STUDENT && classId > 0) {
                requireClass(classId, Role.STUDENT);
                classes.moveStudent(id, classId);
            }
            if (u.role() == Role.TEACHER && !newClass.isEmpty()) {
                classes.ensure(newClass, id);
            }
            ctx.audit().record(admin.username(), "ACCOUNT_EDIT", u.username());
            message = "Đã lưu " + fullName + " (" + u.username() + ")";
        }
        c.reply(MessageType.ADMIN_OK, new PayloadWriter().str(message).toBytes());
    }

    /**
     * {@code ACCOUNT_LOCK(i64 id, bool locked)} → {@code ADMIN_OK}. Khoá thì đăng xuất ngay mọi phiên của người đó;
     * không khoá được tài khoản quản trị.
     */
    private void lock(Ctx c, Frame f) throws Exception {
        PayloadReader r = f.reader();
        long id = r.i64();
        boolean locked = r.bool();
        User u = users.byId(id).orElseThrow(() -> new ProtocolException(ErrorCode.NOT_FOUND, "Không có tài khoản"));
        if (u.role() == Role.ADMIN) {
            throw new ProtocolException(ErrorCode.FORBIDDEN, "Không khoá được tài khoản quản trị");
        }
        users.setDisabled(id, locked);
        if (locked) ctx.sessions().revoke(id);
        ctx.audit().record(c.session().username(), locked ? "ACCOUNT_LOCK" : "ACCOUNT_UNLOCK", u.username());
        c.reply(
                MessageType.ADMIN_OK,
                new PayloadWriter()
                        .str((locked ? "Đã khoá " : "Đã mở khoá ") + u.fullName() + " (" + u.username() + ")")
                        .toBytes());
    }

    private static Role role(String s) throws ProtocolException {
        return switch (s) {
            case "TEACHER" -> Role.TEACHER;
            case "STUDENT" -> Role.STUDENT;
            default -> throw new ProtocolException(
                    ErrorCode.BAD_FRAME, "Chỉ tạo được giáo viên hoặc học viên; quản trị tạo trong file cấu hình");
        };
    }

    private void requireClass(long classId, Role role) throws ProtocolException, SQLException {
        if (role != Role.STUDENT) return;
        Optional<Boolean> ok = ctx.db().read(db -> {
            try (PreparedStatement ps = db.prepareStatement("SELECT 1 FROM classes WHERE id = ?")) {
                ps.setLong(1, classId);
                try (ResultSet rs = ps.executeQuery()) {
                    return Optional.of(rs.next());
                }
            }
        });
        if (!ok.orElse(false)) throw new ProtocolException(ErrorCode.NOT_FOUND, "Không có lớp này");
    }
}
