package labcast.client.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import labcast.client.net.LcpClient;
import labcast.common.protocol.CodeDelta;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.journal.CodeStore;
import labcast.server.journal.JournalModule;
import labcast.server.lesson.LessonModule;
import labcast.server.store.Database;
import org.junit.jupiter.api.Test;

class JournalClientTest {
    static final String BAI = "dsa.sap-xep.noi-bot";

    static Path repoRoot() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.exists(p.resolve("runner/run.py"))) {
            p = p.getParent();
        }
        return p;
    }

    @Test
    void guiBaDelta_nhanAck_codeTrenServerKhop() throws Exception {
        var cfg = ServerConfig.forTest().withContent(repoRoot().resolve("content"));
        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(
                        cfg, db, List.of(new AccountModule(), new LessonModule(), new JournalModule()));
                var c = new LcpClient("may-test")) {
            server.start();
            long sv = new UserRepository(db).create("sv", "SV", Role.STUDENT, "1");
            c.connect("127.0.0.1", server.port());
            var ok = c.expect(MessageType.AUTH_OK);
            c.send(MessageType.AUTH, new PayloadWriter().str("sv").str("1").toBytes());
            ok.get(5, TimeUnit.SECONDS);

            var j = new JournalClient(c);
            String khung =
                    server.context().require(CodeStore.class).current(sv, BAI).code();
            int end = khung.length();
            j.submit(new CodeDelta(BAI, 1, end, 0, "a", 0));
            j.submit(new CodeDelta(BAI, 2, end + 1, 0, "b", 0));
            j.submit(new CodeDelta(BAI, 3, end + 2, 0, "c", 0));
            for (int i = 0; i < 100 && j.unacked() > 0; i++) {
                Thread.sleep(30);
            }
            assertEquals(0, j.unacked());
            var snap = server.context().require(CodeStore.class).current(sv, BAI);
            assertEquals(khung + "abc", snap.code());
            assertEquals(3, snap.seq());
        }
    }
}
