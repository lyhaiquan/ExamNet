package labcast.server.lesson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
}
