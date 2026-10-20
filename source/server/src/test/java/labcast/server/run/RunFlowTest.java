package labcast.server.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import labcast.common.protocol.CodeDelta;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.TestClient;
import labcast.server.TestPaths;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.journal.JournalModule;
import labcast.server.lesson.LessonModule;
import labcast.server.store.Database;
import org.junit.jupiter.api.Test;

/**
 * Đường đi đầy đủ của giai đoạn 1 phía server: mở bài → gõ code → chạy thử → nhận trace của chính
 * code mình. Chạy runner bằng Python cục bộ; bỏ qua nếu máy không có Python.
 */
class RunFlowTest {
    static final String BAI = "dsa.sap-xep.noi-bot";
    static final String CODE =
            """
            import viz

            def sap_xep(a):
                for i in range(len(a) - 1):
                    for j in range(len(a) - 1 - i):
                        if a[j] > a[j + 1]:
                            a[j], a[j + 1] = a[j + 1], a[j]

            a = viz.Mang(list(map(int, input().split())), ten="a")
            sap_xep(a)
            print(*a)
            """;

    @Test
    void moBai_goCode_chayThu_nhanTraceVaOk() throws Exception {
        assumeTrue(LocalPythonSandbox.detectPython() != null, "máy không có Python 3");
        var cfg = ServerConfig.forTest()
                .withContent(TestPaths.repoRoot().resolve("content"))
                .withKhongSandbox(true);
        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(
                        cfg,
                        db,
                        List.of(new AccountModule(), new LessonModule(), new RunModule(), new JournalModule()))) {
            server.start();
            new UserRepository(db).create("sv", "SV", Role.STUDENT, "1");
            try (var c = TestClient.connect(server.port())) {
                assertEquals(MessageType.AUTH_OK, c.login("sv", "1").type());

                c.send(MessageType.LESSON_LIST, new byte[0]);
                JsonNode list = JsonPayload.tree(c.readUntil(MessageType.LESSON_DATA));
                assertEquals("list", list.get("kind").asText());

                c.send(MessageType.LESSON_FETCH, new PayloadWriter().str(BAI).toBytes());
                JsonNode lesson = JsonPayload.tree(c.readUntil(MessageType.LESSON_DATA));
                assertEquals("lesson", lesson.get("kind").asText());
                assertTrue(lesson.get("trace").size() > 10, "trace giảng giải rỗng");
                String khung = lesson.get("code").asText();
                assertEquals(0, lesson.get("codeSeq").asLong());

                // Thay cả khung bằng code mới: một delta
                c.send(new CodeDelta(BAI, 1, 0, khung.length(), CODE, 0).toFrame());
                PayloadReader ack = c.readUntil(MessageType.CODE_ACK).reader();
                assertEquals(BAI, ack.str());
                assertEquals(1, ack.i64());

                c.send(
                        MessageType.RUN_REQ,
                        new PayloadWriter().str(BAI).i64(1).str("").toBytes());
                assertEquals(
                        MessageType.RUN_QUEUED,
                        c.readUntil(MessageType.RUN_QUEUED).type());
                StringBuilder trace = new StringBuilder();
                while (true) {
                    PayloadReader ch = c.readUntil(MessageType.TRACE_CHUNK).reader();
                    ch.i64();
                    ch.i32();
                    boolean last = ch.bool();
                    trace.append(new String(ch.blob(), StandardCharsets.UTF_8)).append('\n');
                    if (last) {
                        break;
                    }
                }
                assertTrue(trace.toString().contains("\"set\""), "trace của code học viên phải có lệnh set");
                Frame result = c.readUntil(MessageType.RUN_RESULT);
                JsonNode r = JsonPayload.tree(result);
                assertTrue(r.get("ok").asBoolean(), r.toString());
                assertEquals("1 2 3\n", r.get("stdout").asText());
                assertEquals("OK", r.get("tests").get(0).get("verdict").asText());
            }
        }
    }

    @Test
    void normalize_boKhoangTrangCuoi() {
        assertEquals("1 2\n3", RunModule.normalize("1 2  \n3\n\n"));
        assertEquals(RunModule.normalize("a\r\nb\r\n"), RunModule.normalize("a\nb"));
    }

    @Test
    void verdict_cacTruongHop() {
        assertEquals("TIME_LIMIT", RunModule.verdict(new Outcome(false, "", "x", false, 1, true), "1", 1000));
        assertEquals("RUNTIME_ERROR", RunModule.verdict(new Outcome(false, "", "x", false, 1, false), "1", 1000));
        assertEquals("WRONG", RunModule.verdict(new Outcome(true, "2\n", null, false, 1, false), "1\n", 1000));
        assertEquals("OK", RunModule.verdict(new Outcome(true, "1 \n", null, false, 1, false), "1", 1000));
        // chạy xong trong runner nhưng quá giới hạn của bài
        assertEquals("TIME_LIMIT", RunModule.verdict(new Outcome(true, "1", null, false, 1500, false), "1", 1000));
    }
}
