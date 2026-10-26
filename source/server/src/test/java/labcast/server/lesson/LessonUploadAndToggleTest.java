package labcast.server.lesson;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.TestClient;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.store.Database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LessonUploadAndToggleTest {

    @Test
    void toggleLessonActive_and_uploadZipLesson(@TempDir Path tempContent) throws Exception {
        Path baiGoc = Files.createDirectories(tempContent.resolve("dsa/sap-xep/noi-bot"));
        Files.writeString(
                baiGoc.resolve("lesson.yaml"),
                """
                id: dsa.sap-xep.noi-bot
                mon: dsa
                chuong: sap-xep
                ten: Sắp xếp nổi bọt
                loai: bai-tap
                """);
        Files.writeString(baiGoc.resolve("de.md"), "# Đề bài nổi bọt");
        Files.writeString(baiGoc.resolve("khung.py"), "def sort(a):\\n    pass\\n");

        var cfg = ServerConfig.forTest().withContent(tempContent).withKhongSandbox(true);

        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(cfg, db, List.of(new AccountModule(), new LessonModule()))) {
            server.start();
            var users = new UserRepository(db);
            users.create("gv", "Giáo viên", Role.TEACHER, "123");
            users.create("sv", "Sinh viên", Role.STUDENT, "123");

            // 1. Giáo viên đăng nhập và toggle bài dsa.sap-xep.noi-bot sang inactive (false)
            try (var gvClient = TestClient.connect(server.port())) {
                assertEquals(MessageType.AUTH_OK, gvClient.login("gv", "123").type());

                gvClient.send(
                        MessageType.ADMIN_LESSON_TOGGLE,
                        new PayloadWriter()
                                .str("dsa.sap-xep.noi-bot")
                                .bool(false)
                                .toBytes());
                Frame okFrame = gvClient.readUntil(MessageType.ADMIN_OK, MessageType.ERROR);
                assertEquals(MessageType.ADMIN_OK, okFrame.type());

                Path inactiveFile = tempContent.resolve(".inactive_lessons.json");
                assertTrue(Files.exists(inactiveFile));
                String content = Files.readString(inactiveFile);
                assertTrue(content.contains("dsa.sap-xep.noi-bot"));
            }

            // 2. Học viên đăng nhập và gọi LESSON_LIST: thấy bài bị khóa / active: false
            try (var svClient = TestClient.connect(server.port())) {
                assertEquals(MessageType.AUTH_OK, svClient.login("sv", "123").type());

                svClient.send(MessageType.LESSON_LIST, new byte[0]);
                JsonNode list = JsonPayload.tree(svClient.readUntil(MessageType.LESSON_DATA));
                JsonNode lessons = list.path("lessons");
                assertEquals(1, lessons.size());
                assertFalse(lessons.get(0).path("active").asBoolean());
                assertEquals("LOCKED", lessons.get(0).path("status").asText());

                // Học viên thử fetch bài đang bị inactive: bị chặn bởi ERROR
                svClient.send(
                        MessageType.LESSON_FETCH,
                        new PayloadWriter().str("dsa.sap-xep.noi-bot").toBytes());
                Frame resp = svClient.readUntil(MessageType.LESSON_DATA, MessageType.ERROR);
                assertEquals(MessageType.ERROR, resp.type());
            }

            // 3. Giáo viên tải lên bài tập mới dạng file ZIP
            byte[] zipData;
            try (var baos = new ByteArrayOutputStream();
                    var zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry("lesson.yaml"));
                zos.write(
                        """
                        id: dsa.sap-xep.chen-moi
                        mon: dsa
                        chuong: sap-xep
                        ten: Sắp xếp chèn mới
                        loai: bai-tap
                        """
                                .getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();

                zos.putNextEntry(new ZipEntry("de.md"));
                zos.write("# Đề chèn mới".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();

                zos.putNextEntry(new ZipEntry("khung.py"));
                zos.write("print('hello')\\n".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();

                zos.finish();
                zipData = baos.toByteArray();
            }

            try (var gvClient = TestClient.connect(server.port())) {
                assertEquals(MessageType.AUTH_OK, gvClient.login("gv", "123").type());

                gvClient.send(
                        MessageType.ADMIN_LESSON_UPLOAD,
                        new PayloadWriter().str("bai_chen.zip").blob(zipData).toBytes());
                Frame uploadResp = gvClient.readUntil(MessageType.ADMIN_OK, MessageType.ERROR);
                assertEquals(MessageType.ADMIN_OK, uploadResp.type());

                // Gọi LESSON_LIST: phải thấy cả 2 bài
                gvClient.send(MessageType.LESSON_LIST, new byte[0]);
                JsonNode list = JsonPayload.tree(gvClient.readUntil(MessageType.LESSON_DATA));
                assertEquals(2, list.path("lessons").size());

                // Giáo viên toggle lại bài noi-bot thành active (true)
                gvClient.send(
                        MessageType.ADMIN_LESSON_TOGGLE,
                        new PayloadWriter()
                                .str("dsa.sap-xep.noi-bot")
                                .bool(true)
                                .toBytes());
                assertEquals(
                        MessageType.ADMIN_OK,
                        gvClient.readUntil(MessageType.ADMIN_OK).type());
            }

            // 4. Học viên check lại: bài noi-bot đã active và fetch được
            try (var svClient = TestClient.connect(server.port())) {
                assertEquals(MessageType.AUTH_OK, svClient.login("sv", "123").type());

                svClient.send(MessageType.LESSON_LIST, new byte[0]);
                JsonNode list = JsonPayload.tree(svClient.readUntil(MessageType.LESSON_DATA));
                assertEquals(2, list.path("lessons").size());

                svClient.send(
                        MessageType.LESSON_FETCH,
                        new PayloadWriter().str("dsa.sap-xep.noi-bot").toBytes());
                Frame fetchResp = svClient.readUntil(MessageType.LESSON_DATA, MessageType.ERROR);
                assertEquals(MessageType.LESSON_DATA, fetchResp.type());
            }

            // 5. Giáo viên xoá bài chèn vừa tải lên
            try (var gvClient = TestClient.connect(server.port())) {
                assertEquals(MessageType.AUTH_OK, gvClient.login("gv", "123").type());

                gvClient.send(
                        MessageType.ADMIN_LESSON_DELETE,
                        new PayloadWriter().str("dsa.sap-xep.chen-moi").toBytes());
                Frame delResp = gvClient.readUntil(MessageType.ADMIN_OK, MessageType.ERROR);
                assertEquals(MessageType.ADMIN_OK, delResp.type());

                // Gọi LESSON_LIST: chỉ còn 1 bài duy nhất
                gvClient.send(MessageType.LESSON_LIST, new byte[0]);
                JsonNode list = JsonPayload.tree(gvClient.readUntil(MessageType.LESSON_DATA));
                assertEquals(1, list.path("lessons").size());
            }
        }
    }
}
