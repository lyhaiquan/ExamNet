package labcast.bench.exp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import labcast.client.cast.CastReceiver;
import labcast.client.cast.CastSession;
import labcast.client.cast.LocalNic;
import labcast.client.clock.ClockSync;
import labcast.client.net.LcpClient;
import labcast.common.cast.CastMessages;
import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;

/**
 * Thí nghiệm 2, máy học viên — nghe buổi chiếu bằng đúng {@link CastReceiver} và {@link CastSession} của
 * app (không giao diện), ghi mỗi gói điều khiển nhận được: giờ hẹn {@code at} của server, giờ nhận quy
 * về đồng hồ server (ClockSync), đi đường multicast hay TCP, cùng bộ đếm gói / NACK / byte.
 *
 * <p>Lệnh trên stdin (do {@code tn2.py} gửi): {@code LUOT <tên> <tỉ lệ bỏ gói 0..1> <chặn multicast 0|1>}
 * đổi lượt đo; {@code THOAT} ghi dòng tổng kết rồi thoát. Bỏ gói giả lập bằng {@link
 * CastReceiver#setDrop}, đếm riêng với gói mất thật trên Wi-Fi.
 */
public final class Exp2Nghe {
    private static volatile String luot = "-";
    private static volatile double matGoi;
    private static volatile boolean chan;
    private static final AtomicLong byteMc = new AtomicLong();
    private static final AtomicLong goiMc = new AtomicLong();
    private static final AtomicLong goiBo = new AtomicLong();

    public static void main(String[] args) throws Exception {
        String host = args[0];
        String user = args[1];
        Path out = Path.of(args.length > 2 ? args[2] : "tn2_" + user + ".csv");
        String may = args.length > 3 ? args[3] : user;
        ExecutorService ui = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "tn2-ui");
            t.setDaemon(true);
            return t;
        });
        try (var csv = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8));
                var c = new LcpClient("tn2-" + may)) {
            csv.println("may,luot,loai,lenh,buoc,dang_phat,gio_hen_at,gio_server_nhan,som_ms,duong,"
                    + "offset_ms,rtt_ms,goi_mc,goi_tcp,nack,goi_bo_gia_lap,byte_mc");
            var helloAck = c.expect(MessageType.HELLO_ACK);
            c.connect(host, 7000);
            PayloadReader h = helloAck.get(5, TimeUnit.SECONDS).reader();
            h.i64();
            h.str();
            int castId = h.hasRemaining() ? h.i32() : 0;
            var ok = c.expect(MessageType.AUTH_OK, MessageType.AUTH_FAIL);
            c.send(MessageType.AUTH, new PayloadWriter().str(user).str("1").toBytes());
            Frame auth = ok.get(5, TimeUnit.SECONDS);
            if (auth.type() != MessageType.AUTH_OK) {
                throw new IllegalStateException(user + " không đăng nhập được");
            }
            c.setToken(auth.reader().str());
            var clock = new ClockSync();
            clock.start(c);
            Thread.sleep(1_000);
            System.out.println("DONG_BO offset " + clock.offsetMs() + " ms, RTT " + clock.bestDelayMs() + " ms");

            AtomicLong nack = new AtomicLong();
            CastSession[] session = new CastSession[1];
            Object lock = new Object();
            java.util.function.Consumer<CastMessages.Nack> sendNack = n -> {
                nack.incrementAndGet();
                c.send(MessageType.CAST_NACK, n.encode());
            };
            session[0] = new CastSession(clock, sendNack);
            List<NetworkInterface> nics = LocalNic.forLocalAddress(c.localAddress());
            CastReceiver[] rx = new CastReceiver[1];
            rx[0] = new CastReceiver(
                    nics,
                    CastReceiver.DEFAULT_GROUP,
                    f -> {
                        long recv = clock.serverNow();
                        String duong = Thread.currentThread().getName().equals("nghe-chieu") ? "mc" : "tcp";
                        ui.execute(() -> {
                            if (f.type() == MessageType.CAST_CONTROL) {
                                try {
                                    var k = CastMessages.Control.decode(f.payload());
                                    synchronized (lock) {
                                        csv.println(String.join(
                                                ",",
                                                may,
                                                luot,
                                                "CONTROL",
                                                k.action(),
                                                "" + k.step(),
                                                "" + k.playing(),
                                                "" + k.at(),
                                                "" + recv,
                                                "" + (k.at() - recv),
                                                duong,
                                                "" + clock.offsetMs(),
                                                "" + clock.bestDelayMs(),
                                                counters(rx[0], nack)));
                                        csv.flush();
                                    }
                                } catch (Exception e) {
                                    // gói hỏng: CastSession tự bỏ
                                }
                            }
                            session[0].onFrame(f);
                        });
                    },
                    sendNack);
            rx[0].expectCastId(castId);
            rx[0].setDrop(d -> {
                byteMc.addAndGet(d.length);
                goiMc.incrementAndGet();
                if (chan || (matGoi > 0 && ThreadLocalRandom.current().nextDouble() < matGoi)) {
                    goiBo.incrementAndGet();
                    return true;
                }
                return false;
            });
            for (MessageType t : List.of(
                    MessageType.CAST_CONTROL,
                    MessageType.CAST_DATA,
                    MessageType.CAST_KEYFRAME,
                    MessageType.CAST_REPAIR)) {
                c.on(t, rx[0]::offerTcp);
            }
            c.heartbeat().addField("lastCastSeq", rx[0]::lastSeq);
            boolean[] ready = {false};
            session[0].onChange(() -> {
                boolean now = session[0].ready();
                if (now && !ready[0]) {
                    synchronized (lock) {
                        csv.println(String.join(
                                ",",
                                may,
                                luot,
                                "READY",
                                "",
                                "",
                                "",
                                "",
                                "" + clock.serverNow(),
                                "",
                                "",
                                "" + clock.offsetMs(),
                                "" + clock.bestDelayMs(),
                                counters(rx[0], nack)));
                        csv.flush();
                    }
                }
                ready[0] = now;
            });
            int joined = rx[0].start();
            System.out.println("NGHE " + joined + " card, castId " + castId);

            var in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            String cmd;
            while ((cmd = in.readLine()) != null) {
                String[] p = cmd.trim().split("\\s+");
                if (p[0].equals("LUOT")) {
                    luot = p[1];
                    matGoi = Double.parseDouble(p[2]);
                    chan = p[3].equals("1");
                    System.out.println("LUOT " + luot + " bo-goi " + matGoi + " chan " + chan);
                } else if (p[0].equals("THOAT")) {
                    break;
                }
            }
            synchronized (lock) {
                csv.println(String.join(
                        ",",
                        may,
                        luot,
                        "TONG",
                        "",
                        "",
                        "",
                        "",
                        "" + clock.serverNow(),
                        "",
                        "",
                        "" + clock.offsetMs(),
                        "" + clock.bestDelayMs(),
                        counters(rx[0], nack)));
            }
            rx[0].close();
            ui.shutdown();
            System.out.println("DA_THOAT");
        }
    }

    private static String counters(CastReceiver rx, AtomicLong nack) {
        return String.join(
                ",",
                "" + rx.framesViaMulticast(),
                "" + rx.framesViaTcp(),
                "" + nack.get(),
                "" + goiBo.get(),
                "" + byteMc.get());
    }
}
