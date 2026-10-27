package labcast.client.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Lớp phóng to: nâng một khối nội dung lên giữa màn, chiếm {@code fraction} khung (0.7 = 70%), nền phía sau
 * mờ và tối đi. Bấm ra ngoài hoặc Esc thì đóng. Nằm trong màn chính ({@link #HOST}) nên vẫn nhận CSS và mức
 * phóng của app; người gọi tự đưa nút nội dung vào và trả về chỗ cũ trong {@code onClosed}.
 */
public final class FocusLayer {
    /** Khoá trong {@code getProperties()} của màn làm nền cho lớp phóng to (màn chính). */
    public static final String HOST = "focus-host";

    private static final Duration IN = Duration.millis(260);
    private static final Duration OUT = Duration.millis(170);

    private final Pane host;
    private final StackPane layer;
    private final Region backdrop = new Region();
    private final StackPane card;
    private final List<Node> blurred = new ArrayList<>();
    private final List<Effect> previous = new ArrayList<>();
    private final Runnable onClosed;
    private boolean closing;

    private FocusLayer(Pane host, Node content, double fraction, Runnable onClosed) {
        this.host = host;
        this.onClosed = onClosed;
        backdrop.getStyleClass().add("focus-backdrop");
        backdrop.setOnMouseClicked(e -> close());
        card = new StackPane(content);
        card.getStyleClass().add("focus-card");
        card.maxWidthProperty().bind(host.widthProperty().multiply(fraction));
        card.maxHeightProperty().bind(host.heightProperty().multiply(fraction));
        card.prefWidthProperty().bind(card.maxWidthProperty());
        card.prefHeightProperty().bind(card.maxHeightProperty());
        layer = new StackPane(backdrop, card);
        layer.getStyleClass().add("focus-layer");
        layer.setManaged(false);
        layer.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                close();
            }
        });
        Runnable fit = () -> layer.resizeRelocate(0, 0, host.getWidth(), host.getHeight());
        host.widthProperty().addListener((o, a, b) -> fit.run());
        host.heightProperty().addListener((o, a, b) -> fit.run());
        fit.run();
    }

    /**
     * Mở lớp phóng to trên màn chứa {@code anchor}.
     *
     * @param onClosed chạy sau khi lớp đã tắt hẳn (trả nút nội dung về chỗ cũ, lưu chữ…)
     */
    public static FocusLayer open(Node anchor, Node content, double fraction, Runnable onClosed) {
        FocusLayer f = new FocusLayer(hostOf(anchor), content, fraction, onClosed);
        f.show();
        return f;
    }

    private static Pane hostOf(Node anchor) {
        Pane fallback = null;
        for (Parent p = anchor instanceof Parent self ? self : anchor.getParent(); p != null; p = p.getParent()) {
            if (p instanceof Pane pane) {
                if (p.getProperties().containsKey(HOST)) return pane;
                fallback = pane;
            }
        }
        if (fallback == null) throw new IllegalStateException("Không có màn nào để phóng to");
        return fallback;
    }

    private void show() {
        for (Node n : host.getChildren()) {
            blurred.add(n);
            previous.add(n.getEffect());
            n.setEffect(new GaussianBlur(7));
        }
        host.getChildren().add(layer);
        backdrop.setOpacity(0);
        card.setOpacity(0);
        card.setScaleX(0.9);
        card.setScaleY(0.9);
        card.setTranslateY(28);
        Interpolator ease = Interpolator.SPLINE(0.2, 0.8, 0.2, 1);
        FadeTransition back = new FadeTransition(IN, backdrop);
        back.setToValue(1);
        FadeTransition fade = new FadeTransition(IN, card);
        fade.setToValue(1);
        ScaleTransition grow = new ScaleTransition(IN, card);
        grow.setToX(1);
        grow.setToY(1);
        grow.setInterpolator(ease);
        TranslateTransition lift = new TranslateTransition(IN, card);
        lift.setToY(0);
        lift.setInterpolator(ease);
        new ParallelTransition(back, fade, grow, lift).play();
        layer.requestFocus();
    }

    public boolean isOpen() {
        return !closing;
    }

    /** Đóng có hiệu ứng thu lại; gọi nhiều lần cũng chỉ đóng một lần. */
    public void close() {
        if (closing) return;
        closing = true;
        FadeTransition back = new FadeTransition(OUT, backdrop);
        back.setToValue(0);
        FadeTransition fade = new FadeTransition(OUT, card);
        fade.setToValue(0);
        ScaleTransition shrink = new ScaleTransition(OUT, card);
        shrink.setToX(0.94);
        shrink.setToY(0.94);
        ParallelTransition all = new ParallelTransition(back, fade, shrink);
        all.setOnFinished(e -> {
            host.getChildren().remove(layer);
            for (int i = 0; i < blurred.size(); i++) blurred.get(i).setEffect(previous.get(i));
            card.getChildren().clear(); // nhả nút nội dung để người gọi đặt lại chỗ cũ
            onClosed.run();
        });
        all.play();
    }
}
