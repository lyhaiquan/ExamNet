package labcast.server.run;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Chạy một script của runner ({@code runner/<script>.py}) ngoài {@code run.py}: chấm bài nộp ({@code cham}),
 * chuẩn bị test ẩn ({@code chuan_bi}), kiểm luật trace ({@code kiem_luat}). Chủ: Người 3.
 */
public interface ScriptRunner {

    /** Các dòng stdout (không gồm dòng trống), mã thoát, stderr (đã cắt ngắn), có bị giết vì quá giờ không. */
    record Result(List<String> lines, int exitCode, String stderr, boolean killed, long ms) {}

    /**
     * @param lessonDir khác null: gắn thư mục bài CHỈ ĐỌC và truyền làm tham số đầu của script. CHỈ dùng cho
     *     script của nhóm chạy trên dữ liệu tin cậy ({@code chuan_bi}, {@code kiem_luat}), KHÔNG BAO GIỜ cho
     *     lượt chạy code học viên: gắn vào thì code học viên đọc được {@code loi_giai.py} và output test ẩn.
     * @param deadlineMs quá thời gian này (gồm cả dựng container) thì giết, {@link Result#killed()} = true
     * @throws IOException lỗi hạ tầng (Docker không chạy, thiếu image, thiếu Python)
     */
    Result runScript(String script, Path lessonDir, String stdin, long deadlineMs)
            throws IOException, InterruptedException;
}
