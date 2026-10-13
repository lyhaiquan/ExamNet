package labcast.client.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import labcast.client.cast.CastClient;
import labcast.client.clock.ClockSync;
import labcast.client.journal.JournalClient;
import labcast.client.net.AppFocusReporter;
import labcast.client.ui.HomeScreen;
import labcast.client.ui.LessonLock;
import labcast.client.ui.LoginController;
import labcast.client.ui.LoginView;
import labcast.common.protocol.Role;

/** App desktop LabCast: màn đăng nhập, rồi màn chính theo vai trò. */
public final class LabCastApp extends Application {
    private Stage stage;
    private LoginController.LoggedIn session;
    private LessonLock lock;
    /** Nhận chiếu của giáo viên (ĐG1): nghe multicast từ lúc đăng nhập để vào bài là có ngay. */
    private CastClient cast;
    /** Giờ server nhìn từ máy này; giai đoạn 2 dùng cho phát hẹn giờ và câu hỏi nhanh. */
    private final ClockSync clock = new ClockSync();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("LabCast");
        stage.setScene(new Scene(new StackPane(), 1200, 760));
        showLogin();
        stage.setOnCloseRequest(e -> {
            if (session != null) {
                session.client().close();
            }
            Platform.exit();
        });
        stage.show();
    }

    private void showLogin() {
        if (lock != null) {
            lock.release();
            lock = null;
        }
        if (cast != null) {
            cast.close();
            cast = null;
        }
        session = null;
        stage.getScene().setRoot(new LoginView(this::loggedIn));
    }

    private void loggedIn(LoginController.LoggedIn li) {
        session = li;
        clock.start(li.client());
        JournalClient journal = new JournalClient(li.client());
        stage.setTitle("LabCast — " + li.className() + " — " + li.fullName());
        cast = new CastClient(li.client(), clock, Platform::runLater);
        cast.start();
        HomeScreen home = new HomeScreen(
                li,
                journal,
                () -> {
                    showLogin(); // thả khoá toàn màn hình trước, hộp thoại mới không bị che
                    Alert a = new Alert(
                            Alert.AlertType.WARNING,
                            "Phiên làm việc đã kết thúc (có thể tài khoản vừa đăng nhập ở máy khác). Hãy đăng nhập lại.");
                    a.initOwner(stage);
                    a.show();
                },
                cast.session());
        // Học viên mở bài thì khoá toàn màn hình; giáo viên mở bài để giảng thì không
        if (li.role() == Role.STUDENT) {
            AppFocusReporter reporter = new AppFocusReporter(li.client());
            lock = new LessonLock(
                    stage,
                    inApp -> reporter.changed(inApp, home.openLessonProperty().get()));
            home.openLessonProperty().addListener((obs, was, now) -> {
                if (now != null) {
                    lock.engage();
                } else {
                    lock.release();
                }
            });
        }
        stage.getScene().setRoot(home);
    }
}
