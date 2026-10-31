package labcast.server.lesson;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.TestClient;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.store.Database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Độ khó của bài: khai trong lesson.yaml, giáo viên gán đè hàng loạt. */
class LessonDifficultyTest {

    private static void bai(Path root, String slug, String extraYaml) throws Exception {
        Path dir = Files.createDirectories(root.resolve("dsa/sap-xep/" + slug));
        Files.writeString(
                dir.resolve("lesson.yaml"),
                """
                id: dsa.sap-xep.%s
                mon: dsa
                chuong: sap-xep
                ten: Bài %s
                loai: bai-tap
                %s
                """
                        .formatted(slug, slug, extraYaml));
        Files.writeString(dir.resolve("de.md"), "# Đề");
        Files.writeString(dir.resolve("khung.py"), "pass\n");
    }

    private static JsonNode lessonsOf(TestClient c) throws Exception {
        c.send(MessageType.LESSON_LIST, new byte[0]);
        return JsonPayload.tree(c.readUntil(MessageType.LESSON_DATA)).path("lessons");
    }

    private static String doKho(JsonNode lessons, String slug) {
        for (JsonNode l : lessons) {
            if (l.path("id").asText().equals("dsa.sap-xep." + slug))
                return l.path("doKho").asText("?");
        }
        return "khong-co";
    }

    private static void set(TestClient c, List<String> ids, String level) throws Exception {
        var req = JsonNodeFactory.instance.objectNode();
        var arr = req.putArray("lessonIds");
        ids.forEach(arr::add);
        req.put("doKho", level);
        c.send(JsonPayload.frame(MessageType.LESSON_META_SET, req));
    }

    @Test
    void yamlCoDoKho_listTraVe_baiThieuHoacSaiThiChuaPhanLoai(@TempDir Path content) throws Exception {
        bai(content, "de-bai", "do_kho: de");
        bai(content, "kho-bai", "do_kho: kho");
        bai(content, "khong-khai", "");
        bai(content, "khai-sai", "do_kho: sieu-kho");
        var cfg = ServerConfig.forTest().withContent(content).withKhongSandbox(true);
        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(cfg, db, List.of(new AccountModule(), new LessonModule()))) {
            server.start();
            new UserRepository(db).create("sv", "SV", Role.STUDENT, "123");
            try (var c = TestClient.connect(server.port())) {
                c.login("sv", "123");
                var lessons = lessonsOf(c);
                assertEquals("de", doKho(lessons, "de-bai"));
                assertEquals("kho", doKho(lessons, "kho-bai"));
                assertEquals("", doKho(lessons, "khong-khai"));
                assertEquals("", doKho(lessons, "khai-sai"), "giá trị sai không làm hỏng bài");
            }
        }
    }

    @Test
    void giaoVienGanDeBangMeta_hangLoat_vaBoGanThiVeYaml(@TempDir Path content) throws Exception {
        bai(content, "a", "do_kho: de");
        bai(content, "b", "");
        var cfg = ServerConfig.forTest().withContent(content).withKhongSandbox(true);
        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(cfg, db, List.of(new AccountModule(), new LessonModule()))) {
            server.start();
            var users = new UserRepository(db);
            users.create("gv", "GV", Role.TEACHER, "123");
            users.create("sv", "SV", Role.STUDENT, "123");
            try (var gv = TestClient.connect(server.port())) {
                gv.login("gv", "123");
                set(gv, List.of("dsa.sap-xep.a", "dsa.sap-xep.b"), "kho");
                gv.readUntil(MessageType.ADMIN_OK, MessageType.ERROR);
                var lessons = lessonsOf(gv);
                assertEquals("kho", doKho(lessons, "a"), "gán đè yaml");
                assertEquals("kho", doKho(lessons, "b"));

                set(gv, List.of("dsa.sap-xep.a"), "");
                gv.readUntil(MessageType.ADMIN_OK, MessageType.ERROR);
                lessons = lessonsOf(gv);
                assertEquals("de", doKho(lessons, "a"), "bỏ gán thì trở về giá trị trong yaml");
                assertEquals("kho", doKho(lessons, "b"));
            }
            try (var sv = TestClient.connect(server.port())) {
                sv.login("sv", "123");
                assertEquals("kho", doKho(lessonsOf(sv), "b"), "học viên cũng thấy độ khó đã gán");
            }
        }
    }

    @Test
    void hocVienGanDoKho_biTuChoi_giaTriSaiHoacBaiLa_biBao(@TempDir Path content) throws Exception {
        bai(content, "a", "");
        var cfg = ServerConfig.forTest().withContent(content).withKhongSandbox(true);
        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(cfg, db, List.of(new AccountModule(), new LessonModule()))) {
            server.start();
            var users = new UserRepository(db);
            users.create("gv", "GV", Role.TEACHER, "123");
            users.create("sv", "SV", Role.STUDENT, "123");
            try (var sv = TestClient.connect(server.port())) {
                sv.login("sv", "123");
                set(sv, List.of("dsa.sap-xep.a"), "kho");
                assertEquals(
                        ErrorCode.FORBIDDEN,
                        ErrorMessage.from(sv.readUntil(MessageType.ERROR)).code());
            }
            try (var gv = TestClient.connect(server.port())) {
                gv.login("gv", "123");
                set(gv, List.of("dsa.sap-xep.a"), "sieu-kho");
                assertEquals(
                        ErrorCode.BAD_FRAME,
                        ErrorMessage.from(gv.readUntil(MessageType.ERROR)).code());
                set(gv, List.of("dsa.khong.co"), "de");
                assertEquals(
                        ErrorCode.NOT_FOUND,
                        ErrorMessage.from(gv.readUntil(MessageType.ERROR)).code());
                set(gv, List.of(), "de");
                assertEquals(
                        ErrorCode.BAD_FRAME,
                        ErrorMessage.from(gv.readUntil(MessageType.ERROR)).code());
            }
        }
    }
}
