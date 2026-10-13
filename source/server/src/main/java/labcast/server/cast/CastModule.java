package labcast.server.cast;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import labcast.common.cast.CastMessages;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.Frame;
import labcast.common.protocol.FrameCodec;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.ProtocolException;
import labcast.common.protocol.Role;
import labcast.server.ServerContext;
import labcast.server.ServerModule;
import labcast.server.discovery.LanInterface;
import labcast.server.discovery.NicInfo;
import labcast.server.lesson.DemoTraces;
import labcast.server.net.Ctx;
import labcast.server.net.Priority;
import labcast.server.session.Session;

/**
 * Chiếu animation cho cả lớp qua multicast (ĐG1, spec §13). Giáo viên gửi {@code CAST_CONTROL} qua
 * TCP; server đóng dấu giờ hẹn rồi phát cho cả lớp trên nhóm 239.255.70.1:7001.
 *
 * <p>Mọi lần phát chạy trên <b>một luồng riêng</b> ("phat-chieu"): bộ giãn nhịp có thể ngủ, không được
 * chiếm luồng nghiệp vụ hay luồng hẹn giờ chung. Socket multicast chỉ mở ở lần chiếu đầu.
 *
 * <p>Giáo viên luôn nhận thêm một bản mọi gói chiếu qua TCP (cờ {@code VIA_TCP}): màn giáo viên thấy
 * ngay kết quả lệnh của mình, kể cả khi máy giáo viên không nghe được multicast.
 */
public final class CastModule implements ServerModule {
    private static final Logger LOG = Logger.getLogger(CastModule.class.getName());
    public static final InetSocketAddress DEFAULT_GROUP = new InetSocketAddress("239.255.70.1", 7001);
    static final long KEYFRAME_MS = 2_000;
    private static final Set<String> ACTIONS = Set.of(
            CastMessages.LOAD,
            CastMessages.PLAY,
            CastMessages.PAUSE,
            CastMessages.SEEK,
            CastMessages.SPEED,
            CastMessages.STOP);

    /** Mở bộ phát. Test thay bằng bộ ghi datagram. */
    @FunctionalInterface
    interface SenderFactory {
        CastSender open(ServerContext ctx) throws IOException;
    }

    private final SenderFactory factory;
    private final int castId;
    private final CastState state = new CastState();
    private ServerContext ctx;
    private ExecutorService exec;
    private NackAggregator nacks;
    private final AtomicLong nacksReceived = new AtomicLong();
    private final AtomicLong repairsUnicast = new AtomicLong();
    private final AtomicLong repairsMulticast = new AtomicLong();
    /** Chỉ luồng phát chiếu mở và dùng; dashboard đọc số liệu nên volatile. */
    private volatile CastSender sender;

    /** Server thật: phát trên card LAN chọn theo {@code --lan}, nhóm mặc định. */
    public CastModule() {
        this(ctx -> CastSender.open(lanCard(ctx.config().lan()), DEFAULT_GROUP));
    }

    /** Chỉ định card và nhóm, ví dụ card loopback và cổng riêng khi test. */
    public CastModule(NetworkInterface nic, InetSocketAddress group) {
        this(ctx -> CastSender.open(nic, group));
    }

    CastModule(SenderFactory factory) {
        this.factory = factory;
        int id;
        do {
            id = new SecureRandom().nextInt();
        } while (id == 0);
        this.castId = id;
    }

