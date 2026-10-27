package labcast.client.ui;

import static labcast.client.ui.UiSmokeTest.chup;
import static labcast.client.ui.UiSmokeTest.fx;
import static labcast.client.ui.UiSmokeTest.repoRoot;
import static labcast.client.ui.UiSmokeTest.until;

import java.util.List;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import labcast.client.journal.JournalClient;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.classroom.ClassRepository;
import labcast.server.classroom.ClassroomWorkspaceModule;
import labcast.server.home.HomeModule;
import labcast.server.lesson.LessonModule;
import labcast.server.store.Database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Chạy TAY như {@link UiSmokeTest}: lớp chỉ có môn SQL, chụp trang chủ, kho bài và màn Thêm bài SQL vào
 * {@code client/target/ui-smoke/}.
 */
@EnabledIfEnvironmentVariable(named = "LABCAST_UI_SMOKE", matches = "1")
class LopSqlSmokeTest {

    @Test
    void lopSql_trangChu_khoBai_themBai() throws Exception {
        var cfg = ServerConfig.forTest()
                .withContent(repoRoot().resolve("content"))
                .withKhongSandbox(true)
                .withMon("sql")
                .withLop("SQL cơ bản - K24");
        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(
                        cfg,
                        db,
                        List.of(
                                new AccountModule(),
                                new LessonModule(),
                                new ClassroomWorkspaceModule(),
                                new HomeModule()))) {
            server.start();
            var users = new UserRepository(db);
            long gv = users.create("gv", "Trần Minh Tâm", Role.TEACHER, "1");
            long lop = new ClassRepository(db).ensure("SQL cơ bản - K24", gv);
            long sv = users.create("sv", "Hoàng Nam", Role.STUDENT, "1");
            new ClassRepository(db).enroll(lop, sv);
            db.tx(c -> {
                c.createStatement()
                        .executeUpdate("INSERT INTO progress (user_id, lesson_id, tier, status, score, wrong_submits,"
                                + " updated_at) SELECT " + sv + ", id, 'code', 'MASTERED', 100, 0, 0 FROM lessons"
                                + " WHERE chuong='co-ban' ORDER BY id LIMIT 4");
                c.createStatement()
                        .executeUpdate("INSERT INTO progress (user_id, lesson_id, tier, status, wrong_submits,"
                                + " updated_at) SELECT " + sv + ", id, 'code', 'OPEN', 2, 0 FROM lessons"
                                + " WHERE chuong='co-ban' ORDER BY id DESC LIMIT 2");
                return null;
            });
            try {
                Platform.startup(() -> {});
            } catch (IllegalStateException alreadyStarted) {
                // đã chạy test giao diện khác trong cùng JVM
            }
            fx(() -> {
                Theme.setDark(false);
                return null;
            });
            for (String who : new String[] {"sv", "gv"}) {
                var li = LoginController.login("127.0.0.1", server.port(), who, "1");
                Scene scene =
                        fx(() -> new Scene(new HomeScreen(li, new JournalClient(li.client()), () -> {}), 1440, 900));
                until(
                        () -> {
                            scene.getRoot().applyCss();
                            scene.getRoot().layout();
                            return scene.getRoot().lookupAll(".dash-stat-value").stream()
                                    .anyMatch(n -> !((Label) n).getText().startsWith("0"));
                        },
                        "trang chủ " + who);
                Thread.sleep(700);
                chup(scene, "sql-trang-chu-" + who);
                fx(() -> {
                    ((Button) scene.lookup("#nav-exercises")).fire();
                    return null;
                });
                chup(scene, "sql-kho-bai-" + who);
                if (who.equals("gv")) {
                    fx(() -> {
                        ((Button) scene.lookup("#nav-add")).fire();
                        return null;
                    });
                    Thread.sleep(300);
                    chup(scene, "sql-them-bai-gv");
                }
                li.client().close();
            }
        }
    }
}
