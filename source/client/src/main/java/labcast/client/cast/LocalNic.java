package labcast.client.cast;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Chọn card mạng để nghe nhóm chiếu (spec §9). Multicast chỉ tới socket tham gia nhóm <b>trên đúng card
 * server phát ra</b> (đã thử trên Windows: phát trên Wi-Fi, nghe trên loopback là không nhận được gói
 * nào), nên lấy card chứa địa chỉ cục bộ của kết nối TCP tới server — tránh nghe nhầm card ảo VirtualBox,
 * VMware, Hyper-V.
 *
 * <p>App nối tới server qua 127.0.0.1 (server chạy trên chính máy này) thì không biết server phát trên
 * card nào: nghe trên mọi card có IPv4.
 */
public final class LocalNic {
    private LocalNic() {}

    public static List<NetworkInterface> forLocalAddress(InetAddress local) throws SocketException {
        if (local != null && !local.isLoopbackAddress() && !local.isAnyLocalAddress()) {
            NetworkInterface nic = NetworkInterface.getByInetAddress(local);
            if (nic != null) {
                return List.of(nic);
            }
        }
        return allIpv4();
    }

    /** Mọi card đang bật, nhận multicast, có địa chỉ IPv4. */
    public static List<NetworkInterface> allIpv4() throws SocketException {
        List<NetworkInterface> out = new ArrayList<>();
        for (NetworkInterface n : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            try {
                if (n.isUp()
                        && n.supportsMulticast()
                        && Collections.list(n.getInetAddresses()).stream().anyMatch(a -> a instanceof Inet4Address)) {
                    out.add(n);
                }
            } catch (SocketException e) {
                // card vừa bị gỡ: bỏ qua
            }
        }
        return out;
    }
}
