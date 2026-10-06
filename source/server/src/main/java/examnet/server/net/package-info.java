/**
 * S16 Quản lý nhiều client đồng thời: vòng {@code accept()}, thread pool, mỗi kết nối một
 * luồng ghi riêng với hàng đợi có giới hạn (backpressure), router chuyển lệnh tới module đã
 * đăng ký.
 *
 * <p>Chủ: Người 2 (leader) — Kết nối và tài khoản. Spec §9, §9.1. Thí nghiệm 4, 5, 8.
 */
package examnet.server.net;
