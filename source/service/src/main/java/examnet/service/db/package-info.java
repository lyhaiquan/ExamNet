/**
 * Mở kết nối SQLite (bật khoá ngoại, chế độ ghi WAL), chạy {@code schema.sql}.
 * Mỗi bảng trong {@code schema.sql} có ghi chủ; đổi bảng của ai thì người đó duyệt.
 *
 * <p>Chủ: Người 3 — Kỳ thi và thời gian. Spec §13.
 */
package examnet.service.db;
