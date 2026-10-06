/**
 * Khung EXP/1.0 ({@code Frame}), codec ({@code FrameCodec}, {@code PayloadReader},
 * {@code PayloadWriter}) và danh sách mã lệnh.
 *
 * <p>Riêng {@code MessageType}, {@code ErrorCode}, {@code Priority} là file dùng chung: leader điền
 * đủ một lần theo spec §8.2, mỗi người dùng đúng dải mã của mình, không đánh số lại.
 *
 * <p>Chủ: Người 1 — Làm bài và không mất bài. Spec §8, §9.3. Thí nghiệm 6, 10.
 */
package examnet.common.protocol;
