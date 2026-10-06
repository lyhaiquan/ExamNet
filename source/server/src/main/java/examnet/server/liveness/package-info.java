/**
 * ★ĐG3 — S14 Phát hiện mất kết nối: heartbeat có chu kỳ co giãn theo RTT (công thức RTO
 * của TCP), {@code ALIVE → SUSPECT → DISCONNECTED}.
 *
 * <p>Chủ: Người 2 (leader) — Kết nối và tài khoản. Spec §11 ĐG3. Thí nghiệm 3.
 */
package examnet.server.liveness;
