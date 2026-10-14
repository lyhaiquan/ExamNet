package labcast.server.lesson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.TestClient;
import labcast.server.TestPaths;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.store.Database;
import org.junit.jupiter.api.Test;

/** Lớp DSA và lớp SQL là hai lớp riêng: server đặt {@code mon} chỉ đưa ra bài của môn đó. */
class MonCuaLopTest {

    @Test
    void lopSql_chiThayBaiSql_xinBaiDsaThiKhongCo() throws Exception {
        var cfg = ServerConfig.forTest()
                .withContent(TestPaths.repoRoot().resolve("content"))
                .withMon("sql");
        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(cfg, db, List.of(new AccountModule(), new LessonModule()))) {
            server.start();
            new UserRepository(db).create("sv", "SV", Role.STUDENT, "1");
            try (var c = TestClient.connect(server.port())) {
                assertEquals(MessageType.AUTH_OK, c.login("sv", "1").type());

                c.send(MessageType.LESSON_LIST, new byte[0]);
                JsonNode list = JsonPayload.tree(c.readUntil(MessageType.LESSON_DATA));
                assertTrue(list.get("lessons").size() > 0);
                for (JsonNode l : list.get("lessons")) {
                    assertEquals("sql", l.get("mon").asText(), l.get("id").asText());
                }

                c.send(
                        MessageType.LESSON_FETCH,
                        new PayloadWriter().str("dsa.sap-xep.noi-bot").toBytes());
                assertEquals(
                        ErrorCode.NOT_FOUND,
                        ErrorMessage.from(c.readUntil(MessageType.ERROR)).code());
            }
        }
    }
}
