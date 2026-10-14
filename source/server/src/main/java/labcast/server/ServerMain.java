package labcast.server;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.logging.Logger;
import labcast.common.protocol.PayloadWriter;
import labcast.server.account.User;
import labcast.server.account.UserRepository;
import labcast.server.cast.CastModule;
import labcast.server.discovery.DiscoveryResponder;
import labcast.server.discovery.LanInterface;
import labcast.server.discovery.NicInfo;
import labcast.server.lesson.LessonPreparer;
import labcast.server.lesson.LessonRepository;
import labcast.server.ops.BuildInfo;
import labcast.server.ops.ConsoleUtf8;
import labcast.server.ops.Dashboard;
import labcast.server.ops.LogBuffer;
import labcast.server.ops.StatusEndpoint;
import labcast.server.run.DockerSandbox;
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
        ConsoleUtf8.install(); // trước mọi dòng in ra: chữ tiếng Việt không vỡ dấu trong cmd
        if (java.util.List.of(args).contains("--help")
                || java.util.List.of(args).contains("-h")) {
            System.out.println(ServerConfig.USAGE);
            return;
        }
        ServerConfig cfg;
        try {
            cfg = ServerConfig.parse(args, System.getenv());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(2);
            return;
        }

        // Giữ log gần đây trong bộ nhớ cho dashboard
        LogBuffer logs = new LogBuffer(500);
        Logger.getLogger("").addHandler(logs);

        BuildInfo info = BuildInfo.current();
        StatusEndpoint status = StatusEndpoint.start(cfg.statusPort(), info);

        Database db = Database.open(cfg.db());
        LabCastServer server = new LabCastServer(cfg, db, LabCastServer.modules());
        server.start();
        status.enableDashboard(new Dashboard(server.context(), info, logs, server.port())::json);
        // Sinh trước test ẩn của mọi bài ở luồng nền (lưu đệm theo hash), lượt nộp đầu khỏi phải chờ
        server.context().find(LessonPreparer.class).ifPresent(p -> {
            Thread t = new Thread(
                    () -> p.warmUp(
                            server.context().require(LessonRepository.class).all()),
                    "chuan-bi-bai");
            t.setDaemon(true);
            t.start();
        });

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
        System.out.println("  Môn:            "
                + (cfg.mon() == null
                        ? "cả DSA và SQL — mỗi lớp một môn thì ghi mon = dsa hoặc mon = sql vào file cấu hình"
                        : cfg.mon().toUpperCase()));
        System.out.println("  App nối tới:    " + host + ":" + server.port() + " (TCP)");
        System.out.println("  Tìm server:     UDP " + discovery.port());
        System.out.println("  Chiếu bài:      multicast "
                + CastModule.DEFAULT_GROUP.getAddress().getHostAddress() + ":" + CastModule.DEFAULT_GROUP.getPort()
                + " (UDP, TTL 1, trên card LAN)");
        System.out.println("  Bài học:        " + cfg.content().toAbsolutePath().normalize());
        System.out.println("  Trạng thái:     http://0.0.0.0:" + status.port() + "/version");
        System.out.println(
                "  Dashboard:      http://localhost:" + status.port() + "/  (chỉ mở được trên chính máy này)");
        if (!cfg.khongSandbox()) {
            if (!DockerSandbox.available()) {
                System.out.println(
                        "  CẢNH BÁO: Docker chưa chạy — bật Docker Desktop, nếu không mọi lượt chạy thử sẽ báo"
                                + " lỗi SANDBOX_DOWN");
            } else if (!DockerSandbox.imageAvailable(DockerSandbox.DEFAULT_IMAGE)) {
                System.out.println(
                        "  CẢNH BÁO: thiếu image " + DockerSandbox.DEFAULT_IMAGE + ". Đứng ở thư mục gốc repo"
                                + " chạy: docker build -t " + DockerSandbox.DEFAULT_IMAGE + " runner");
            } else {
                System.out.println("  Sandbox:        Docker, image " + DockerSandbox.DEFAULT_IMAGE);
            }
        }
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
            // --lan chỉ đúng card chỉ có địa chỉ tự cấp 169.254.x: vẫn theo ý người chạy
            String ip = LanInterface.usableAddress(nic)
                    .or(() -> nic.ipv4().stream().findFirst())
                    .orElse(InetAddress.getLoopbackAddress())
                    .getHostAddress();
            System.out.println("  Card LAN:       " + nic.displayName() + " " + ip);
            return ip;
        } catch (SocketException e) {
            LOG.warning(e.getMessage());
            return InetAddress.getLoopbackAddress().getHostAddress();
        }
    }
}
