package labcast.client.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import labcast.client.cast.CastClient;
import labcast.client.clock.ClockSync;
import labcast.client.journal.JournalClient;
import labcast.client.ui.HomeScreen;
import labcast.client.ui.LoginController;
import labcast.client.ui.LoginView;
import labcast.client.ui.Theme;
import labcast.client.ui.ZoomPane;
import labcast.common.protocol.Role;

/** App desktop LabCast: màn đăng nhập, rồi màn chính theo vai trò. */
public final class LabCastApp extends Application {
    private Stage stage;
    private LoginController.LoggedIn session;
    /** Nhận chiếu của giáo viên (ĐG1): nghe multicast từ lúc đăng nhập để vào bài là có ngay. */
    private CastClient cast;
    /** Giờ server nhìn từ máy này; giai đoạn 2 dùng cho phát hẹn giờ và câu hỏi nhanh. */
    private final ClockSync clock = new ClockSync();
    /** Gốc cố định của cửa sổ; đổi màn thì thay nội dung bên trong để giữ mức phóng chữ. */
    private ZoomPane root;
    /** Thẻ vai trò mở sẵn ở màn đăng nhập ({@code --vai=quan-tri|giao-vien|hoc-vien}); mặc định sinh viên. */
    private Role loginRole = Role.STUDENT;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Theme.loadFonts(); // font đóng gói phải nạp trước khi dựng màn đầu tiên
        this.stage = stage;
        Role r = LoginView.roleFromArg(getParameters().getNamed().get("vai"));
        if (r != null) loginRole = r;
        stage.setTitle("LabCast");
        // Icon trên thanh tiêu đề và taskbar; Windows chọn cỡ hợp nhất trong danh sách
        for (int s : new int[] {16, 32, 48, 64, 128, 256}) {
            try (var in = LabCastApp.class.getResourceAsStream("/labcast/client/ui/icon/labcast-" + s + ".png")) {
                if (in != null) {
                    stage.getIcons().add(new Image(in));
                }
            } catch (java.io.IOException ignored) {
                // thiếu icon thì dùng icon mặc định của Java
            }
        }
        root = new ZoomPane();
        Scene scene = new Scene(root, 1200, 760);
        root.install(scene);
        stage.setScene(scene);
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
        if (cast != null) {
            cast.close();
            cast = null;
        }
        if (session != null) {
            session.client().close();
            session = null;
        }
        stage.setTitle("LabCast — Đăng nhập " + LoginView.roleName(loginRole));
        root.setContent(new LoginView(this::loggedIn, loginRole));
    }

    private void loggedIn(LoginController.LoggedIn li) {
        session = li;
        clock.start(li.client());
        JournalClient journal = new JournalClient(li.client());
        stage.setTitle("LabCast — " + li.className() + " — " + li.fullName());
        cast = new CastClient(li.client(), li.castId(), clock, Platform::runLater);
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
                cast.session(),
                this::showLogin);
        root.setContent(home);
    }
}
