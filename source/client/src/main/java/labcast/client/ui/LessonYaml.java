package labcast.client.ui;

import java.util.Map;

/** Sinh {@code lesson.yaml} cho màn Thêm bài tập; thuần Java để test không cần JavaFX. */
final class LessonYaml {
    private LessonYaml() {}

    /** Bài DSA: {@code views} là biến → mã bộ vẽ; {@code sampleTest}: có test mẫu {@code mau-1}. */
    static String dsa(
            String id, String chuong, String ten, String loai, Map<String, String> views, boolean sampleTest) {
        return dsa(id, chuong, ten, loai, views, sampleTest, "");
    }

    /** Như trên, kèm độ khó {@code de | vua | kho} (rỗng: chưa phân loại, không ghi khoá do_kho). */
    static String dsa(
            String id,
            String chuong,
            String ten,
            String loai,
            Map<String, String> views,
            boolean sampleTest,
            String doKho) {
        StringBuilder y = head(id, "dsa", chuong, ten, loai, doKho);
        y.append("bac: [xem, code]\n");
        y.append("khung_nhin:").append(views.isEmpty() ? " []" : "").append('\n');
        views.forEach((bien, kieu) -> y.append("  - bien: ")
                .append(bien)
                .append("\n    kieu: ")
                .append(kieu)
                .append('\n'));
        return y.append("cam_dung: []\n")
                .append("cam_import: []\n")
                .append("cam_cu_phap: []\n")
                .append("gioi_han: { thoi_gian_ms: 2000, bo_nho_mb: 256 }\n")
                .append("test_xem: ")
                .append(sampleTest ? "[mau-1]" : "[]")
                .append('\n')
                .append("so_test_an: 0\n")
                .append("mo_khoa: []\n")
                .toString();
    }

    /**
     * Bài SQL, giống các bài có sẵn trong content/sql: vẽ bằng bảng, {@code loaiCau} là {@code select} hoặc
     * {@code dml}, {@code ordered}: so kết quả theo đúng thứ tự dòng (đề có ORDER BY).
     */
    static String sql(String id, String chuong, String ten, String loai, String loaiCau, boolean ordered) {
        return sql(id, chuong, ten, loai, loaiCau, ordered, "");
    }

    static String sql(
            String id, String chuong, String ten, String loai, String loaiCau, boolean ordered, String doKho) {
        return head(id, "sql", chuong, ten, loai, doKho)
                .append("bac: ")
                .append("giang-giai".equals(loai) ? "[xem, code]" : "[code]")
                .append('\n')
                .append("khung_nhin:\n  - bien: sql\n    kieu: bang-sql\n")
                .append("loai_cau: ")
                .append(loaiCau)
                .append('\n')
                .append("thu_tu: ")
                .append(ordered ? "co" : "khong")
                .append('\n')
                .append("gioi_han: { thoi_gian_ms: 2000 }\n")
                .append("mo_khoa: []\n")
                .toString();
    }

    private static StringBuilder head(String id, String mon, String chuong, String ten, String loai, String doKho) {
        StringBuilder b = head(id, mon, chuong, ten, loai);
        if (doKho != null && !doKho.isBlank())
            b.append("do_kho: ").append(doKho).append('\n');
        return b;
    }

    private static StringBuilder head(String id, String mon, String chuong, String ten, String loai) {
        return new StringBuilder()
                .append("id: ")
                .append(id)
                .append("\nmon: ")
                .append(mon)
                .append("\nchuong: ")
                .append(chuong)
                .append("\nten: ")
                .append(quote(ten))
                .append("\nloai: ")
                .append(loai)
                .append('\n');
    }

    /** Chuỗi YAML trong nháy kép, thoát ký tự đặc biệt, gộp xuống dòng. */
    static String quote(String v) {
        return "\""
                + v.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\r", " ")
                        .replace("\n", " ") + "\"";
    }
}
