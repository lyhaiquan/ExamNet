/**
 * Tầng mạng phía server: vòng {@code accept()}, thread pool, mỗi kết nối một luồng
 * ghi riêng với hàng đợi có giới hạn (backpressure), chuyển thông điệp tới đúng hàm
 * xử lý, đặt tường minh các tùy chọn socket.
 *
 * <p>Phụ trách: Người 2 (tùy chọn socket: Người 3). Spec §9, §9.1, §9.2. Thí nghiệm 4, 5, 8 (thí nghiệm 7: Người 3).
 */
package examnet.server.net;
