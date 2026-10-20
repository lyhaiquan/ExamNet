package labcast.server.ops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.TestClient;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.presence.AppFocusModule;
import labcast.server.store.Database;
import labcast.server.sync.SyncModule;
import org.junit.jupiter.api.Test;

class DashboardTest {
    static final BuildInfo INFO = new BuildInfo("0.1.0", "abc1234", Instant.EPOCH);

    static HttpResponse<String> get(String host, int port, String path) throws Exception {
        return HttpClient.newHttpClient()
                .send(
                        HttpRequest.newBuilder(URI.create("http://" + host + ":" + port + path))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
    }

    /** Một địa chỉ IPv4 không phải loopback của máy này (card LAN), nếu có. */
    static Optional<InetAddress> lanAddress() throws Exception {
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!ni.isUp() || ni.isLoopback()) {
                continue;
            }
            for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                if (a instanceof Inet4Address && !a.isLoopbackAddress() && !a.isLinkLocalAddress()) {
                    return Optional.of(a);
                }
            }
        }
        return Optional.empty();
    }

    @Test
    void chiMayServerMoiXemDuoc() throws Exception {
        assertTrue(StatusEndpoint.isLocal(new InetSocketAddress("127.0.0.1", 50000)));
        assertTrue(StatusEndpoint.isLocal(new InetSocketAddress("::1", 50000)));
        assertFalse(StatusEndpoint.isLocal(new InetSocketAddress("192.168.1.10", 50000)));
        assertFalse(StatusEndpoint.isLocal(null));
    }

    @Test
    void quaHttp_trangVaApiChoLoopback_versionVanMo() throws Exception {
        try (StatusEndpoint status = StatusEndpoint.start(0, INFO)) {
            status.enableDashboard(() -> "{\"ok\":true}");
            var page = get("127.0.0.1", status.port(), "/");
            assertEquals(200, page.statusCode());
            assertTrue(page.body().contains("LabCast · Dashboard"));
            assertFalse(page.body().contains("http://cdn"), "không tải gì từ Internet");
            assertEquals(
                    "{\"ok\":true}",
                    get("127.0.0.1", status.port(), "/api/dashboard").body());
            assertEquals(404, get("127.0.0.1", status.port(), "/favicon.ico").statusCode());
            assertEquals(200, get("127.0.0.1", status.port(), "/version").statusCode());
        }
    }

    @Test
    void quaCardLan_dashboardBiChan_versionVanMo() throws Exception {
        Optional<InetAddress> lan = lanAddress();
        assumeTrue(lan.isPresent(), "máy không có card LAN");
        try (StatusEndpoint status = StatusEndpoint.start(0, INFO)) {
            status.enableDashboard(() -> "{\"bi-mat\":true}");
            String host = lan.get().getHostAddress();
            assertEquals(403, get(host, status.port(), "/").statusCode());
            var api = get(host, status.port(), "/api/dashboard");
            assertEquals(403, api.statusCode());
            assertFalse(api.body().contains("bi-mat"));
            assertEquals(200, get(host, status.port(), "/version").statusCode());
        }
    }

    @Test
    void soLieu_coHocVienRoiApp_khongLoToken() throws Exception {
        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(
                        ServerConfig.forTest().withLop("Lớp D22CQCN01"),
                        db,
                        List.of(
                                new AccountModule(),
                                new SyncModule(),
                                new AppFocusModule(),
                                new labcast.server.cast.CastModule()))) {
            server.start();
            var users = new UserRepository(db);
            users.create("sv1", "Nguyễn Văn An", Role.STUDENT, "1");
            users.create("sv2", "Trần Thị Bình", Role.STUDENT, "2");
            LogBuffer logs = new LogBuffer(10);
            logs.publish(new LogRecord(Level.WARNING, "thử cảnh báo"));

            try (var c = TestClient.connect(server.port())) {
                String token = c.login("sv1", "1").reader().str();
                c.send(
                        MessageType.APP_FOCUS,
                        new PayloadWriter()
                                .bool(false)
                                .i64(0)
                                .str("dsa.sap-xep.noi-bot")
                                .toBytes());
                c.send(MessageType.TIME_SYNC_REQ, new PayloadWriter().i64(0).toBytes());
                c.readUntil(MessageType.TIME_SYNC_RESP); // APP_FOCUS đã xử lý xong

                String json = new Dashboard(server.context(), INFO, logs, server.port()).json();
                assertFalse(json.contains(token), "không được lộ token phiên");
                assertFalse(json.contains("pwd"), "không được lộ mật khẩu băm");

                JsonNode d = JsonPayload.MAPPER.readTree(json);
                assertEquals("Lớp D22CQCN01", d.path("server").path("lop").asText());
                assertFalse(d.path("cast").path("dangChieu").asBoolean(true), "chưa chiếu");
                JsonNode an = d.path("students").get(0);
                assertEquals("sv1", an.path("username").asText());
                assertTrue(an.path("online").asBoolean());
                assertFalse(an.path("inApp").asBoolean());
                assertEquals(1, an.path("leaves").asInt());
                assertEquals("dsa.sap-xep.noi-bot", an.path("lessonId").asText());
                JsonNode binh = d.path("students").get(1);
                assertEquals("CHUA_VAO", binh.path("presence").asText());
                assertTrue(binh.path("inApp").asBoolean());
                assertEquals("APP_LEAVE", d.path("audit").get(0).path("action").asText());
                assertEquals(
                        "thử cảnh báo", d.path("logs").get(0).path("message").asText());
            }
        }
    }
}
