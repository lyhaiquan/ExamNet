package labcast.server.lesson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import labcast.common.protocol.Language;
import labcast.server.TestPaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LessonRepositoryTest {

    private static Path contentGia() throws Exception {
        return Path.of(LessonRepositoryTest.class.getResource("/content-gia").toURI());
    }

    @Test
    void chiMotMon_giuDungBaiCuaMonDo() {
        var all = LessonRepository.load(TestPaths.repoRoot().resolve("content"));
        var sql = all.chiMon("sql");
        assertTrue(sql.all().size() > 0);
        assertTrue(sql.all().stream().allMatch(Lesson::isSql));
        assertEquals(all.all().stream().filter(Lesson::isSql).count(), sql.all().size());
        assertTrue(sql.get("dsa.sap-xep.noi-bot").isEmpty());
        assertTrue(all.chiMon("dsa").get("dsa.sap-xep.noi-bot").isPresent());
    }

    @Test
    void motBaiDungMotBaiThieuYaml() throws Exception {
        var repo = LessonRepository.load(contentGia());
        assertEquals(1, repo.all().size());
        Lesson l = repo.all().get(0);
        assertEquals("dsa.sap-xep.tot", l.id());
        assertEquals(List.of(new ViewSpec("a", "mang")), l.khungNhin());
        assertEquals(1500, l.thoiGianMs());
        assertEquals("print(1)\n", repo.khung(l.id()));
        assertEquals(1, repo.errors().size());
        assertTrue(
                repo.errors().get(0).contains("dsa/sap-xep/hong"), repo.errors().get(0));
        assertTrue(repo.errors().get(0).contains("lesson.yaml"));
    }

    @Test
    void idKhongKhopDuongDan_laLoi(@TempDir Path dir) throws Exception {
        Path bai = Files.createDirectories(dir.resolve("dsa/cay/bst"));
        Files.writeString(
                bai.resolve("lesson.yaml"), "id: dsa.cay.khac\nmon: dsa\nchuong: cay\nten: X\nloai: bai-tap\n");
        var repo = LessonRepository.load(dir);
        assertEquals(0, repo.all().size());
        assertTrue(repo.errors().get(0).contains("dsa.cay.bst"));
    }

    @Test
    void chayTungDongChiChoBaiGiangGiai(@TempDir Path dir) throws Exception {
        Path bai = Files.createDirectories(dir.resolve("dsa/de-quy/x"));
        String yaml =
                """
                id: dsa.de-quy.x
                mon: dsa
                chuong: de-quy
                ten: X
                loai: %s
                khung_nhin:
                  - bien: code
                    kieu: code-bien
                """;
        Files.writeString(bai.resolve("lesson.yaml"), yaml.formatted("bai-tap"));
        var repo = LessonRepository.load(dir);
        assertEquals(0, repo.all().size());
        assertTrue(repo.errors().get(0).contains("code-bien"), repo.errors().get(0));
        Files.writeString(bai.resolve("lesson.yaml"), yaml.formatted("giang-giai"));
        assertEquals(1, LessonRepository.load(dir).all().size());
    }

    @Test
    void testIdKhongChoThoatThuMuc() throws Exception {
        var repo = LessonRepository.load(contentGia());
        assertTrue(repo.testInput("dsa.sap-xep.tot", "../../../x").isEmpty());
    }

    @Test
    void baiNoiBotThat_napDuoc() {
        var repo = LessonRepository.load(TestPaths.repoRoot().resolve("content"));
        assertEquals(List.of(), repo.errors());
        Lesson l = repo.get("dsa.sap-xep.noi-bot").orElseThrow();
        assertEquals("giang-giai", l.loai());
        assertEquals("1 2 3\n", repo.testOutput(l.id(), "nho-1").orElseThrow());
        assertTrue(repo.loiGiai(l.id()).isPresent());
        assertEquals(64, l.contentHash().length());
    }

    @Test
    void ngonNgu_macDinhCaBon_khaiBaoThiTheoKhaiBao(@TempDir Path dir) throws Exception {
        Path bai = Files.createDirectories(dir.resolve("dsa/mang/tong"));
        String yaml = "id: dsa.mang.tong\nmon: dsa\nchuong: mang\nten: Tong\nloai: luyen-tap\n";
        Files.writeString(bai.resolve("lesson.yaml"), yaml);
        Lesson l = LessonRepository.load(dir).all().get(0);
        assertEquals(List.of(Language.values()), l.ngonNgu());
        assertTrue(l.camTheoNgonNgu().isEmpty());

        Files.writeString(
                bai.resolve("lesson.yaml"),
                yaml + "ngon_ngu: [python, cpp]\ncam_theo_ngon_ngu:\n  cpp: [sort, priority_queue]\n");
        l = LessonRepository.load(dir).all().get(0);
        assertEquals(List.of(Language.PYTHON, Language.CPP), l.ngonNgu());
        assertTrue(l.choPhep(Language.CPP) && !l.choPhep(Language.JAVA));
        assertEquals(Map.of("cpp", List.of("sort", "priority_queue")), l.camTheoNgonNgu());

        Files.writeString(bai.resolve("lesson.yaml"), yaml + "ngon_ngu: [python, go]\n");
        var repo = LessonRepository.load(dir);
        assertEquals(0, repo.all().size());
        assertTrue(repo.errors().get(0).contains("go"), repo.errors().get(0));
    }

    @Test
    void khungTheoKhoaTaiLieu_vaLoiGiaiNgonNguKhac(@TempDir Path dir) throws Exception {
        Path bai = Files.createDirectories(dir.resolve("dsa/mang/tong"));
        Files.writeString(
                bai.resolve("lesson.yaml"), "id: dsa.mang.tong\nmon: dsa\nchuong: mang\nten: Tong\nloai: giang-giai\n");
        Files.writeString(bai.resolve("khung.py"), "print(0)\n");
        Files.writeString(bai.resolve("khung.cpp"), "// khung C++ của bài\n");
        Files.writeString(bai.resolve("loi_giai.py"), "print(1)\n");
        Files.writeString(bai.resolve("loi_giai.java"), "public class Main {}\n");
        var repo = LessonRepository.load(dir);
        assertEquals("print(0)\n", repo.khung("dsa.mang.tong"));
        assertEquals("// khung C++ của bài\n", repo.khung("dsa.mang.tong#cpp"));
        assertEquals(Language.JAVA.starter(), repo.khung("dsa.mang.tong#java"), "thiếu khung.java: khung mặc định");
        assertEquals("", repo.khung("dsa.mang.tong#go"));
        // loi_giai.py không đưa ra (lộ đáp án), chỉ file ngôn ngữ khác người soạn bài thêm vào
        assertEquals(Map.of("java", "public class Main {}\n"), repo.loiGiaiKhac("dsa.mang.tong"));
    }
}
