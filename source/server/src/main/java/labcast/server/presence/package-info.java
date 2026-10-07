/**
 * Heartbeat thích nghi (SRTT/RTTVAR/RTO), {@code Presence.srtt(userId)}, trạng thái có mặt;
 * cho module khác đọc trường mở rộng của {@code HEARTBEAT} qua
 * {@code presence.onHeartbeat(listener)}.
 *
 * <p>Phụ trách: Người 2 (leader) — Kết nối, lớp học, câu hỏi nhanh. Spec §12, §17.3. Thí nghiệm 9.
 */
package labcast.server.presence;
