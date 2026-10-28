package labcast.client.ui;

import static labcast.client.ui.UiSmokeTest.chup;
import static labcast.client.ui.UiSmokeTest.fx;
import static labcast.client.ui.UiSmokeTest.until;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import labcast.client.net.DiscoveryClient;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.discovery.DiscoveryResponder;
import labcast.server.store.Database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Chạy TAY như {@link UiSmokeTest}: màn đăng nhập ba thẻ vai trò, sáng và tối, rồi đăng nhập nhầm thẻ. Ảnh vào
 * {@code client/target/ui-smoke/dang-nhap-*.png}.
 */
@EnabledIfEnvironmentVariable(named = "LABCAST_UI_SMOKE", matches = "1")
class DangNhapSmokeTest {

    @Test
    void baVaiTro_sangToi_vaDangNhapNhamThe() throws Exception {
        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(
                        ServerConfig.forTest().withLop("Lớp xem thử"), db, List.of(new AccountModule()))) {
            server.start();
            new UserRepository(db).create("gvtam", "Trần Minh Tâm", Role.TEACHER, "1234");
            try {
                Platform.startup(() -> {});
            } catch (IllegalStateException alreadyStarted) {
                // đã chạy test giao diện khác trong cùng JVM
            }
            Platform.setImplicitExit(false);
            // Trả lời tìm lớp ở cổng thật 7002 (cổng bận vì máy đang chạy server thật thì thấy lớp thật)
            DiscoveryResponder dsa = null;
            DiscoveryResponder sql = null;
            try {
                // một máy chủ nhiều lớp (có tên rất dài): danh sách phải cuộn được, tên dài cắt "…"
                var lop = List.of(
                        new labcast.server.classroom.ClassRepository.Listed(1, "DSA Nâng cao - K23", "Nguyễn Thị Lan"),
                        new labcast.server.classroom.ClassRepository.Listed(
                                2, "Cấu trúc dữ liệu cơ bản - D22CQCN01", "Trần Minh Tâm"),
                        new labcast.server.classroom.ClassRepository.Listed(
                                3, "Giải thuật nâng cao - D21CQCN02", "Lê Hoàng Phúc"),
                        new labcast.server.classroom.ClassRepository.Listed(
                                4,
                                "Lớp ôn thi Olympic Tin học sinh viên toàn quốc - đội tuyển Học viện khoá 2022 và 2023",
                                "Phạm Thu Hà"),
                        new labcast.server.classroom.ClassRepository.Listed(
                                5, "Đồ thị và ứng dụng - D22CQCN03", "Nguyễn Thị Lan"),
                        new labcast.server.classroom.ClassRepository.Listed(
                                6, "Lập trình thi đấu - CLB", "Đỗ Quang Huy"));
                dsa = new DiscoveryResponder(
                        DiscoveryClient.DEFAULT_PORT,
                        () -> labcast.server.discovery.DiscoveryReply.build(
                                "DSA Nâng cao - K23", "Nguyễn Thị Lan", "192.168.1.10", 7100, lop));
                dsa.start();
                sql = responder("SQL cơ bản - K24", "Trần Minh Tâm", 7101);
            } catch (Exception busy) {
                // dùng trả lời của server thật
            }
            AtomicReference<LoginController.LoggedIn> vao = new AtomicReference<>();
            for (boolean dark : new boolean[] {true, false}) {
                fx(() -> {
                    Theme.setDark(dark);
                    return null;
                });
                for (Role r : new Role[] {Role.STUDENT, Role.TEACHER, Role.ADMIN}) {
                    Scene s = fx(() -> new Scene(new LoginView(vao::set, r), 1440, 900));
                    Thread.sleep(2_600); // tìm lớp
                    chup(s, "dang-nhap-" + r.name().toLowerCase() + (dark ? "-toi" : "-sang"));
                }
            }
            fx(() -> {
                Theme.setDark(true);
                return null;
            });
            // cửa sổ mặc định 1200×760: bỏ cột lời chào
            Scene hep = fx(() -> new Scene(new LoginView(vao::set, Role.STUDENT), 1200, 760));
            Thread.sleep(2_600);
            chup(hep, "dang-nhap-cua-so-hep");
            // cuộn tới lớp tên rất dài: tên cắt "…", mũi tên vẫn trong thẻ
            fx(() -> {
                @SuppressWarnings("unchecked")
                var ds = (javafx.scene.control.ListView<DiscoveryClient.ServerInfo>) field(hep.getRoot(), "servers");
                for (int i = 0; i < ds.getItems().size(); i++) {
                    if (ds.getItems().get(i).className().contains("Olympic")) {
                        ds.getSelectionModel().select(i);
                        ds.scrollTo(i);
                    }
                }
                return null;
            });
            chup(hep, "dang-nhap-nhieu-lop-cuon");

            // giáo viên đăng nhập ở thẻ Sinh viên: không vào, chuyển sang thẻ Giáo viên
            Scene s = fx(() -> new Scene(new LoginView(vao::set, Role.STUDENT), 1440, 900));
            LoginView v = (LoginView) s.getRoot();
            fx(() -> {
                set(v, "manual", "127.0.0.1:" + server.port());
                set(v, "user", "gvtam");
                set(v, "pass", "1234");
                ((Button) field(v, "loginBtn")).fire();
                return null;
            });
            until(
                    () -> ((javafx.scene.control.ToggleGroup) field(v, "roles"))
                                    .getSelectedToggle()
                                    .getUserData()
                            == Role.TEACHER,
                    "chuyển thẻ Giáo viên");
            Label st = (Label) fx(() -> field(v, "status"));
            assertTrue(fx(st::getText).contains("là giáo viên"), fx(st::getText));
            assertEquals(null, vao.get());
            chup(s, "dang-nhap-nham-the");
            fx(() -> {
                ((Button) field(v, "loginBtn")).fire();
                return null;
            });
            until(() -> vao.get() != null, "đăng nhập ở đúng thẻ");
            assertEquals(Role.TEACHER, vao.get().role());
            vao.get().client().close();

            // quản trị: không có cột chọn lớp, vẫn đăng nhập được vào máy chủ (lớp chọn sau ở trang chủ)
            new UserRepository(db).create("adthu", "Quản trị thử", Role.ADMIN, "1234");
            vao.set(null);
            Scene ad = fx(() -> new Scene(new LoginView(vao::set, Role.ADMIN), 1440, 900));
            LoginView av = (LoginView) ad.getRoot();
            Thread.sleep(2_600);
            chup(ad, "dang-nhap-quan-tri-khong-chon-lop");
            fx(() -> {
                set(av, "manual", "127.0.0.1:" + server.port());
                set(av, "user", "adthu");
                set(av, "pass", "1234");
                ((Button) field(av, "loginBtn")).fire();
                return null;
            });
            until(() -> vao.get() != null, "quản trị đăng nhập");
            assertEquals(Role.ADMIN, vao.get().role());
            vao.get().client().close();
            if (dsa != null) dsa.close();
            if (sql != null) sql.close();
        }
    }

    private static DiscoveryResponder responder(String lop, String gv, int port) throws Exception {
        var d = new DiscoveryResponder(DiscoveryClient.DEFAULT_PORT, () -> new PayloadWriter()
                .str(lop)
                .str(gv)
                .str("192.168.1.10")
                .u16(port)
                .bool(false)
                .str("")
                .toBytes());
        d.start();
        return d;
    }

    private static Object field(Object o, String name) throws Exception {
        var f = LoginView.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    private static void set(Object o, String name, String text) throws Exception {
        ((TextField) field(o, name)).setText(text);
    }
}
