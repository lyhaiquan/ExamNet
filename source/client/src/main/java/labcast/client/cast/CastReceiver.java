package labcast.client.cast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import labcast.common.cast.CastMessages;
import labcast.common.protocol.Frame;
import labcast.common.protocol.FrameCodec;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.ProtocolException;

/**
 * Nghe nhóm chiếu và nhận gói chiếu đến qua TCP (gói sửa {@code CAST_REPAIR}, gói chuyển đường {@code
 * VIA_TCP}), đưa tất cả qua cùng một {@link SeqTracker}: gói đến bằng hai đường chỉ giao một lần.
 *
 * <p>Gói mới giao ngay cho {@code deliver} (trên luồng nghe hoặc luồng đọc TCP — bên nhận tự chuyển sang
 * luồng giao diện). Khúc dữ liệu luôn được giao kể cả khi cũ hơn mốc theo dõi: máy vào muộn xin lại khúc
 * của trace đã phát từ trước, {@code CastSession} tự bỏ khúc trùng.
 */
public final class CastReceiver implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(CastReceiver.class.getName());
    /** Cùng giá trị với {@code CastModule.DEFAULT_GROUP} phía server (spec §9). */
    public static final InetSocketAddress DEFAULT_GROUP = new InetSocketAddress("239.255.70.1", 7001);

    private final List<NetworkInterface> nics;
    private final InetSocketAddress group;
    private final Consumer<Frame> deliver;
    private final Consumer<CastMessages.Nack> nack;
    private final LongSupplier clock;
    private volatile Predicate<byte[]> drop = d -> false;
    private SeqTracker tracker;
    private MulticastSocket socket;
    private ScheduledExecutorService timer;
    private volatile boolean closed;
    private final AtomicLong viaMulticast = new AtomicLong();
    private final AtomicLong viaTcp = new AtomicLong();
    private final AtomicLong nacksSent = new AtomicLong();

    public CastReceiver(
            List<NetworkInterface> nics,
            InetSocketAddress group,
            Consumer<Frame> deliver,
            Consumer<CastMessages.Nack> nack) {
        this(nics, group, deliver, nack, System::currentTimeMillis);
    }

    CastReceiver(
            List<NetworkInterface> nics,
            InetSocketAddress group,
            Consumer<Frame> deliver,
            Consumer<CastMessages.Nack> nack,
            LongSupplier clock) {
        this.nics = List.copyOf(nics);
        this.group = group;
        this.deliver = deliver;
        this.nack = nack;
        this.clock = clock;
    }

    /**
     * Bắt đầu hẹn giờ xin gói thiếu, mở socket và tham gia nhóm trên từng card; trả số card tham gia
     * được. Không mở được socket hay 0 card vẫn chạy: gói qua TCP vẫn nhận, {@code lastCastSeq} đứng ở
     * −1 và server chuyển máy này sang nhận chiếu qua TCP.
     */
    public int start() {
        timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread th = new Thread(r, "xin-goi-chieu");
            th.setDaemon(true);
            return th;
        });
        timer.scheduleAtFixedRate(this::sendNacks, 10, 10, TimeUnit.MILLISECONDS);
        int joined = 0;
        try {
            MulticastSocket s = new MulticastSocket(null);
            s.setReuseAddress(true); // trước khi bind: nhiều app trên một máy cùng nghe (khi đo, khi demo)
            s.setReceiveBufferSize(1 << 20); // chịu được đợt CAST_DATA (spec §11)
            s.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, true);
            s.bind(new InetSocketAddress(group.getPort())); // bind cổng, KHÔNG bind địa chỉ nhóm (Windows từ chối)
            for (NetworkInterface n : nics) {
                try {
                    s.joinGroup(group, n);
                    joined++;
                } catch (IOException e) {
                    LOG.fine("Không tham gia nhóm chiếu trên " + n.getName() + ": " + e.getMessage());
                }
            }
            socket = s;
            Thread t = new Thread(this::receiveLoop, "nghe-chieu");
            t.setDaemon(true);
            t.start();
        } catch (IOException e) {
            LOG.warning("Không nghe được nhóm chiếu " + group + ": " + e.getMessage() + " — sẽ nhận qua TCP");
        }
        LOG.info("Nghe chiếu " + group + " trên " + joined + "/" + nics.size() + " card");
        return joined;
    }

    /** Gói chiếu nhận qua TCP: {@code CAST_REPAIR(blob datagram)} hoặc gói có cờ {@code VIA_TCP}. */
    public void offerTcp(Frame f) {
        try {
            if (f.type() == MessageType.CAST_REPAIR) {
                byte[] d = f.reader().blob();
                f = FrameCodec.fromBytes(d, 0, d.length);
            }
            viaTcp.incrementAndGet();
            handle(f);
        } catch (ProtocolException e) {
            LOG.fine("Gói chiếu qua TCP hỏng: " + e.getMessage());
        }
    }

    /** SEQ lớn nhất đã nhận; −1 nếu chưa nhận gì. Gửi kèm heartbeat cho server ({@code lastCastSeq}). */
    public synchronized int lastSeq() {
        return tracker == null ? -1 : tracker.lastSeq();
    }

    /** Test và thí nghiệm mất gói (TN3): datagram multicast nào khớp thì coi như mất. */
    public void setDrop(Predicate<byte[]> p) {
        this.drop = p == null ? d -> false : p;
    }

    public long framesViaMulticast() {
        return viaMulticast.get();
    }

    public long framesViaTcp() {
        return viaTcp.get();
    }

    public long nacksSent() {
        return nacksSent.get();
    }

    private void receiveLoop() {
        byte[] buf = new byte[2048];
        while (!closed) {
            try {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                socket.receive(p);
                byte[] d = java.util.Arrays.copyOfRange(p.getData(), p.getOffset(), p.getOffset() + p.getLength());
                if (drop.test(d)) {
                    continue;
                }
                Frame f = FrameCodec.fromBytes(d, 0, d.length);
                viaMulticast.incrementAndGet();
                handle(f);
            } catch (ProtocolException e) {
                LOG.fine("Datagram chiếu hỏng: " + e.getMessage()); // gói rác trên cổng: bỏ
            } catch (SocketException e) {
                if (!closed) {
                    LOG.log(Level.WARNING, "Socket nghe chiếu lỗi", e);
                }
                return;
            } catch (IOException e) {
                LOG.log(Level.FINE, "Nhận datagram lỗi", e);
            }
        }
    }

    private void handle(Frame f) throws ProtocolException {
        MessageType t = f.type();
        if (t != MessageType.CAST_CONTROL && t != MessageType.CAST_DATA && t != MessageType.CAST_KEYFRAME) {
            return;
        }
        int id = CastMessages.castId(f);
        boolean fresh;
        synchronized (this) {
            if (tracker == null || (id != tracker.castId() && t != MessageType.CAST_DATA)) {
                tracker = new SeqTracker(id, f.seq()); // lần đầu, hoặc server đã khởi động lại
            }
            if (id != tracker.castId()) {
                return;
            }
            fresh = tracker.accept(id, f.seq(), clock.getAsLong());
        }
        if (fresh || t == MessageType.CAST_DATA) {
            deliver.accept(f);
        }
    }

    private void sendNacks() {
        try {
            List<int[]> ranges;
            int id;
            synchronized (this) {
                if (tracker == null) {
                    return;
                }
                ranges = tracker.due(clock.getAsLong());
                id = tracker.castId();
            }
            for (int[] r : ranges) {
                nacksSent.incrementAndGet();
                nack.accept(new CastMessages.Nack(id, r[0], r[1]));
            }
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Gửi CAST_NACK lỗi", e);
        }
    }

    @Override
    public void close() {
        closed = true;
        if (timer != null) {
            timer.shutdownNow();
        }
        if (socket != null) {
            socket.close();
        }
    }
}
