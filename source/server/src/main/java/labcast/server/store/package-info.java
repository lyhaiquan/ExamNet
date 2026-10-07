/**
 * Mở SQLite ({@code journal_mode=WAL}), chạy {@code schema.sql} lúc khởi động. Mỗi nhóm bảng
 * có chủ riêng, ghi trong file schema.
 *
 * <p>Phụ trách: Người 2 (leader) — Kết nối, lớp học, câu hỏi nhanh. Spec §15.
 */
package labcast.server.store;
