package labcast.server.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.TestClient;
import labcast.server.store.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TcpServerTest {
    private LabCastServer server;

    @BeforeEach
    void batServer() throws Exception {
        server = new LabCastServer(ServerConfig.forTest(), Database.open(":memory:"), List.of(ctx -> ctx.router()
                .on(
                        MessageType.HELLO,
                        (c, f) -> c.reply(
                                MessageType.HELLO_ACK,
                                new PayloadWriter()
                                        .i64(System.currentTimeMillis())
                                        .str("Lớp thử")
                                        .toBytes()))));
        server.start();
    }

    @AfterEach
    void tatServer() {
        server.close();
    }

    @Test
    void guiHello_nhanHelloAck() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.send(MessageType.HELLO, new byte[0]);
            Frame ack = c.read();
            assertEquals(MessageType.HELLO_ACK, ack.type());
            var r = ack.reader();
            r.i64();
            assertEquals("Lớp thử", r.str());
            assertTrue(ack.seq() > 0, "server phải gán SEQ");
        }
    }

    @Test
    void guiRac_nhanBadFrameRoiBiDong() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.sendRaw("GET / HTTP/1.1\r\n".getBytes()); // 16 byte, MAGIC sai
            Frame err = c.read();
            assertEquals(MessageType.ERROR, err.type());
            assertEquals(ErrorCode.BAD_FRAME, ErrorMessage.from(err).code());
            assertEquals(-1, c.readRawByte());
        }
    }

    @Test
    void haiMuoiClientCungLuc_deuDuocTraLoi() throws Exception {
        var pool = Executors.newFixedThreadPool(20);
        try {
            List<Future<MessageType>> kq = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                Callable<MessageType> job = () -> {
                    try (var c = TestClient.connect(server.port())) {
                        c.send(MessageType.HELLO, new byte[0]);
                        return c.read().type();
                    }
                };
                kq.add(pool.submit(job));
            }
            for (Future<MessageType> f : kq) {
                assertEquals(MessageType.HELLO_ACK, f.get());
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void thongDiepCanDangNhap_biTuChoiNhungKhongDong() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.send(MessageType.LESSON_LIST, new byte[0]);
            Frame err = c.read();
            assertEquals(MessageType.ERROR, err.type());
            c.send(MessageType.HELLO, new byte[0]);
            assertEquals(MessageType.HELLO_ACK, c.read().type());
        }
    }
}
