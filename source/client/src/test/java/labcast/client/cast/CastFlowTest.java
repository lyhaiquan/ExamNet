package labcast.client.cast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import labcast.client.player.Player;
import labcast.client.ui.LoginController;
import labcast.common.cast.CastMessages;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.Role;
import labcast.common.trace.Trace;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.cast.CastModule;
import labcast.server.lesson.DemoTraces;
import labcast.server.store.Database;
import labcast.server.sync.SyncModule;
import org.junit.jupiter.api.Test;

/**
 * Đầu–cuối trên một máy (tiêu chí O2): server thật, multicast thật trên card loopback, mỗi học viên mất
 * 5% gói. Mọi máy nhận đủ trace, dừng ở cùng một bước với server; máy vào muộn bắt kịp.
 */
class CastFlowTest {
    static final String BAI = "dsa.sap-xep.noi-bot";

    /** Trace ~150 KB chữ ngẫu nhiên: nén không nhỏ đi bao nhiêu, ra hơn trăm khúc. */
    static List<String> bigTrace() {
        Random r = new Random(7);
        List<String> lines = new ArrayList<>();
        lines.add("{\"t\":\"hdr\",\"lesson\":\"" + BAI + "\",\"views\":[{\"v\":\"a\",\"kieu\":\"mang\"}]}");
        for (int i = 0; i < 6000; i++) {
            lines.add("{\"t\":\"say\",\"s\":\"" + Long.toString(r.nextLong(), 36) + "\"}");
        }
        lines.add("{\"t\":\"end\",\"truncated\":false}");
        return lines;
    }

    /** Một máy học viên: phần nhận chiếu thật, "luồng giao diện" là một luồng riêng. */
    static final class Student implements AutoCloseable {
        final LoginController.LoggedIn li;
        final ExecutorService ui = Executors.newSingleThreadExecutor();
        final CastClient cast;
        volatile Player player;

        Student(int port, String user, InetSocketAddress group, double loss, long seed) throws Exception {
            li = LoginController.login("127.0.0.1", port, user, "1");
            cast = new CastClient(li.client(), System::currentTimeMillis, ui, group);
            Random r = new Random(seed);
            cast.receiver().setDrop(d -> r.nextDouble() < loss);
            on(() -> {
                cast.session().attach(new CastSession.Screen() {
                    @Override
                    public Player show(String traceId, Trace trace) {
                        player = new Player(trace, Map.of(), System::currentTimeMillis);
                        return player;
                    }

                    @Override
                    public void stopped() {}
                });
                return null;
            });
        }

        <T> T on(Callable<T> c) throws Exception {
            return ui.submit(c).get(5, TimeUnit.SECONDS);
        }

        boolean ready() throws Exception {
            return on(() -> cast.session().ready());
        }

        /** Vị trí và đang chạy hay không, sau một khung hình. */
        int[] state() throws Exception {
            return on(() -> {
                player.tick();
                return new int[] {player.position(), player.playing() ? 1 : 0};
            });
        }

        @Override
        public void close() {
            cast.close();
            li.client().close();
            ui.shutdownNow();
        }
    }

    static void waitUntil(Callable<Boolean> ok, long ms, String what) throws Exception {
        long end = System.currentTimeMillis() + ms;
        while (!ok.call()) {
            if (System.currentTimeMillis() > end) {
                throw new AssertionError("Hết giờ chờ: " + what);
            }
            Thread.sleep(20);
        }
    }

