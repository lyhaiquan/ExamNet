package labcast.client.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void serverNhieuLop_moiLopMotDong_cungMayChu() throws Exception {
        var lop = new java.util.ArrayList<labcast.server.classroom.ClassRepository.Listed>();
        for (int i = 1; i <= 40; i++)
            lop.add(new labcast.server.classroom.ClassRepository.Listed(
                    i, "Lớp số " + i + " - Cấu trúc dữ liệu và giải thuật", "GV " + i));
        byte[] goi = labcast.server.discovery.DiscoveryReply.build("Lớp cấu hình", "Cô Lan", "192.168.1.10", 7000, lop);
        assertTrue(goi.length <= 1400, "vừa một datagram: " + goi.length);
        try (var r = new DiscoveryResponder(0, () -> goi)) {
            r.start();
            var found =
                    DiscoveryClient.findAt(List.of(InetAddress.getLoopbackAddress()), r.port(), Duration.ofMillis(800));
            assertTrue(found.size() > 10 && found.size() < 40, "cắt bớt cho vừa gói: " + found.size());
            assertEquals(
                    "Lớp số 1 - Cấu trúc dữ liệu và giải thuật", found.get(0).className());
            assertEquals("GV 1", found.get(0).teacherName());
            assertEquals(1, found.get(0).classId());
            assertEquals("192.168.1.10", found.get(1).host());
            assertEquals(7000, found.get(1).port());
        }
        // server chưa có lớp nào: vẫn một dòng tên lớp cấu hình
        byte[] rong = labcast.server.discovery.DiscoveryReply.build(
                "Lớp cấu hình", "Cô Lan", "192.168.1.10", 7000, List.of());
        try (var r = new DiscoveryResponder(0, () -> rong)) {
            r.start();
            var found =
                    DiscoveryClient.findAt(List.of(InetAddress.getLoopbackAddress()), r.port(), Duration.ofMillis(800));
            assertEquals(1, found.size());
            assertEquals("Lớp cấu hình", found.get(0).className());
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
    void serverQuangBaLoopback_dungDiaChiNguonCuaGoiTraLoi() throws Exception {
        var info = new DiscoveryClient.ServerInfo("Lớp", "GV", "127.0.0.1", 7000, false, "");
        var nguon = InetAddress.getByName("192.168.1.10");
        assertEquals(
                "192.168.1.10", DiscoveryClient.withReachableHost(info, nguon).host());
        var rong = new DiscoveryClient.ServerInfo("Lớp", "GV", "", 7000, false, "");
        assertEquals(
                "192.168.1.10", DiscoveryClient.withReachableHost(rong, nguon).host());
        var tot = new DiscoveryClient.ServerInfo("Lớp", "GV", "192.168.1.20", 7000, false, "");
        assertEquals(
                "192.168.1.20", DiscoveryClient.withReachableHost(tot, nguon).host());
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
