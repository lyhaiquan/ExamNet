package labcast.client.ui;

import java.util.prefs.Preferences;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

/**
 * Gốc của cửa sổ app: phóng to/thu nhỏ cả giao diện như trình duyệt (chữ, khung, animation, ô code). Ctrl
 * và + hoặc −, Ctrl 0 về 100%, hoặc giữ Ctrl lăn chuột. Màn bên trong được bố trí trên khổ nhỏ hơn rồi
 * phóng lên đủ cửa sổ, nên phóng to thì chữ to ra và các khung tự xếp lại cho vừa, không phải kéo ngang.
 *
 * <p>Phóng bằng biến đổi tỉ lệ chứ không đổi cỡ chữ trong CSS: CSS và các bộ vẽ animation đặt cỡ chữ bằng
 * pixel, đổi cỡ chữ gốc thì chỉ một phần giao diện to lên. Mức đã chọn được nhớ cho lần mở app sau trên
 * máy này.
 *
 * <p>Trên 100% thì chỉ phóng tới mức màn bài học còn xếp vừa ({@link #MIN_LAYOUT_W} × {@link
 * #MIN_LAYOUT_H}): to hơn nữa thì khung animation hay ô code bị cắt mất. Cửa sổ rộng ra thì tự phóng tiếp
 * tới mức đã chọn.
 */
public final class ZoomPane extends Region {
    /** Các mức phóng, bấm + hay − thì nhảy một mức. */
    static final double[] LEVELS = {0.75, 0.85, 1.0, 1.1, 1.25, 1.4, 1.6, 1.8, 2.0};

    /** Khổ nhỏ nhất (đơn vị giao diện ở 100%) mà animation và ô code còn nằm cạnh nhau đủ nút. */
    static final double MIN_LAYOUT_W = 860;

    static final double MIN_LAYOUT_H = 520;

    private static final String PREF_KEY = "zoom";

    private final Scale scale = new Scale(1, 1, 0, 0);
    private final Label toast = new Label();
    private final SequentialTransition toastAnim;
    private Parent content;
    /** Mức người dùng chọn; mức thật đang hiện là {@link #zoom()}, có thể nhỏ hơn khi cửa sổ hẹp. */
    private double chosen = 1.0;
    /** Lăn chuột cộng dồn tới đủ một nấc mới đổi mức (bàn di chuột gửi nhiều sự kiện nhỏ). */
    private double wheel;

    public ZoomPane() {
        Theme.apply(this);
        toast.getStyleClass().add("zoom-toast");
        toast.setManaged(false);
        toast.setMouseTransparent(true);
        toast.setOpacity(0);
        getChildren().add(toast);
        FadeTransition out = new FadeTransition(Duration.millis(300), toast);
        out.setToValue(0);
        toastAnim = new SequentialTransition(new PauseTransition(Duration.millis(1100)), out);
        setZoom(load(), false);
    }

    /** Thay màn đang hiện (màn đăng nhập, màn chính…); mức phóng giữ nguyên. */
    public void setContent(Parent p) {
        if (content != null) {
            content.getTransforms().remove(scale);
            getChildren().remove(content);
        }
        content = p;
        p.getTransforms().add(scale);
        getChildren().add(0, p);
        requestLayout();
    }

    public Parent content() {
        return content;
    }

    /** Mức phóng đang hiện. */
    public double zoom() {
        return effective(chosen, getWidth(), getHeight());
    }

