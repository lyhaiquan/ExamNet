package labcast.client.ui;

import static labcast.client.ui.UiSmokeTest.chup;
import static labcast.client.ui.UiSmokeTest.fx;
import static labcast.client.ui.UiSmokeTest.repoRoot;
import static labcast.client.ui.UiSmokeTest.until;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import javafx.application.Platform;
import javafx.scene.Scene;
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
 * Chạy TAY như {@link UiSmokeTest}: dựng trang chủ của học viên, giáo viên, quản trị trên server trong
 * bộ nhớ rồi chụp ảnh (sáng và tối) vào {@code client/target/ui-smoke/}.
 *
 * <pre>
 * set LABCAST_UI_SMOKE=1
 * mvnw -pl client test -Dtest=TrangChuSmokeTest
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "LABCAST_UI_SMOKE", matches = "1")
class TrangChuSmokeTest {

    @Test
    void trangChu_baVaiTro() throws Exception {
        var cfg = ServerConfig.forTest()
                .withContent(repoRoot().resolve("content"))
                .withKhongSandbox(true)
                .withLop("DSA Nâng cao - K23");
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
            users.create("ad", "Quản trị hệ thống", Role.ADMIN, "1");
            var classes = new ClassRepository(db);
            long lop = classes.ensure("DSA Nâng cao - K23", gv);
            classes.ensure("SQL cơ bản - K24", gv);
            String[] ten = {"Hoàng Nam", "Phương Anh", "Quang Huy", "Minh Đức"};
            long now = System.currentTimeMillis();
            for (int i = 0; i < ten.length; i++) {
                long id = users.create("sv" + i, ten[i], Role.STUDENT, "1");
                classes.enroll(lop, id);
                int k = i;
                db.tx(c -> {
                    try (var ps = c.prepareStatement("INSERT INTO progress (user_id, lesson_id, tier, status,"
                            + " score, updated_at) SELECT ?, id, 'code', ?, ?, ? FROM lessons ORDER BY id LIMIT ?")) {
                        ps.setLong(1, id);
                        ps.setString(2, "MASTERED");
                        ps.setInt(3, 100);
                        ps.setLong(4, now);
                        ps.setInt(5, 6 - k);
                        ps.executeUpdate();
                    }
                    // Lượt chạy/nộp rải trên 30 ngày để có biểu đồ; 6 ngày gần nhất ngày nào cũng làm
                    try (var ps = c.prepareStatement("INSERT INTO submissions (user_id, lesson_id, code_seq, kind,"
                            + " verdict, score, created_at) SELECT ?, id, 1, ?, ?, 100, ? FROM lessons LIMIT 1")) {
                        var rnd = new java.util.Random(k);
                        for (int d = 0; d < 30; d++) {
                            int n = d < 6 ? 2 + rnd.nextInt(4) : rnd.nextInt(5);
                            for (int j = 0; j < n; j++) {
                                ps.setLong(1, id);
                                ps.setString(2, j % 3 == 0 ? "SUBMIT" : "RUN");
                                ps.setString(3, j % 4 == 3 ? "WRONG" : "OK");
                                ps.setLong(4, now - d * 86_400_000L - j * 600_000L);
                                ps.executeUpdate();
                            }
                        }
                    }
                    try (var ps = c.prepareStatement("INSERT INTO student_activity VALUES (?, ?, ?, ?, ?)")) {
                        ps.setLong(1, id);
                        ps.setLong(2, lop);
                        ps.setString(3, k % 2 == 0 ? "Đang làm bài tập" : "Xem danh sách bài tập");
                        ps.setString(4, k % 2 == 0 ? "dsa.sap-xep.noi-bot" : "");
                        ps.setLong(5, now - k * 7 * 60_000L);
                        ps.executeUpdate();
                    }
                    return null;
                });
            }
            db.tx(c -> c.createStatement()
                    .executeUpdate("UPDATE progress SET status='OPEN', score=NULL WHERE user_id=(SELECT id FROM"
                            + " users WHERE username='sv0') AND lesson_id=(SELECT max(lesson_id) FROM progress"
                            + " WHERE user_id=(SELECT id FROM users WHERE username='sv0'))"));
            // sv0 nộp sai hai bài chương Băm/Tìm kiếm: hiện màu đỏ trong kho bài
            db.tx(c -> c.createStatement()
                    .executeUpdate("INSERT OR REPLACE INTO progress (user_id, lesson_id, tier, status, wrong_submits,"
                            + " updated_at) SELECT u.id, l.id, 'code', 'OPEN', 2, 0 FROM users u, lessons l"
                            + " WHERE u.username='sv0' AND l.id IN ('dsa.tim-kiem.nhi-phan','dsa.sap-xep.noi-bot')"));
            try {
                Platform.startup(() -> {});
            } catch (IllegalStateException alreadyStarted) {
                // đã chạy UiSmokeTest trong cùng JVM
            }
            for (String[] who : new String[][] {{"sv0", "hoc-vien"}, {"gv", "giao-vien"}, {"ad", "quan-tri"}}) {
                var li = LoginController.login("127.0.0.1", server.port(), who[0], "1");
                for (boolean dark : new boolean[] {false, true}) {
                    fx(() -> {
                        Theme.setDark(dark);
                        return null;
                    });
                    Scene scene = fx(
                            () -> new Scene(new HomeScreen(li, new JournalClient(li.client()), () -> {}), 1440, 900));
                    until(
                            () -> {
                                scene.getRoot().applyCss();
                                scene.getRoot().layout();
                                return scene.getRoot().lookupAll(".dash-stat-value").stream()
                                        .anyMatch(n -> !((Label) n).getText().startsWith("0"));
                            },
                            "số liệu trang chủ " + who[0]);
                    Thread.sleep(700); // chờ danh sách học viên
                    chup(scene, "trang-chu-" + who[1] + (dark ? "-toi" : "-sang"));
                    fx(() -> {
                        ((javafx.scene.control.Button) scene.lookup("#nav-exercises")).fire();
                        return null;
                    });
                    chup(scene, "kho-bai-" + who[1] + (dark ? "-toi" : "-sang"));
                    if (who[0].equals("gv") && !dark) {
                        fx(() -> {
                            ((javafx.scene.control.Button) scene.lookup("#nav-add")).fire();
                            return null;
                        });
                        chup(scene, "them-bai-giao-vien");
                    }
                }
                li.client().close();
            }
            assertTrue(
                    java.nio.file.Files.exists(java.nio.file.Path.of("target/ui-smoke/trang-chu-giao-vien-sang.png")));
        }
    }
}
