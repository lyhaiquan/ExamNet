package labcast.server.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import labcast.common.protocol.CodeDelta;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.TestClient;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.classroom.ClassEvents;
import labcast.server.ops.BuildInfo;
import labcast.server.ops.Dashboard;
import labcast.server.store.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Dán khối lớn qua server thật: delta → cờ trên ClassEvents, dashboard, audit_log; nộp đúng thì mất cờ. */
class PasteFlagFlowTest {
    static final String BAI = "dsa.sap-xep.noi-bot";
    Database db;
    LabCastServer server;
    long sv;

    @BeforeEach
    void bat() throws Exception {
        db = Database.open(":memory:");
        server = new LabCastServer(ServerConfig.forTest(), db, List.of(new AccountModule(), new JournalModule()));
        server.start();
        var users = new UserRepository(db);
        sv = users.create("sv", "Sinh Viên", Role.STUDENT, "1");
        users.create("gv", "Giáo Viên", Role.TEACHER, "1");
        db.read(c -> c.createStatement()
                .executeUpdate("INSERT INTO lessons (id, mon, chuong, ten, loai, path, content_hash) VALUES ('" + BAI
                        + "', 'dsa', 'sap-xep', 'Nổi bọt', 'giang-giai', 'x', 'h')"));
    }

    @AfterEach
    void tat() throws Exception {
        server.close();
        db.close();
    }

    static String khoiMoi(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; sb.length() < n; i++) {
            sb.append("x").append(i * 7919 % 1000).append(' ');
        }
        return sb.substring(0, n);
    }

    @Test
    void hocVienChen250KyTu_coTrenDashboard_nopDungThiMat() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            assertEquals(MessageType.AUTH_OK, c.login("sv", "1").type());
            c.send(new CodeDelta(BAI, 1, 0, 0, khoiMoi(250), 0).toFrame());
            c.readUntil(MessageType.CODE_ACK);
        }
        ClassEvents events = server.context().require(ClassEvents.class);
        List<ClassEvents.PasteFlag> flags = events.pasteFlags(sv);
        assertEquals(1, flags.size());
        assertEquals(BAI, flags.get(0).lessonId());
        assertEquals(250, flags.get(0).chars());

        JsonNode d = JsonPayload.MAPPER.readTree(
                new Dashboard(server.context(), new BuildInfo("0.1.0", "abc1234", Instant.EPOCH), null, server.port())
                        .json());
        JsonNode an = d.path("students").get(0);
        assertEquals(1, an.path("pasteFlags").get(0).path("times").asInt());
        assertTrue(an.path("pasteFlags").get(0).path("detail").asText().contains("250"));
        assertEquals("PASTE_FLAG", d.path("audit").get(0).path("action").asText());
        assertEquals("sv", d.path("audit").get(0).path("actor").asText());

        events.publish(sv, BAI, ClassEvents.StudentStatus.SAI_TEST, "an-1");
        assertEquals(1, events.pasteFlags(sv).size(), "nộp sai thì cờ vẫn còn");
        events.publish(sv, BAI, ClassEvents.StudentStatus.DA_QUA, null);
        assertEquals(0, events.pasteFlags(sv).size(), "nộp đúng bài đó thì mất cờ");
    }

    @Test
    void giaoVienDanCodeMau_khongCo() throws Exception {
        long gv;
        try (var c = TestClient.connect(server.port())) {
            assertEquals(MessageType.AUTH_OK, c.login("gv", "1").type());
            c.send(new CodeDelta(BAI, 1, 0, 0, khoiMoi(400), 0).toFrame());
            c.readUntil(MessageType.CODE_ACK);
            gv = server.context().sessions().all().stream()
                    .filter(s -> s.username().equals("gv"))
                    .findFirst()
                    .orElseThrow()
                    .userId();
        }
        assertEquals(
                0, server.context().require(ClassEvents.class).pasteFlags(gv).size());
    }
}
