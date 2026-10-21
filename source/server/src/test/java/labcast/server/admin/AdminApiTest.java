package labcast.server.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.TestClient;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.classroom.ClassDirectory;
import labcast.server.classroom.ClassRepository;
import labcast.server.ops.BuildInfo;
import labcast.server.ops.StatusEndpoint;
import labcast.server.store.Database;
import org.junit.jupiter.api.Test;

class AdminApiTest {
    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    static LabCastServer server(Database db) throws Exception {
        LabCastServer s = new LabCastServer(ServerConfig.forTest(), db, List.of(new AccountModule()));
        s.start();
        return s;
    }

    static JsonNode ok(AdminApi.Reply r) throws Exception {
        assertEquals(200, r.status(), r.json());
        return JsonPayload.MAPPER.readTree(r.json());
    }

    static AdminApi.Reply post(AdminApi api, String path, String json) {
        return api.handle("POST", path, Map.of(), json);
    }

    static AdminApi.Reply get(AdminApi api, String path, Map<String, String> q) {
        return api.handle("GET", path, q, "");
    }

    @Test
    void taoLopGiaoVienHocVien_locTheoLop_vaTongQuan() throws Exception {
        try (Database db = Database.open(":memory:");
                var server = server(db)) {
            AdminApi api = new AdminApi(server.context(), System::currentTimeMillis, ZONE);
            long gv = ok(post(
                            api,
                            "users",
                            "{\"username\":\"gv1\",\"ten\":\"Cô Lan\",\"vaiTro\":\"TEACHER\","
                                    + "\"matKhau\":\"123456\"}"))
                    .path("id")
                    .asLong();
            long dsa1 = ok(post(api, "classes", "{\"ten\":\"DSA1\",\"giaoVien\":[" + gv + "]}"))
                    .path("id")
                    .asLong();
            long dsa2 = ok(post(api, "classes", "{\"ten\":\"DSA2\",\"giaoVien\":[]}"))
                    .path("id")
                    .asLong();
            ok(post(
                    api,
                    "users",
                    "{\"username\":\"B22DCCN001\",\"ten\":\"Nguyễn Văn An\",\"vaiTro\":\"STUDENT\","
                            + "\"matKhau\":\"matkhau\",\"lop\":[" + dsa1 + "]}"));
            ok(post(
                    api,
                    "users",
                    "{\"username\":\"B22DCCN002\",\"ten\":\"Trần Bình\",\"vaiTro\":\"STUDENT\","
                            + "\"matKhau\":\"matkhau\",\"lop\":[]}"));

            JsonNode lop1 = ok(get(api, "users", Map.of("vaiTro", "STUDENT", "lop", String.valueOf(dsa1))));
            assertEquals(1, lop1.size());
            assertEquals("B22DCCN001", lop1.get(0).path("username").asText());
            assertEquals("DSA1", lop1.get(0).path("lop").get(0).path("ten").asText());
            JsonNode chuaXep = ok(get(api, "users", Map.of("vaiTro", "STUDENT", "lop", "0")));
            assertEquals("B22DCCN002", chuaXep.get(0).path("username").asText());

            JsonNode o = ok(get(api, "overview", Map.of()));
            assertEquals(2, o.path("tong").asInt());
            assertEquals(1, o.path("chuaXepLop").asInt());
            assertEquals("DSA1", o.path("lop").get(0).path("ten").asText());
            assertEquals(1, o.path("lop").get(0).path("siSo").asInt());
            assertEquals("Cô Lan", o.path("lop").get(0).path("giaoVien").get(0).asText());
            assertEquals(0, o.path("lop").get(1).path("giaoVien").size());

            ClassDirectory dir = server.context().require(ClassDirectory.class);
            assertEquals(List.of(gv), dir.teachersOf(dsa1));
            assertEquals(
                    List.of(dsa1),
                    dir.classesOf(gv).stream().map(ClassDirectory.ClassInfo::id).toList());
            assertEquals(1, dir.studentsOf(dsa1).size());
            assertTrue(dir.studentsOf(dsa2).isEmpty());
            assertFalse(o.toString().contains("pwd"), "không lộ mật khẩu băm");
        }
    }

