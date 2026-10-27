package labcast.common.protocol;

/**
 * Loại thông điệp của LCP/1.0 — một byte trong header (spec §10.3).
 *
 * <p>File chung do leader giữ. Mỗi người một dải mã (docs/plans/README.md), KHÔNG đánh số lại mã đã
 * có: app và server khác phiên bản vẫn phải hiểu nhau ở các mã cũ.
 */
public enum MessageType {
    // 0x01–0x0F: kết nối, phiên, liveness, lỗi — Người 2
    HELLO(0x01),
    HELLO_ACK(0x02),
    AUTH(0x03),
    AUTH_OK(0x04),
    AUTH_FAIL(0x05),
    HEARTBEAT(0x06),
    HEARTBEAT_ACK(0x07),
    RESUME_REQ(0x08),
    RESUME_STATE(0x09),
    /** {@code bool inApp, i64 clientTs, str lessonId}: học viên vừa rời app (Alt+Tab, thu nhỏ) hoặc quay lại. */
    APP_FOCUS(0x0A),
    ERROR(0x0E),
    BYE(0x0F),

    // 0x10–0x17: tìm server qua UDP broadcast — Người 2
    DISCOVER(0x10),
    DISCOVER_REPLY(0x11),

    // 0x18–0x1F: đồng bộ đồng hồ — Người 3
    TIME_SYNC_REQ(0x18),
    TIME_SYNC_RESP(0x19),
    CLASS_JOIN_RESULT(0x1A),

    // 0x20–0x2F: lớp học, quản trị, câu hỏi nhanh — Người 2
    CLASS_JOIN(0x20),
    CLASS_STATE(0x21),
    LESSON_PUSH(0x22),
    CLASS_LIST_REQ(0x23),
    CLASS_LIST_DATA(0x24),
    CLASS_SELECT_REQ(0x25),
    ADMIN_LESSON_DELETE(0x26),
    ADMIN_LESSON_TOGGLE(0x27),
    ADMIN_ROSTER_IMPORT(0x28),
    ADMIN_PROGRESS_EXPORT(0x29),
    ADMIN_OK(0x2A),
    ADMIN_LESSON_UPLOAD(0x2B),
    QUIZ_OPEN(0x2C),
    QUIZ_ANSWER(0x2D),
    QUIZ_CLOSE(0x2E),
    QUIZ_RESULT(0x2F),

    // 0x30–0x4F: bài học, luyện tập, chạy và chấm — Người 3
    LESSON_LIST(0x30),
    LESSON_FETCH(0x31),
    LESSON_DATA(0x32),
    PREDICT_REQ(0x38),
    PREDICT_QUESTION(0x39),
    PREDICT_ANSWER(0x3A),
    PREDICT_RESULT(0x3B),
    SIM_START(0x3C),
    SIM_STEP(0x3D),
    SIM_RESULT(0x3E),
    RUN_REQ(0x40),
    RUN_QUEUED(0x41),
    TRACE_CHUNK(0x42),
    RUN_RESULT(0x43),
    SUBMIT_REQ(0x44),
    SUBMIT_RESULT(0x45),
    HINT_REQ(0x46),
    HINT_DATA(0x47),
    // Workspace độc lập: không yêu cầu lessonId, không tác động nộp bài.
    PLAYGROUND_RUN_REQ(0x48),
    PLAYGROUND_RUN_QUEUED(0x49),
    PLAYGROUND_TRACE_CHUNK(0x4A),
    PLAYGROUND_RUN_RESULT(0x4B),

