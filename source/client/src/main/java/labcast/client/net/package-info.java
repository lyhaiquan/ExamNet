/**
 * Kết nối TCP suốt buổi, tự kết nối lại, {@code client.send(...)} / {@code client.on(...)};
 * tìm server bằng UDP broadcast; trường mở rộng của {@code HEARTBEAT}.
 *
 * <p>Phụ trách: Người 2 (leader) — Kết nối, lớp học, câu hỏi nhanh. Spec §9, §17.3.
 */
package labcast.client.net;
