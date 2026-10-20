package labcast.server.sqlviz;

import java.util.List;
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
            throw new SqlRejected("Không phân tích được câu lệnh, kiểm lại cú pháp: " + firstLine(e));
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

    private static String firstLine(JSQLParserException e) {
        String m = e.getCause() != null && e.getCause().getMessage() != null
                ? e.getCause().getMessage()
                : String.valueOf(e.getMessage());
        int nl = m.indexOf('\n');
        return nl < 0 ? m : m.substring(0, nl);
    }
}
