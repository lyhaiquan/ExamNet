package labcast.bench.exp;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import labcast.bench.Csv;
import labcast.bench.DelayProxy;
import labcast.bench.Repo;
import labcast.client.journal.JournalClient;
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

/**
 * Thí nghiệm 3 — học viên mất mạng giữa ca thực hành, code có mất không (ĐG2).
 *
 * <p>Một học viên ảo gõ đều {@value #KEYS_PER_SEC} phím/giây vào một bài, mỗi phím là một {@code
 * CODE_DELTA} gửi qua {@link JournalClient} thật của app. Giữa chừng {@link DelayProxy} "rút dây" trong X
 * giây; học viên vẫn gõ tiếp. Ba tình huống:
 *
 * <ul>
 *   <li>{@code rut-day}: rút dây X giây rồi cắm lại, app tự nối lại;
 *   <li>{@code tat-app-khi-mat-mang}: rút dây, gõ thêm, rồi tắt app khi chưa có mạng lại; học viên
 *       đăng nhập máy khác;
 *   <li>{@code tat-app-khi-co-mang}: đang có mạng thì tắt app đột ngột, đăng nhập máy khác.
 * </ul>
 *
 * Đo: số phím đã gõ, số phím có trên server, số phím mất, code trên server có đúng hệt bản trên máy không,
 * thời gian từ lúc cắm dây tới khi server có đủ.
 */
public final class Exp3MatMang {
    static final String BAI = "dsa.sap-xep.noi-bot";
    static final int KEYS_PER_SEC = 20;
    static final long BEFORE_MS = 3_000;
    static final long AFTER_MS = 3_000;
    static final long[] OUTAGE_MS = {0, 2_000, 5_000, 10_000, 30_000};

