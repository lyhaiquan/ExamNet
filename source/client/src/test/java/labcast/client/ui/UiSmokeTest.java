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
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;
import labcast.client.journal.JournalClient;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
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
        WritableImage img = fx(() -> scene.snapshot(null));
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

            Scene loginScene = fx(() -> new Scene(new LoginView(li -> {}), 520, 520));
            Thread.sleep(2_000); // chờ tìm lớp xong
            chup(loginScene, "1-dang-nhap");

            var li = LoginController.login("127.0.0.1", server.port(), "sv", "1");
            Scene scene = fx(() -> new Scene(new HomeScreen(li, new JournalClient(li.client()), () -> {}), 1280, 800));
            @SuppressWarnings("unchecked")
            ListView<Object> list = (ListView<Object>) fx(() -> scene.lookup(".list-view"));
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
            Button play = (Button) fx(() -> scene.getRoot().lookupAll(".button").stream()
                    .filter(n -> n instanceof Button b && b.getText().startsWith("▶ Phát"))
                    .findFirst()
                    .orElseThrow());
            fx(() -> {
                play.fire();
                return null;
            });
            Thread.sleep(3_000);
            chup(scene, "2-bai-hoc-giang-giai");

            CodeArea area = fx(() -> tim(scene.getRoot(), CodeArea.class));
            fx(() -> {
                area.replaceText(CODE);
                return null;
            });
            Button run = (Button) fx(() -> scene.getRoot().lookupAll(".button").stream()
                    .filter(n -> n instanceof Button b && b.getText().contains("Chạy thử"))
                    .findFirst()
                    .orElseThrow());
            fx(() -> {
                run.fire();
                return null;
            });
            until(
                    () -> scene.getRoot().lookupAll(".label").stream()
                            .anyMatch(n -> n instanceof Label l && l.getText().contains("Đúng")),
                    "kết quả chạy thử");
            Node source = fx(() -> scene.getRoot().lookupAll(".label").stream()
                    .filter(n -> n instanceof Label l && l.getText().startsWith("Đang xem"))
                    .findFirst()
                    .orElseThrow());
            assertEquals("Đang xem: code của em", ((Label) source).getText());
            Thread.sleep(2_500);
            chup(scene, "3-chay-thu-code-cua-em");
            assertTrue(Files.exists(Path.of("target/ui-smoke/3-chay-thu-code-cua-em.png")));
            li.client().close();
        }
    }
}
