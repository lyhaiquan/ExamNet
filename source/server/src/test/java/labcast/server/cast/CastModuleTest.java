package labcast.server.cast;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import labcast.common.cast.CastMessages;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.FrameCodec;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.TestClient;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.lesson.DemoTraces;
import labcast.server.store.Database;
import labcast.server.sync.SyncModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CastModuleTest {
    static final String BAI = "dsa.sap-xep.noi-bot";

    Database db;
    LabCastServer server;
    CastSenderTest.Captured cap;
    CastModule cast;

    static List<String> trace(int steps) {
        List<String> lines = new ArrayList<>();
        lines.add("{\"t\":\"hdr\",\"lesson\":\"" + BAI + "\",\"views\":[{\"v\":\"a\",\"kieu\":\"mang\"}]}");
        for (int i = 0; i < steps; i++) {
            lines.add("{\"t\":\"say\",\"s\":\"bước " + i + "\"}");
        }
        lines.add("{\"t\":\"end\",\"truncated\":false}");
        return lines;
    }

    @BeforeEach
    void setUp() throws Exception {
        db = Database.open(":memory:");
        cap = new CastSenderTest.Captured();
        cast = new CastModule(ctx -> CastSenderTest.sender(cap));
        server = new LabCastServer(
                ServerConfig.forTest(),
                db,
                List.of(
                        new AccountModule(),
                        new SyncModule(),
                        ctx -> ctx.register(
                                DemoTraces.class,
                                id -> CompletableFuture.completedFuture(id.equals(BAI) ? trace(3000) : List.of())),
                        cast));
        server.start();
        var users = new UserRepository(db);
        users.create("gv", "Nguyễn Thị Lan", Role.TEACHER, "2");
        users.create("sv1", "Nguyễn Văn An", Role.STUDENT, "1");
    }

    @AfterEach
    void tearDown() throws Exception {
        server.close();
        db.close();
    }

    static byte[] req(String action, String id, int step, int speed) {
        return new CastMessages.Request(action, id, step, speed).encode();
    }

    @Test
    void hocVien_khongDieuKhienDuoc() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.login("sv1", "1");
            c.send(MessageType.CAST_CONTROL, req(CastMessages.LOAD, BAI, 0, 1000));
            assertEquals(
                    ErrorCode.FORBIDDEN,
                    ErrorMessage.from(c.readUntil(MessageType.ERROR)).code());
        }
        assertTrue(cap.datagrams.isEmpty());
    }

    /**
     * Hai lớp (DSA, SQL) cùng một mạng LAN phát lên cùng nhóm multicast: app chỉ nhận gói mang castId của
     * server mình đăng nhập, nên server báo castId ngay trong {@code HELLO_ACK}.
     */
    @Test
    void helloAck_baoCastId_deAppBoGoiChieuCuaLopKhac() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.send(
                    MessageType.HELLO,
                    new labcast.common.protocol.PayloadWriter()
                            .str("1.0")
                            .str("may-1")
                            .toBytes());
            var r = c.readUntil(MessageType.HELLO_ACK).reader();
            r.i64();
            r.str();
            assertEquals(cast.castId(), r.i32());
        }
    }

    @Test
    void baiKhongCoAnimation_baoLoi() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.login("gv", "2");
            c.send(MessageType.CAST_CONTROL, req(CastMessages.LOAD, "khong.co.bai", 0, 1000));
            assertEquals(
                    ErrorCode.NOT_FOUND,
                    ErrorMessage.from(c.readUntil(MessageType.ERROR)).code());
            c.send(MessageType.CAST_CONTROL, req(CastMessages.PLAY, BAI, 0, 1000));
            assertEquals(
                    ErrorCode.NOT_FOUND,
                    ErrorMessage.from(c.readUntil(MessageType.ERROR)).code(),
                    "chưa nạp bài thì không phát được");
        }
    }

    @Test
    void napBai_phatDuKhucRoiMoiPhatLenh_giaoVienNhanBanQuaTcp() throws Exception {
        try (var gv = TestClient.connect(server.port())) {
            gv.login("gv", "2");
            long before = System.currentTimeMillis();
            gv.send(MessageType.CAST_CONTROL, req(CastMessages.LOAD, BAI, 0, 1000));
            Frame viaTcp = gv.readUntil(MessageType.CAST_CONTROL);
            assertTrue(viaTcp.has(Frame.FLAG_VIA_TCP));

            List<Frame> frames = cap.frames();
            Frame last = frames.get(frames.size() - 1);
            assertEquals(MessageType.CAST_CONTROL, last.type());
            assertEquals(last.seq(), viaTcp.seq(), "cùng SEQ với bản multicast, app bỏ trùng được");
            int chunks = frames.size() - 1;
            assertTrue(chunks > 1, "trace 3000 bước phải chia nhiều khúc: " + chunks);
            for (int i = 0; i < chunks; i++) {
                assertEquals(MessageType.CAST_DATA, frames.get(i).type());
                var d = CastMessages.Data.decode(frames.get(i).payload());
                assertEquals(i, d.chunkIndex());
                assertEquals(chunks, d.totalChunks());
                assertEquals(cast.castId(), d.castId());
            }
            var ctl = CastMessages.Control.decode(last.payload());
            assertEquals(CastMessages.LOAD, ctl.action());
            assertEquals(-1, ctl.step());
            assertFalse(ctl.playing());
            assertTrue(ctl.at() >= before + CastState.LEAD_MS, "giờ hẹn = lúc nhận + 300 ms");
            assertTrue(cap.datagrams.stream().allMatch(d -> d.length <= CastMessages.MAX_DATAGRAM));
        }
    }

    static byte[] nack(int castId, int from, int to) {
        return new CastMessages.Nack(castId, from, to).encode();
    }

    @Test
    void xinKhucDaTroiKhoiVongDem_vanNhanDungByteGoc() throws Exception {
        try (var gv = TestClient.connect(server.port());
                var sv = TestClient.connect(server.port())) {
            gv.login("gv", "2");
            sv.login("sv1", "1");
            gv.send(MessageType.CAST_CONTROL, req(CastMessages.LOAD, BAI, 0, 1000));
            gv.readUntil(MessageType.CAST_CONTROL);
            byte[] first = cap.datagrams.get(0);
            for (int i = 0; i < 4100; i++) {
                cast.keyframeNow(); // đẩy khúc 1 ra khỏi vòng đệm 4096 gói
            }
            sv.send(MessageType.CAST_NACK, nack(cast.castId(), 1, 1));
            Frame r = sv.readUntil(MessageType.CAST_REPAIR);
            assertArrayEquals(first, r.reader().blob(), "dựng lại đúng từng byte");

            sv.send(MessageType.CAST_NACK, nack(cast.castId() + 1, 1, 1)); // lần chạy server khác: bỏ
            sv.send(MessageType.CAST_NACK, nack(cast.castId(), 3_000, 3_000)); // khung khoá, còn trong vòng đệm
            Frame r2 = sv.readUntil(MessageType.CAST_REPAIR);
            assertEquals(
                    3_000,
                    FrameCodec.fromBytes(r2.reader().blob(), 0, r2.reader().blob().length)
                            .seq());
        }
    }

    @Test
    void haiHocVienCungThieuMotGoi_phatLaiMotLanQuaMulticast() throws Exception {
        try (var gv = TestClient.connect(server.port());
                var a = TestClient.connect(server.port());
                var b = TestClient.connect(server.port())) {
            new UserRepository(db).create("sv2", "Trần Thị Bình", Role.STUDENT, "1");
            gv.login("gv", "2");
            a.login("sv1", "1");
            b.login("sv2", "1");
            gv.send(MessageType.CAST_CONTROL, req(CastMessages.LOAD, BAI, 0, 1000));
            gv.readUntil(MessageType.CAST_CONTROL);
            int before = cap.datagrams.size();

            a.send(MessageType.CAST_NACK, nack(cast.castId(), 2, 2));
            a.readUntil(MessageType.CAST_REPAIR); // máy đầu: gửi riêng
            b.send(MessageType.CAST_NACK, nack(cast.castId(), 2, 2));
            long end = System.currentTimeMillis() + 2_000;
            while (cap.datagrams.size() == before && System.currentTimeMillis() < end) {
                Thread.sleep(5);
            }
            assertEquals(before + 1, cap.datagrams.size(), "máy thứ hai (2/2 = 100% lớp): phát lại cho cả lớp");
            assertArrayEquals(cap.datagrams.get(1), cap.datagrams.get(before), "phát lại nguyên văn gói SEQ 2");
        }
    }

    @Test
    void hocVienBaoKhongNhanDuocMulticast_baHeartbeat_nhanChieuQuaTcp() throws Exception {
        try (var gv = TestClient.connect(server.port());
                var sv = TestClient.connect(server.port())) {
            gv.login("gv", "2");
            sv.login("sv1", "1");
            gv.send(MessageType.CAST_CONTROL, req(CastMessages.LOAD, BAI, 0, 1000));
            gv.readUntil(MessageType.CAST_CONTROL);
            var s = server.context().sessions().all().stream()
                    .filter(x -> x.username().equals("sv1"))
                    .findFirst()
                    .orElseThrow();
            long now = System.currentTimeMillis();
            for (int i = 0; i < 3; i++) {
                server.context().presence().ack(s, now, java.util.Map.of("lastCastSeq", -1L), now);
            }
            Frame kf = sv.readUntil(MessageType.CAST_KEYFRAME);
            assertTrue(kf.has(Frame.FLAG_VIA_TCP));
            assertEquals(1, cast.status().get("mayQuaTcp"));
            gv.send(MessageType.CAST_CONTROL, req(CastMessages.PLAY, BAI, 0, 1000));
            assertTrue(sv.readUntil(MessageType.CAST_CONTROL).has(Frame.FLAG_VIA_TCP), "từ đó mọi gói chiếu đi cả TCP");
        }
    }

    @Test
    void phatRoiKhungKhoa_mangTrangThaiDangChay() throws Exception {
        try (var gv = TestClient.connect(server.port())) {
            gv.login("gv", "2");
            gv.send(MessageType.CAST_CONTROL, req(CastMessages.LOAD, BAI, 0, 1000));
            gv.readUntil(MessageType.CAST_CONTROL);
            gv.send(MessageType.CAST_CONTROL, req(CastMessages.PLAY, BAI, 5, 2000));
            var play = CastMessages.Control.decode(
                    gv.readUntil(MessageType.CAST_CONTROL).payload());
            assertTrue(play.playing());
            assertEquals(5, play.step());

            cast.keyframeNow();
            Frame kf = gv.readUntil(MessageType.CAST_KEYFRAME);
            var k = CastMessages.Keyframe.decode(kf.payload());
            assertTrue(k.playing());
            assertEquals(5, k.step());
            assertEquals(2000, k.speedMilli());
            assertEquals(play.at(), k.anchorAt());
            assertEquals(kf.seq() - 1, k.lastSeq());
            assertEquals(1, k.firstDataSeq());

            gv.send(MessageType.CAST_CONTROL, req(CastMessages.STOP, BAI, 0, 0));
            assertEquals(
                    CastMessages.STOP,
                    CastMessages.Control.decode(
                                    gv.readUntil(MessageType.CAST_CONTROL).payload())
                            .action());
            int before = cap.datagrams.size();
            cast.keyframeNow();
            assertEquals(before, cap.datagrams.size(), "hết chiếu thì không phát khung khoá");
        }
    }
}
