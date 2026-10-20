package labcast.server.lesson;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Trace giảng giải của một bài (các dòng JSON), cho module khác dùng — chiếu animation (ĐG1) lấy trace
 * từ đây. Danh sách rỗng: bài không có, không phải bài giảng giải, hoặc lời giải mẫu chạy lỗi.
 */
@FunctionalInterface
public interface DemoTraces {
    CompletableFuture<List<String>> demoTrace(String lessonId);
}
