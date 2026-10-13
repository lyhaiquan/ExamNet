package labcast.server.sqlviz;

import java.util.List;

/**
 * Kết quả một câu SELECT. Giá trị đã chuẩn hoá: số nguyên là {@code Long}, số thực {@code Double}, chuỗi
 * {@code String}, NULL là {@code null}. {@code truncated}: kết quả dài hơn {@link SqliteEngine#MAX_ROWS}
 * dòng, chỉ giữ phần đầu.
 */
public record QueryResult(List<String> columns, List<List<Object>> rows, boolean truncated) {}
