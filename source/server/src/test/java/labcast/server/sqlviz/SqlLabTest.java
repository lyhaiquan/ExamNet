package labcast.server.sqlviz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import labcast.server.TestPaths;
import labcast.server.grade.Grader;
import labcast.server.grade.Verdict;
import labcast.server.lesson.Lesson;
import labcast.server.lesson.LessonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqlLabTest {
    static final String BAI = "sql.null-case.chua-co-lop";

    @TempDir
    Path tmp;

    LessonRepository repo = LessonRepository.load(TestPaths.repoRoot().resolve("content"));

    Lesson bai() {
        return repo.get(BAI).orElseThrow();
    }

    /** Một database ẩn: có sinh viên chưa xếp lớp với id > 10 và tên riêng, dễ nhận ra nếu bị lộ. */
    static final String AN =
            """
            INSERT INTO lop VALUES (1, 'L1', 'CNTT', NULL);
            INSERT INTO sinh_vien VALUES (11, 'Bí Mật Một', 'Nam', '2005-01-01', 'Huế', NULL);
            INSERT INTO sinh_vien VALUES (12, 'Bí Mật Hai', 'Nữ', '2005-02-02', NULL, 1);
            """;

    @Test
    void duLieuAnHong_laLoiCuaNguoiSoan_khongPhaiCuaHocVien() {
        var lab = new SqlLab(repo, tmp);
        assertThrows(
                IOException.class,
                () -> lab.grade(
                        bai(),
                        "SELECT id, ho_ten FROM sinh_vien WHERE lop_id IS NULL",
                        List.of("INSERT INTO khong_co_bang VALUES (1);")));
    }

    @Test
    void cauCoYGayLoiTrenDuLieuAn_khongLoGiaTriAn() throws Exception {
        var lab = new SqlLab(repo, tmp);
        // Đúng trên dữ liệu mẫu (id ≤ 10); trên dữ liệu ẩn json_extract báo lỗi với tên sinh viên làm đường dẫn
        String sql = "SELECT id, ho_ten FROM sinh_vien WHERE lop_id IS NULL"
                + " AND CASE WHEN id <= 10 THEN 1 ELSE json_extract('{}', ho_ten) IS NULL END";
        Grader.Result r = lab.grade(bai(), sql, List.of(AN));
        assertEquals(Verdict.RUNTIME_ERROR, r.verdict(), r.message());
        assertEquals("an-1", r.failedTest());
        assertFalse(r.message().contains("Bí Mật"), "lời báo không được chứa dữ liệu ẩn: " + r.message());
        assertTrue(r.message().contains("bộ dữ liệu ẩn"));
        assertEquals(null, r.tests().get(1).error(), "lỗi chi tiết của test ẩn không gửi đi");
    }

    @Test
    void lamDung_quaDuLieuMauVaDuLieuAn() throws Exception {
        var lab = new SqlLab(repo, tmp);
        Grader.Result r = lab.grade(bai(), "SELECT id, ho_ten FROM sinh_vien WHERE lop_id IS NULL", List.of(AN));
        assertEquals(Verdict.OK, r.verdict(), r.message());
        assertEquals(2, r.tests().size());
    }

    @Test
    void hetNganSachThoiGian_chiHienKetQuaCuoi() throws Exception {
        var lab = new SqlLab(repo, tmp);
        Path db = lab.sample(bai());
        List<String> lines = new StepPlanner(new SqliteEngine(), db, SqlLab.TIMEOUT, java.time.Duration.ofNanos(1))
                .explainSelect("x", SqlGuard.check("SELECT id FROM sinh_vien WHERE lop_id IS NULL", false));
        String all = String.join("\n", lines);
        assertTrue(all.contains("Chỉ hiện kết quả cuối"), all);
        assertTrue(all.contains("sql.result"));
    }
}
