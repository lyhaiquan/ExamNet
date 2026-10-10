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
    void khongCoCardHopLe_rong() throws Exception {
        var chiCoAo = List.of(
                nic("eth5", "vEthernet (WSL)", "172.25.16.1", false),
                nic("lo", "Loopback", "127.0.0.1", true),
                nic("eth9", "Ethernet công cộng", "8.8.8.8", false));
        assertTrue(LanInterface.pick(chiCoAo, null).isEmpty());
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
