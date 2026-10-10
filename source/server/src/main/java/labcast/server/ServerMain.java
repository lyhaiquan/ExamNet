package labcast.server;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.logging.Logger;
import labcast.common.protocol.PayloadWriter;
import labcast.server.account.User;
import labcast.server.account.UserRepository;
import labcast.server.discovery.DiscoveryResponder;
import labcast.server.discovery.LanInterface;
import labcast.server.discovery.NicInfo;
import labcast.server.ops.BuildInfo;
import labcast.server.ops.StatusEndpoint;
import labcast.server.store.Database;

/**
 * Điểm vào của server: mở database, cắm các module, nghe TCP 7000, trả lời tìm server trên UDP 7002.
 *
 * <p>GIỮ dòng {@code StatusEndpoint.start(...)}: bước deploy lên VPS dựa vào {@code /version} để biết
 * VPS đang chạy đúng commit.
 */
public final class ServerMain {
    private static final Logger LOG = Logger.getLogger(ServerMain.class.getName());

    private ServerMain() {}

    public static void main(String[] args) throws Exception {
        ServerConfig cfg;
        try {
            cfg = ServerConfig.parse(args, System.getenv());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(2);
            return;
        }

        BuildInfo info = BuildInfo.current();
        StatusEndpoint status = StatusEndpoint.start(cfg.statusPort(), info);

        Database db = Database.open(cfg.db());
        LabCastServer server = new LabCastServer(cfg, db, LabCastServer.modules());
        server.start();

        String host = lanAddress(cfg);
        UserRepository users = server.context().require(UserRepository.class);
        DiscoveryResponder discovery = new DiscoveryResponder(cfg.discoveryPort(), () -> {
            String teacher = "";
            try {
                teacher = users.firstTeacher().map(User::fullName).orElse("");
            } catch (Exception e) {
                // tên giáo viên chỉ để hiển thị
            }
            return new PayloadWriter()
                    .str(cfg.lop())
                    .str(teacher)
                    .str(host)
                    .u16(server.port())
                    .bool(false) // TLS: giai đoạn 3
                    .str("")
                    .toBytes();
        });
        discovery.start();

        System.out.println("LabCast " + info.version() + " (commit " + info.commit() + ")");
        System.out.println("  Lớp:            " + cfg.lop());
        System.out.println("  App nối tới:    " + host + ":" + server.port() + " (TCP)");
        System.out.println("  Tìm server:     UDP " + discovery.port());
        System.out.println("  Bài học:        " + cfg.content().toAbsolutePath().normalize());
        System.out.println("  Trạng thái:     http://0.0.0.0:" + status.port() + "/version");
        if (cfg.khongSandbox()) {
            System.out.println(
                    "  CẢNH BÁO: --khong-sandbox — code học viên chạy thẳng trên máy này, chỉ dùng khi phát triển");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            discovery.close();
            server.close();
            try {
                db.close();
            } catch (Exception ignored) {
                // đang tắt
            }
        }));
        Thread.currentThread().join();
    }

    /** Địa chỉ IPv4 của card LAN để quảng bá. Không chọn được card nào thì báo và dùng loopback. */
    private static String lanAddress(ServerConfig cfg) {
        try {
            NicInfo nic = LanInterface.pickOrThrow(cfg.lan());
            String ip = LanInterface.privateAddress(nic)
                    .orElse(
                            nic.ipv4().isEmpty()
                                    ? InetAddress.getLoopbackAddress()
                                    : nic.ipv4().get(0))
                    .getHostAddress();
            System.out.println("  Card LAN:       " + nic.displayName() + " " + ip);
            return ip;
        } catch (SocketException e) {
            LOG.warning(e.getMessage());
            return InetAddress.getLoopbackAddress().getHostAddress();
        }
    }
}
