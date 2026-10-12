package labcast.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javax.imageio.ImageIO;
import labcast.client.journal.JournalClient;
import labcast.client.net.DiscoveryClient;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.discovery.DiscoveryResponder;
import labcast.server.journal.JournalModule;
import labcast.server.lesson.LessonModule;
import labcast.server.run.RunModule;
import labcast.server.store.Database;
import labcast.server.sync.SyncModule;
import org.fxmisc.richtext.CodeArea;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Chạy TAY trên máy có màn hình: đi trọn luồng của nghiệm thu M1 qua giao diện thật rồi chụp ảnh vào
 * {@code client/target/ui-smoke/}. CI không có màn hình nên bỏ qua.
 *
 * <pre>
 * set LABCAST_UI_SMOKE=1
 * mvnw -pl client test -Dtest=UiSmokeTest
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "LABCAST_UI_SMOKE", matches = "1")
class UiSmokeTest {
    static final String CODE =
            """
            import viz

            def sap_xep(a):
                n = len(a)
                for i in range(n - 1):
                    for j in range(n - 1 - i):
                        if a[j] > a[j + 1]:
                            a[j], a[j + 1] = a[j + 1], a[j]

            a = viz.Mang(list(map(int, input().split())), ten="a")
            sap_xep(a)
            print(*a)
            """;

