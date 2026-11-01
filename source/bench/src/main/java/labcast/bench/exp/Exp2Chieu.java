package labcast.bench.exp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import labcast.client.net.LcpClient;
import labcast.common.cast.CastMessages;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.ServerContext;
import labcast.server.ServerModule;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.cast.CastModule;
import labcast.server.lesson.DemoTraces;
import labcast.server.session.Session;
import labcast.server.store.Database;
import labcast.server.sync.SyncModule;

/**
 * Thí nghiệm 2, máy giáo viên — chiếu animation cho cả lớp qua multicast (ĐG1).
 *
 * <p>Bật server LabCast thật (đăng nhập, đồng bộ giờ, chiếu) trên cổng 7000 với trace dựng sẵn, tạo tài
 * khoản {@code gv} và {@code sv1..sv9} (mật khẩu {@code 1}), đăng nhập giáo viên rồi chờ lệnh trên stdin
 * (do {@code tn2.py} gửi):
 *
 * <ul>
 *   <li>{@code CHO <k>}: đợi đủ k học viên online, in {@code SAN_SANG};
 *   <li>{@code LUOT <tên> <n>}: chiếu n lần, mỗi lần LOAD → PLAY → SPEED → PAUSE → SEEK → PLAY → STOP, in
 *       {@code XONG <tên>};
 *   <li>{@code THOAT}.
 * </ul>
 *
 * Ghi {@code tn2_giao-vien.csv}: mỗi lệnh một dòng kèm bộ đếm của server (byte, gói đã phát, NACK, sửa
 * riêng, phát lại, số máy qua TCP).
 */
public final class Exp2Chieu {
    static final String BAI = "dsa.do-thi.dijkstra";

    /** Kịch bản một lần chiếu: lệnh, bước, tốc độ (‰), nghỉ sau lệnh (ms). */
    private static final Object[][] KICH_BAN = {
        {CastMessages.LOAD, -1, 1000, 2_000},
        {CastMessages.PLAY, -1, 1000, 3_000},
        {CastMessages.SPEED, -1, 2000, 2_000},
        {CastMessages.PAUSE, -1, 0, 1_000},
        {CastMessages.SEEK, 5, 0, 1_000},
        {CastMessages.PLAY, 5, 0, 2_000},
        {CastMessages.STOP, -1, 0, 1_000},
    };

    public static void main(String[] args) throws Exception {
        Path trace = Path.of(args.length > 0 ? args[0] : "tn2.trace");
        Path out = Path.of(args.length > 1 ? args[1] : "tn2_giao-vien.csv");
        List<String> lines = Files.readAllLines(trace, StandardCharsets.UTF_8);
        var cfg = ServerConfig.defaults().withDb(":memory:").withStatusPort(0);
        ServerModule traces = ctx -> ctx.register(
                DemoTraces.class, id -> CompletableFuture.completedFuture(id.equals(BAI) ? lines : List.of()));
        // Phát multicast trên đúng card có IP mà học viên dùng để nối (máy nhiều card: Wi-Fi, VPN, Hyper-V…)
        CastModule cast = args.length > 2
                ? new CastModule(
                        NetworkInterface.getByInetAddress(InetAddress.getByName(args[2])), CastModule.DEFAULT_GROUP)
                : new CastModule();
        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(cfg, db, List.of(new AccountModule(), new SyncModule(), traces, cast));
                var csv = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
            server.start();
            var users = new UserRepository(db);
            users.create("gv", "Giáo viên", Role.TEACHER, "1");
            for (int i = 1; i <= 9; i++) {
                users.create("sv" + i, "Học viên " + i, Role.STUDENT, "1");
            }
            ServerContext ctx = server.context();
            LcpClient gv = login(server.port());
            System.out.println("SERVER " + server.port() + " trace " + lines.size() + " dòng");
            csv.println(
                    "luot,lan,lenh,gio_server_gui,byte_da_phat,goi_da_phat,nack_nhan,sua_rieng,phat_lai,may_qua_tcp,"
                            + "hoc_vien_online");
            var in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            String cmd;
            while ((cmd = in.readLine()) != null) {
                String[] p = cmd.trim().split("\\s+");
                switch (p[0]) {
                    case "CHO" -> {
                        int k = Integer.parseInt(p[1]);
                        while (online(ctx) < k) {
                            Thread.sleep(200);
                        }
                        System.out.println("SAN_SANG " + online(ctx));
                    }
                    case "LUOT" -> {
                        for (int lan = 1; lan <= Integer.parseInt(p[2]); lan++) {
                            for (Object[] b : KICH_BAN) {
                                var r = new CastMessages.Request((String) b[0], BAI, (int) b[1], (int) b[2]);
                                long sent = System.currentTimeMillis();
                                gv.send(MessageType.CAST_CONTROL, r.encode());
                                Thread.sleep((int) b[3]);
                                Map<String, Object> s = cast.status();
                                csv.println(String.join(
                                        ",",
                                        p[1],
                                        "" + lan,
                                        (String) b[0],
                                        "" + sent,
                                        "" + s.get("byteDaPhat"),
                                        "" + s.get("goiDaPhat"),
                                        "" + s.get("nackNhan"),
                                        "" + s.get("suaRieng"),
                                        "" + s.get("phatLai"),
                                        "" + s.get("mayQuaTcp"),
                                        "" + online(ctx)));
                                csv.flush();
                            }
                            System.out.println("LAN " + p[1] + " " + lan);
                        }
                        System.out.println("XONG " + p[1]);
                    }
                    case "THOAT" -> {
                        gv.close();
                        System.out.println("DA_THOAT");
                        return;
                    }
                    default -> System.out.println("LOI lệnh lạ: " + cmd);
                }
            }
        }
    }

    private static int online(ServerContext ctx) {
        int n = 0;
        for (Session s : ctx.sessions().all()) {
            if (s.online() && s.role() == Role.STUDENT) {
                n++;
            }
        }
        return n;
    }

    private static LcpClient login(int port) throws Exception {
        var c = new LcpClient("tn2-giao-vien");
        c.connect("127.0.0.1", port);
        var ok = c.expect(MessageType.AUTH_OK, MessageType.AUTH_FAIL);
        c.send(MessageType.AUTH, new PayloadWriter().str("gv").str("1").toBytes());
        if (ok.get(5, TimeUnit.SECONDS).type() != MessageType.AUTH_OK) {
            throw new IllegalStateException("giáo viên không đăng nhập được");
        }
        return c;
    }
}
