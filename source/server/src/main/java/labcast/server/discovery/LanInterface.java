package labcast.server.discovery;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Chọn card LAN của máy giáo viên (spec §9). Máy có Docker Desktop hay WSL có thêm card ảo
 * (vEthernet 172.x); quảng bá nhầm địa chỉ card ảo thì app học viên không nối được.
 */
public final class LanInterface {
    private static final List<String> VIRTUAL =
            List.of("vethernet", "wsl", "docker", "virtualbox", "vmware", "hyper-v", "vboxnet", "br-", "loopback");

    private LanInterface() {}

    /**
     * Card đang bật, không phải loopback, có IPv4 riêng (10.x, 172.16–31.x, 192.168.x), tên không
     * giống card ảo. {@code override} (tên card hoặc IP, từ {@code --lan}) được ưu tiên tuyệt đối.
     */
    static Optional<NicInfo> pick(List<NicInfo> all, String override) {
        if (override != null && !override.isBlank()) {
            String o = override.strip();
            for (NicInfo n : all) {
                if (o.equalsIgnoreCase(n.name())
                        || o.equalsIgnoreCase(n.displayName())
                        || n.ipv4().stream().anyMatch(a -> a.getHostAddress().equals(o))) {
                    return Optional.of(n);
                }
            }
            return Optional.empty();
        }
        return all.stream()
                .filter(n -> n.up() && !n.loopback())
                .filter(n -> n.ipv4().stream().anyMatch(LanInterface::isPrivate))
                .filter(n -> !looksVirtual(n))
                .min(Comparator.comparingInt(LanInterface::rank));
    }

    /** Đọc card thật của máy. Không chọn được card nào thì báo lỗi kèm danh sách để dùng {@code --lan}. */
    public static NicInfo pickOrThrow(String override) throws SocketException {
        List<NicInfo> all = list();
        return pick(all, override)
                .orElseThrow(() -> new SocketException("Không tìm thấy card LAN"
                        + (override == null ? "" : " khớp '" + override + "'")
                        + ". Các card hiện có: " + describe(all) + ". Chỉ định bằng --lan <tên card hoặc IP>."));
    }

    public static List<NicInfo> list() throws SocketException {
        List<NicInfo> out = new ArrayList<>();
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            List<InetAddress> v4 = new ArrayList<>();
            for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                if (a instanceof Inet4Address) {
                    v4.add(a);
                }
            }
            out.add(new NicInfo(ni.getName(), ni.getDisplayName(), v4, ni.isUp(), ni.isLoopback()));
        }
        return out;
    }

    /** IPv4 riêng đầu tiên của card. */
    public static Optional<InetAddress> privateAddress(NicInfo n) {
        return n.ipv4().stream().filter(LanInterface::isPrivate).findFirst();
    }

    static boolean isPrivate(InetAddress a) {
        if (!(a instanceof Inet4Address)) {
            return false;
        }
        byte[] b = a.getAddress();
        int x = b[0] & 0xFF;
        int y = b[1] & 0xFF;
        return x == 10 || (x == 172 && y >= 16 && y <= 31) || (x == 192 && y == 168);
    }

    private static boolean looksVirtual(NicInfo n) {
        String s = (n.name() + " " + n.displayName()).toLowerCase(Locale.ROOT);
        return VIRTUAL.stream().anyMatch(s::contains);
    }

    /** Ưu tiên card dây: phòng máy cắm dây, Wi-Fi chỉ là dự phòng. */
    private static int rank(NicInfo n) {
        String s = (n.name() + " " + n.displayName()).toLowerCase(Locale.ROOT);
        if (s.contains("eth")) {
            return 0;
        }
        if (s.contains("wi-fi") || s.contains("wlan") || s.contains("wireless")) {
            return 2;
        }
        return 1;
    }

    private static String describe(List<NicInfo> all) {
        List<String> parts = new ArrayList<>();
        for (NicInfo n : all) {
            if (n.up() && !n.ipv4().isEmpty()) {
                parts.add(n.displayName() + " " + n.ipv4().get(0).getHostAddress());
            }
        }
        return parts.isEmpty() ? "(không có)" : String.join(", ", parts);
    }
}
