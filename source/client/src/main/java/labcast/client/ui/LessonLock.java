package labcast.client.ui;

import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Khi học viên mở một bài: app chiếm toàn màn hình, Esc không thoát được, và theo dõi học viên có rời
 * app không (Alt+Tab, phím Windows, thu nhỏ cửa sổ).
 *
 * <p>Một app Java KHÔNG chặn được Alt+Tab hay phím Windows — việc đó cần quyền của hệ điều hành. Lớp này
 * chỉ làm được hai điều: kéo app về toàn màn hình khi học viên quay lại, và báo mỗi lần rời/quay lại
 * qua {@code onPresenceChange} để server ghi và báo giáo viên.
 *
 * <p>"Còn trong app" = có cửa sổ nào của app đang được focus (kể cả hộp thoại, danh sách thả xuống), nên
 * mở hộp thoại không bị tính là rời app. Kiểm mỗi 400 ms thay vì nghe từng cửa sổ: cửa sổ phụ sinh ra và
 * mất đi liên tục, nghe sự kiện dễ sót.
 */
public final class LessonLock {
    private static final Duration CHECK = Duration.millis(400);

    private final Stage stage;
    private final Consumer<Boolean> onPresenceChange;
    private final Timeline watcher;
    private boolean engaged;
    private boolean inApp = true;

    /** @param onPresenceChange nhận {@code false} khi học viên rời app, {@code true} khi quay lại */
    public LessonLock(Stage stage, Consumer<Boolean> onPresenceChange) {
        this.stage = stage;
        this.onPresenceChange = onPresenceChange;
        this.watcher = new Timeline(new KeyFrame(CHECK, e -> check()));
        watcher.setCycleCount(Timeline.INDEFINITE);
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setFullScreenExitHint("");
        // Chỉ đòi lại toàn màn hình khi học viên đã quay về app. Đòi lúc cửa sổ đang mất focus sẽ giành
        // nhau với Alt+Tab.
        stage.focusedProperty().addListener((obs, was, now) -> {
            if (now) {
                restoreFullScreen();
            }
        });
        stage.iconifiedProperty().addListener((obs, was, now) -> {
            if (!now) {
                restoreFullScreen();
            }
        });
    }

    public boolean engaged() {
        return engaged;
    }

    /** Bắt đầu khoá (học viên vừa mở bài). Gọi lại khi đang khoá thì không làm gì. */
    public void engage() {
        if (engaged) {
            return;
        }
        engaged = true;
        inApp = true;
        stage.setFullScreen(true);
        watcher.play();
    }

    /** Thả khoá (đóng bài, hết phiên, về màn đăng nhập). */
    public void release() {
        if (!engaged) {
            return;
        }
        engaged = false;
        watcher.stop();
        if (!inApp) {
            inApp = true;
            onPresenceChange.accept(true);
        }
        stage.setFullScreen(false);
    }

    /** Trạng thái hiện tại để gửi lại sau khi nối lại server. */
    public boolean inApp() {
        return inApp;
    }

    private void restoreFullScreen() {
        if (engaged && !stage.isFullScreen()) {
            stage.setFullScreen(true);
        }
    }

    private void check() {
        boolean now = Window.getWindows().stream().anyMatch(Window::isFocused);
        if (now != inApp) {
            inApp = now;
            onPresenceChange.accept(now);
        }
    }
}
