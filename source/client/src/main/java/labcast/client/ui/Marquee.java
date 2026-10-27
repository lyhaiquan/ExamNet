package labcast.client.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Labeled;
import javafx.scene.control.OverrunStyle;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Chữ chạy cho nhãn hẹp: khi cửa sổ thu nhỏ làm chữ không vừa, thay vì cắt "…" thì chữ trôi ngang như bảng
 * điện tử. Vừa chỗ thì đứng yên như cũ. {@code hoverOnly}: chỉ chạy khi rê chuột vào (dùng cho các dòng trong
 * bảng để màn không nhảy múa cùng lúc).
 */
public final class Marquee {
    private static final String GAP = "      ";
    private static final Duration STEP = Duration.millis(170);
    /** Đứng yên một lúc ở đầu chữ trước khi chạy tiếp vòng mới. */
    private static final int PAUSE_STEPS = 8;

    private final Labeled label;
    private final boolean hoverOnly;
    private final Timeline timeline;
    private String original;
    private int offset;
    private int pause;
    private boolean writing;

    private Marquee(Labeled label, boolean hoverOnly) {
        this.label = label;
        this.hoverOnly = hoverOnly;
        this.original = label.getText() == null ? "" : label.getText();
        timeline = new Timeline(new KeyFrame(STEP, e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        label.textProperty().addListener((o, a, b) -> {
            if (writing) return;
            original = b == null ? "" : b; // đổi chữ từ ngoài (đổi ngôn ngữ, đổi tên…)
            restart();
        });
        label.widthProperty().addListener((o, a, b) -> restart());
        label.fontProperty().addListener((o, a, b) -> restart());
        if (hoverOnly) {
            label.hoverProperty().addListener((o, a, b) -> restart());
        }
        label.sceneProperty().addListener((o, a, b) -> {
            if (b == null) timeline.stop();
            else restart();
        });
    }

    public static void install(Labeled label) {
        new Marquee(label, false);
    }

    public static void installOnHover(Labeled label) {
        new Marquee(label, true);
    }

    private double available() {
        double w = label.getWidth() - label.snappedLeftInset() - label.snappedRightInset();
        if (label.getGraphic() != null && label.getContentDisplay() != ContentDisplay.TEXT_ONLY) {
            w -= label.getGraphic().prefWidth(-1) + label.getGraphicTextGap();
        }
        return w;
    }

    private double textWidth(String s) {
        Text t = new Text(s);
        t.setFont(label.getFont());
        return t.getLayoutBounds().getWidth();
    }

    private boolean overflowing() {
        return label.getWidth() > 0 && textWidth(original) > available() + 0.5;
    }

    private void restart() {
        offset = 0;
        pause = PAUSE_STEPS;
        boolean run = label.getScene() != null && overflowing() && (!hoverOnly || label.isHover());
        if (run) {
            label.setTextOverrun(OverrunStyle.CLIP);
            show(original);
            timeline.play();
        } else {
            timeline.stop();
            label.setTextOverrun(OverrunStyle.ELLIPSIS);
            show(original);
        }
    }

    private void tick() {
        if (pause > 0) {
            pause--;
            return;
        }
        String loop = original + GAP;
        offset = (offset + 1) % loop.length();
        if (offset == 0) pause = PAUSE_STEPS;
        show(loop.substring(offset) + loop.substring(0, offset));
    }

    private void show(String s) {
        writing = true;
        try {
            label.setText(s);
        } finally {
            writing = false;
        }
    }
}
