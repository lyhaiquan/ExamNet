package labcast.server.classroom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
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
import labcast.server.store.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClassAdminTest {
    private Database db;
    private LabCastServer server;
    private long coLan;
    private long thayNam;

    @BeforeEach
    void batServer() throws Exception {
        db = Database.open(":memory:");
        var cfg = ServerConfig.forTest()
                .withLop("Lớp D22")
                .withTaoGiaoVien("gv:matkhau:Cô Lan")
                .withTaoAdmin("ad:quantri:Quản trị viên");
        server = new LabCastServer(cfg, db, List.of(new AccountModule(), new ClassAdminModule()));
        server.start();
        var users = new UserRepository(db);
        coLan = users.byUsername("gv").orElseThrow().id();
        thayNam = users.create("gv2", "Thầy Nam", Role.TEACHER, "matkhau2");
        users.create("sv", "Học viên", Role.STUDENT, "matkhau3");
    }

    @AfterEach
    void tatServer() throws Exception {
        server.close();
        db.close();
    }

    private static JsonNode list(TestClient c) throws Exception {
        c.send(MessageType.ADMIN_CLASS_LIST, new byte[0]);
        return JsonPayload.tree(c.readUntil(MessageType.ADMIN_CLASS_DATA));
    }

    private static Frame save(TestClient c, long id, String name, long teacherId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        body.put("name", name);
        body.put("teacherId", teacherId);
        c.send(JsonPayload.frame(MessageType.ADMIN_CLASS_SAVE, body));
        return c.readUntil(MessageType.ADMIN_OK, MessageType.ERROR);
    }

    private static JsonNode lop(JsonNode data, String name) {
        for (JsonNode c : data.path("classes")) {
            if (c.path("name").asText().equals(name)) return c;
        }
        return null;
    }

    @Test
    void quanTri_taoLop_giaoChoGiaoVienKhac_doiTen_doiGiaoVien() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.login("ad", "quantri");
            JsonNode truoc = list(c);
            assertEquals(1, truoc.path("classes").size(), "chỉ có lớp từ cấu hình");
            assertEquals(2, truoc.path("teachers").size());

            Frame ok = save(c, 0, "Lớp D23 – Đồ thị", thayNam);
            assertEquals(MessageType.ADMIN_OK, ok.type());
            JsonNode moi = lop(list(c), "Lớp D23 – Đồ thị");
            assertEquals(thayNam, moi.path("teacherId").asLong(), "lớp thuộc giáo viên được chọn, không phải quản trị");
            assertEquals("Thầy Nam", moi.path("teacher").asText());
            assertEquals(0, moi.path("students").asInt());

            long id = moi.path("id").asLong();
            assertEquals(
                    MessageType.ADMIN_OK,
                    save(c, id, "Lớp D23 – Đồ thị nâng cao", coLan).type());
            JsonNode sua = lop(list(c), "Lớp D23 – Đồ thị nâng cao");
            assertEquals(id, sua.path("id").asLong());
            assertEquals(coLan, sua.path("teacherId").asLong());
        }
    }

    @Test
    void tenTrung_giaoVienKhongHopLe_biTuChoi() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.login("ad", "quantri");
            Frame trung = save(c, 0, "lớp d22", coLan);
            assertEquals(MessageType.ERROR, trung.type());
            assertTrue(ErrorMessage.from(trung).message().contains("Đã có lớp"));
            long hocVien = new UserRepository(db).byUsername("sv").orElseThrow().id();
            Frame khongPhaiGv = save(c, 0, "Lớp mới", hocVien);
            assertEquals(MessageType.ERROR, khongPhaiGv.type());
            new UserRepository(db).setDisabled(thayNam, true);
            Frame biKhoa = save(c, 0, "Lớp mới", thayNam);
            assertTrue(ErrorMessage.from(biKhoa).message().contains("đang bị khoá"));
            assertEquals(MessageType.ERROR, save(c, 0, "  ", coLan).type());
        }
    }

    @Test
    void xoaLop_chiKhiChuaCoHocVien() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.login("ad", "quantri");
            save(c, 0, "Lớp trống", thayNam);
            long trong = lop(list(c), "Lớp trống").path("id").asLong();
            long coHocVien = lop(list(c), "Lớp D22").path("id").asLong();
            new ClassRepository(db)
                    .enroll(
                            coHocVien,
                            new UserRepository(db)
                                    .byUsername("sv")
                                    .orElseThrow()
                                    .id());

            c.send(
                    MessageType.ADMIN_CLASS_DELETE,
                    new PayloadWriter().i64(coHocVien).toBytes());
            Frame tuChoi = c.readUntil(MessageType.ADMIN_OK, MessageType.ERROR);
            assertEquals(MessageType.ERROR, tuChoi.type());
            assertTrue(ErrorMessage.from(tuChoi).message().contains("1 học viên"));

            c.send(
                    MessageType.ADMIN_CLASS_DELETE,
                    new PayloadWriter().i64(trong).toBytes());
            assertEquals(
                    MessageType.ADMIN_OK,
                    c.readUntil(MessageType.ADMIN_OK, MessageType.ERROR).type());
            JsonNode sau = list(c);
            assertEquals(null, lop(sau, "Lớp trống"));
            assertEquals(1, lop(sau, "Lớp D22").path("students").asInt());
        }
    }

    @Test
    void giaoVien_khongDungDuoc() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            c.login("gv", "matkhau");
            c.send(MessageType.ADMIN_CLASS_LIST, new byte[0]);
            Frame f = c.readUntil(MessageType.ADMIN_CLASS_DATA, MessageType.ERROR);
            assertEquals(MessageType.ERROR, f.type());
            assertEquals(ErrorCode.FORBIDDEN, ErrorMessage.from(f).code());
        }
    }
}
