package labcast.server.lesson;

import java.nio.file.Path;
import java.util.List;

/**
 * Cấu hình một bài, đọc từ {@code lesson.yaml} (spec §7.1). Bài SQL có thêm {@code loai_cau} ({@code
 * select} | {@code dml}) và {@code thu_tu} ({@code co}: so kết quả theo đúng thứ tự dòng).
 */
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
        int soTestAn,
        String loaiCau,
        boolean thuTu) {

    /** Bài DSA (và các chỗ tạo bài không nói gì về SQL): câu SELECT, không xét thứ tự dòng. */
    public Lesson(
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
        this(
                id,
                mon,
                chuong,
                ten,
                loai,
                bac,
                khungNhin,
                camDung,
                thoiGianMs,
                testXem,
                moKhoa,
                dir,
                contentHash,
                camImport,
                camCuPhap,
                soTestAn,
                "select",
                false);
    }

    /** Bài SQL: học viên viết câu SQL, chạy bằng bộ máy SQL thay cho sandbox Python. */
    public boolean isSql() {
        return mon.equals("sql");
    }

    /** Bài SQL cho phép INSERT/UPDATE/DELETE ({@code loai_cau: dml}); chấm bằng trạng thái database sau khi chạy. */
    public boolean isDml() {
        return loaiCau.equals("dml");
    }

    /** Bài có luật soi trace ({@code luat.py}): chấm thêm bước kiểm trace sau khi qua hết test. */
    public boolean hasTraceRule() {
        return java.nio.file.Files.exists(dir.resolve("luat.py"));
    }
}