    @Override
    public void install(ServerContext ctx) {
        this.ctx = ctx;
        this.exec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "phat-chieu");
            t.setDaemon(true);
            return t;
        });
        this.nacks = new NackAggregator(this::classSize, System::currentTimeMillis);
        ctx.router().on(MessageType.CAST_CONTROL, Set.of(Role.TEACHER), this::onControl);
        ctx.router().on(MessageType.CAST_NACK, this::onNack);
        ctx.scheduler()
                .scheduleAtFixedRate(
                        () -> exec.execute(this::keyframe), KEYFRAME_MS, KEYFRAME_MS, TimeUnit.MILLISECONDS);
        ctx.register(CastModule.class, this);
    }

    /** castId của lần chạy server này. */
    public int castId() {
        return castId;
    }

    /** Tình trạng chiếu cho dashboard. */
    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        CastState.Loaded l = state.loaded();
        m.put("dangChieu", l != null);
        m.put("traceId", l == null ? "" : l.traceId());
        m.put("buoc", l == null ? -1 : state.position(System.currentTimeMillis()));
        CastSender s = sender;
        m.put("seqCuoi", s == null ? 0 : s.lastSeq());
        m.put("goiDaPhat", s == null ? 0 : s.datagramsSent());
        m.put("byteDaPhat", s == null ? 0 : s.bytesSent());
        m.put("nackNhan", nacksReceived.get());
        m.put("suaRieng", repairsUnicast.get());
        m.put("phatLai", repairsMulticast.get());
        return m;
    }

    /**
     * {@code CAST_NACK(i32 castId, i32 fromSeq, i32 toSeq)} → {@code CAST_REPAIR(blob datagram)} qua TCP
     * cho máy xin, hoặc phát lại qua multicast khi nhiều máy cùng thiếu ({@link NackAggregator}).
     */
    private void onNack(Ctx c, Frame f) throws Exception {
        CastMessages.Nack n = CastMessages.Nack.decode(f.payload());
        CastSender s = sender;
        if (n.castId() != castId || s == null) {
            return; // xin gói của lần chạy server trước, hoặc chưa chiếu gì
        }
        nacksReceived.incrementAndGet();
        for (Map.Entry<Integer, NackAggregator.Mode> e :
                nacks.onNack(c.session().userId(), n.from(), n.to()).entrySet()) {
            Optional<byte[]> d = datagram(s, e.getKey());
            if (d.isEmpty()) {
                continue; // đã trôi khỏi vòng đệm và không phải khúc dữ liệu: khung khoá kế tiếp bù
            }
            switch (e.getValue()) {
                case UNICAST -> {
                    repairsUnicast.incrementAndGet();
                    c.reply(
                            Frame.of(
                                    MessageType.CAST_REPAIR,
                                    new PayloadWriter().blob(d.get()).toBytes()),
                            Priority.NORMAL);
                }
                case MULTICAST -> {
                    repairsMulticast.incrementAndGet();
                    exec.execute(() -> {
                        try {
                            s.resend(d.get());
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
                case NONE -> {}
            }
        }
    }

    /** Datagram gốc của SEQ đó: lấy trong vòng đệm, không còn thì dựng lại nếu là khúc của trace đang chiếu. */
    private Optional<byte[]> datagram(CastSender s, int seq) {
        Optional<byte[]> d = s.recent(seq);
        if (d.isPresent()) {
            return d;
        }
        CastState.Loaded l = state.loaded();
        int i = state.chunkIndexOf(seq);
        if (l == null || i < 0) {
            return Optional.empty();
        }
        // Dựng lại đúng từng byte như lần phát đầu: cùng castId, traceId, khúc, SEQ
        return Optional.of(FrameCodec.toBytes(new Frame(
                MessageType.CAST_DATA, (byte) 0, seq, l.data(castId, i).encode())));
    }

    /** Số máy đang trong lớp, để tính ngưỡng 20% khi gộp NACK. */
    private int classSize() {
        int n = 0;
        for (Session s : ctx.sessions().all()) {
            if (s.online() && s.role() == Role.STUDENT) {
                n++;
            }
        }
        return n;
    }

    /** {@code CAST_CONTROL(str action, str traceId, i32 step, i32 speedMilli)}, chỉ giáo viên. */
    private void onControl(Ctx c, Frame f) throws Exception {
        CastMessages.Request r = CastMessages.Request.decode(f.payload());
        if (!ACTIONS.contains(r.action())) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "Lệnh chiếu lạ: " + r.action());
        }
        if (!r.action().equals(CastMessages.LOAD)) {
            exec.execute(() -> guarded(c, () -> act(c, r)));
            return;
        }
        try {
            CastMessages.chunkSize(r.traceId());
        } catch (IllegalArgumentException e) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, e.getMessage());
        }
        DemoTraces traces = ctx.find(DemoTraces.class)
                .orElseThrow(() -> new ProtocolException(ErrorCode.NOT_FOUND, "Server không có kho bài"));
        traces.demoTrace(r.traceId()).whenComplete((lines, err) -> {
            if (err != null || lines == null || lines.isEmpty()) {
                c.error(ErrorCode.NOT_FOUND, "Bài " + r.traceId() + " không có animation giảng giải để chiếu", 0);
                return;
            }
            exec.execute(() -> guarded(c, () -> load(r, lines)));
        });
    }

    private interface Step {
        void run() throws Exception;
    }

    private void guarded(Ctx c, Step s) {
        try {
            s.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Chiếu lỗi", e);
            c.error(ErrorCode.INTERNAL, "Lỗi server khi chiếu: " + e.getMessage(), 0);
        }
    }

    /** Phát mọi khúc dữ liệu trước, rồi mới phát lệnh LOAD: lệnh tới nơi thì máy đã có (gần) đủ trace. */
    private void load(CastMessages.Request r, List<String> lines) throws Exception {
        CastState.Loaded l = CastState.prepare(r.traceId(), lines);
        int first = 0;
        for (int i = 0; i < l.totalChunks(); i++) {
            Frame f = publish(MessageType.CAST_DATA, l.data(castId, i).encode());
            if (i == 0) {
                first = f.seq();
            }
        }
        state.load(l, first);
        publish(MessageType.CAST_CONTROL, state.apply(castId, r, now()).encode());
        LOG.info("Chiếu " + l.traceId() + ": " + l.size() + " bước, " + l.totalChunks() + " khúc, "
                + l.compressed().length + " byte đã nén");
    }

    private void act(Ctx c, CastMessages.Request r) throws Exception {
        if (state.loaded() == null) {
            c.error(ErrorCode.NOT_FOUND, "Chưa chọn bài nào để chiếu", 0);
            return;
        }
        publish(MessageType.CAST_CONTROL, state.apply(castId, r, now()).encode());
        if (r.action().equals(CastMessages.STOP)) {
            state.clear();
        }
    }

    /** Khung khoá cho máy vào muộn và máy lỡ gói cuối; chỉ phát khi đang chiếu. */
    private void keyframe() {
        if (state.loaded() == null) {
            return;
        }
        try {
            publish(
                    MessageType.CAST_KEYFRAME,
                    state.keyframe(castId, sender().lastSeq()).encode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Không phát được khung khoá", e);
        }
    }

    /** Test gọi để khỏi chờ chu kỳ 2 s. */
    void keyframeNow() throws Exception {
        exec.submit(this::keyframe).get(5, TimeUnit.SECONDS);
    }

    private Frame publish(MessageType t, byte[] payload) throws InterruptedException {
        Frame f = sender().publish(t, payload);
        Frame viaTcp = new Frame(f.type(), Frame.FLAG_VIA_TCP, f.seq(), f.payload());
        for (Session s : ctx.sessions().all()) {
            if (s.online() && s.role() == Role.TEACHER) {
                // DROPPABLE: luồng phát không bao giờ phải chờ một máy chậm; mất thì máy đó tự xin lại
                s.send(viaTcp, Priority.DROPPABLE);
            }
        }
        return f;
    }

    private CastSender sender() {
        if (sender == null) {
            try {
                sender = factory.open(ctx);
            } catch (IOException e) {
                // Vẫn chạy được: không máy nào nhận multicast thì cả lớp chuyển sang nhận qua TCP
                LOG.warning("Không mở được socket multicast, chỉ chiếu qua TCP: " + e.getMessage());
                sender = new CastSender(
                        new CastSender.Sink() {
                            @Override
                            public void send(byte[] d) {}

                            @Override
                            public void close() {}
                        },
                        Pacer.standard());
            }
        }
        return sender;
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    /** Card LAN như server quảng bá trong {@code DISCOVER_REPLY}; không chọn được thì để hệ điều hành chọn. */
    static NetworkInterface lanCard(String override) {
        try {
            NicInfo n = LanInterface.pickOrThrow(override);
            return NetworkInterface.getByName(n.name());
        } catch (IOException e) {
            LOG.warning("Chiếu: không chọn được card LAN (" + e.getMessage() + "), để hệ điều hành chọn");
            return null;
        }
    }
}
