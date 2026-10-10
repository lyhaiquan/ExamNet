package labcast.server.run;

import java.util.List;
import java.util.Map;

/**
 * Một lượt chạy gửi vào runner. {@code lessonId} và {@code views} chỉ để ghi vào sự kiện {@code hdr}
 * của trace (trình phát cần biết vẽ biến nào bằng bộ vẽ nào).
 */
public record Job(
        String code, String input, boolean trace, int timeLimitMs, String lessonId, List<Map<String, String>> views) {

    public Job(String code, String input, boolean trace, int timeLimitMs) {
        this(code, input, trace, timeLimitMs, "", List.of());
    }
}
