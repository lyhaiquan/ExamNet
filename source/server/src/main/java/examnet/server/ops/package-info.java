/**
 * Hạ tầng vận hành: cổng trạng thái {@code :8080/version} để bước deploy xác nhận đúng commit.
 * Không thuộc hệ thống thi; giữ nguyên khi viết server thật.
 *
 * <p>Chủ: leader (Người 2) — file dùng chung, sửa qua PR có leader duyệt. Xem docs/deploy/VPS-SETUP.md.
 */
package examnet.server.ops;
