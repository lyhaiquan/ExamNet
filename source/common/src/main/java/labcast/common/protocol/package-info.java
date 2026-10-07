/**
 * Protocol LCP/1.0: khung 13 byte ({@code Frame}: MAGIC 'L''C', VER, TYPE, FLAGS, SEQ, LEN),
 * codec ({@code FrameCodec}, {@code PayloadWriter}, {@code PayloadReader}), danh sách
 * {@code MessageType} và {@code ErrorCode}. Đọc lặp cho đủ LEN byte, kiểm LEN trước khi cấp
 * bộ nhớ; UDP dùng {@code Frame.fromBytes}.
 *
 * <p>Phụ trách: leader (Người 2) — file dùng chung, sửa bằng PR nhỏ riêng có leader duyệt. Spec §10.
 */
package labcast.common.protocol;
