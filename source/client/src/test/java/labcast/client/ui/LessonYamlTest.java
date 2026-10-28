package labcast.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import labcast.server.lesson.Lesson;
import labcast.server.lesson.LessonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** yaml do màn Thêm bài tập sinh ra phải đọc được bằng đúng bộ đọc của server. */
class LessonYamlTest {

    private static Lesson parse(Path root, String rel, String yaml) throws Exception {
        Path dir = Files.createDirectories(root.resolve(rel));
        Files.writeString(dir.resolve("lesson.yaml"), yaml);
        Files.writeString(dir.resolve("de.md"), "Đề");
        if (rel.startsWith("sql/")) {
            for (String f : java.util.List.of("schema.sql", "du_lieu_mau.sql", "dap_an.sql", "khung.sql"))
                Files.writeString(dir.resolve(f), "SELECT 1;");
        }
        return LessonRepository.parseLesson(dir, rel);
    }

    @Test
    void baiSqlDml_serverDocDung(@TempDir Path root) throws Exception {
        String yaml = LessonYaml.sql("sql.dml.tang-luong", "dml", "Tăng lương \"nhân viên\"", "bai-tap", "dml", false);
        Lesson l = parse(root, "sql/dml/tang-luong", yaml);
        assertEquals("Tăng lương \"nhân viên\"", l.ten());
        assertEquals("dml", l.loaiCau());
        assertFalse(l.thuTu());
        assertEquals("bang-sql", l.khungNhin().get(0).kieu());
        assertEquals(java.util.List.of("code"), l.bac());
    }

    @Test
    void baiSqlMau_coThuTu_coBacXem(@TempDir Path root) throws Exception {
        Lesson l = parse(
                root,
                "sql/co-ban/top-3",
                LessonYaml.sql("sql.co-ban.top-3", "co-ban", "Top 3", "giang-giai", "select", true));
        assertTrue(l.thuTu());
        assertEquals("select", l.loaiCau());
        assertEquals(java.util.List.of("xem", "code"), l.bac());
    }

    @Test
    void baiDsaMau_coKhungNhin(@TempDir Path root) throws Exception {
        Map<String, String> views = new LinkedHashMap<>();
        views.put("a", "mang");
        views.put("st", "ngan-xep");
        Lesson l = parse(
                root,
                "dsa/ngan-xep/dao-mang",
                LessonYaml.dsa("dsa.ngan-xep.dao-mang", "ngan-xep", "Đảo mảng", "giang-giai", views, true));
        assertEquals(2, l.khungNhin().size());
        assertEquals("ngan-xep", l.khungNhin().get(1).kieu());
        assertEquals(java.util.List.of("mau-1"), l.testXem());
    }
}
