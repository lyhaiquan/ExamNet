package labcast.server.lesson;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ChapterNameTest {

    @Test
    void docTenChuongTuFile_khongCoFileThiRong(@TempDir Path dir) throws Exception {
        Path chuong = Files.createDirectories(dir.resolve("dsa/hang-doi-uu-tien"));
        assertEquals("", LessonModule.chapterName(chuong));
        Files.writeString(chuong.resolve(LessonModule.TEN_CHUONG), "Hàng đợi ưu tiên\n");
        assertEquals("Hàng đợi ưu tiên", LessonModule.chapterName(chuong));
        assertEquals("", LessonModule.chapterName(null));
    }
}
