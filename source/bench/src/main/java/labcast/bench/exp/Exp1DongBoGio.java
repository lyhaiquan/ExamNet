package labcast.bench.exp;

import java.util.List;
import labcast.bench.Csv;
import labcast.bench.DelayProxy;
import labcast.client.clock.ClockSync;
import labcast.client.net.LcpClient;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.store.Database;
import labcast.server.sync.SyncModule;

/**
 * Thí nghiệm 1 — đồng bộ giờ cho ca thực hành (ĐG3).
 *
 * <p>App và server chạy cùng máy nên giờ thật của server chính là {@code System.currentTimeMillis()}.
 * Máy học viên lệch giờ được giả lập bằng đồng hồ giả {@code giờ thật + lệch} đưa vào {@link ClockSync};
 * độ trễ mạng cấy bằng {@link DelayProxy} (mỗi chiều riêng, để thấy cả trường hợp trễ không đối xứng).
 *
 * <p>Mỗi lần đo: app mới nối qua proxy, chạy đúng {@code ClockSync.start} như app thật (5 mẫu cách 100
 * ms), rồi so:
 *
 * <ul>
 *   <li>baseline — dùng giờ máy: sai số = giờ máy − giờ server = độ lệch;
 *   <li>LabCast — dùng {@code serverNow()}: sai số = serverNow − giờ server.
 * </ul>
 *
 * Sai số này cũng chính là độ sớm/muộn mà máy đó thấy ca thực hành mở và đóng.
 *
 * <pre>
 * mvnw -q -pl bench -am package -DskipTests
 * java -cp ... labcast.bench.exp.Exp1DongBoGio [số lần lặp, mặc định 10]
 * </pre>
 */
public final class Exp1DongBoGio {
    static final long[] SKEW_MS = {0, 30_000, -60_000, 300_000};
    static final long[][] DELAY_MS = {{0, 0}, {25, 25}, {100, 100}, {20, 180}};

    public static void main(String[] args) throws Exception {
        int reps = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(ServerConfig.forTest(), db, List.of(new SyncModule()));
                var csv = new Csv(
                        "exp1_dong-bo-gio",
                        "lech_gio_ms",
                        "tre_len_ms",
                        "tre_xuong_ms",
                        "lan",
                        "sai_so_baseline_ms",
                        "sai_so_labcast_ms",
                        "offset_uoc_luong_ms",
                        "rtt_mau_tot_nhat_ms")) {
            server.start();
            for (long[] d : DELAY_MS) {
                try (var proxy = new DelayProxy(server.port(), d[0], d[1])) {
                    for (long skew : SKEW_MS) {
                        for (int rep = 1; rep <= reps; rep++) {
                            measure(proxy, skew, d, rep, csv);
                        }
                    }
                }
            }
            System.out.println("Đã ghi " + csv.file());
        }
    }

    private static void measure(DelayProxy proxy, long skew, long[] d, int rep, Csv csv) throws Exception {
        var sync = new ClockSync(() -> System.currentTimeMillis() + skew);
        try (var c = new LcpClient("bench-exp1")) {
            c.connect("127.0.0.1", proxy.port());
            sync.start(c);
            // 5 yêu cầu cách 100 ms, câu trả lời cuối về sau thêm một vòng trễ
            Thread.sleep(500 + 2 * (d[0] + d[1]) + 300);
            if (!sync.isSynced()) {
                throw new IllegalStateException("không nhận được mẫu đồng bộ nào");
            }
            long real = System.currentTimeMillis();
            long labcast = sync.serverNow() - real;
            long baseline = sync.localNow() - real;
            csv.row(skew, d[0], d[1], rep, baseline, labcast, sync.offsetMs(), sync.bestDelayMs());
            System.out.printf(
                    "lệch %+7d ms, trễ %3d/%3d ms, lần %2d: baseline %+7d ms, LabCast %+4d ms (RTT %d)%n",
                    skew, d[0], d[1], rep, baseline, labcast, sync.bestDelayMs());
        }
    }
}