    @Test
    void duLieuSai_traLoiDeHieu() throws Exception {
        try (Database db = Database.open(":memory:");
                var server = server(db)) {
            AdminApi api = new AdminApi(server.context(), System::currentTimeMillis, ZONE);
            assertEquals(
                    400,
                    post(
                                    api,
                                    "users",
                                    "{\"username\":\"có dấu\",\"ten\":\"A\",\"vaiTro\":\"STUDENT\","
                                            + "\"matKhau\":\"123456\"}")
                            .status());
            assertEquals(
                    400,
                    post(
                                    api,
                                    "users",
                                    "{\"username\":\"a\",\"ten\":\"A\",\"vaiTro\":\"STUDENT\","
                                            + "\"matKhau\":\"123\"}")
                            .status());
            assertEquals(
                    400,
                    post(api, "users", "{\"username\":\"a\",\"ten\":\"A\",\"vaiTro\":\"STUDENT\"}")
                            .status());
            assertEquals(400, post(api, "users", "không phải json").status());
            assertEquals(405, get(api, "users/delete", Map.of()).status());
            assertEquals(404, get(api, "khong-co", Map.of()).status());

            // chưa có giáo viên thì chưa tạo được lớp
            assertEquals(400, post(api, "classes", "{\"ten\":\"DSA1\"}").status());
            ok(post(
                    api,
                    "users",
                    "{\"username\":\"gv\",\"ten\":\"GV\",\"vaiTro\":\"TEACHER\",\"matKhau\":\"123456\"}"));
            ok(post(api, "classes", "{\"ten\":\"DSA1\"}"));
            AdminApi.Reply trung = post(api, "classes", "{\"ten\":\"dsa1\"}");
            assertEquals(409, trung.status());
            assertTrue(trung.json().contains("Đã có lớp"));

            AdminApi.Reply trungTen = post(
                    api, "users", "{\"username\":\"gv\",\"ten\":\"X\",\"vaiTro\":\"STUDENT\",\"matKhau\":\"123456\"}");
            assertEquals(409, trungTen.status());
        }
    }

    @Test
    void doiLop_dangXuatHocVienDangOnline() throws Exception {
        try (Database db = Database.open(":memory:");
                var server = server(db)) {
            AdminApi api = new AdminApi(server.context(), System::currentTimeMillis, ZONE);
            new UserRepository(db).create("gv", "GV", Role.TEACHER, "123456");
            long dsa1 =
                    ok(post(api, "classes", "{\"ten\":\"DSA1\"}")).path("id").asLong();
            long dsa2 =
                    ok(post(api, "classes", "{\"ten\":\"DSA2\"}")).path("id").asLong();
            long sv = ok(post(
                            api,
                            "users",
                            "{\"username\":\"sv\",\"ten\":\"SV\",\"vaiTro\":\"STUDENT\","
                                    + "\"matKhau\":\"123456\",\"lop\":[" + dsa1 + "]}"))
                    .path("id")
                    .asLong();

            try (var c = TestClient.connect(server.port())) {
                var auth = c.login("sv", "123456").reader();
                String token = auth.str();
                auth.str();
                auth.i64();
                auth.str();
                assertEquals(dsa1, auth.i64(), "đăng nhập vào đúng lớp");
                assertEquals(
                        1, ok(get(api, "overview", Map.of())).path("online").asInt());

                // sửa họ tên thôi thì không bị đăng xuất
                JsonNode keep = ok(post(
                        api,
                        "users",
                        "{\"id\":" + sv + ",\"username\":\"sv\",\"ten\":\"SV mới\"," + "\"lop\":[" + dsa1 + "]}"));
                assertFalse(keep.path("dangXuat").asBoolean());

                JsonNode moved = ok(post(
                        api,
                        "users",
                        "{\"id\":" + sv + ",\"username\":\"sv\",\"ten\":\"SV mới\"," + "\"lop\":[" + dsa2 + "]}"));
                assertTrue(moved.path("dangXuat").asBoolean());
                assertEquals(-1, c.readRawByte(), "kết nối bị đóng");

                try (var c2 = TestClient.connect(server.port())) {
                    c2.send(
                            MessageType.RESUME_REQ,
                            new PayloadWriter().str(token).str("").i64(0).toBytes());
                    assertFalse(c2.readUntil(MessageType.RESUME_STATE).reader().bool(), "token cũ hết hiệu lực");
                }
            }
            assertEquals(
                    List.of(dsa2),
                    server.context().require(ClassDirectory.class).classesOf(sv).stream()
                            .map(ClassDirectory.ClassInfo::id)
                            .toList());
        }
    }

