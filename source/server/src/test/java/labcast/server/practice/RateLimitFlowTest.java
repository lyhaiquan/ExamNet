package labcast.server.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.TestClient;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.store.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Giới hạn tốc độ đi qua server thật: luật cài bởi {@link PracticeModule}, chặn trên luồng đọc. */
class RateLimitFlowTest {
    Database db;
    LabCastServer server;
    final AtomicInteger runsHandled = new AtomicInteger();

    @BeforeEach
    void bat() throws Exception {
        db = Database.open(":memory:");
        server = new LabCastServer(
                ServerConfig.forTest(),
                db,
                List.of(
                        new AccountModule(),
                        new PracticeModule(),
                        // RUN_REQ giả: chỉ đếm và trả RUN_QUEUED, không chạy code
                        ctx -> ctx.router().on(MessageType.RUN_REQ, (c, f) -> {
                            runsHandled.incrementAndGet();
                            c.reply(
                                    MessageType.RUN_QUEUED,
                                    new PayloadWriter().i64(1).u16(0).toBytes());
                        })));
        server.start();
        new UserRepository(db).create("sv", "Sinh Viên", Role.STUDENT, "1");
    }

    @AfterEach
    void tat() throws Exception {
        server.close();
        db.close();
    }

    @Test
    void chayThuLuotThu4_biChanTruocKhiToiNghiepVu() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            assertEquals(MessageType.AUTH_OK, c.login("sv", "1").type());
            byte[] req = new PayloadWriter().str("bai").i64(0).str("").toBytes();
            for (int i = 0; i < 4; i++) {
                c.send(MessageType.RUN_REQ, req);
            }
            int queued = 0;
            ErrorMessage err = null;
            while (queued + (err == null ? 0 : 1) < 4) {
                Frame f = c.readUntil(MessageType.RUN_QUEUED, MessageType.ERROR);
                if (f.type() == MessageType.RUN_QUEUED) {
                    queued++;
                } else {
                    err = ErrorMessage.from(f);
                }
            }
            assertEquals(3, queued);
            assertEquals(3, runsHandled.get(), "lượt bị chặn không được tới handler");
            assertEquals(ErrorCode.RATE_LIMITED, err.code());
            assertTrue(
                    err.retryAfterMs() > 9_000 && err.retryAfterMs() <= 10_000, "retryAfterMs=" + err.retryAfterMs());
            assertTrue(err.message().startsWith("Chạy thử nhanh quá"), err.message());
        }
    }
}
