package labcast.server.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.TestClient;
import labcast.server.classroom.ClassRepository;
import labcast.server.classroom.ClassroomWorkspaceModule;
import labcast.server.presence.AppFocusModule;
import labcast.server.store.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminRoleTest {
    private Database db;
    private LabCastServer server;
    private long lopCuaGiaoVien;

    @BeforeEach
    void batServer() throws Exception {
        db = Database.open(":memory:");
        var cfg = ServerConfig.forTest()
                .withLop("Lớp D22")
                .withTaoGiaoVien("gv:matkhau:Cô Giáo")
                .withTaoAdmin("ad:quantri:Quản trị viên");
        server = new LabCastServer(
                cfg, db, List.of(new AccountModule(), new ClassroomWorkspaceModule(), new AppFocusModule()));
        server.start();
        lopCuaGiaoVien = new ClassRepository(db)
                .classOf(new UserRepository(db).byUsername("gv").orElseThrow().id());
    }

    @AfterEach
    void tatServer() throws Exception {
        server.close();
        db.close();
    }

    @Test
    void taoAdminTuCauHinh_dangNhapDuoc_vaiTroAdmin_khongTuTaoLop() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            Frame ok = c.login("ad", "quantri");
            assertEquals(MessageType.AUTH_OK, ok.type());
            PayloadReader r = ok.reader();
            r.str();
            assertEquals("ADMIN", r.str());
            r.i64();
            assertEquals("Quản trị viên", r.str());
            assertEquals(0, r.i64(), "admin không gắn với lớp nào lúc đăng nhập");
        }
    }

    @Test
    void cauHinhTaoAdmin_docTuDongLenh() {
        var cfg = ServerConfig.parse(new String[] {"--tao-admin", "ad:mk:Admin"}, Map.of());
        assertEquals("ad:mk:Admin", cfg.taoAdmin());
    }

    @Test
    void taoAdminSaiDang_baoLoi() throws Exception {
        try (Database d = Database.open(":memory:")) {
            var cfg = ServerConfig.forTest().withTaoAdmin("chi-co-ten");
            // Module được cài ngay trong constructor: cấu hình sai thì dừng trước khi mở cổng
            assertThrows(IllegalArgumentException.class, () -> new LabCastServer(cfg, d, List.of(new AccountModule())));
        }
    }

    @Test
    void adminThayMoiLop_vaChonDuocLopCuaGiaoVien() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            assertEquals(MessageType.AUTH_OK, c.login("ad", "quantri").type());
            c.send(MessageType.CLASS_LIST_REQ, new byte[0]);
            JsonNode list = JsonPayload.tree(c.readUntil(MessageType.CLASS_LIST_DATA));
            assertEquals(1, list.path("classes").size());
            assertEquals(lopCuaGiaoVien, list.path("classes").get(0).path("id").asLong());

            c.send(
                    MessageType.CLASS_SELECT_REQ,
                    new PayloadWriter().i64(lopCuaGiaoVien).toBytes());
            assertEquals(
                    MessageType.CLASS_SELECT_RESULT,
                    c.readUntil(MessageType.CLASS_SELECT_RESULT, MessageType.ERROR)
                            .type());
        }
    }

    @Test
    void adminNhapDanhSach_phaiChonLopTruoc_roiGhiDanhVaoLopDo() throws Exception {
        String csv = "username,full_name,password\nsv01,Học viên Một,mk1\n";
        try (var c = TestClient.connect(server.port())) {
            assertEquals(MessageType.AUTH_OK, c.login("ad", "quantri").type());

            c.send(MessageType.ADMIN_ROSTER_IMPORT, new PayloadWriter().str(csv).toBytes());
            Frame chuaChon = c.readUntil(MessageType.ADMIN_OK, MessageType.ERROR);
            assertEquals(MessageType.ERROR, chuaChon.type(), "admin chưa chọn lớp: không tự tạo lớp mới");

            c.send(
                    MessageType.CLASS_SELECT_REQ,
                    new PayloadWriter().i64(lopCuaGiaoVien).toBytes());
            c.readUntil(MessageType.CLASS_SELECT_RESULT);
            c.send(MessageType.ADMIN_ROSTER_IMPORT, new PayloadWriter().str(csv).toBytes());
            Frame ok = c.readUntil(MessageType.ADMIN_OK, MessageType.ERROR);
            assertEquals(MessageType.ADMIN_OK, ok.type());
            var r = ok.reader();
            r.str();
            assertEquals("sv01", r.str(), "tài khoản vừa tạo, để app xuất file mật khẩu tạm");
            assertEquals("", r.str());
            // nhập lại: sv01 đã có, chỉ ghi danh, không tính là tạo mới
            c.send(MessageType.ADMIN_ROSTER_IMPORT, new PayloadWriter().str(csv).toBytes());
            var again = c.readUntil(MessageType.ADMIN_OK).reader();
            again.str();
            assertEquals("", again.str());
            assertEquals("sv01", again.str());
        }
        long sv = new UserRepository(db).byUsername("sv01").orElseThrow().id();
        assertEquals(lopCuaGiaoVien, new ClassRepository(db).classOf(sv));
    }

    @Test
    void adminGuiViecCuaHocVien_biTuChoi() throws Exception {
        try (var c = TestClient.connect(server.port())) {
            assertEquals(MessageType.AUTH_OK, c.login("ad", "quantri").type());
            c.send(
                    MessageType.APP_FOCUS,
                    new PayloadWriter().bool(false).i64(0).str("").toBytes());
            Frame err = c.readUntil(MessageType.ERROR);
            assertEquals(ErrorCode.FORBIDDEN, ErrorMessage.from(err).code());
        }
    }

    @Test
    void vaiTro_staff() {
        assertTrue(Role.ADMIN.isStaff());
        assertTrue(Role.TEACHER.isStaff());
        assertEquals(false, Role.STUDENT.isStaff());
        assertEquals(java.util.Set.of(Role.TEACHER, Role.ADMIN), Role.STAFF);
    }
}