    @Test
    void haiMayMat5PhanTramGoi_vaMotMayVaoMuon_deuDungCungBuocVoiServer() throws Exception {
        NetworkInterface lo = NetworkInterface.getByName("lo");
        assumeTrue(lo != null, "không có card loopback tên lo");
        InetSocketAddress group;
        try (var s = new DatagramSocket(0)) {
            group = new InetSocketAddress("239.255.70.1", s.getLocalPort());
        }
        List<String> trace = bigTrace();
        CastModule castModule = new CastModule(lo, group);
        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(
                        ServerConfig.forTest(),
                        db,
                        List.of(
                                new AccountModule(),
                                new SyncModule(),
                                ctx -> ctx.register(
                                        DemoTraces.class,
                                        id -> CompletableFuture.completedFuture(id.equals(BAI) ? trace : List.of())),
                                castModule))) {
            server.start();
            var users = new UserRepository(db);
            users.create("gv", "Nguyễn Thị Lan", Role.TEACHER, "2");
            for (String u : List.of("sv1", "sv2", "sv3")) {
                users.create(u, "Học viên " + u, Role.STUDENT, "1");
            }
            var gv = LoginController.login("127.0.0.1", server.port(), "gv", "2");
            try (var a = new Student(server.port(), "sv1", group, 0.05, 1);
                    var b = new Student(server.port(), "sv2", group, 0.05, 2)) {
                assumeTrue(a.cast.start() > 0 && b.cast.start() > 0, "không tham gia được nhóm multicast");

                long t0 = System.currentTimeMillis();
                send(gv, CastMessages.LOAD, -1, 1000);
                waitUntil(() -> a.ready() && b.ready(), 15_000, "hai máy ghép đủ trace");
                long loadMs = System.currentTimeMillis() - t0;

                send(gv, CastMessages.PLAY, 0, 4000);
                Thread.sleep(1_500);
                assertEquals(1, a.state()[1], "đang chạy theo giáo viên");

                // Máy thứ ba vào lớp giữa buổi chiếu
                try (var late = new Student(server.port(), "sv3", group, 0.05, 3)) {
                    late.cast.start();
                    long j0 = System.currentTimeMillis();
                    waitUntil(late::ready, 10_000, "máy vào muộn ghép đủ trace");
                    long joinMs = System.currentTimeMillis() - j0;

                    send(gv, CastMessages.PAUSE, 0, 0);
                    waitUntil(
                            () -> a.state()[1] == 0 && b.state()[1] == 0 && late.state()[1] == 0,
                            5_000,
                            "cả ba máy dừng");
                    int serverStep = (int) castModule.status().get("buoc");
                    assertEquals(serverStep, a.state()[0], "máy 1 dừng đúng bước server tính");
                    assertEquals(serverStep, b.state()[0], "máy 2");
                    assertEquals(serverStep, late.state()[0], "máy vào muộn");
                    assertTrue(serverStep > 20, "đã chạy được một đoạn: " + serverStep);

                    System.out.printf(
                            "O2: nạp %d ms; vào muộn bắt kịp %d ms; multicast %d/%d/%d gói; NACK %d/%d/%d;"
                                    + " qua TCP %d/%d/%d; %s%n",
                            loadMs,
                            joinMs,
                            a.cast.receiver().framesViaMulticast(),
                            b.cast.receiver().framesViaMulticast(),
                            late.cast.receiver().framesViaMulticast(),
                            a.cast.nacksSent(),
                            b.cast.nacksSent(),
                            late.cast.nacksSent(),
                            a.cast.receiver().framesViaTcp(),
                            b.cast.receiver().framesViaTcp(),
                            late.cast.receiver().framesViaTcp(),
                            castModule.status());
                    // Gói thật sự đi bằng multicast, không lén đi đường TCP: phần lớn trace tới bằng multicast
                    long chunks = CastMessages.compress(trace).length / CastMessages.chunkSize(BAI) + 1;
                    assertTrue(chunks > 30, "trace phải ra nhiều khúc: " + chunks);
                    assertTrue(a.cast.receiver().framesViaMulticast() >= chunks * 8 / 10);
                    assertTrue(a.cast.receiver().framesViaTcp() < chunks / 2, "chỉ gói sửa đi TCP");
                    assertTrue(a.cast.nacksSent() + b.cast.nacksSent() > 0, "mất 5% thì phải xin lại");
                    assertTrue((long) castModule.status().get("nackNhan") > 0);
                    assertTrue(joinMs <= 3_000, "máy vào muộn bắt kịp trong ≤ 2 s + thời gian xin lại: " + joinMs);
                }
            } finally {
                gv.client().close();
            }
        }
    }

    static void send(LoginController.LoggedIn gv, String action, int step, int speed) {
        gv.client().send(MessageType.CAST_CONTROL, new CastMessages.Request(action, BAI, step, speed).encode());
    }
}