    @Test
    void xoa_nguoiDaCoBaiLam_khongXoaDuoc_lopXoaThiHocVienThanhChuaXep() throws Exception {
        try (Database db = Database.open(":memory:");
                var server = server(db)) {
            AdminApi api = new AdminApi(server.context(), System::currentTimeMillis, ZONE);
            var users = new UserRepository(db);
            users.create("gv", "GV", Role.TEACHER, "123456");
            long lop = ok(post(api, "classes", "{\"ten\":\"SQL1\"}")).path("id").asLong();
            long a = ok(post(
                            api,
                            "users",
                            "{\"username\":\"a\",\"ten\":\"A\",\"vaiTro\":\"STUDENT\","
                                    + "\"matKhau\":\"123456\",\"lop\":[" + lop + "]}"))
                    .path("id")
                    .asLong();
            long b = ok(post(
                            api,
                            "users",
                            "{\"username\":\"b\",\"ten\":\"B\",\"vaiTro\":\"STUDENT\","
                                    + "\"matKhau\":\"123456\",\"lop\":[" + lop + "]}"))
                    .path("id")
                    .asLong();
            db.tx(c -> c.createStatement()
                    .executeUpdate("INSERT INTO lessons VALUES ('sql.x', 'sql', 'c', 'X', 'bai-tap', 'p', 'h')"));
            db.tx(c -> c.createStatement()
                    .executeUpdate("INSERT INTO submissions (user_id, lesson_id, code_seq, kind, verdict, created_at)"
                            + " VALUES (" + a + ", 'sql.x', 1, 'SUBMIT', 'OK', 0)"));

            AdminApi.Reply r = post(api, "users/delete", "{\"id\":" + a + "}");
            assertEquals(409, r.status());
            assertTrue(users.byId(a).isPresent(), "không xoá gì khi lỗi");
            ok(post(api, "users/delete", "{\"id\":" + b + "}"));
            assertTrue(users.byId(b).isEmpty());

            ok(post(api, "classes/delete", "{\"id\":" + lop + "}"));
            JsonNode o = ok(get(api, "overview", Map.of()));
            assertEquals(0, o.path("lop").size());
            assertEquals(1, o.path("chuaXepLop").asInt());
            assertEquals(0, o.path("luotNopHomNay").asInt(), "bài nộp lúc 1970 không tính vào hôm nay");
        }
    }

    @Test
    void nhapCsv_vaoLop_boQuaDongTrung() throws Exception {
        try (Database db = Database.open(":memory:");
                var server = server(db)) {
            AdminApi api = new AdminApi(server.context(), System::currentTimeMillis, ZONE);
            new UserRepository(db).create("gv", "GV", Role.TEACHER, "123456");
            long lop = ok(post(api, "classes", "{\"ten\":\"DSA1\"}")).path("id").asLong();
            String csv = "username,ho ten,mat khau\\nsv1,Một,111111\\nsv2,Hai,222222\\nsv1,Trùng,333333";
            JsonNode r = ok(post(api, "import", "{\"lop\":" + lop + ",\"csv\":\"" + csv + "\"}"));
            assertEquals(2, r.path("taoMoi").asInt());
            assertEquals(1, r.path("boQua").size());
            assertEquals(
                    2,
                    server.context()
                            .require(ClassRepository.class)
                            .studentsOf(lop)
                            .size());
        }
    }