    static <T> T fx(Callable<T> c) throws Exception {
        CompletableFuture<T> f = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                f.complete(c.call());
            } catch (Exception e) {
                f.completeExceptionally(e);
            }
        });
        return f.get(10, TimeUnit.SECONDS);
    }

    static void until(Callable<Boolean> cond, String what) throws Exception {
        for (int i = 0; i < 200; i++) {
            if (fx(cond)) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Hết giờ chờ: " + what);
    }

    static void chup(Scene scene, String name) throws Exception {
        WritableImage img = fx(() -> {
            // cảnh không gắn cửa sổ thì không có pulse: nút vừa tạo chưa nhận CSS, tự áp trước khi chụp
            for (int i = 0; i < 2; i++) {
                scene.getRoot().applyCss();
                scene.getRoot().layout();
            }
            return scene.snapshot(null);
        });
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader pr = img.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out.setRGB(x, y, pr.getArgb(x, y));
            }
        }
        Path dir = Files.createDirectories(Path.of("target/ui-smoke"));
        ImageIO.write(out, "png", new File(dir.toFile(), name + ".png"));
    }

    static KeyEvent phim(KeyCode code, boolean shift, boolean ctrl) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, shift, ctrl, false, false);
    }

    /** Nút đầu tiên thuộc kiểu {@code type} trong cây giao diện (duyệt sâu). */
    static <T> T tim(Node n, Class<T> type) {
        if (type.isInstance(n)) {
            return type.cast(n);
        }
        if (n instanceof javafx.scene.Parent p) {
            for (Node c : p.getChildrenUnmodifiable()) {
                T t = tim(c, type);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    static Path repoRoot() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.exists(p.resolve("runner/run.py"))) {
            p = p.getParent();
        }
        return p;
    }

    @Test
    void dangNhap_moBai_chayThu() throws Exception {
        var cfg = ServerConfig.forTest()
                .withContent(repoRoot().resolve("content"))
                .withKhongSandbox(true)
                .withLop("Lớp D22CQCN01");
        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(
                        cfg,
                        db,
                        List.of(
                                new AccountModule(),
                                new SyncModule(),
                                new LessonModule(),
                                new RunModule(),
                                new JournalModule()))) {
            server.start();
            new UserRepository(db).create("sv", "Nguyễn Văn An", Role.STUDENT, "1");
            Platform.startup(() -> {});

            // Trả lời tìm lớp ở cổng thật 7002 để màn đăng nhập hiện tên lớp (địa chỉ giả, chỉ để chụp
            // ảnh — bước sau đăng nhập thẳng bằng LoginController). Cổng đang bận (máy đang chạy server
            // thật) thì bỏ qua, màn đăng nhập chỉ báo không thấy lớp.
            DiscoveryResponder discovery = null;
            try {
                discovery = new DiscoveryResponder(DiscoveryClient.DEFAULT_PORT, () -> new PayloadWriter()
                        .str("Lớp D22CQCN01")
                        .str("Cô Lan")
                        .str("192.168.1.10")
                        .u16(7000)
                        .bool(false)
                        .str("")
                        .toBytes());
                discovery.start();
            } catch (Exception e) {
                discovery = null;
            }
            Scene loginScene = fx(() -> new Scene(new LoginView(li -> {}), 960, 680));
            Thread.sleep(2_000); // chờ tìm lớp xong
            chup(loginScene, "1-dang-nhap");
            if (discovery != null) {
                discovery.close();
            }

            var li = LoginController.login("127.0.0.1", server.port(), "sv", "1");
            Scene scene = fx(() -> new Scene(new HomeScreen(li, new JournalClient(li.client()), () -> {}), 1280, 800));
            @SuppressWarnings("unchecked")
            ListView<Object> list = (ListView<Object>) fx(() -> scene.lookup("#lessons"));
            until(() -> !list.getItems().isEmpty(), "danh sách bài");
            fx(() -> {
                list.getSelectionModel().select(0);
                return null;
            });
            try {
                until(
                        () -> {
                            // cảnh không gắn cửa sổ thì không có pulse: tự áp CSS và bố trí để
                            // SplitPane dựng skin, nếu không thì chưa có nút con nào
                            scene.getRoot().applyCss();
                            scene.getRoot().layout();
                            return tim(scene.getRoot(), CodeArea.class) != null;
                        },
                        "màn bài học");
            } catch (AssertionError e) {
                chup(scene, "loi-mo-bai");
                fx(() -> {
                    scene.getRoot()
                            .lookupAll(".label")
                            .forEach(n -> System.err.println("LABEL: " + ((Label) n).getText()));
                    return null;
                });
                throw e;
            }
            Thread.sleep(1_500); // để trình phát chạy vài bước giảng giải
            Button play = (Button) fx(() -> scene.lookup("#play"));
            fx(() -> {
                play.fire();
                return null;
            });
            Thread.sleep(3_000);
            chup(scene, "2-bai-hoc-giang-giai");

            // Học viên: chữ copy từ ngoài (clipboard của Windows) không dán được vào ô code, kể cả
            // Shift+Insert; copy trong ô code không ra clipboard của Windows nhưng dán lại được trong
            // ô code. Bắn phím thật để chắc RichTextFX gọi đúng copy()/paste() đã chặn.
            // (Test này ghi đè clipboard của máy đang chạy.)
            CodeArea area = fx(() -> tim(scene.getRoot(), CodeArea.class));
            String truoc = fx(area::getText);
            fx(() -> {
                ClipboardContent cc = new ClipboardContent();
                cc.putString("print(sorted(a))");
                Clipboard.getSystemClipboard().setContent(cc);
                area.moveTo(area.getLength());
                area.fireEvent(phim(KeyCode.V, false, true));
                area.fireEvent(phim(KeyCode.INSERT, true, false));
                return null;
            });
            assertEquals(truoc, fx(area::getText), "dán từ ngoài phải bị chặn");
            fx(() -> {
                area.selectRange(0, 6); // "import"
                area.fireEvent(phim(KeyCode.C, false, true));
                area.moveTo(area.getLength());
                area.fireEvent(phim(KeyCode.V, false, true));
                return null;
            });
            assertEquals(
                    "print(sorted(a))", fx(() -> Clipboard.getSystemClipboard().getString()));
            assertTrue(fx(area::getText).endsWith("import"), "dán đoạn copy trong ô code phải được");
            fx(() -> {
                area.replaceText(CODE);
                return null;
            });
            Button run = (Button) fx(() -> scene.lookup("#run"));
            fx(() -> {
                run.fire();
                return null;
            });
            Label verdict = (Label) fx(() -> scene.lookup("#verdict"));
            until(() -> verdict.getText().contains("Đúng"), "kết quả chạy thử");
            Label source = (Label) fx(() -> scene.lookup("#trace-source"));
            assertEquals("Đang xem: code của em", fx(source::getText));
            fx(() -> {
                play.fire();
                return null;
            });
            Thread.sleep(2_500);
            chup(scene, "3-chay-thu-code-cua-em");

            TabPane tabs = fx(() -> tim(scene.getRoot(), TabPane.class));
            fx(() -> {
                tabs.getSelectionModel().select(1);
                return null;
            });
            chup(scene, "4-de-bai");

            HomeScreen home = (HomeScreen) scene.getRoot();
            assertEquals(
                    "dsa.sap-xep.noi-bot", fx(() -> home.openLessonProperty().get()));
            fx(() -> {
                ((Button) scene.lookup("#close-lesson")).fire();
                return null;
            });
            assertEquals(null, fx(() -> home.openLessonProperty().get()));
            assertTrue(Files.exists(Path.of("target/ui-smoke/3-chay-thu-code-cua-em.png")));
            li.client().close();
        }
    }
}
