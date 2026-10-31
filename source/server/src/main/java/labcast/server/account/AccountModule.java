package labcast.server.account;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Logger;
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
import labcast.server.cast.CastModule;
import labcast.server.classroom.ClassRepository;
import labcast.server.net.Ctx;
import labcast.server.net.Priority;
import labcast.server.session.Session;

/** Chào hỏi, đăng nhập, khôi phục phiên, nhập danh sách lớp. Chủ: Người 2. */
public final class AccountModule implements ServerModule {
    private static final Logger LOG = Logger.getLogger(AccountModule.class.getName());
    static final String AUTH_FAIL_MESSAGE = "Sai tên đăng nhập hoặc mật khẩu";
    static final String LOCKED_MESSAGE = "Tài khoản đã bị khoá. Liên hệ quản trị để mở lại.";
    public static final int MIN_PASSWORD = 4;
    public static final int MAX_PASSWORD = 128;

    private ServerContext ctx;
    private UserRepository users;
    private ClassRepository classes;
    private RosterImporter importer;
    /** Để tên đăng nhập không có thật cũng tốn đúng thời gian băm như sai mật khẩu. */
    private volatile PasswordHasher.Hashed dummy;

    @Override
    public void install(ServerContext ctx) {
        this.ctx = ctx;
        this.users = new UserRepository(ctx.db());
        this.classes = new ClassRepository(ctx.db());
        this.importer = new RosterImporter(users, classes);
        ctx.register(UserRepository.class, users);
        ctx.register(ClassRepository.class, classes);
        ctx.sessions().setClassLookup(userId -> {
            try {
                return classes.classOf(userId);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        });
        createFromConfig("--tao-giao-vien", ctx.config().taoGiaoVien(), Role.TEACHER);
        createFromConfig("--tao-admin", ctx.config().taoAdmin(), Role.ADMIN);

        ctx.router()
                .on(MessageType.HELLO, this::hello)
                .on(MessageType.AUTH, this::auth)
                .on(MessageType.RESUME_REQ, this::resume)
                // nhập CSV học viên: giáo viên nhập vào lớp mình (chưa có thì tạo), quản trị nhập vào lớp đang chọn
                .on(MessageType.ADMIN_ROSTER_IMPORT, Role.STAFF, this::importRoster)
                .on(MessageType.PASSWORD_CHANGE, this::changePassword)
                .on(MessageType.PASSWORD_RESET, Role.STAFF, this::resetPassword)
                .on(MessageType.PASSWORD_FORGOT, this::forgotPassword)
                .on(MessageType.PASSWORD_REQUESTS_REQ, Role.STAFF, this::passwordRequests);
    }

    /** Mật khẩu mới hợp lệ không; {@code null} nếu được, ngược lại là lý do để báo người dùng. */
    public static String checkNewPassword(String pwd) {
        if (pwd == null || pwd.isBlank()) return "Mật khẩu mới không được để trống";
        if (pwd.length() < MIN_PASSWORD) return "Mật khẩu mới cần ít nhất " + MIN_PASSWORD + " ký tự";
        if (pwd.length() > MAX_PASSWORD) return "Mật khẩu mới dài quá " + MAX_PASSWORD + " ký tự";
        return null;
    }

    /** {@code PASSWORD_CHANGE(str cũ, str mới)} → {@code ADMIN_OK(str)}. Phiên đang dùng vẫn giữ nguyên. */
    private void changePassword(Ctx c, Frame f) throws Exception {
        PayloadReader r = f.reader();
        String oldPwd = r.str();
        String newPwd = r.str();
        Session s = c.session();
        User u = users.byId(s.userId())
                .orElseThrow(() -> new ProtocolException(ErrorCode.NOT_FOUND, "Không tìm thấy tài khoản"));
        if (!PasswordHasher.verify(oldPwd, u.password())) {
            ctx.audit()
                    .record(u.username(), "PASSWORD_CHANGE_FAIL", c.connection().remoteAddress());
            throw new ProtocolException(ErrorCode.FORBIDDEN, "Mật khẩu hiện tại không đúng");
        }
        String bad = checkNewPassword(newPwd);
        if (bad != null) throw new ProtocolException(ErrorCode.BAD_FRAME, bad);
        if (newPwd.equals(oldPwd) && users.mustChangePassword(u.id())) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "Chọn mật khẩu khác mật khẩu tạm giáo viên đưa");
        }
        users.setPassword(u.id(), newPwd);
        users.setMustChangePassword(u.id(), false);
        users.clearPasswordRequest(u.id());
        ctx.audit().record(u.username(), "PASSWORD_CHANGE", c.connection().remoteAddress());
        c.reply(MessageType.ADMIN_OK, new PayloadWriter().str("Đã đổi mật khẩu").toBytes());
    }

    /**
     * {@code PASSWORD_RESET(str username, str mới)} → {@code ADMIN_OK(str)}. Quản trị: mọi tài khoản. Giáo viên: chỉ
     * học viên ghi danh trong lớp mình dạy (không dựa vào lớp đang chọn của phiên).
     */
    private void resetPassword(Ctx c, Frame f) throws Exception {
        PayloadReader r = f.reader();
        String username = r.str().strip();
        String newPwd = r.str();
        Session s = c.session();
        User target = users.byUsername(username)
                .orElseThrow(() -> new ProtocolException(ErrorCode.NOT_FOUND, "Không có tài khoản " + username));
        boolean allowed =
                s.role() == Role.ADMIN || (target.role() == Role.STUDENT && users.isStudentOf(target.id(), s.userId()));
        if (!allowed) {
            throw new ProtocolException(
                    ErrorCode.FORBIDDEN, "Giáo viên chỉ đặt lại được mật khẩu của học viên trong lớp mình dạy");
        }
        String bad = checkNewPassword(newPwd);
        if (bad != null) throw new ProtocolException(ErrorCode.BAD_FRAME, bad);
        users.setPassword(target.id(), newPwd);
        users.setTemporaryPassword(target.id(), newPwd); // lần đăng nhập sau tự đặt mật khẩu riêng
        users.clearPasswordRequest(target.id());
        ctx.audit().record(s.username(), "PASSWORD_RESET", "đặt lại mật khẩu cho " + target.username());
        c.reply(
                MessageType.ADMIN_OK,
                new PayloadWriter()
                        .str("Đã đặt lại mật khẩu cho " + target.fullName() + " (" + target.username() + ")")
                        .toBytes());
    }

    /**
     * {@code --tao-giao-vien} / {@code --tao-admin user:mật khẩu:họ tên}; đã có tài khoản đó thì bỏ qua. Giáo
     * viên được tạo kèm lớp {@code --lop}; quản trị không gắn lớp nào.
     */
    private void createFromConfig(String flag, String spec, Role role) {
        if (spec == null || spec.isBlank()) {
            return;
        }
        String[] p = spec.split(":", 3);
        if (p.length != 3 || p[0].isBlank() || p[1].isBlank() || p[2].isBlank()) {
            throw new IllegalArgumentException(flag + " phải có dạng user:mật khẩu:họ tên");
        }
        try {
            if (users.byUsername(p[0]).isEmpty()) {
                long id = users.create(p[0], p[2], role, p[1]);
                if (role == Role.TEACHER) {
                    classes.ensure(ctx.config().lop(), id);
                }
                LOG.info("Đã tạo tài khoản " + (role == Role.ADMIN ? "quản trị " : "giáo viên ") + p[0]);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không tạo được tài khoản " + p[0], e);
        }
    }

    /**
     * {@code HELLO(str clientVersion, str device)} → {@code HELLO_ACK(i64 serverTime, str className, i32 castId)}.
     * {@code castId} để app bỏ gói chiếu của lớp khác cùng mạng LAN (0: server không chiếu).
     */
    private void hello(Ctx c, Frame f) throws Exception {
        PayloadReader r = f.reader();
        String clientVersion = r.hasRemaining() ? r.str() : "";
        String device = r.hasRemaining() ? r.str() : "";
        c.connection().setAttr("clientVersion", clientVersion);
        c.connection().setAttr("device", device);
        c.reply(
                MessageType.HELLO_ACK,
                new PayloadWriter()
                        .i64(System.currentTimeMillis())
                        .str(ctx.config().lop())
                        .i32(ctx.find(CastModule.class).map(CastModule::castId).orElse(0))
                        .toBytes());
    }

    /** {@code AUTH(str username, str password)} → {@code AUTH_OK} hoặc {@code AUTH_FAIL}. */
    private void auth(Ctx c, Frame f) throws Exception {
        PayloadReader r = f.reader();
        String username = r.str().strip();
        String password = r.str();
        Optional<User> found = users.byUsername(username);
        boolean ok =
                found.isPresent() ? PasswordHasher.verify(password, found.get().password()) : verifyDummy(password);
        if (!ok) {
            // Không nói rõ sai tên hay sai mật khẩu, để không dò được tên đăng nhập nào có thật.
            ctx.audit().record(username, "AUTH_FAIL", c.connection().remoteAddress());
            c.reply(
                    MessageType.AUTH_FAIL,
                    new PayloadWriter().str(AUTH_FAIL_MESSAGE).toBytes());
            return;
        }
        User u = found.get();
        if (users.isDisabled(u.id())) {
            // chỉ báo khoá khi đã đúng mật khẩu: không để lộ tài khoản nào có thật
            ctx.audit().record(username, "AUTH_LOCKED", c.connection().remoteAddress());
            c.reply(
                    MessageType.AUTH_FAIL,
                    new PayloadWriter().str(LOCKED_MESSAGE).toBytes());
            return;
        }
        long classId = classes.classOf(u.id());
        if (classId == 0 && u.role() == Role.TEACHER) {
            classId = classes.ensure(ctx.config().lop(), u.id());
        }
        Object device = c.connection().attr("device");
        Session s = ctx.sessions().login(u, classId, c.connection(), device == null ? "" : device.toString());
        ctx.audit().record(u.username(), "AUTH_OK", c.connection().remoteAddress());
        c.reply(
                MessageType.AUTH_OK,
                new PayloadWriter()
                        .str(s.token())
                        .str(u.role().name())
                        .i64(u.id())
                        .str(u.fullName())
                        .i64(classId)
                        .bool(users.mustChangePassword(u.id())) // mật khẩu tạm: app bắt đặt mật khẩu riêng
                        .toBytes());
    }

    /**
     * {@code RESUME_REQ(str token, str lessonId, i64 lastAckedSeq)} → {@code RESUME_STATE(bool ok, i64
     * serverLastSeq)}. App so {@code serverLastSeq} với nhật ký của nó để gửi bù phần còn thiếu.
     */
    private void resume(Ctx c, Frame f) throws Exception {
        PayloadReader r = f.reader();
        String token = r.str();
        String lessonId = r.str();
        r.i64(); // lastAckedSeq: giai đoạn 2 dùng để đối chiếu
        Optional<Session> s = ctx.sessions().resume(token, c.connection());
        long serverLastSeq = -1;
        if (s.isPresent() && !lessonId.isEmpty()) {
            serverLastSeq = ctx.sessions().codeSeqLookup().lastSeq(s.get().userId(), lessonId);
        }
        c.reply(
                Frame.of(
                        MessageType.RESUME_STATE,
                        new PayloadWriter()
                                .bool(s.isPresent())
                                .i64(serverLastSeq)
                                .toBytes()),
                Priority.NORMAL);
    }

    /**
     * {@code ADMIN_ROSTER_IMPORT(str csv)} → {@code ADMIN_OK(str message, str created, str enrolled)}. Giáo viên chưa có lớp thì tạo lớp
     * {@code --lop} cho họ; quản trị phải chọn lớp trước ({@code CLASS_SELECT_REQ}), không tự tạo lớp.
     */
    private void importRoster(Ctx c, Frame f) throws Exception {
        String csv = f.reader().str();
        Session s = c.session();
        if (s.classId() <= 0 && s.role() == Role.ADMIN) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "Chọn lớp trước khi nhập danh sách học viên");
        }
        long classId =
                s.classId() > 0 ? s.classId() : classes.ensure(ctx.config().lop(), s.userId());
        RosterImporter.Result res = importer.importCsv(csv, classId);
        ctx.audit().record(s.username(), "ADMIN_ROSTER_IMPORT", res.message());
        // hai trường sau (thêm sau, app cũ bỏ qua): tài khoản mới tạo, tài khoản có sẵn chỉ ghi danh; mỗi tên
        // một dòng. App dùng để xuất file tài khoản + mật khẩu tạm gửi giáo viên.
        c.reply(
                MessageType.ADMIN_OK,
                new PayloadWriter()
                        .str(res.message())
                        .str(String.join("\n", res.createdNames()))
                        .str(String.join("\n", res.enrolledNames()))
                        .toBytes());
    }

    /** Câu trả lời của PASSWORD_FORGOT, giống nhau cho mọi tên đăng nhập. */
    static final String FORGOT_REPLY = "Đã gửi yêu cầu tới giáo viên. Hỏi giáo viên mật khẩu tạm, đăng nhập bằng"
            + " mật khẩu đó rồi đặt mật khẩu mới của bạn.";

    /** {@code PASSWORD_FORGOT(str username)} → {@code ADMIN_OK(str)}. Không cần đăng nhập. */
    private void forgotPassword(Ctx c, Frame f) throws Exception {
        String username = f.reader().str().strip();
        Optional<User> u = username.isEmpty() ? Optional.empty() : users.byUsername(username);
        if (u.isPresent()) {
            users.requestPasswordReset(u.get().id(), System.currentTimeMillis());
            ctx.audit().record(username, "PASSWORD_FORGOT", c.connection().remoteAddress());
        }
        c.reply(MessageType.ADMIN_OK, new PayloadWriter().str(FORGOT_REPLY).toBytes());
    }

    /** {@code PASSWORD_REQUESTS_REQ} → {@code PASSWORD_REQUESTS_DATA}. */
    private void passwordRequests(Ctx c, Frame f) throws Exception {
        Session s = c.session();
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        ArrayNode arr = root.putArray("requests");
        for (UserRepository.PasswordRequest r : users.passwordRequests(s.userId(), s.role() == Role.ADMIN)) {
            arr.addObject()
                    .put("username", r.username())
                    .put("fullName", r.fullName())
                    .put("role", r.role().name())
                    .put("at", r.at());
        }
        c.reply(JsonPayload.frame(MessageType.PASSWORD_REQUESTS_DATA, root), Priority.NORMAL);
    }

    private boolean verifyDummy(String password) {
        PasswordHasher.Hashed d = dummy;
        if (d == null) {
            d = PasswordHasher.hash("tai-khoan-khong-co-that");
            dummy = d;
        }
        PasswordHasher.verify(password, d);
        return false;
    }
}
