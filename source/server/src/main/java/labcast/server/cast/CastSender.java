package labcast.server.cast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.StandardSocketOptions;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import labcast.common.cast.CastMessages;
import labcast.common.protocol.Frame;
import labcast.common.protocol.FrameCodec;
import labcast.common.protocol.MessageType;

/**
 * Phát datagram chiếu lên nhóm multicast: gán SEQ của luồng chiếu (bắt đầu từ 1 — SEQ 0 trên TCP nghĩa
 * là "kết nối tự gán"), giữ bản gốc trong {@link RecentFrames} để sửa gói, giãn nhịp bằng {@link Pacer}.
 * Chỉ luồng phát chiếu gọi {@link #publish}: hàm này có thể ngủ.
 */
public final class CastSender implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(CastSender.class.getName());

    /** Đích của datagram: socket multicast thật, hoặc bộ ghi trong test. */
    interface Sink extends AutoCloseable {
        void send(byte[] datagram) throws IOException;

        @Override
        void close();
    }

    private final Sink sink;
    private final Pacer pacer;
    private final RecentFrames recent = new RecentFrames(4096);
    private final AtomicLong datagrams = new AtomicLong();
    private final AtomicLong bytes = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();
    private volatile int seq;

    CastSender(Sink sink, Pacer pacer) {
        this.sink = sink;
        this.pacer = pacer;
    }

    /**
     * Socket multicast trên đúng card LAN (spec §9): TTL 1 để gói không ra khỏi phòng máy, bật vòng lại
     * để app chạy trên chính máy server cũng nhận được.
     */
    public static CastSender open(NetworkInterface nic, InetSocketAddress group) throws IOException {
        MulticastSocket s = new MulticastSocket();
        s.setTimeToLive(1);
        s.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, true);
        if (nic != null) {
            s.setNetworkInterface(nic);
        }
        return new CastSender(
                new Sink() {
                    @Override
                    public void send(byte[] d) throws IOException {
                        s.send(new DatagramPacket(d, d.length, group));
                    }

                    @Override
                    public void close() {
                        s.close();
                    }
                },
                Pacer.standard());
    }

    /**
     * Gán SEQ kế tiếp, ghi vào vòng đệm, chờ đủ token rồi phát. Lỗi mạng chỉ ghi log: gói vẫn giữ trong
     * vòng đệm để sửa qua TCP, và máy không nhận được sẽ tự chuyển sang nhận qua TCP.
     *
     * @return frame đã gán SEQ, để gửi kèm qua TCP cho máy cần
     */
    public synchronized Frame publish(MessageType t, byte[] payload) throws InterruptedException {
        int s = seq + 1;
        Frame f = new Frame(t, (byte) 0, s, payload);
        byte[] d = FrameCodec.toBytes(f);
        if (d.length > CastMessages.MAX_DATAGRAM) {
            throw new IllegalArgumentException(t + " dài " + d.length + " byte, quá " + CastMessages.MAX_DATAGRAM);
        }
        seq = s;
        recent.put(s, d);
        send(d);
        return f;
    }

    /** Phát lại nguyên văn một datagram đã phát (nhiều máy cùng thiếu một gói). */
    public synchronized void resend(byte[] datagram) throws InterruptedException {
        send(datagram);
    }

    private void send(byte[] d) throws InterruptedException {
        pacer.acquire(d.length);
        try {
            sink.send(d);
            datagrams.incrementAndGet();
            bytes.addAndGet(d.length);
        } catch (IOException e) {
            if (errors.getAndIncrement() % 100 == 0) {
                LOG.warning("Không phát được multicast: " + e.getMessage());
            }
        }
    }

    /** Datagram gốc của SEQ đó nếu còn trong vòng đệm. */
    public Optional<byte[]> recent(int seq) {
        return recent.get(seq);
    }

    /** SEQ của gói cuối đã phát; 0 nếu chưa phát gì. */
    public int lastSeq() {
        return seq;
    }

    public long datagramsSent() {
        return datagrams.get();
    }

    public long bytesSent() {
        return bytes.get();
    }

    @Override
    public void close() {
        sink.close();
    }
}
