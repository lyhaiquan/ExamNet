package labcast.server.presence;

import java.util.Set;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.Role;
import labcast.server.ServerContext;
import labcast.server.ServerModule;

/**
 * {@code APP_FOCUS(bool inApp, i64 clientTs, str lessonId)} từ app học viên → {@link AppFocus}. Không trả
 * lời: TCP đã giữ thứ tự và không mất, nối lại thì app tự gửi lại trạng thái hiện tại. Chỉ học viên đã
 * đăng nhập được gửi. {@code clientTs} chỉ để đối chiếu khi cần, mốc giờ dùng giờ server. Chủ: Người 2.
 */
public final class AppFocusModule implements ServerModule {

    @Override
    public void install(ServerContext ctx) {
        AppFocus focus = new AppFocus(ctx.audit(), System::currentTimeMillis);
        ctx.register(AppFocus.class, focus);
        ctx.router().on(MessageType.APP_FOCUS, Set.of(Role.STUDENT), (c, f) -> {
            PayloadReader r = f.reader();
            boolean inApp = r.bool();
            r.i64(); // clientTs
            String lessonId = r.str();
            focus.update(c.session(), inApp, lessonId);
        });
    }
}