    public static void main(String[] args) throws Exception {
        int reps = args.length > 0 ? Integer.parseInt(args[0]) : 5;
        Path root = Repo.root();
        var cfg = ServerConfig.forTest().withContent(root.resolve("content"));
        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(
                        cfg, db, List.of(new AccountModule(), new LessonModule(), new JournalModule()));
                var csv = new Csv(
                        "exp3_mat-mang",
                        "tinh_huong",
                        "mat_mang_ms",
                        "lan",
                        "phim_da_go",
                        "phim_tren_server",
                        "phim_mat",
                        "code_khop",
                        "dong_bo_lai_ms")) {
            server.start();
            new UserRepository(db).create("sv", "SV", Role.STUDENT, "1");
            long uid = new UserRepository(db).byUsername("sv").orElseThrow().id();
            CodeStore store = server.context().require(CodeStore.class);
            for (long outage : OUTAGE_MS) {
                for (int rep = 1; rep <= reps; rep++) {
                    run(server, store, uid, "rut-day", outage, rep, csv);
                }
            }
            for (long outage : new long[] {2_000, 5_000, 10_000}) {
                for (int rep = 1; rep <= reps; rep++) {
                    run(server, store, uid, "tat-app-khi-mat-mang", outage, rep, csv);
                }
            }
            for (int rep = 1; rep <= reps; rep++) {
                run(server, store, uid, "tat-app-khi-co-mang", 0, rep, csv);
            }
            System.out.println("Đã ghi " + csv.file());
        }
    }

    private static void run(
            LabCastServer server, CodeStore store, long uid, String scenario, long outage, int rep, Csv csv)
            throws Exception {
        String start = store.current(uid, BAI).code();
        long seq0 = store.current(uid, BAI).seq();
        StringBuilder doc = new StringBuilder(start);
        long seq = seq0;
        var resync = new java.util.concurrent.atomic.AtomicLong(-1);
        Thread resyncThread = null;
        long period = 1_000 / KEYS_PER_SEC;
        try (var proxy = new DelayProxy(server.port(), 0, 0)) {
            LcpClient c = login(proxy.port());
            var j = new JournalClient(c);
            // nhật ký chẩn đoán: in ra khi lần đo bị mất phím
            long tLog = System.currentTimeMillis();
            var trace = new java.util.concurrent.ConcurrentLinkedQueue<String>();
            java.util.function.Consumer<String> note = s -> trace.add((System.currentTimeMillis() - tLog) + "ms "
                    + Thread.currentThread().getName() + " " + s);
            c.onStateChange(s -> note.accept("STATE " + s));
            c.on(MessageType.ERROR, f -> note.accept("ERROR " + describe(f)));
            c.on(MessageType.RESUME_STATE, f -> note.accept("RESUME_STATE " + describe(f)));
            c.on(MessageType.BYE, f -> note.accept("BYE"));
            traceRef = trace;
            noteRef = note;
            unackedRef = j;
            try {
                long t0 = System.currentTimeMillis();
                long unplugAt = t0 + BEFORE_MS;
                long plugAt = unplugAt + outage;
                long endAt = plugAt + AFTER_MS;
                boolean unplugged = false;
                boolean crashed = false;
                while (System.currentTimeMillis() < endAt) {
                    long now = System.currentTimeMillis();
                    if (!unplugged && now >= unplugAt && outage > 0) {
                        proxy.unplug();
                        unplugged = true;
                        noteRef.accept("UNPLUG seq=" + seq);
                    }
                    if (unplugged && now >= plugAt) {
                        if (scenario.equals("tat-app-khi-mat-mang")) {
                            crashed = true;
                            break;
                        }
                        proxy.plug();
                        noteRef.accept("PLUG seq=" + seq);
                        long target = seq;
                        long plugged = System.currentTimeMillis();
                        unplugged = false;
                        // đo trong luồng riêng để học viên vẫn gõ tiếp như người thật
                        resyncThread = new Thread(() -> resync.set(waitFor(store, uid, target, plugged, 60_000)));
                        resyncThread.start();
                        continue;
                    }
                    if (scenario.equals("tat-app-khi-co-mang") && now >= unplugAt) {
                        crashed = true;
                        break;
                    }
                    char ch = (char) ('a' + (int) (seq % 26));
                    seq++;
                    j.submit(new CodeDelta(BAI, seq, doc.length(), 0, String.valueOf(ch), now));
                    doc.append(ch);
                    Thread.sleep(period);
                }
                if (!crashed) {
                    waitFor(store, uid, seq, System.currentTimeMillis(), 60_000);
                }
                if (resyncThread != null) resyncThread.join();
                noteRef.accept("END unacked=" + unackedRef.unacked() + " state=" + c.state() + " serverSeq="
                        + store.lastSeq(uid, BAI));
            } finally {
                // tắt app đột ngột: không BYE, chỉ đóng socket (như kill tiến trình)
                proxy.unplug();
                c.close();
            }
        }
        // "đăng nhập máy khác": đọc lại code server đang giữ
        var snap = store.current(uid, BAI);
        long typed = seq - seq0;
        long onServer = snap.seq() - seq0;
        boolean match = snap.code().equals(doc.substring(0, start.length() + (int) onServer));
        csv.row(scenario, outage, rep, typed, onServer, typed - onServer, match, resync.get());
        System.out.printf(
                "%-22s mất mạng %5d ms, lần %d: gõ %4d, server có %4d, mất %4d, khớp %s, đồng bộ lại %d ms%n",
                scenario, outage, rep, typed, onServer, typed - onServer, match, resync.get());
        if (scenario.equals("rut-day") && typed != onServer) {
            traceRef.forEach(s -> System.out.println("    " + s));
        }
    }

    private static java.util.Queue<String> traceRef;
    private static java.util.function.Consumer<String> noteRef;
    private static JournalClient unackedRef;

    private static String describe(labcast.common.protocol.Frame f) {
        byte[] p = f.payload();
        return new String(p, java.nio.charset.StandardCharsets.UTF_8).replaceAll("\\p{Cntrl}", ".");
    }

    /** Đợi server có tới {@code seq}; trả số ms kể từ {@code since}, -1 nếu quá hạn. */
    private static long waitFor(CodeStore store, long uid, long seq, long since, long timeoutMs) {
        try {
            while (System.currentTimeMillis() - since < timeoutMs) {
                if (store.lastSeq(uid, BAI) >= seq) return System.currentTimeMillis() - since;
                Thread.sleep(5);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return -1;
    }

    private static LcpClient login(int port) throws Exception {
        var c = new LcpClient("bench-exp3");
        c.connect("127.0.0.1", port);
        var ok = c.expect(MessageType.AUTH_OK);
        c.send(MessageType.AUTH, new PayloadWriter().str("sv").str("1").toBytes());
        String token = ok.get(5, TimeUnit.SECONDS).reader().str();
        c.setToken(token);
        c.setResumeLesson(() -> BAI);
        return c;
    }
}