    /** Gắn phím tắt và lăn chuột vào cảnh. Lọc ở cảnh nên bấm ở đâu trong cửa sổ cũng được. */
    public void install(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!e.isShortcutDown() || e.isAltDown()) {
                return;
            }
            KeyCode k = e.getCode();
            if (k == KeyCode.EQUALS || k == KeyCode.PLUS || k == KeyCode.ADD) {
                setZoom(step(zoom(), +1), true);
            } else if (k == KeyCode.MINUS || k == KeyCode.SUBTRACT) {
                setZoom(step(zoom(), -1), true);
            } else if (k == KeyCode.DIGIT0 || k == KeyCode.NUMPAD0) {
                setZoom(1.0, true);
            } else {
                return;
            }
            e.consume();
        });
        scene.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (!e.isShortcutDown() || e.getDeltaY() == 0) {
                return;
            }
            e.consume(); // không để danh sách hay ô code cuộn theo
            double notch = e.getMultiplierY() > 0 ? e.getMultiplierY() : 40;
            wheel += e.getDeltaY();
            while (Math.abs(wheel) >= notch) {
                int dir = wheel > 0 ? +1 : -1;
                wheel -= dir * notch;
                setZoom(step(zoom(), dir), true);
            }
        });
    }

    /** Mức kế tiếp theo hướng {@code dir} (+1 to lên, −1 nhỏ đi); đã ở mức cuối thì giữ nguyên. */
    static double step(double current, int dir) {
        if (dir > 0) {
            for (double l : LEVELS) {
                if (l > current + 1e-6) {
                    return l;
                }
            }
            return LEVELS[LEVELS.length - 1];
        }
        for (int i = LEVELS.length - 1; i >= 0; i--) {
            if (LEVELS[i] < current - 1e-6) {
                return LEVELS[i];
            }
        }
        return LEVELS[0];
    }

    /**
     * Mức thật trong khung {@code w} × {@code h}: trên 100% thì không quá mức màn bài học còn xếp vừa; dưới
     * 100% (người dùng tự thu nhỏ) giữ nguyên. Khung chưa có kích thước thì dùng mức đã chọn.
     */
    static double effective(double chosen, double w, double h) {
        if (chosen <= 1.0 || w <= 0 || h <= 0) {
            return chosen;
        }
        double fit = Math.min(w / MIN_LAYOUT_W, h / MIN_LAYOUT_H);
        return Math.max(1.0, Math.min(chosen, fit));
    }

    /** Ép về khoảng các mức có; giá trị lạ (bản ghi hỏng) thì về 100%. */
    static double clamp(double z) {
        if (Double.isNaN(z) || z <= 0) {
            return 1.0;
        }
        return Math.max(LEVELS[0], Math.min(LEVELS[LEVELS.length - 1], z));
    }

    void setZoom(double z, boolean announce) {
        chosen = clamp(z);
        requestLayout();
        if (announce) {
            showToast(zoom(), zoom() < chosen - 1e-6);
            save(chosen);
        }
    }

    private void showToast(double z, boolean capped) {
        int pct = (int) Math.round(z * 100);
        toast.setText(
                capped
                        ? "Cỡ chữ " + pct + "%  ·  to nhất vừa cửa sổ này"
                        : pct == 100 ? "Cỡ chữ 100%" : "Cỡ chữ " + pct + "%  ·  Ctrl 0 để về 100%");
        toast.setOpacity(1);
        toast.applyCss();
        toast.autosize();
        layoutToast();
        toastAnim.playFromStart();
    }

    private void layoutToast() {
        toast.relocate(Math.round((getWidth() - toast.getWidth()) / 2), 18);
    }

    @Override
    protected void layoutChildren() {
        double z = zoom();
        scale.setX(z);
        scale.setY(z);
        if (content != null) {
            content.resizeRelocate(0, 0, getWidth() / z, getHeight() / z);
        }
        layoutToast();
    }

    private static double load() {
        try {
            return clamp(Preferences.userNodeForPackage(ZoomPane.class).getDouble(PREF_KEY, 1.0));
        } catch (RuntimeException e) {
            return 1.0; // máy chặn ghi cấu hình người dùng: dùng 100%
        }
    }

    private static void save(double z) {
        try {
            Preferences.userNodeForPackage(ZoomPane.class).putDouble(PREF_KEY, z);
        } catch (RuntimeException e) {
            // không nhớ được thì lần sau về 100%
        }
    }
}
