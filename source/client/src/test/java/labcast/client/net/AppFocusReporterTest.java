package labcast.client.net;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.presence.AppFocus;
import labcast.server.presence.AppFocusModule;
import labcast.server.store.Database;
import org.junit.jupiter.api.Test;

class AppFocusReporterTest {

    static void until(BooleanSupplier cond, String what) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            if (cond.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Hết giờ chờ: " + what);
    }

    @Test
    void roiApp_serverBiet_noiLaiThiGuiLaiTrangThai() throws Exception {
        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(
                        ServerConfig.forTest(), db, List.of(new AccountModule(), new AppFocusModule()));
                var c = new LcpClient("may-test")) {
            server.start();
            long sv = new UserRepository(db).create("sv", "Sinh Viên", Role.STUDENT, "123");
            AppFocus focus = server.context().require(AppFocus.class);
            var reporter = new AppFocusReporter(c);

            c.connect("127.0.0.1", server.port());
            var auth = c.expect(MessageType.AUTH_OK);
            c.send(MessageType.AUTH, new PayloadWriter().str("sv").str("123").toBytes());
            c.setToken(auth.get(5, TimeUnit.SECONDS).reader().str());

            reporter.changed(false, "dsa.sap-xep.noi-bot");
            until(() -> focus.state(sv).map(s -> !s.inApp()).orElse(false), "server biết học viên rời app");

            // Server mất trạng thái (ví dụ khởi động lại) rồi kết nối đứt: nối lại xong app phải tự gửi
            // lại "đang ở ngoài app"
            var session = server.context().sessions().byUser(sv).orElseThrow();
            focus.update(session, true, "");
            assertTrue(focus.state(sv).orElseThrow().inApp());
            session.connection().close();
            until(() -> focus.state(sv).map(s -> !s.inApp()).orElse(false), "app gửi lại sau khi nối lại");
            assertFalse(reporter.inApp());
        }
    }
}
