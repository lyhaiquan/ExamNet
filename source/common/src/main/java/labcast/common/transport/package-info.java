/**
 * Kênh truyền: đặt tùy chọn socket tường minh ({@code TCP_NODELAY}, timeout, buffer, TTL
 * multicast…) và bọc TLS. Server và app cùng gọi vào đây, không tự đặt tùy chọn ở chỗ khác.
 *
 * <p>Phụ trách: Người 2 (leader) — Kết nối, lớp học, câu hỏi nhanh. Spec §9, §11. Thí nghiệm 10.
 */
package labcast.common.transport;
