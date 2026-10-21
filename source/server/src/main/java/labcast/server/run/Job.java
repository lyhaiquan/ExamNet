package labcast.server.run;

import java.util.List;
import java.util.Map;
import labcast.common.protocol.Language;

/**
 * Một lượt chạy gửi vào runner. {@code lessonId} và {@code views} chỉ để ghi vào sự kiện {@code hdr}
 * của trace (trình phát cần biết vẽ biến nào bằng bộ vẽ nào). {@code language}: ngôn ngữ của code (spec đa ngôn
 * ngữ); C/C++/Java chỉ ghi trace khi {@code lineTrace}.
 */
public record Job(
        String code,
        String input,
        boolean trace,
        int timeLimitMs,
        String lessonId,
        List<Map<String, String>> views,
        boolean lineTrace,
        Language language) {

    public Job(
            String code,
            String input,
            boolean trace,
            int timeLimitMs,
            String lessonId,
            List<Map<String, String>> views,
            boolean lineTrace) {
        this(code, input, trace, timeLimitMs, lessonId, views, lineTrace, Language.PYTHON);
    }

    public Job(
            String code,
            String input,
            boolean trace,
            int timeLimitMs,
            String lessonId,
            List<Map<String, String>> views) {
        this(code, input, trace, timeLimitMs, lessonId, views, false, Language.PYTHON);
    }

    public Job(String code, String input, boolean trace, int timeLimitMs) {
        this(code, input, trace, timeLimitMs, "", List.of(), false, Language.PYTHON);
    }

    /** Giới hạn thời gian sau khi nhân hệ số của ngôn ngữ (Java ×2). */
    public int effectiveTimeLimitMs() {
        return timeLimitMs * language.timeFactor();
    }

    /**
     * Thời gian ngoài giới hạn của bài mà lượt chạy được phép tốn: biên dịch, và debugger dừng ở từng dòng khi
     * chạy từng dòng C/C++/Java (runner tự cắt ở giới hạn + 15 s).
     */
    public long overheadMs() {
        long t = language.compileBudgetMs();
        if (lineTrace && language != Language.PYTHON) {
            t += 16_000;
        }
        return t;
    }
}