    @Test
    void caHomNay_vaBaiDangGiao_hienTrenTongQuan() throws Exception {
        // Thứ 3, 22/10/2024 10:00 giờ Việt Nam
        long now = Instant.parse("2024-10-22T03:00:00Z").toEpochMilli();
        try (Database db = Database.open(":memory:");
                var server = server(db)) {
            AdminApi api = new AdminApi(server.context(), () -> now, ZONE);
            long gv = new UserRepository(db).create("gv", "GV", Role.TEACHER, "123456");
            long lop = ok(post(api, "classes", "{\"ten\":\"DSA2\"}")).path("id").asLong();
            db.tx(c -> c.createStatement()
                    .executeUpdate("INSERT INTO class_schedule (class_id, weekday, start_min, end_min) VALUES" + " ("
                            + lop + ", 2, 810, 960), (" + lop + ", 2, 420, 570), (" + lop + ", 3, 0, 60)"));
            db.tx(c -> c.createStatement()
                    .executeUpdate("INSERT INTO assignments (class_id, lesson_id, loai, opens_at, closes_at, minutes,"
                            + " active, created_by, created_at) VALUES"
                            + " (" + lop + ", 'a', 'kiem-tra', " + (now - 1) + ", " + (now + 1) + ", 60, 1, " + gv
                            + ", 0)," + " (" + lop + ", 'b', 'thuc-hanh', " + (now - 1) + ", " + (now + 1)
                            + ", NULL, 0, " + gv + ", 0)," + " (" + lop + ", 'c', 'thuc-hanh', " + (now + 1) + ", "
                            + (now + 2) + ", NULL, 1, " + gv + ", 0)"));
            JsonNode l = ok(get(api, "overview", Map.of())).path("lop").get(0);
            assertEquals("07:00–09:30, 13:30–16:00", l.path("ca").asText());
            assertEquals(1, l.path("kiemTra").asInt(), "chỉ bài đang bật và trong giờ");
            assertEquals(0, l.path("thucHanh").asInt());
        }
    }

    // ---------------------------------------------------------------- qua HTTP thật

    static String raw(int port, String request) throws Exception {
        try (Socket s = new Socket("127.0.0.1", port)) {
            OutputStream out = s.getOutputStream();
            out.write(request.getBytes(StandardCharsets.UTF_8));
            out.flush();
            InputStream in = s.getInputStream();
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            in.transferTo(buf);
            return buf.toString(StandardCharsets.UTF_8);
        }
    }

    @Test
    void http_chanHostLa_vaPostThieuHeader() throws Exception {
        try (Database db = Database.open(":memory:");
                var server = server(db);
                StatusEndpoint status = StatusEndpoint.start(0, new BuildInfo("0", "x", Instant.EPOCH))) {
            status.enableAdmin(new AdminApi(server.context()));
            int port = status.port();
            String page = raw(port, "GET /admin HTTP/1.1\r\nHost: localhost:" + port + "\r\nConnection: close\r\n\r\n");
            assertTrue(page.startsWith("HTTP/1.1 200"), page);
            assertTrue(page.contains("Quản trị"));

            String rebinding = raw(
                    port,
                    "GET /api/admin/overview HTTP/1.1\r\nHost: evil.example:" + port + "\r\nConnection: close\r\n\r\n");
            assertTrue(rebinding.startsWith("HTTP/1.1 403"), rebinding);

            String body = "{\"username\":\"x\",\"ten\":\"X\",\"vaiTro\":\"TEACHER\",\"matKhau\":\"123456\"}";
            byte[] b = body.getBytes(StandardCharsets.UTF_8);
            String noHeader = raw(
                    port,
                    "POST /api/admin/users HTTP/1.1\r\nHost: 127.0.0.1:" + port
                            + "\r\nContent-Type: application/json\r\nContent-Length: " + b.length
                            + "\r\nConnection: close\r\n\r\n" + body);
            assertTrue(noHeader.startsWith("HTTP/1.1 403"), noHeader);
            assertTrue(new UserRepository(db).byUsername("x").isEmpty());

            String withHeader = raw(
                    port,
                    "POST /api/admin/users HTTP/1.1\r\nHost: 127.0.0.1:" + port
                            + "\r\nContent-Type: application/json\r\nX-LabCast-Admin: 1\r\nContent-Length: " + b.length
                            + "\r\nConnection: close\r\n\r\n" + body);
            assertTrue(withHeader.startsWith("HTTP/1.1 200"), withHeader);
            assertTrue(new UserRepository(db).byUsername("x").isPresent());
        }
    }
}
