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

    static javafx.scene.input.MouseEvent mouse(javafx.event.EventType<javafx.scene.input.MouseEvent> type) {
        return new javafx.scene.input.MouseEvent(
                type,
                5,
                5,
                5,
                5,
                javafx.scene.input.MouseButton.PRIMARY,
                1,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                true,
                false,
                true,
                null);
    }

    static javafx.scene.input.MouseEvent press() {
        return mouse(javafx.scene.input.MouseEvent.MOUSE_PRESSED);
    }

    static javafx.scene.input.MouseEvent click() {
        return mouse(javafx.scene.input.MouseEvent.MOUSE_CLICKED);
    }

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
                                new HomeModule(),
                                new labcast.server.discuss.DiscussionModule()))) {
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
            // vài bình luận mẫu ở mọi bài chương Cơ bản: học viên hỏi, giáo viên trả lời
            long[] authors = {sv, gv};
            db.tx(c -> {
                String[] texts = {
                    "Em chưa hiểu sao phải thêm ORDER BY id khi đã ORDER BY gia rồi ạ?",
                    "Vì ba sản phẩm cùng giá 16 000 000, thiếu id thì thứ tự giữa chúng không xác định.",
                    "Dạ em hiểu rồi, cảm ơn cô!"
                };
                for (int k = 0; k < texts.length; k++) {
                    c.createStatement()
                            .executeUpdate("INSERT INTO discussions (class_id, lesson_id, parent_id, author_id, text,"
                                    + " created_at, hidden) SELECT " + lop + ", id, "
                                    + (k == 0 ? "NULL" : "(SELECT min(d.id) FROM discussions d WHERE d.lesson_id=l.id)")
                                    + ", " + authors[k % 2] + ", '" + texts[k] + "', "
                                    + (System.currentTimeMillis() - (3 - k) * 600_000L)
                                    + ", 0 FROM lessons l WHERE chuong='co-ban'");
                }
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
                if (who.equals("sv")) {
                    // mở bài đầu bảng rồi vào chế độ tập trung
                    fx(() -> {
                        javafx.event.Event.fireEvent(scene.getRoot().lookup(".catalog-row"), click());
                        return null;
                    });
                    until(
                            () -> {
                                scene.getRoot().applyCss();
                                scene.getRoot().layout();
                                return scene.getRoot().lookup(".editor-expand-button") != null;
                            },
                            "màn làm bài");
                    Thread.sleep(800);
                    chup(scene, "sql-thao-luan-dong-sv");
                    fx(() -> {
                        ((Button) scene.getRoot().lookup(".discuss-toggle")).fire();
                        return null;
                    });
                    Thread.sleep(600);
                    chup(scene, "sql-thao-luan-sv");
                    fx(() -> {
                        ((Button) scene.getRoot().lookup(".editor-expand-button")).fire();
                        return null;
                    });
                    Thread.sleep(600);
                    chup(scene, "sql-tap-trung-sv");
                }
                if (who.equals("gv")) {
                    fx(() -> {
                        ((Button) scene.lookup("#nav-add")).fire();
                        return null;
                    });
                    Thread.sleep(300);
                    chup(scene, "sql-them-bai-gv");
                    fx(() -> {
                        var areas = scene.getRoot().lookupAll(".text-area").toArray(new javafx.scene.Node[0]);
                        // ô đáp án SQL: ô text-area có prompt "SELECT ..."
                        for (var n : areas) {
                            var t = (javafx.scene.control.TextArea) n;
                            if (t.getPromptText().startsWith("SELECT")) {
                                t.setText("select ho_ten, count(*) as so from khach_hang kh join don_hang dh on"
                                        + " dh.khach_hang_id = kh.id where thanh_pho = 'Hà Nội' and nam > 2000"
                                        + " group by ho_ten order by so desc;");
                                javafx.event.Event.fireEvent(t, press());
                            }
                        }
                        return null;
                    });
                    Thread.sleep(600);
                    chup(scene, "sql-phong-to-gv");
                    fx(() -> {
                        for (var n : scene.getRoot().lookupAll(".button")) {
                            if (n instanceof Button b && b.getText().contains("Định dạng")) b.fire();
                        }
                        return null;
                    });
                    Thread.sleep(200);
                    chup(scene, "sql-phong-to-dinh-dang-gv");
                }
                li.client().close();
            }
        }
    }
}
