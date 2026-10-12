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
        String contentHash,
        List<String> camImport,
        List<String> camCuPhap,
        int soTestAn) {

    /** Bài có luật soi trace ({@code luat.py}): chấm thêm bước kiểm trace sau khi qua hết test. */
    public boolean hasTraceRule() {
        return java.nio.file.Files.exists(dir.resolve("luat.py"));
    }
}