    // 0x50–0x6F: dòng code, mốc lưu, trợ giúp — Người 1
    CODE_DELTA(0x50),
    CODE_ACK(0x51),
    CHECKPOINT_LIST(0x52),
    CHECKPOINT_FETCH(0x53),
    CHECKPOINT_DATA(0x54),
    /** {@code str docKey}: hỏi code đang lưu của một khoá tài liệu (đổi ngôn ngữ trong ô code). */
    CODE_FETCH(0x55),
    /** {@code str docKey, i64 seq, str code}: code đang lưu trên server, hoặc khung nếu chưa có. */
    CODE_DATA(0x56),
    HAND_RAISE(0x58),
    HAND_LOWER(0x59),
    MIRROR_SUBSCRIBE(0x5A),
    MIRROR_UNSUBSCRIBE(0x5B),
    MIRROR_SNAPSHOT(0x5C),
    MIRROR_DELTA(0x5D),
    COMMENT_ADD(0x60),
    COMMENT(0x61),
    VIEW_JOIN(0x64),
    VIEW_CONTROL(0x65),
    VIEW_STATE(0x66),
    PRACTICE_CREATE_REQ(0x67),
    PRACTICE_JOIN_REQ(0x68),
    PRACTICE_ACTION_RESULT(0x69),
    STUDENT_OVERVIEW_REQ(0x6A),
    STUDENT_OVERVIEW_DATA(0x6B),
    STUDENT_ACTIVITY(0x6C),
    CLASS_CREATE_REQ(0x6D),
    CLASS_CREATE_RESULT(0x6E),
    CLASS_JOIN_REQ(0x6F),

    // 0x70–0x7F: chiếu animation qua multicast — Người 4
    CAST_CONTROL(0x70),
    CAST_DATA(0x71),
    CAST_KEYFRAME(0x72),
    CAST_NACK(0x73),
    CAST_REPAIR(0x74),
    /** Giáo viên gán bài có sẵn cho phiên: i64 roomId, str lessonId. */
    PRACTICE_ASSIGN_REQ(0x75),
    /** Đọc danh sách bài gắn với phiên: i64 roomId. */
    PRACTICE_LESSONS_REQ(0x76),
    PRACTICE_LESSONS_DATA(0x77),
    /** Giáo viên bỏ bài khỏi phiên: i64 roomId, str lessonId. */
    PRACTICE_UNASSIGN_REQ(0x78),
    // 0x26, 0x27, 0x2B đã thuộc ADMIN_LESSON_* (bản #7) nên ba mã lớp/phiên này dời về đây
    CLASS_SELECT_RESULT(0x79),
    PRACTICE_LIST_REQ(0x7A),
    PRACTICE_LIST_DATA(0x7B),
    // 0xA0–0xBF: giai đoạn 6 (docs/plans/giai-doan-6.md); 0x80–0x9F để dành cho đợt 2
    /** Thảo luận ở từng bài (DiscussionModule): str lessonId → DISCUSS_DATA. */
    DISCUSS_REQ(0xA0),
    DISCUSS_DATA(0xA1),
    /** JSON{lessonId, parentId?, text} → DISCUSS_NEW cho người gửi và cả lớp. */
    DISCUSS_POST(0xA2),
    /** Giáo viên/quản trị: i64 id, bool hidden. */
    DISCUSS_HIDE(0xA3),
    DISCUSS_NEW(0xA4),
    /** Số liệu trang chủ theo vai trò, payload rỗng. */
    HOME_REQ(0xA8),
    /** JSON số liệu trang chủ: xem HomeModule. */
    HOME_DATA(0xA9);

    private static final MessageType[] BY_CODE = new MessageType[256];

    static {
        for (MessageType t : values()) {
            if (BY_CODE[t.code] != null) {
                throw new ExceptionInInitializerError("Trùng mã thông điệp 0x" + Integer.toHexString(t.code));
            }
            BY_CODE[t.code] = t;
        }
    }

    private final int code;

    MessageType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    /** Mã lạ (không có trong bảng) là frame hỏng: kết nối gửi nó sẽ bị đóng. */
    public static MessageType fromCode(int code) throws ProtocolException {
        MessageType t = code >= 0 && code < BY_CODE.length ? BY_CODE[code] : null;
        if (t == null) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "Loại thông điệp lạ: 0x" + Integer.toHexString(code));
        }
        return t;
    }
}
