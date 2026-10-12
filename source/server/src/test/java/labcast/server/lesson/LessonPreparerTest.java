package labcast.server.lesson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import labcast.server.run.ScriptRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LessonPreparerTest {
    static Lesson bai(String hash) {
        return new Lesson(
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
                List.of(),
                Path.of("."),
                hash,
                List.of(),
                List.of(),
                2);
    }

    static ScriptRunner gia(AtomicInteger dem, String dong) {
        return (script, dir, stdin, deadline) -> {
            assertEquals("chuan_bi", script);
            dem.incrementAndGet();
            return new ScriptRunner.Result(List.of(dong), 0, "", false, 5);
        };
    }

    static final String KET_QUA =
            "{\"t\":\"chuan_bi\",\"tests\":[{\"id\":\"an-1\",\"input\":\"2 1\\n\",\"output\":\"1 2\\n\"},"
                    + "{\"id\":\"an-2\",\"input\":\"5\\n\",\"output\":\"5\\n\"}],\"tb\":{\"input\":\"3 2 1\\n\",\"output\":\"1 2 3\\n\"},"
                    + "\"max_ms\":12}";

    @Test
    void sinhMotLan_khoiDongLaiDocDem_doiNoiDungThiSinhLai(@TempDir Path cache) throws Exception {
        var dem = new AtomicInteger();
        LessonPreparer.Prepared p = new LessonPreparer(gia(dem, KET_QUA), cache).prepare(bai("h1"));
        assertEquals(
                List.of("an-1", "an-2"),
                p.tests().stream().map(LessonPreparer.HiddenTest::id).toList());
        assertEquals("1 2 3\n", p.tb().output());
        assertEquals(1, dem.get());

        // "khởi động lại server": đối tượng mới, cùng thư mục đệm → không chạy sandbox nữa
        new LessonPreparer(gia(dem, KET_QUA), cache).prepare(bai("h1"));
        assertEquals(1, dem.get());

        // nội dung bài đổi → hash đổi → sinh lại
        new LessonPreparer(gia(dem, KET_QUA), cache).prepare(bai("h2"));
        assertEquals(2, dem.get());
    }

    @Test
    void boSinhTestHong_baoLoiBai(@TempDir Path cache) {
        var dem = new AtomicInteger();
        var ex = assertThrows(IOException.class, () -> new LessonPreparer(
                        gia(dem, "{\"t\":\"loi\",\"loi\":\"sinh_test.py lon 3 lỗi\"}"), cache)
                .prepare(bai("h1")));
        assertEquals(true, ex.getMessage().contains("sinh_test.py"));
    }
}
