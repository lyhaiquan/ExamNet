package labcast.server.sqlviz;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * So kết quả câu của học viên với câu đáp án (spec §7.6): số cột phải bằng nhau, tên cột không cần; so
 * như <b>đa tập</b> (không quan tâm thứ tự dòng, dòng trùng phải đủ số lần) trừ khi bài đặt {@code
 * thu_tu: co}; số thực so tới 1e-6, số nguyên bằng số thực cùng giá trị; NULL bằng NULL, khác chuỗi rỗng.
 */
public final class ResultComparer {
    static final double EPS = 1e-6;

    private ResultComparer() {}

    public static boolean same(QueryResult a, QueryResult b, boolean ordered) {
        if (a.columns().size() != b.columns().size()
                || a.rows().size() != b.rows().size()) {
            return false;
        }
        List<List<Object>> x = new ArrayList<>(a.rows());
        List<List<Object>> y = new ArrayList<>(b.rows());
        if (!ordered) {
            // Sắp theo giá trị đầy đủ rồi so từng cặp: hai giá trị gần nhau (≤ 1e-6) đứng cùng chỗ ở hai bên
            x.sort(ROW_ORDER);
            y.sort(ROW_ORDER);
        }
        for (int i = 0; i < x.size(); i++) {
            if (!sameRow(x.get(i), y.get(i))) {
                return false;
            }
        }
        return true;
    }

    static boolean sameRow(List<Object> r, List<Object> s) {
        if (r.size() != s.size()) {
            return false;
        }
        for (int i = 0; i < r.size(); i++) {
            if (!sameValue(r.get(i), s.get(i))) {
                return false;
            }
        }
        return true;
    }

    static boolean sameValue(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a instanceof Number p && b instanceof Number q) {
            double u = p.doubleValue();
            double v = q.doubleValue();
            return Math.abs(u - v) <= EPS * Math.max(1.0, Math.max(Math.abs(u), Math.abs(v)));
        }
        if (a instanceof Number || b instanceof Number) {
            return false;
        }
        return a.toString().equals(b.toString());
    }

    /** NULL trước, rồi số (theo giá trị), rồi chuỗi — như thứ tự SQLite sắp các kiểu. */
    static final Comparator<Object> VALUE_ORDER = (a, b) -> {
        int ra = rank(a);
        int rb = rank(b);
        if (ra != rb) {
            return Integer.compare(ra, rb);
        }
        if (ra == 1) {
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        }
        return ra == 0 ? 0 : a.toString().compareTo(b.toString());
    };

    static final Comparator<List<Object>> ROW_ORDER = (r, s) -> {
        for (int i = 0; i < Math.min(r.size(), s.size()); i++) {
            int c = VALUE_ORDER.compare(r.get(i), s.get(i));
            if (c != 0) {
                return c;
            }
        }
        return Integer.compare(r.size(), s.size());
    };

    private static int rank(Object v) {
        return v == null ? 0 : v instanceof Number ? 1 : 2;
    }
}
