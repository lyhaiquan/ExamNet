package labcast.server.account;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;
import labcast.server.ServerContext;
import labcast.server.ServerModule;
import labcast.server.classroom.ClassRepository;
import labcast.server.net.Ctx;
import labcast.server.net.Priority;
import labcast.server.session.Session;

/** Chào hỏi, đăng nhập, khôi phục phiên, nhập danh sách lớp. Chủ: Người 2. */
public final class AccountModule implements ServerModule {
    private static final Logger LOG = Logger.getLogger(AccountModule.class.getName());
    static final String AUTH_FAIL_MESSAGE = "Sai tên đăng nhập hoặc mật khẩu";

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
        createTeacherFromConfig();

        ctx.router()
                .on(MessageType.HELLO, this::hello)
                .on(MessageType.AUTH, this::auth)
                .on(MessageType.RESUME_REQ, this::resume)
                .on(MessageType.ADMIN_ROSTER_IMPORT, Set.of(Role.TEACHER), this::importRoster);
    }

    /** {@code --tao-giao-vien user:mật khẩu:họ tên}; đã có tài khoản đó thì bỏ qua. */
    private void createTeacherFromConfig() {
        String spec = ctx.config().taoGiaoVien();
        if (spec == null || spec.isBlank()) {
            return;
        }
        String[] p = spec.split(":", 3);
        if (p.length != 3 || p[0].isBlank() || p[1].isBlank() || p[2].isBlank()) {
            throw new IllegalArgumentException("--tao-giao-vien phải có dạng user:mật khẩu:họ tên");
        }
        try {
            if (users.byUsername(p[0]).isEmpty()) {
                long id = users.create(p[0], p[2], Role.TEACHER, p[1]);
                classes.ensure(ctx.config().lop(), id);
                LOG.info("Đã tạo tài khoản giáo viên " + p[0]);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không tạo được giáo viên", e);
        }
    }

    /** {@code HELLO(str clientVersion, str device)} → {@code HELLO_ACK(i64 serverTime, str className)}. */
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

    /** {@code ADMIN_ROSTER_IMPORT(str csv)} → {@code ADMIN_OK(str message)}. Chỉ giáo viên. */
    private void importRoster(Ctx c, Frame f) throws Exception {
        String csv = f.reader().str();
        Session s = c.session();
        long classId =
                s.classId() > 0 ? s.classId() : classes.ensure(ctx.config().lop(), s.userId());
        RosterImporter.Result res = importer.importCsv(csv, classId);
        ctx.audit().record(s.username(), "ADMIN_ROSTER_IMPORT", res.message());
        c.reply(MessageType.ADMIN_OK, new PayloadWriter().str(res.message()).toBytes());
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
