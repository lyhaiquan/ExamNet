package labcast.client.cast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import labcast.common.cast.CastMessages;
import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import org.junit.jupiter.api.Test;

/** Phần lọc gói của {@link CastReceiver}, không cần multicast thật: gói đưa vào qua đường TCP. */
class CastReceiverTest {

    static Frame keyframe(int castId, int seq) {
        return new Frame(
                MessageType.CAST_KEYFRAME,
                (byte) 0,
                seq,
                new CastMessages.Keyframe(castId, "x", 1, 1, 0, false, 1000, 0, 0).encode());
    }

    /**
     * Lớp DSA và lớp SQL cùng một mạng LAN phát lên cùng nhóm: app đã biết castId của server mình (từ
     * {@code HELLO_ACK}) thì bỏ hẳn gói của lớp kia, không coi đó là "server vừa khởi động lại".
     */
    @Test
    void daBietCastId_boGoiCuaLopKhac_khongLamLechSoThuTu() {
        List<Frame> got = new CopyOnWriteArrayList<>();
        try (var r = new CastReceiver(List.of(), new InetSocketAddress("239.255.70.1", 7001), got::add, n -> {})) {
            r.expectCastId(7);
            r.offerTcp(keyframe(9, 100));
            assertEquals(0, got.size(), "gói của lớp khác");
            assertEquals(-1, r.lastSeq());

            r.offerTcp(keyframe(7, 1));
            r.offerTcp(keyframe(9, 101));
            r.offerTcp(keyframe(7, 2));
            assertEquals(2, got.size());
            assertEquals(2, r.lastSeq());

            // Server của mình khởi động lại: app nối lại, HELLO_ACK mới báo castId mới
            r.expectCastId(11);
            r.offerTcp(keyframe(7, 3));
            r.offerTcp(keyframe(11, 1));
            assertEquals(3, got.size());
            assertEquals(1, r.lastSeq());
        }
    }

    @Test
    void chuaBietCastId_nhanGoiDauTienNhuTruoc() {
        List<Frame> got = new CopyOnWriteArrayList<>();
        try (var r = new CastReceiver(List.of(), new InetSocketAddress("239.255.70.1", 7001), got::add, n -> {})) {
            r.offerTcp(keyframe(9, 5));
            assertEquals(1, got.size());
            assertEquals(5, r.lastSeq());
        }
    }
}
