package labcast.server.discovery;

import java.nio.charset.StandardCharsets;
import java.util.List;
import labcast.common.protocol.PayloadWriter;
import labcast.server.classroom.ClassRepository;

/**
 * Nội dung {@code DISCOVER_REPLY}: phần đầu như cũ (lớp trong cấu hình, giáo viên, địa chỉ, cổng…) để app cũ vẫn đọc
 * được, rồi danh sách mọi lớp trên server ({@code u16 n, n × (i64 id, str tên, str giáo viên)}) để màn đăng nhập hiện
 * từng lớp. Cả gói phải vừa một datagram app đọc (1500 byte): nhiều lớp quá thì cắt bớt ở cuối.
 */
public final class DiscoveryReply {
    /** Chừa chỗ cho đầu khung LCP trong 1500 byte app cấp cho một gói. */
    static final int MAX_PAYLOAD = 1400;

    private DiscoveryReply() {}

    public static byte[] build(
            String className, String teacher, String host, int port, List<ClassRepository.Listed> classes) {
        PayloadWriter w = new PayloadWriter()
                .str(className)
                .str(teacher)
                .str(host)
                .u16(port)
                .bool(false) // TLS: giai đoạn 3
                .str("");
        int size = w.toBytes().length + 2;
        int n = 0;
        for (ClassRepository.Listed c : classes) {
            int entry = 8 + 2 + utf8(c.name()) + 2 + utf8(c.teacher());
            if (size + entry > MAX_PAYLOAD) break;
            size += entry;
            n++;
        }
        w.u16(n);
        for (int i = 0; i < n; i++) {
            ClassRepository.Listed c = classes.get(i);
            w.i64(c.id()).str(c.name()).str(c.teacher());
        }
        return w.toBytes();
    }

    private static int utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8).length;
    }
}
