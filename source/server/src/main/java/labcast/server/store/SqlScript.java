package labcast.server.store;

import java.util.ArrayList;
import java.util.List;

/** Tách một file SQL thành từng câu lệnh. */
final class SqlScript {
    private SqlScript() {}

    /**
     * Bỏ chú thích {@code --} tới cuối dòng, rồi tách theo {@code ;}. schema.sql có {@code ;} nằm
     * trong chú thích (ví dụ cột {@code sessions.device}), nên PHẢI bỏ chú thích trước khi tách.
     *
     * <p>Không xử lý {@code --} hay {@code ;} nằm trong chuỗi ký tự: schema.sql không có trường hợp
     * đó.
     */
    static List<String> split(String script) {
        StringBuilder noComments = new StringBuilder(script.length());
        for (String line : script.split("\n", -1)) {
            int i = line.indexOf("--");
            noComments.append(i >= 0 ? line.substring(0, i) : line).append('\n');
        }
        List<String> out = new ArrayList<>();
        for (String part : noComments.toString().split(";")) {
            String s = part.strip();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }
}
