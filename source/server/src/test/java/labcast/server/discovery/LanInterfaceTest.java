package labcast.server.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.util.List;
import org.junit.jupiter.api.Test;

class LanInterfaceTest {

    private static NicInfo nic(String name, String display, String ip, boolean loopback) throws Exception {
        return new NicInfo(name, display, List.of(InetAddress.getByName(ip)), true, loopback);
    }

    private List<NicInfo> mayGiaoVien() throws Exception {
        return List.of(
                nic("eth5", "vEthernet (WSL)", "172.25.16.1", false),
                nic("eth2", "Ethernet", "192.168.1.10", false),
                nic("lo", "Software Loopback Interface 1", "127.0.0.1", true));
    }

    @Test
    void boCardAoVaLoopback_chonEthernet() throws Exception {
        assertEquals(
                "Ethernet", LanInterface.pick(mayGiaoVien(), null).orElseThrow().displayName());
    }

    @Test
    void override_duocUuTien() throws Exception {
        assertEquals(
                "vEthernet (WSL)",
                LanInterface.pick(mayGiaoVien(), "vEthernet (WSL)")
                        .orElseThrow()
                        .displayName());
        assertEquals(
                "eth2",
                LanInterface.pick(mayGiaoVien(), "192.168.1.10").orElseThrow().name());
    }

    @Test
    void chiCoCardAo_rong() throws Exception {
        var chiCoAo = List.of(
                nic("eth5", "vEthernet (WSL)", "172.25.16.1", false),
                nic("lo", "Loopback", "127.0.0.1", true),
                nic("eth8", "Ethernet chưa có DHCP", "169.254.10.20", false));
        assertTrue(LanInterface.pick(chiCoAo, null).isEmpty());
    }

    /**
     * Đúng máy của leader ngày 12/10/2026: mạng Wi-Fi cấp 192.99.3.115 (ngoài dải riêng). Trước đây server
     * bỏ card này, không còn card nào và quảng bá 127.0.0.1 — máy khác không vào được lớp.
     */
    @Test
    void wifiCapIpNgoaiDaiRieng_vanChonWifi_khongChonCardAoCoIpRieng() throws Exception {
        var may = List.of(
                nic("lo", "Software Loopback Interface 1", "127.0.0.1", true),
                nic("eth1", "VMware Virtual Ethernet Adapter for VMnet8", "192.168.41.1", false),
                nic(
                        "wlan2",
                        "Killer(R) Wi-Fi 6 AX1650i 160MHz Wireless Network Adapter (201NGW)",
                        "192.99.3.115",
                        false),
                nic("eth3", "VMware Virtual Ethernet Adapter for VMnet1", "192.168.2.1", false),
                nic("eth10", "ZeroTier Virtual Port", "10.140.231.224", false),
                nic("net7", "Tailscale Tunnel", "169.254.83.107", false));
        NicInfo chon = LanInterface.pick(may, null).orElseThrow();
        assertEquals("wlan2", chon.name());
        assertEquals(
                "192.99.3.115", LanInterface.usableAddress(chon).orElseThrow().getHostAddress());
    }

    @Test
    void cardThatCoCaIpRieng_uuTienIpRieng() throws Exception {
        var may = List.of(
                nic("eth0", "Ethernet trường", "203.162.10.5", false), nic("wlan0", "Wi-Fi", "192.168.0.5", false));
        assertEquals("wlan0", LanInterface.pick(may, null).orElseThrow().name());
    }

    @Test
    void diaChiQuangBaDuoc() throws Exception {
        var n = new NicInfo(
                "eth0",
                "Ethernet",
                List.of(InetAddress.getByName("169.254.1.1"), InetAddress.getByName("203.162.10.5")),
                true,
                false);
        assertEquals("203.162.10.5", LanInterface.usableAddress(n).orElseThrow().getHostAddress());
        var chiTuCap = new NicInfo("eth0", "Ethernet", List.of(InetAddress.getByName("169.254.1.1")), true, false);
        assertTrue(LanInterface.usableAddress(chiTuCap).isEmpty());
    }

    @Test
    void uuTienDayHonWifi() throws Exception {
        var hai = List.of(
                nic("wlan0", "Wi-Fi", "192.168.0.5", false), nic("eth0", "Realtek Ethernet", "10.0.0.5", false));
        assertEquals("eth0", LanInterface.pick(hai, null).orElseThrow().name());
    }

    /** Đúng máy của leader: không có cổng dây, có máy ảo, ZeroTier, Tailscale. */
    @Test
    void mayCoVpnVaMayAo_chonWifiThat() throws Exception {
        var may = List.of(
                nic("eth1", "VMware Virtual Ethernet Adapter for VMnet8", "192.168.41.1", false),
                nic("wlan2", "Killer(R) Wi-Fi 6 AX1650i", "172.20.8.114", false),
                nic("eth10", "ZeroTier Virtual Port", "10.140.231.224", false),
                nic("net7", "Tailscale Tunnel", "100.64.0.5", false));
        assertEquals("wlan2", LanInterface.pick(may, null).orElseThrow().name());
    }

    @Test
    void diaChiRieng() throws Exception {
        assertTrue(LanInterface.isPrivate(InetAddress.getByName("172.31.0.1")));
        assertFalse(LanInterface.isPrivate(InetAddress.getByName("172.32.0.1")));
        assertFalse(LanInterface.isPrivate(InetAddress.getByName("127.0.0.1")));
    }
}
