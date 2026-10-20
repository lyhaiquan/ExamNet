package labcast.server.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.TestClient;
import labcast.server.store.Database;
import org.junit.jupiter.api.Test;

class SyncModuleTest {

    @Test
    void chuaDangNhapCungDongBoDuoc_t2KhongSauT3() throws Exception {
        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(ServerConfig.forTest(), db, List.of(new SyncModule()))) {
            server.start();
            try (var c = TestClient.connect(server.port())) {
                long t1 = System.currentTimeMillis();
                c.send(MessageType.TIME_SYNC_REQ, new PayloadWriter().i64(t1).toBytes());
                PayloadReader r = c.readUntil(MessageType.TIME_SYNC_RESP).reader();
                assertEquals(t1, r.i64());
                long t2 = r.i64();
                long t3 = r.i64();
                assertTrue(t2 <= t3);
                assertTrue(t3 - t2 < 1_000);
            }
        }
    }
}
