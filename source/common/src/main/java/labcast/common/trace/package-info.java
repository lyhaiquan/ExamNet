/**
 * Định dạng trace (JSON từng dòng): {@code TraceEvent}, {@code TraceReader}, keyframe
 * {@code snap} mỗi 200 sự kiện, trần 10 000 sự kiện / 2 MB.
 *
 * <p>Người 3 sinh trace (runner Python, {@code server.sqlviz}), Người 4 đọc trace. Đổi định
 * dạng phải có cả hai duyệt.
 *
 * <p>Phụ trách: Người 4 — Giảng và chiếu. Spec §7.3.
 */
package labcast.common.trace;
