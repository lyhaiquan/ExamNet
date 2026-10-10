package labcast.server.net;

/** Lớp ưu tiên của hàng đợi ghi (spec §11). */
public enum Priority {
    /** Chen lên đầu, không bao giờ bỏ, được vượt sức chứa: câu hỏi nhanh, đồng bộ giờ, lỗi giao thức. */
    CRITICAL,
    /** Hàng đầy thì chờ tối đa một khoảng rồi báo thất bại. */
    NORMAL,
    /** Chỉ cần bản mới nhất: có frame cùng khoá đang chờ thì thay tại chỗ (sơ đồ lớp, trạng thái xem chung). */
    CONFLATABLE,
    /** Mất cũng được, đã có đường bù: hàng đầy thì bỏ frame loại này cũ nhất (gói chiếu qua TCP). */
    DROPPABLE
}
