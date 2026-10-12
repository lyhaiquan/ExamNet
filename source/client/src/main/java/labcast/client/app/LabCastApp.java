package labcast.client.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import labcast.client.clock.ClockSync;
import labcast.client.journal.JournalClient;
import labcast.client.ui.HomeScreen;
import labcast.client.ui.LoginController;
import labcast.client.ui.LoginView;

/** App desktop LabCast: màn đăng nhập, rồi màn chính theo vai trò. */
public final class LabCastApp extends Application {
    private Stage stage;
    private LoginController.LoggedIn session;
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
        session = null;
        stage.getScene().setRoot(new LoginView(this::loggedIn));
    }

    private void loggedIn(LoginController.LoggedIn li) {
        session = li;
        clock.start(li.client());
        JournalClient journal = new JournalClient(li.client());
        stage.setTitle("LabCast — " + li.className() + " — " + li.fullName());
        stage.getScene().setRoot(new HomeScreen(li, journal, () -> {
            new Alert(
                            Alert.AlertType.WARNING,
                            "Phiên làm việc đã kết thúc (có thể tài khoản vừa đăng nhập ở máy khác). Hãy đăng nhập lại.")
                    .show();
            showLogin();
        }));
    }
}
