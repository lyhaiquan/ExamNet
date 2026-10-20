package labcast.client.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.store.Database;
import labcast.server.sync.SyncModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Client thật nói chuyện với server thật trong cùng tiến trình. */
class LcpClientTest {
    private Database db;
    private LabCastServer server;
    private long svId;

    @BeforeEach
    void bat() throws Exception {
        db = Database.open(":memory:");
        server = new LabCastServer(
                ServerConfig.forTest().withLop("Lớp thử"), db, List.of(new AccountModule(), new SyncModule()));
        server.start();
        svId = new UserRepository(db).create("sv", "Sinh Viên", Role.STUDENT, "123");
    }

    @AfterEach
    void tat() throws Exception {
        server.close();
        db.close();
    }

    private static Frame cho(java.util.concurrent.CompletableFuture<Frame> f) throws Exception {
        return f.get(5, TimeUnit.SECONDS);
    }

    private Frame dangNhap(LcpClient c) throws Exception {
        var auth = c.expect(MessageType.AUTH_OK, MessageType.AUTH_FAIL);
        c.send(MessageType.AUTH, new PayloadWriter().str("sv").str("123").toBytes());
        Frame ok = cho(auth);
        c.setToken(ok.reader().str());
        return ok;
    }

    @Test
    void ketNoi_helloAck_dangNhap() throws Exception {
        try (var c = new LcpClient("may-test")) {
            var hello = c.expect(MessageType.HELLO_ACK);
            c.connect("127.0.0.1", server.port());
            PayloadReader r = cho(hello).reader();
            r.i64();
            assertEquals("Lớp thử", r.str());
            assertEquals(MessageType.AUTH_OK, dangNhap(c).type());
            assertEquals(LcpClient.State.CONNECTED, c.state());
        }
    }

    @Test
    void heartbeat_tuTraLoi_coTruongMoRong() throws Exception {
        var fields = new AtomicReference<Map<String, Long>>();
        var got = new CountDownLatch(1);
        server.context().presence().onHeartbeat((s, f) -> {
            fields.set(f);
            got.countDown();
        });
        try (var c = new LcpClient("may-test")) {
            c.heartbeat().addField("lastCastSeq", () -> 42);
            c.connect("127.0.0.1", server.port());
            dangNhap(c);
            assertTrue(got.await(5, TimeUnit.SECONDS), "server không nhận HEARTBEAT_ACK");
            assertEquals(Map.of("lastCastSeq", 42L), fields.get());
            assertTrue(server.context().presence().srtt(svId) >= 0);
        }
    }

    @Test
    void serverDongSocket_tuNoiLai_vaKhoiPhucPhien() throws Exception {
        List<LcpClient.State> states = new CopyOnWriteArrayList<>();
        try (var c = new LcpClient("may-test")) {
            c.onStateChange(states::add);
            c.connect("127.0.0.1", server.port());
            dangNhap(c);
            var resume = c.expect(MessageType.RESUME_STATE);
            server.context().sessions().byUser(svId).orElseThrow().connection().close();
            Frame st = cho(resume);
            assertTrue(st.reader().bool(), "RESUME_REQ với token cũ phải thành công");
            assertEquals(
                    List.of(LcpClient.State.CONNECTED, LcpClient.State.RECONNECTING, LcpClient.State.CONNECTED),
                    states);
            // gửi được tiếp sau khi nối lại
            var sync = c.expect(MessageType.TIME_SYNC_RESP);
            c.send(MessageType.TIME_SYNC_REQ, new PayloadWriter().i64(1).toBytes());
            assertEquals(1, cho(sync).reader().i64());
        }
    }

    @Test
    void framGuiKhiMatMang_duocGuiSauKhiNoiLai() throws Exception {
        try (var c = new LcpClient("may-test")) {
            c.connect("127.0.0.1", server.port());
            dangNhap(c);
            var lost = new CountDownLatch(1);
            var reconnected = new CountDownLatch(1);
            c.onStateChange(s -> {
                if (s == LcpClient.State.RECONNECTING) {
                    lost.countDown();
                } else if (s == LcpClient.State.CONNECTED) {
                    reconnected.countDown();
                }
            });
            server.context().sessions().byUser(svId).orElseThrow().connection().close();
            assertTrue(lost.await(5, TimeUnit.SECONDS));
            var sync = c.expect(MessageType.TIME_SYNC_RESP);
            c.send(MessageType.TIME_SYNC_REQ, new PayloadWriter().i64(7).toBytes()); // gửi lúc đang mất mạng
            assertTrue(reconnected.await(5, TimeUnit.SECONDS));
            assertEquals(7, cho(sync).reader().i64());
        }
    }
}
