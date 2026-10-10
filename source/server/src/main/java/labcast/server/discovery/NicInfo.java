package labcast.server.discovery;

import java.net.InetAddress;
import java.util.List;

/** Thông tin một card mạng, tách khỏi {@code NetworkInterface} để test được việc chọn card. */
public record NicInfo(String name, String displayName, List<InetAddress> ipv4, boolean up, boolean loopback) {}
