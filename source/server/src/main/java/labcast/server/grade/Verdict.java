package labcast.server.grade;

/** Kết quả chấm một test hay cả bài nộp (spec §8.2). */
public enum Verdict {
    OK,
    WRONG,
    TIME_LIMIT,
    RUNTIME_ERROR,
    /** Dùng hàm, import hay cú pháp bị cấm ({@code cam_dung}, {@code cam_import}, {@code cam_cu_phap}). */
    FORBIDDEN_CALL,
    /** Qua mọi test nhưng trace vi phạm luật của bài ({@code luat.py}), ví dụ đổi chỗ hai phần tử không kề nhau. */
    TRACE_RULE
}
