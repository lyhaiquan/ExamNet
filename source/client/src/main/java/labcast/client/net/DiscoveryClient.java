package labcast.client.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import labcast.common.protocol.Frame;
import labcast.common.protocol.FrameCodec;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.ProtocolException;

/** Tìm server LabCast trong mạng LAN bằng UDP broadcast, học viên không phải gõ IP. */
public final class DiscoveryClient {
    public static final int DEFAULT_PORT = 7002;
    public static final String CLIENT_VERSION = "0.1.0";

    public record ServerInfo(
            String className, String teacherName, String host, int port, boolean tls, String fingerprint) {}

    private DiscoveryClient() {}

    /** Gửi {@code DISCOVER} tới 255.255.255.255 và địa chỉ broadcast của từng card, gom trả lời. */
    public static List<ServerInfo> find(int port, Duration timeout) throws IOException {
        return findAt(broadcastTargets(), port, timeout);
    }

    /** Gửi tới các địa chỉ cho trước (test dùng 127.0.0.1). */
    public static List<ServerInfo> findAt(List<InetAddress> targets, int port, Duration timeout) throws IOException {
        byte[] req = FrameCodec.toBytes(Frame.of(
                MessageType.DISCOVER, new PayloadWriter().str(CLIENT_VERSION).toBytes()));
        Map<String, ServerInfo> found = new LinkedHashMap<>();
        try (DatagramSocket s = new DatagramSocket()) {
            s.setBroadcast(true);
            for (InetAddress t : targets) {
                try {
                    s.send(new DatagramPacket(req, req.length, t, port));
                } catch (IOException e) {
                    // card không gửi broadcast được: thử card khác
                }
            }
            long deadline = System.nanoTime() + timeout.toNanos();
            byte[] buf = new byte[1500];
            while (true) {
                long left = (deadline - System.nanoTime()) / 1_000_000;
                if (left <= 0) {
                    break;
                }
                s.setSoTimeout((int) Math.max(1, left));
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                try {
                    s.receive(p);
                } catch (SocketTimeoutException e) {
                    break;
                }
                try {
                    Frame f = FrameCodec.fromBytes(p.getData(), p.getOffset(), p.getLength());
                    if (f.type() != MessageType.DISCOVER_REPLY) {
                        continue;
                    }
                    ServerInfo info = parse(f);
                    found.putIfAbsent(info.host() + ":" + info.port(), info);
                } catch (ProtocolException e) {
                    // trả lời hỏng: bỏ
                }
            }
        }
        return new ArrayList<>(found.values());
    }

    /** {@code DISCOVER_REPLY(str className, str teacherName, str host, u16 port, bool tls, str fingerprint)}. */
    static ServerInfo parse(Frame f) throws ProtocolException {
        PayloadReader r = f.reader();
        return new ServerInfo(r.str(), r.str(), r.str(), r.u16(), r.bool(), r.str());
    }

    private static List<InetAddress> broadcastTargets() throws IOException {
        Set<InetAddress> out = new LinkedHashSet<>();
        out.add(InetAddress.getByName("255.255.255.255"));
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!ni.isUp() || ni.isLoopback()) {
                continue;
            }
            for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                if (ia.getBroadcast() != null) {
                    out.add(ia.getBroadcast());
                }
            }
        }
        return new ArrayList<>(out);
    }
}
