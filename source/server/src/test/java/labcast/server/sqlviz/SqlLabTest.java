package labcast.server.sqlviz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import labcast.server.TestPaths;
import labcast.server.grade.Grader;
import labcast.server.grade.Verdict;
import labcast.server.lesson.Lesson;
import labcast.server.lesson.LessonRepository;
import labcast.server.run.LocalPythonSandbox;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
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

    /** Ba database ẩn của bài, sinh bằng {@code sinh_du_lieu.py} của chính bài (seed 1, 2, 3) như server làm. */
    static List<String> duLieuAn(Lesson l) throws Exception {
        String py = LocalPythonSandbox.detectPython();
        assumeTrue(py != null, "máy không có Python 3");
        List<String> out = new ArrayList<>();
        for (int seed = 1; seed <= 3; seed++) {
            Process p =
                    new ProcessBuilder(py, l.dir().resolve("sinh_du_lieu.py").toString(), String.valueOf(seed)).start();
            String s = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(p.waitFor(30, TimeUnit.SECONDS));
            assertEquals(0, p.exitValue(), l.id() + " sinh_du_lieu.py " + seed);
            out.add(s);
        }
        return out;
    }

    /**
     * Mọi bài SQL (bài nào cũng có animation, kể cả luyện tập): animation đáp án tách được từng bước (không rơi về
     * "chỉ hiện kết quả cuối"), đáp án đạt trên dữ liệu mẫu cùng 3 database ẩn, và mỗi câu trong {@code sai.sql}
     * (câu sai hay gặp, nếu bài có) bị chấm sai.
     */
    @TestFactory
    Stream<DynamicTest> moiBaiSql_coAnimationTungBuoc_dapAnDat_cauSaiBiBat() {
        return repo.all().stream()
                .filter(l -> l.mon().equals("sql"))
                .map(l -> DynamicTest.dynamicTest(l.id(), () -> {
                    var lab = new SqlLab(repo, tmp);
                    String trace = String.join("\n", lab.demoTrace(l));
                    assertFalse(trace.isEmpty(), l.id() + ": không dựng được animation");
                    assertFalse(trace.contains("Chỉ hiện kết quả cuối"), l.id() + ": " + trace);
                    String dapAn =
                            java.nio.file.Files.readString(l.dir().resolve("dap_an.sql"), StandardCharsets.UTF_8);
                    List<String> an = duLieuAn(l);
                    Grader.Result r = lab.grade(l, dapAn, an);
                    assertEquals(Verdict.OK, r.verdict(), l.id() + ": " + r.message());
                    Path sai = l.dir().resolve("sai.sql");
                    if (java.nio.file.Files.exists(sai)) {
                        for (String cau :
                                SqlSplitter.split(java.nio.file.Files.readString(sai, StandardCharsets.UTF_8))) {
                            Grader.Result s = lab.grade(l, cau, an);
                            assertEquals(Verdict.WRONG, s.verdict(), l.id() + " câu sai vẫn đạt: " + cau);
                        }
                    }
                }));
    }

    /** Câu sai hay gặp của từng bài phải bị chấm sai: dữ liệu mẫu hoặc dữ liệu ẩn có đúng cái bẫy đó. */
    @Test
    void cauSaiHayGap_biChamSai() throws Exception {
        Map<String, String> sai = Map.of(
                "sql.co-ban.khong-trung", "SELECT DISTINCT thanh_pho FROM khach_hang",
                "sql.ket-noi.so-don-khach",
                        "SELECT k.id, k.ho_ten, COUNT(*) FROM khach_hang k LEFT JOIN don_hang d"
                                + " ON d.khach_hang_id = k.id GROUP BY k.id, k.ho_ten",
                "sql.null-case.xep-loai",
                        "SELECT sinh_vien_id, mon_hoc_id, diem, CASE WHEN diem >= 8 THEN 'Giỏi' WHEN diem >= 6.5"
                                + " THEN 'Khá' WHEN diem >= 5 THEN 'Trung bình' ELSE 'Yếu' END FROM ket_qua"
                                + " WHERE lan_thi = 1",
                "sql.dml.tang-luong", "UPDATE nhan_vien SET luong = luong * 1.1 WHERE phong_id = 1",
                "sql.truy-van-con.co-don-da-giao",
                        "SELECT k.id, k.ho_ten FROM khach_hang k JOIN don_hang d ON d.khach_hang_id = k.id"
                                + " WHERE d.trang_thai = 'da-giao'",
                "sql.tap-hop.thi-ca-hai-mon",
                        "SELECT sinh_vien_id FROM ket_qua WHERE mon_hoc_id = 1 INTERSECT"
                                + " SELECT sinh_vien_id FROM ket_qua WHERE mon_hoc_id = 2");
        var lab = new SqlLab(repo, tmp);
        for (var e : sai.entrySet()) {
            Lesson l = repo.get(e.getKey()).orElseThrow();
            Grader.Result r = lab.grade(l, e.getValue(), duLieuAn(l));
            assertEquals(Verdict.WRONG, r.verdict(), e.getKey() + ": " + r.message());
        }
    }
}
