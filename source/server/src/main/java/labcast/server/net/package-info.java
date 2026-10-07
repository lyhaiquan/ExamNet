/**
 * Vòng {@code accept} cổng 7000; mỗi kết nối một luồng đọc và một luồng ghi riêng; hàng đợi
 * ghi có ưu tiên (CRITICAL, NORMAL, CONFLATABLE, DROPPABLE); router
 * {@code router.on(MessageType, Role…, handler)}; gọi {@code FramePolicy.check(...)} trước khi
 * chuyển thông điệp cho nghiệp vụ.
 *
 * <p>Phụ trách: Người 2 (leader) — Kết nối, lớp học, câu hỏi nhanh. Spec §10, §11, §17.3.
 */
package labcast.server.net;
