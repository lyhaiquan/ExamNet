package labcast.server.sqlviz;

import java.util.ArrayList;
import java.util.List;

/** Kết quả SELECT dạng bảng chữ thẳng cột, cho khung kết quả của app (như output chương trình Python). */
final class TextTable {
    static final int MAX_SHOWN = 50;

    private TextTable() {}

    static String render(QueryResult r) {
        int n = r.columns().size();
        int shown = Math.min(MAX_SHOWN, r.rows().size());
        int[] w = new int[n];
        List<List<String>> cells = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            w[k] = r.columns().get(k).length();
        }
        for (int i = 0; i < shown; i++) {
            List<String> row = new ArrayList<>();
            for (int k = 0; k < n; k++) {
                Object v = r.rows().get(i).get(k);
                String s = v == null ? "NULL" : v.toString();
                if (s.length() > 40) {
                    s = s.substring(0, 39) + "…";
                }
                row.add(s);
                w[k] = Math.max(w[k], s.length());
            }
            cells.add(row);
        }
        StringBuilder sb = new StringBuilder();
        line(sb, r.columns(), w);
        for (int k = 0; k < n; k++) {
            sb.append(k == 0 ? "" : "-+-").append("-".repeat(w[k]));
        }
        sb.append('\n');
        for (List<String> row : cells) {
            line(sb, row, w);
        }
        int total = r.rows().size();
        if (total > shown || r.truncated()) {
            sb.append("… (")
                    .append(r.truncated() ? "hơn " : "")
                    .append(total)
                    .append(" dòng, chỉ hiện ")
                    .append(shown)
                    .append(")\n");
        } else {
            sb.append("(").append(total).append(" dòng)\n");
        }
        return sb.toString();
    }

    private static void line(StringBuilder sb, List<String> vals, int[] w) {
        for (int k = 0; k < w.length; k++) {
            String s = vals.get(k);
            sb.append(k == 0 ? "" : " | ").append(s).append(" ".repeat(w[k] - s.length()));
        }
        sb.append('\n');
    }
}
