package labcast.server.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import labcast.common.protocol.Role;
import labcast.server.account.UserRepository;
import labcast.server.grade.Grader;
import labcast.server.grade.Verdict;
import labcast.server.lesson.Lesson;
import labcast.server.store.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProgressServiceTest {
    static final Lesson BAI = new Lesson(
            "dsa.thu.bai",
            "dsa",
            "thu",
            "Bài thử",
            "bai-tap",
            List.of("code"),
            List.of(),
            List.of(),
            1_000,
            List.of(),
            List.of("dsa.thu.bai-sau", "dsa.thu.chua-soan"),
            Path.of("."),
            "hash",
            List.of(),
            List.of(),
            0);
    static final Grader.Result SAI = new Grader.Result(Verdict.WRONG, "Sai kết quả ở test ẩn an-1", List.of(), "an-1");
    static final Grader.Result DUNG = new Grader.Result(Verdict.OK, "Qua hết", List.of(), null);

    Database db;
    ProgressService p;
    long sv;

    @BeforeEach
    void bat() throws Exception {
        db = Database.open(":memory:");
        db.tx(c -> {
            try (var st = c.createStatement()) {
                return st.executeUpdate("INSERT INTO lessons (id, mon, chuong, ten, loai, path, content_hash)"
                        + " VALUES ('dsa.thu.bai', 'dsa', 'thu', 'Bài thử', 'bai-tap', 'x', 'h')");
            }
        });
        sv = new UserRepository(db).create("sv", "SV", Role.STUDENT, "1");
        p = new ProgressService(db);
    }

    @AfterEach
    void tat() throws Exception {
        db.close();
    }

    @Test
    void nopSaiLienTiep_choTangDan_15_30_60_120_roiGiu120() throws Exception {
        long now = 1_000_000;
        long[] mong = {15_000, 30_000, 60_000, 120_000, 120_000};
        for (long cd : mong) {
            assertEquals(cd, p.record(sv, BAI, 1, SAI, now).cooldownMs());
            assertEquals(cd, p.cooldownLeft(sv, BAI.id(), now));
            assertEquals(cd - 1_000, p.cooldownLeft(sv, BAI.id(), now + 1_000));
            now += cd;
            assertEquals(0, p.cooldownLeft(sv, BAI.id(), now));
        }
    }

    @Test
    void diem_tru20MoiLanSai_thapNhat40() throws Exception {
        p.record(sv, BAI, 1, SAI, 0);
        p.record(sv, BAI, 2, SAI, 100_000);
        ProgressService.SubmitOutcome ok = p.record(sv, BAI, 3, DUNG, 200_000);
        assertEquals(60, ok.score());
        assertTrue(ok.mastered());
        assertEquals(0, p.cooldownLeft(sv, BAI.id(), 200_000));

        long sv2 = new UserRepository(db).create("sv2", "SV2", Role.STUDENT, "1");
        long now = 0;
        for (int i = 0; i < 6; i++) {
            now += p.record(sv2, BAI, 1, SAI, now).cooldownMs();
        }
        assertEquals(40, p.record(sv2, BAI, 2, DUNG, now).score());
    }

    @Test
    void daThanhThao_nopSaiSauDoKhongMatDiemKhongPhaiCho() throws Exception {
        assertEquals(100, p.record(sv, BAI, 1, DUNG, 0).score());
        ProgressService.SubmitOutcome sau = p.record(sv, BAI, 2, SAI, 1_000);
        assertEquals(100, sau.score());
        assertEquals(0, sau.cooldownMs());
        assertEquals(0, p.cooldownLeft(sv, BAI.id(), 1_000));
    }

    @Test
    void moKhoa_chiBaiCoThat() {
        assertEquals(List.of("dsa.thu.bai-sau"), ProgressService.unlocked(BAI, "dsa.thu.bai-sau"::equals));
    }

    @Test
    void moiLuotNopGhiVaoSubmissions() throws Exception {
        p.record(sv, BAI, 1, SAI, 0);
        p.record(sv, BAI, 2, DUNG, 20_000);
        long n = db.read(c -> {
            try (var st = c.createStatement();
                    var rs = st.executeQuery(
                            "SELECT COUNT(*) FROM submissions WHERE kind = 'SUBMIT' AND lesson_id = 'dsa.thu.bai'")) {
                rs.next();
                return rs.getLong(1);
            }
        });
        assertEquals(2, n);
    }
}
