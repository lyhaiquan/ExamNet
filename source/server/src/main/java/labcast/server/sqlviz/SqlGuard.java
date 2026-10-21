package labcast.server.sqlviz;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;

/**
 * Lớp chặn đầu tiên cho câu SQL của học viên (spec §7.6): đúng <b>một</b> câu, thuộc danh sách cho phép
 * — {@code SELECT}, thêm {@code INSERT/UPDATE/DELETE} nếu bài là {@code dml}. Mọi thứ khác ({@code
 * ATTACH}, {@code PRAGMA}, tạo/xoá bảng…) bị từ chối. Các lớp sau: SELECT mở chỉ đọc, chạy trên bản sao,
 * cấm {@code ATTACH}, trần độ dài chuỗi, trần thời gian ({@link SqliteEngine}).
 */
public final class SqlGuard {
    private SqlGuard() {}

    public enum Kind {
        SELECT,
        DML
    }

    /** Câu đã qua kiểm: {@code sql} là đúng một câu, đã bỏ {@code ;} cuối. */
    public record Checked(String sql, Kind kind, Statement statement) {}

    public static Checked check(String sql, boolean allowDml) throws SqlRejected {
        List<String> parts = SqlSplitter.split(sql == null ? "" : sql);
        if (parts.isEmpty()) {
            throw new SqlRejected("Chưa có câu lệnh nào.");
        }
        if (parts.size() > 1) {
            throw new SqlRejected("Mỗi lần chỉ được chạy một câu lệnh (đang có " + parts.size() + " câu).");
        }
        String one = parts.get(0);
        Statement st;
        try {
            st = CCJSqlParserUtil.parse(one);
        } catch (JSQLParserException e) {
            // Bộ phân tích không đọc được "a IS b" / "a IS NOT b" giữa hai biểu thức (SQLite có): viết lại thành
            // dạng tương đương nó đọc được, rồi chạy câu đã viết lại
            String rewritten = isDistinct(one);
            try {
                if (rewritten.equals(one)) {
                    throw e;
                }
                st = CCJSqlParserUtil.parse(rewritten);
                one = rewritten;
            } catch (JSQLParserException e2) {
                throw new SqlRejected("Không phân tích được câu lệnh, kiểm lại cú pháp: " + firstLine(e));
            }
        }
        if (st instanceof Select) {
            return new Checked(one, Kind.SELECT, st);
        }
        if (st instanceof Insert || st instanceof Update || st instanceof Delete) {
            if (!allowDml) {
                throw new SqlRejected("Bài này chỉ dùng câu SELECT để truy vấn, không được thay đổi dữ liệu.");
            }
            return new Checked(one, Kind.DML, st);
        }
        String name = st.getClass().getSimpleName().toUpperCase();
        throw new SqlRejected("Không cho phép câu lệnh " + name + " trong bài tập.");
    }

    /**
     * {@code a IS NOT b} → {@code a IS DISTINCT FROM b}, {@code a IS b} → {@code a IS NOT DISTINCT FROM b} (cùng
     * nghĩa trong SQLite: so sánh coi NULL bằng NULL). Giữ nguyên {@code IS [NOT] NULL/TRUE/FALSE/DISTINCT} và
     * mọi thứ trong chuỗi, tên đặt trong nháy kép.
     */
    static String isDistinct(String sql) {
        Matcher m = TOKEN.matcher(sql);
        List<int[]> spans = new ArrayList<>();
        List<String> words = new ArrayList<>();
        while (m.find()) {
            spans.add(new int[] {m.start(), m.end()});
            words.add(m.group().toUpperCase(Locale.ROOT));
        }
        StringBuilder sb = new StringBuilder();
        int last = 0;
        for (int i = 0; i < words.size(); i++) {
            if (!words.get(i).equals("IS")) {
                continue;
            }
            String next = i + 1 < words.size() ? words.get(i + 1) : "";
            boolean not = next.equals("NOT");
            String after = not ? (i + 2 < words.size() ? words.get(i + 2) : "") : next;
            if (KEEP_AFTER_IS.contains(after) || after.isEmpty()) {
                continue;
            }
            int end = not ? spans.get(i + 1)[1] : spans.get(i)[1];
            sb.append(sql, last, spans.get(i)[0]).append(not ? "IS DISTINCT FROM" : "IS NOT DISTINCT FROM");
            last = end;
        }
        return sb.append(sql.substring(last)).toString();
    }

    /** Chuỗi, tên trong nháy kép (bỏ qua), từ, và mọi ký tự khác: chỉ từ mới được so với IS / NOT. */
    private static final Pattern TOKEN =
            Pattern.compile("'(?:[^']|'')*'|\"[^\"]*\"|[A-Za-z_][A-Za-z0-9_]*|[^\\sA-Za-z_'\"]");

    private static final Set<String> KEEP_AFTER_IS = Set.of("NULL", "TRUE", "FALSE", "DISTINCT", "NOT", "IS");

    private static String firstLine(JSQLParserException e) {
        String m = e.getCause() != null && e.getCause().getMessage() != null
                ? e.getCause().getMessage()
                : String.valueOf(e.getMessage());
        int nl = m.indexOf('\n');
        return nl < 0 ? m : m.substring(0, nl);
    }
}
