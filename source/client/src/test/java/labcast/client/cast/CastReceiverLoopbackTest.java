package labcast.client.cast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import labcast.common.cast.CastMessages;
import labcast.common.protocol.Frame;
import labcast.common.protocol.FrameCodec;
import labcast.common.protocol.MessageType;
import labcast.server.cast.CastSender;
import org.junit.jupiter.api.Test;

/** Multicast thật trên card loopback. Máy không có đường multicast (một số máy CI) thì bỏ qua. */
class CastReceiverLoopbackTest {

    static InetSocketAddress freeGroup() throws Exception {
        try (var s = new DatagramSocket(0)) {
            return new InetSocketAddress("239.255.70.1", s.getLocalPort());
        }
    }

    static byte[] keyframe(int castId) {
        return new CastMessages.Keyframe(castId, "x", 1, 1, 0, false, 1000, 0, 0).encode();
    }

    static void waitFor(java.util.function.BooleanSupplier ok, long ms) throws InterruptedException {
        long end = System.currentTimeMillis() + ms;
        while (!ok.getAsBoolean() && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
    }

    @Test
    void gui50Goi_nhanDu50_dungThuTu_haiAppCungMayDeuNhan() throws Exception {
        NetworkInterface lo = NetworkInterface.getByName("lo");
        assumeTrue(lo != null, "không có card loopback tên lo");
        InetSocketAddress group = freeGroup();
        List<Frame> a = new CopyOnWriteArrayList<>();
        List<Frame> b = new CopyOnWriteArrayList<>();
        try (var ra = new CastReceiver(List.of(lo), group, a::add, n -> {});
                var rb = new CastReceiver(List.of(lo), group, b::add, n -> {});
                var sender = CastSender.open(lo, group)) {
            assumeTrue(ra.start() == 1 && rb.start() == 1, "không tham gia được nhóm trên loopback");
            sender.publish(MessageType.CAST_KEYFRAME, keyframe(7));
            waitFor(() -> !a.isEmpty(), 2_000);
            assumeTrue(!a.isEmpty(), "máy này không chuyển multicast trên loopback");

            for (int i = 1; i < 50; i++) {
                sender.publish(MessageType.CAST_KEYFRAME, keyframe(7));
            }
            waitFor(() -> a.size() >= 50 && b.size() >= 50, 5_000);
            assertEquals(50, a.size());
            assertEquals(50, b.size(), "SO_REUSEADDR: app thứ hai trên cùng máy cũng nhận đủ");
            for (int i = 0; i < 50; i++) {
                assertEquals(i + 1, a.get(i).seq());
            }
            assertEquals(50, ra.lastSeq());
            assertEquals(50, ra.framesViaMulticast());
        }
    }

    @Test
    void matGoi_xinLai_goiSuaQuaTcpLapDayLo_khongGiaoTrung() throws Exception {
        NetworkInterface lo = NetworkInterface.getByName("lo");
        assumeTrue(lo != null);
        InetSocketAddress group = freeGroup();
        List<Frame> got = new CopyOnWriteArrayList<>();
        List<CastMessages.Nack> nacks = new CopyOnWriteArrayList<>();
        try (var r = new CastReceiver(List.of(lo), group, got::add, nacks::add);
                var sender = CastSender.open(lo, group)) {
            assumeTrue(r.start() == 1);
            sender.publish(MessageType.CAST_KEYFRAME, keyframe(7));
            waitFor(() -> !got.isEmpty(), 2_000);
            assumeTrue(!got.isEmpty(), "máy này không chuyển multicast trên loopback");

            // Gói SEQ 5..7 "mất trên đường"
            r.setDrop(d -> {
                try {
                    int seq = FrameCodec.fromBytes(d, 0, d.length).seq();
                    return seq >= 5 && seq <= 7;
                } catch (Exception e) {
                    return false;
                }
            });
            for (int i = 2; i <= 10; i++) {
                sender.publish(MessageType.CAST_KEYFRAME, keyframe(7));
            }
            waitFor(() -> !nacks.isEmpty(), 2_000);
            assertEquals(new CastMessages.Nack(7, 5, 7), nacks.get(0));

            // Server gửi lại qua TCP đúng byte gốc
            for (int s = 5; s <= 7; s++) {
                byte[] d = sender.recent(s).orElseThrow();
                r.offerTcp(Frame.of(
                        MessageType.CAST_REPAIR,
                        new labcast.common.protocol.PayloadWriter().blob(d).toBytes()));
            }
            r.offerTcp(Frame.of(
                    MessageType.CAST_REPAIR,
                    new labcast.common.protocol.PayloadWriter()
                            .blob(sender.recent(6).orElseThrow())
                            .toBytes()));
            assertEquals(10, got.size(), "đủ 10 gói, gói 6 sửa hai lần chỉ giao một");
            assertTrue(got.stream().mapToInt(Frame::seq).distinct().count() == 10);
            assertEquals(4, r.framesViaTcp());
        }
    }
}
