/**
 * ★ĐG1 — Chiếu animation: phát multicast 239.255.70.1:7001 trên card LAN, giãn nhịp,
 * datagram ≤ 1400 byte; {@code CAST_CONTROL} hẹn giờ; nhận {@code CAST_NACK} và trả
 * {@code CAST_REPAIR}; phát lại khi nhiều máy cùng thiếu; {@code CAST_KEYFRAME} mỗi 2 s; chuyển
 * máy không nhận được multicast sang TCP.
 *
 * <p>Phụ trách: Người 4 — Giảng và chiếu. Spec §9, §13. Thí nghiệm 1, 3.
 */
package labcast.server.cast;
