package labcast.server.lesson;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import labcast.common.protocol.Language;

/**
 * Cấu hình một bài, đọc từ {@code lesson.yaml} (spec §7.1). Bài SQL có thêm {@code loai_cau} ({@code
 * select} | {@code dml}) và {@code thu_tu} ({@code co}: so kết quả theo đúng thứ tự dòng). {@code ngonNgu}: các
 * ngôn ngữ học viên được chọn (bài SQL: rỗng); {@code camTheoNgonNgu}: hàm cấm riêng cho C/C++/Java (spec đa ngôn
 * ngữ §2), thiếu thì runner suy từ {@code camDung}/{@code camImport}.
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
        boolean thuTu,
        List<Language> ngonNgu,
        Map<String, List<String>> camTheoNgonNgu,
        String doKho) {

    /** Mức độ khó hợp lệ trong {@code lesson.yaml} ({@code do_kho}) và {@code lesson_meta}. */
    public static final List<String> DO_KHO = List.of("de", "vua", "kho");

    /** Bài không khai độ khó: chưa phân loại. */
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
            int soTestAn,
            String loaiCau,
            boolean thuTu,
            List<Language> ngonNgu,
            Map<String, List<String>> camTheoNgonNgu) {
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
                loaiCau,
                thuTu,
                ngonNgu,
                camTheoNgonNgu,
                "");
    }

    /** Bài DSA cho cả bốn ngôn ngữ, bài SQL chỉ viết SQL. */
    public static List<Language> ngonNguMacDinh(String mon) {
        return mon.equals("sql") ? List.of() : List.of(Language.values());
    }

    /** Không nói gì về ngôn ngữ: mặc định theo môn, không có hàm cấm riêng. */
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
            int soTestAn,
            String loaiCau,
            boolean thuTu) {
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
                loaiCau,
                thuTu,
                ngonNguMacDinh(mon),
                Map.of());
    }

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

    /** Học viên được viết bài bằng ngôn ngữ này. */
    public boolean choPhep(Language l) {
        return ngonNgu.contains(l);
    }

    /** Bài có luật soi trace ({@code luat.py}): chấm thêm bước kiểm trace sau khi qua hết test. */
    public boolean hasTraceRule() {
        return java.nio.file.Files.exists(dir.resolve("luat.py"));
    }
}
