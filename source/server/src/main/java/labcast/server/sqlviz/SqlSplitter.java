package labcast.server.sqlviz;

import java.util.ArrayList;
import java.util.List;

/**
 * Tách script SQL thành từng câu theo dấu {@code ;}, bỏ qua {@code ;} nằm trong chuỗi {@code '…'}, tên
 * {@code "…"} và chú thích {@code --}, {@code /* *\/}. sqlite-jdbc mỗi lần chỉ chạy một câu.
 */
final class SqlSplitter {
    private SqlSplitter() {}

    static List<String> split(String script) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int n = script.length();
        for (int i = 0; i < n; i++) {
            char c = script.charAt(i);
            if (c == '\'' || c == '"') {
                int j = i + 1;
                while (j < n) {
                    if (script.charAt(j) == c) {
                        if (j + 1 < n && script.charAt(j + 1) == c) {
                            j += 2; // '' trong chuỗi là một dấu nháy
                            continue;
                        }
                        break;
                    }
                    j++;
                }
                cur.append(script, i, Math.min(n, j + 1));
                i = j;
            } else if (c == '-' && i + 1 < n && script.charAt(i + 1) == '-') {
                while (i < n && script.charAt(i) != '\n') {
                    i++;
                }
                cur.append('\n');
            } else if (c == '/' && i + 1 < n && script.charAt(i + 1) == '*') {
                int end = script.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 1;
                cur.append(' ');
            } else if (c == ';') {
                add(out, cur);
            } else {
                cur.append(c);
            }
        }
        add(out, cur);
        return out;
    }

    private static void add(List<String> out, StringBuilder cur) {
        String s = cur.toString().strip();
        if (!s.isEmpty()) {
            out.add(s);
        }
        cur.setLength(0);
    }
}
