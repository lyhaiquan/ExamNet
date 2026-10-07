/**
 * Phiên và token; giữ phiên khi rớt mạng để khôi phục; đăng nhập ở máy khác thì gửi
 * {@code BYE(LOGGED_IN_ELSEWHERE)} cho phiên cũ.
 *
 * <p>Phụ trách: Người 2 (leader) — Kết nối, lớp học, câu hỏi nhanh. Spec §10.4a, §10.4e, §12.
 */
package labcast.server.session;
