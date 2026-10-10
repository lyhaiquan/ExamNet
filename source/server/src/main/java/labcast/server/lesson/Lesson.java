package labcast.server.lesson;

import java.nio.file.Path;
import java.util.List;

/** Cấu hình một bài, đọc từ {@code lesson.yaml} (spec §7.1). */
public record Lesson(
        String id,
        String mon,
        String chuong,
        String ten,
        String loai,
        List<String> bac,
        List<ViewSpec> khungNhin,
        List<String> camDung,
        int thoiGianMs,
        List<String> testXem,
        List<String> moKhoa,
        Path dir,
        String contentHash) {}
