package labcast.client.ui;

import static labcast.client.ui.UiSmokeTest.chup;
import static labcast.client.ui.UiSmokeTest.fx;
import static labcast.client.ui.UiSmokeTest.until;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import labcast.client.cast.CastClient;
import labcast.client.cast.CastFlowTest;
import labcast.client.journal.JournalClient;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.cast.CastModule;
import labcast.server.classroom.ClassRepository;
import labcast.server.classroom.ClassroomWorkspaceModule;
import labcast.server.lesson.LessonModule;
import labcast.server.practice.PracticeModule;
import labcast.server.store.Database;
import labcast.server.sync.SyncModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Chạy tay: giáo viên mở bài, bấm "Chiếu cho lớp", máy học viên tự mở màn chiếu và chạy theo. Server thật, app
 * thật, multicast thật trên card loopback. Ảnh vào {@code client/target/ui-smoke/chieu-*.png}.
 *
 * <pre>
 * set LABCAST_UI_SMOKE=1
 * mvnw -pl client test -Dtest=CastSmokeTest
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "LABCAST_UI_SMOKE", matches = "1")
class CastSmokeTest {
    private static final String BAI = "dsa.sap-xep.noi-bot";

    @Test
    void giaoVienChieu_hocVienThayVaChayTheo() throws Exception {
        NetworkInterface lo = NetworkInterface.getByName("lo");
        assumeTrue(lo != null, "không có card loopback tên lo");
        InetSocketAddress group;
        try (var s = new DatagramSocket(0)) {
            group = new InetSocketAddress("239.255.70.1", s.getLocalPort());
        }
        assumeTrue(CastFlowTest.multicastWorks(lo, group), "máy này không chuyển multicast trên loopback");

        Path root = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(root.resolve("content"))) root = root.getParent();
        var cfg = ServerConfig.forTest()
                .withContent(root.resolve("content"))
                .withKhongSandbox(true)
                .withMon("dsa")
                .withLop("DSA Nâng cao - K23");
        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(
                        cfg,
                        db,
                        List.of(
                                new AccountModule(),
                                new SyncModule(),
                                new LessonModule(),
                                new labcast.server.run.RunModule(), // chạy lời giải mẫu ra trace để chiếu
                                new labcast.server.home.HomeModule(),
                                new labcast.server.discuss.DiscussionModule(),
                                new ClassroomWorkspaceModule(),
                                new PracticeModule(),
                                new CastModule(lo, group)))) {
            server.start();
            var users = new UserRepository(db);
            long gvId = users.create("gv", "Trần Minh Tâm", Role.TEACHER, "1");
            long svId = users.create("sv", "Hoàng Nam", Role.STUDENT, "1");
            var classes = new ClassRepository(db);
            classes.enroll(classes.ensure("DSA Nâng cao - K23", gvId), svId);
            try {
                Platform.startup(() -> {});
            } catch (IllegalStateException alreadyStarted) {
                // đã chạy test giao diện khác trong cùng JVM
            }
            Platform.setImplicitExit(false);

            var gv = LoginController.login("127.0.0.1", server.port(), "gv", "1");
            var sv = LoginController.login("127.0.0.1", server.port(), "sv", "1");
            CastClient gvCast =
                    new CastClient(gv.client(), gv.castId(), System::currentTimeMillis, Platform::runLater, group);
            CastClient svCast =
                    new CastClient(sv.client(), sv.castId(), System::currentTimeMillis, Platform::runLater, group);
            gvCast.start();
            svCast.start();
            Scene teacher = fx(() -> new Scene(
                    new HomeScreen(gv, new JournalClient(gv.client()), () -> {}, gvCast.session(), () -> {}),
                    1280,
                    800));
            Scene student = fx(() -> new Scene(
                    new HomeScreen(sv, new JournalClient(sv.client()), () -> {}, svCast.session(), () -> {}),
                    1280,
                    800));
            Thread.sleep(1500); // danh sách bài
            chup(student, "chieu-1-hoc-vien-truoc");

            // giáo viên mở bài (như bấm "Xem bài" trong kho) rồi bấm Chiếu cho lớp
            fx(() -> {
                var open = HomeScreen.class.getDeclaredMethod("open", String.class);
                open.setAccessible(true);
                open.invoke(teacher.getRoot(), BAI);
                return null;
            });
            try {
                until(
                        () -> lookup(teacher, "#cast-toggle") != null
                                && !((Button) lookup(teacher, "#cast-toggle")).isDisabled(),
                        "nút Chiếu cho lớp");
            } catch (AssertionError e) {
                chup(teacher, "chieu-loi-giao-vien");
                throw e;
            }
            chup(teacher, "chieu-2-giao-vien-mo-bai");
            fx(() -> {
                ((Button) lookup(teacher, "#cast-toggle")).fire();
                return null;
            });

            // máy học viên tự mở màn chiếu
            until(
                    () -> student.getRoot().lookup(".cast-view") != null
                            || ((HomeScreen) student.getRoot()).getCenter() instanceof CastView,
                    "màn chiếu bên học viên");
            Thread.sleep(2500);
            chup(teacher, "chieu-3-giao-vien-dang-chieu");
            chup(student, "chieu-4-hoc-vien-dang-xem");
            // giáo viên bấm Phát: máy học viên chạy theo cùng bước
            fx(() -> {
                ((Button) lookup(teacher, "#play")).fire();
                return null;
            });
            Thread.sleep(3000);
            chup(teacher, "chieu-5-giao-vien-dang-phat");
            chup(student, "chieu-6-hoc-vien-chay-theo");

            gvCast.close();
            svCast.close();
            gv.client().close();
            sv.client().close();
        }
    }

    /** Gọi trên luồng JavaFX (trong {@code fx} hay {@code until}). */
    private static Node lookup(Scene s, String q) {
        s.getRoot().applyCss();
        s.getRoot().layout();
        return s.getRoot().lookup(q);
    }
}
