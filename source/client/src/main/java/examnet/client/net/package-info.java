/**
 * Kết nối tới server, tự kết nối lại, phân phối frame tới chức năng đã đăng ký
 * ({@code on(MessageType, …)}), trả lời heartbeat; C11 thanh trạng thái kết nối.
 *
 * <p>Chủ: Người 2 (leader) — Kết nối và tài khoản. Spec §7.
 */
package examnet.client.net;
