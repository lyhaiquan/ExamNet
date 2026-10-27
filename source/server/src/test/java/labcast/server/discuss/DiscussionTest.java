package labcast.server.discuss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
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
import labcast.server.classroom.ClassRepository;
import labcast.server.store.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DiscussionTest {
    private static final String BAI = "dsa.sap-xep.a";
    private Database db;
    private LabCastServer server;

    @BeforeEach
    void batServer() throws Exception {
        db = Database.open(":memory:");
        server = new LabCastServer(
                ServerConfig.forTest().withLop("Lớp A"), db, List.of(new AccountModule(), new DiscussionModule()));
        server.start();
        var users = new UserRepository(db);
        var classes = new ClassRepository(db);
        long gvA = users.create("gvA", "Cô A", Role.TEACHER, "1");
        long gvB = users.create("gvB", "Thầy B", Role.TEACHER, "1");
        long lopA = classes.ensure("Lớp A", gvA);
        long lopB = classes.ensure("Lớp B", gvB);
        classes.enroll(lopA, users.create("an", "An", Role.STUDENT, "1"));
        classes.enroll(lopA, users.create("binh", "Bình", Role.STUDENT, "1"));
        classes.enroll(lopB, users.create("cuong", "Cường", Role.STUDENT, "1"));
        db.tx(c -> c.createStatement()
                .executeUpdate("INSERT INTO lessons VALUES ('" + BAI + "','dsa','sap-xep','Bài A','bai-tap','p','h')"));
    }

    @AfterEach
    void tatServer() throws Exception {
        server.close();
        db.close();
    }

    /** Gửi bình luận, bỏ qua bản phát bình luận của người khác, trả về phản hồi cho chính mình hoặc lỗi. */
    private static Frame post(TestClient c, String text, long parentId) throws Exception {
        c.send(JsonPayload.frame(
                MessageType.DISCUSS_POST, Map.of("lessonId", BAI, "text", text, "parentId", parentId)));
        while (true) {
            Frame f = c.readUntil(MessageType.DISCUSS_NEW, MessageType.ERROR);
            if (f.type() == MessageType.ERROR
                    || JsonPayload.tree(f).path("item").path("mine").asBoolean()) return f;
        }
    }

    private static JsonNode list(TestClient c) throws Exception {
        c.send(MessageType.DISCUSS_REQ, new PayloadWriter().str(BAI).toBytes());
        return JsonPayload.tree(c.readUntil(MessageType.DISCUSS_DATA)).path("items");
    }

    @Test
    void hocVienHaiLop_khongThayCuaNhau_vaBanCungLopNhanNgay() throws Exception {
        try (var an = TestClient.connect(server.port());
                var binh = TestClient.connect(server.port());
                var cuong = TestClient.connect(server.port())) {
            an.login("an", "1");
            binh.login("binh", "1");
            cuong.login("cuong", "1");
            Frame mine = post(an, "Em chưa hiểu vòng lặp trong", 0);
            assertEquals(MessageType.DISCUSS_NEW, mine.type());
            assertTrue(JsonPayload.tree(mine).path("item").path("mine").asBoolean());
            // Bình cùng lớp nhận DISCUSS_NEW mà không cần hỏi
            JsonNode pushed = JsonPayload.tree(binh.readUntil(MessageType.DISCUSS_NEW));
            assertEquals("An", pushed.path("item").path("author").asText());
            assertFalse(pushed.path("item").path("mine").asBoolean());
            assertEquals(1, list(binh).size());
            assertEquals(0, list(cuong).size(), "lớp B không thấy thảo luận của lớp A");
        }
    }

    @Test
    void traLoiLongMotCap() throws Exception {
        try (var an = TestClient.connect(server.port());
                var gv = TestClient.connect(server.port())) {
            an.login("an", "1");
            gv.login("gvA", "1");
            long root =
                    JsonPayload.tree(post(an, "Hỏi", 0)).path("item").path("id").asLong();
            gv.readUntil(MessageType.DISCUSS_NEW); // bản phát câu hỏi tới giáo viên cùng lớp
            JsonNode reply = JsonPayload.tree(post(gv, "Cô trả lời", root));
            assertEquals(root, reply.path("item").path("parentId").asLong());
            Thread.sleep(DiscussionModule.MIN_GAP_MS + 50);
            Frame nested = post(
                    an, "Trả lời câu trả lời", reply.path("item").path("id").asLong());
            assertEquals(MessageType.ERROR, nested.type());
        }
    }

    @Test
    void giaoVienAn_hocVienKhongThay() throws Exception {
        try (var an = TestClient.connect(server.port());
                var gv = TestClient.connect(server.port())) {
            an.login("an", "1");
            gv.login("gvA", "1");
            long id = JsonPayload.tree(post(an, "lời giải đây: ...", 0))
                    .path("item")
                    .path("id")
                    .asLong();
            gv.readUntil(MessageType.DISCUSS_NEW);
            gv.send(
                    MessageType.DISCUSS_HIDE,
                    new PayloadWriter().i64(id).bool(true).toBytes());
            assertEquals(
                    MessageType.ADMIN_OK,
                    gv.readUntil(MessageType.ADMIN_OK, MessageType.ERROR).type());
            assertEquals(0, list(an).size());
            JsonNode staff = list(gv);
            assertEquals(1, staff.size());
            assertTrue(staff.get(0).path("hidden").asBoolean());
        }
    }

    @Test
    void hocVienKhongAnDuoc_quaDai_vaGuiDon_biTuChoi() throws Exception {
        try (var an = TestClient.connect(server.port())) {
            an.login("an", "1");
            an.send(
                    MessageType.DISCUSS_HIDE,
                    new PayloadWriter().i64(1).bool(true).toBytes());
            assertEquals(
                    ErrorCode.FORBIDDEN,
                    ErrorMessage.from(an.readUntil(MessageType.ERROR)).code());
            Frame dai = post(an, "x".repeat(DiscussionModule.MAX_LEN + 1), 0);
            assertEquals(MessageType.ERROR, dai.type());
            assertEquals(MessageType.DISCUSS_NEW, post(an, "một", 0).type());
            Frame don = post(an, "hai", 0);
            assertEquals(ErrorCode.RATE_LIMITED, ErrorMessage.from(don).code());
        }
    }

    @Test
    void thich_demSoNguoi_vaBoThich() throws Exception {
        try (var an = TestClient.connect(server.port());
                var binh = TestClient.connect(server.port())) {
            an.login("an", "1");
            binh.login("binh", "1");
            long id = JsonPayload.tree(post(an, "Câu hỏi hay", 0))
                    .path("item")
                    .path("id")
                    .asLong();
            binh.readUntil(MessageType.DISCUSS_NEW);
            binh.send(
                    MessageType.DISCUSS_LIKE,
                    new PayloadWriter().i64(id).bool(true).toBytes());
            JsonNode liked =
                    JsonPayload.tree(binh.readUntil(MessageType.DISCUSS_NEW)).path("item");
            assertEquals(1, liked.path("likes").asInt());
            assertTrue(liked.path("liked").asBoolean());
            // An nhận bản phát: 1 thích nhưng không phải An thích
            JsonNode seen =
                    JsonPayload.tree(an.readUntil(MessageType.DISCUSS_NEW)).path("item");
            assertEquals(1, seen.path("likes").asInt());
            assertFalse(seen.path("liked").asBoolean());
            binh.send(
                    MessageType.DISCUSS_LIKE,
                    new PayloadWriter().i64(id).bool(true).toBytes());
            assertEquals(
                    1,
                    JsonPayload.tree(binh.readUntil(MessageType.DISCUSS_NEW))
                            .path("item")
                            .path("likes")
                            .asInt(),
                    "thích hai lần vẫn tính một");
            binh.send(
                    MessageType.DISCUSS_LIKE,
                    new PayloadWriter().i64(id).bool(false).toBytes());
            assertEquals(
                    0,
                    JsonPayload.tree(binh.readUntil(MessageType.DISCUSS_NEW))
                            .path("item")
                            .path("likes")
                            .asInt());
        }
    }
}
