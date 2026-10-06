/**
 * Protocol EXP/1.0: khung 13 byte (MAGIC, VER, TYPE, FLAGS, SEQ, LEN), danh sách
 * {@code MessageType}, {@code ErrorCode}, và codec chuyển frame ⇄ byte (đọc lặp cho
 * đủ byte, kiểm LEN trước khi cấp phát bộ nhớ).
 *
 * <p>Phụ trách: Người 1. Spec §8, §9.3. Thí nghiệm 6, 10.
 */
package examnet.common.protocol;
