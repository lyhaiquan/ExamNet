/**
 * ★ĐG3 — heartbeat có chu kỳ tự điều chỉnh theo RTT đo được (công thức RTO của TCP),
 * máy trạng thái {@code ALIVE → SUSPECT → DISCONNECTED}.
 *
 * <p>Phụ trách: Người 2. Spec §11 ĐG3. Thí nghiệm 3.
 */
package examnet.server.liveness;
