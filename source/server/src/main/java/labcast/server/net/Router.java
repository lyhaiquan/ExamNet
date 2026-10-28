package labcast.server.net;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.ProtocolException;
import labcast.common.protocol.Role;
import labcast.server.session.Session;

/**
 * Chuyển mỗi frame tới handler đã đăng ký cho loại của nó, sau khi kiểm đăng nhập và vai trò. Mỗi
 * module đăng ký handler của mình trong {@code ServerModule.install}.
 */
public final class Router {
    private static final Logger LOG = Logger.getLogger(Router.class.getName());

    /** Loại được gửi trước khi đăng nhập. */
    private static final Set<MessageType> PUBLIC = EnumSet.of(
            MessageType.HELLO,
            MessageType.AUTH,
            MessageType.PASSWORD_FORGOT,
            MessageType.RESUME_REQ,
            MessageType.TIME_SYNC_REQ,
            MessageType.HEARTBEAT_ACK,
            MessageType.BYE);

    private record Route(Handler handler, Set<Role> roles) {}

    private final Map<MessageType, Route> routes = new ConcurrentHashMap<>();
    private volatile FramePolicy policy = FramePolicy.ALLOW_ALL;

    /** Chỉ cần đã đăng nhập (trừ các loại ở {@link #PUBLIC}). */
    public Router on(MessageType t, Handler h) {
        return on(t, Set.of(), h);
    }

    /** {@code roles} rỗng = mọi vai trò. Mỗi loại chỉ một handler: đăng ký trùng là lỗi lập trình. */
    public Router on(MessageType t, Set<Role> roles, Handler h) {
        if (routes.putIfAbsent(t, new Route(h, Set.copyOf(roles))) != null) {
            throw new IllegalStateException("Đã có handler cho " + t);
        }
        return this;
    }

    public void setPolicy(FramePolicy p) {
        this.policy = p == null ? FramePolicy.ALLOW_ALL : p;
    }

    public FramePolicy policy() {
        return policy;
    }

    /** Không bao giờ ném ra ngoài: mọi lỗi của handler thành {@code ERROR}, kết nối vẫn sống. */
    public void route(Ctx ctx, Frame f) {
        Route r = routes.get(f.type());
        if (r == null) {
            ctx.error(ErrorCode.BAD_FRAME, "Server không nhận " + f.type(), 0);
            return;
        }
        Session s = ctx.session();
        if (s == null && !PUBLIC.contains(f.type())) {
            ctx.error(ErrorCode.AUTH_REQUIRED, "Cần đăng nhập trước khi gửi " + f.type(), 0);
            return;
        }
        if (!r.roles.isEmpty() && (s == null || !r.roles.contains(s.role()))) {
            ctx.error(ErrorCode.FORBIDDEN, f.type() + " chỉ dành cho " + r.roles, 0);
            return;
        }
        try {
            r.handler.handle(ctx, f);
        } catch (ProtocolException e) {
            ctx.error(e.code(), e.getMessage(), 0);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Handler " + f.type() + " lỗi", e);
            ctx.error(ErrorCode.INTERNAL, "Lỗi server khi xử lý " + f.type(), 0);
        }
    }
}
