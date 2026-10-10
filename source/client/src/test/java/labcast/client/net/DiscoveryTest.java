package labcast.client.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import labcast.common.protocol.PayloadWriter;
import labcast.server.discovery.DiscoveryResponder;
import org.junit.jupiter.api.Test;

class DiscoveryTest {

    private static byte[] reply() {
        return new PayloadWriter()
                .str("Lớp D22CQCN01")
                .str("Cô Lan")
                .str("192.168.1.10")
                .u16(7000)
                .bool(false)
                .str("")
                .toBytes();
    }

    @Test
    void guiDiscover_nhanDungTenLop() throws Exception {
        try (var r = new DiscoveryResponder(0, DiscoveryTest::reply)) {
            r.start();
            var found =
                    DiscoveryClient.findAt(List.of(InetAddress.getLoopbackAddress()), r.port(), Duration.ofMillis(800));
            assertEquals(1, found.size());
            assertEquals("Lớp D22CQCN01", found.get(0).className());
            assertEquals("192.168.1.10", found.get(0).host());
            assertEquals(7000, found.get(0).port());
        }
    }

    @Test
    void datagramRac_responderKhongChet() throws Exception {
        try (var r = new DiscoveryResponder(0, DiscoveryTest::reply);
                var rac = new DatagramSocket()) {
            r.start();
            byte[] junk = "xin chào, tôi là rác".getBytes();
            rac.send(new DatagramPacket(junk, junk.length, InetAddress.getLoopbackAddress(), r.port()));
            var found =
                    DiscoveryClient.findAt(List.of(InetAddress.getLoopbackAddress()), r.port(), Duration.ofMillis(800));
            assertEquals(1, found.size());
        }
    }

    @Test
    void khongCoServer_traDanhSachRong() throws Exception {
        int cong;
        try (var tam = new DatagramSocket()) {
            cong = tam.getLocalPort();
        }
        assertEquals(
                0,
                DiscoveryClient.findAt(List.of(InetAddress.getLoopbackAddress()), cong, Duration.ofMillis(300))
                        .size());
    }
}
