/**
 * Kênh truyền: đặt tùy chọn socket ({@code TCP_NODELAY}, timeout, buffer…) và bọc TLS.
 * Server và client cùng gọi vào đây, không tự đặt tùy chọn socket ở chỗ khác.
 *
 * <p>Chủ: Người 3 — Kỳ thi và thời gian. Spec §9.2, §3. Thí nghiệm 7, 11.
 */
package examnet.common.transport;
