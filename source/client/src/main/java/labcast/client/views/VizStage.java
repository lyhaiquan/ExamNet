package labcast.client.views;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.Animation;
import javafx.animation.TranslateTransition;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import labcast.client.player.VizPalette;
import labcast.client.ui.Theme;

/**
 * Phần chung của các bộ vẽ dạng nút và cạnh (cây, heap, đồ thị, danh sách liên kết, bảng băm, cây lời gọi):
 * một khung vẽ rộng theo trình phát, các hiệu ứng đang chạy (để tua thì dừng hết), và cách vẽ nút, mũi tên
 * giống nhau giữa các bộ vẽ. Mỗi bộ vẽ giữ một {@code VizStage} và tự dựng hình trong {@link #canvas()}.
 */
public final class VizStage {
    public static final Color OUTLINE = Color.web("#94a3b8");
    public static final Color EDGE = Color.web("#64748b");
    public static final Color TEXT_DARK = Color.web("#1e293b");

    private final VBox root = new VBox(4);
    private final Pane canvas = new Pane();
    private final List<Animation> running = new ArrayList<>();

    /** @param onResize gọi khi khung đổi bề ngang (bộ vẽ dựng lại hình cho vừa) */
    public VizStage(Runnable onResize) {
        canvas.setMaxWidth(Double.MAX_VALUE);
        canvas.widthProperty().addListener((obs, o, n) -> onResize.run());
        root.getChildren().add(canvas);
        root.setFillWidth(true);
    }

    public Node node() {
        return root;
    }

    public Pane canvas() {
        return canvas;
    }

    /** Bề ngang đang có; khung chưa hiện thì lấy 480 như các bộ vẽ khác. */
    public double width() {
        return canvas.getWidth() > 0 ? canvas.getWidth() : 480;
    }

    /** Đặt chiều cao khung vẽ (khung cuộn dọc của trình phát lo phần dài hơn màn hình). */
    public void height(double h) {
        canvas.setPrefHeight(h);
        canvas.setMinHeight(h);
    }

    /** Xoá hết hình, dừng mọi hiệu ứng. */
    public void clear() {
        stopAll();
        canvas.getChildren().clear();
    }

    public void add(Node n) {
        canvas.getChildren().add(n);
    }

    public void play(Animation a) {
        running.add(a);
        var then = a.getOnFinished();
        a.setOnFinished(ev -> {
            running.remove(a);
            if (then != null) {
                then.handle(ev);
            }
        });
        a.play();
    }

    public void stopAll() {
        for (Animation a : List.copyOf(running)) {
            a.stop();
        }
        running.clear();
    }

    public static javafx.util.Duration fx(Duration d) {
        return javafx.util.Duration.millis(Math.max(1, d.toMillis()));
    }

    /** Nút trượt từ chỗ cũ tới chỗ mới (đặt sẵn layoutX/Y ở chỗ mới, dịch ngược rồi trượt về 0). */
    public void slide(Node n, double fromDx, double fromDy, Duration d) {
        if (fromDx == 0 && fromDy == 0) {
            return;
        }
        TranslateTransition t = new TranslateTransition(fx(d), n);
        t.setFromX(fromDx);
        t.setFromY(fromDy);
        t.setToX(0);
        t.setToY(0);
        play(t);
    }

    // ---------------- hình dùng chung ----------------

    /** Nút tròn có chữ ở giữa; toạ độ của Group là tâm. Phần tử 0 là hình tròn, phần tử 1 là chữ. */
    public static Group circle(String text, double r) {
        Circle c = new Circle(r, VizPalette.NORMAL);
        c.setStroke(Color.WHITE);
        c.setStrokeWidth(1.5);
        Text t = centered(text, r * 2.4, fontSize(text, r * 2));
        t.setX(-r * 1.2);
        t.setY(t.getFont().getSize() * 0.35);
        return new Group(c, t);
    }

    /** Ô chữ nhật có chữ ở giữa; toạ độ của Group là góc trên trái. Phần tử 0 là ô, phần tử 1 là chữ. */
    public static Group box(String text, double w, double h) {
        Rectangle r = new Rectangle(w, h, VizPalette.NORMAL);
        r.setArcWidth(8);
        r.setArcHeight(8);
        Text t = centered(text, w, fontSize(text, w));
        t.setY(h / 2 + t.getFont().getSize() * 0.35);
        return new Group(r, t);
    }

    /** Cỡ chữ vừa ô rộng {@code w}: chữ dài thì nhỏ lại, không nhỏ hơn 9. */
    public static double fontSize(String text, double w) {
        double size = Math.min(15, w * 0.42);
        int len = Math.max(1, text.length());
        return Math.max(9, Math.min(size, (w - 4) / (0.62 * len)));
    }

    public static Text centered(String s, double w, double size) {
        Text t = new Text(s);
        t.setFont(Font.font(Theme.UI_FONT, FontWeight.BOLD, size));
        t.setFill(TEXT_DARK);
        t.setTextAlignment(TextAlignment.CENTER);
        t.setWrappingWidth(w);
        return t;
    }

    /** Tô nền nút; nền đậm (vừa ghi, xong, bị loại) thì chữ trắng cho dễ đọc. */
    public static void fill(Group g, Color c) {
        if (g.getChildren().get(0) instanceof Shape s) {
            s.setFill(c);
        }
        if (g.getChildren().size() > 1 && g.getChildren().get(1) instanceof Text t) {
            boolean dark = c.equals(VizPalette.WRITTEN) || c.equals(VizPalette.DONE) || c.equals(VizPalette.REJECTED);
            t.setFill(dark ? Color.WHITE : TEXT_DARK);
        }
    }

    /** Màu của tên trạng thái trong trace ({@code dang-xet}, {@code xong}, {@code loai}, {@code bo}). */
    public static Color mark(String m) {
        if (m == null) {
            return VizPalette.NORMAL;
        }
        return switch (m) {
            case "dang-xet" -> VizPalette.CURRENT;
            case "xong" -> VizPalette.DONE;
            case "loai" -> VizPalette.REJECTED;
            case "bo" -> VizPalette.SKIPPED;
            case "ghi" -> VizPalette.WRITTEN;
            default -> VizPalette.NORMAL;
        };
    }

    public static Line line(double x1, double y1, double x2, double y2, Color c, double w) {
        Line l = new Line(x1, y1, x2, y2);
        l.setStroke(c);
        l.setStrokeWidth(w);
        return l;
    }

    /**
     * Mũi tên từ (x1, y1) tới (x2, y2), lùi lại {@code cutStart}/{@code cutEnd} ở hai đầu để không đè lên nút.
     * {@code head} false thì chỉ là đoạn thẳng (cạnh vô hướng).
     */
    public static Group arrow(
            double x1,
            double y1,
            double x2,
            double y2,
            double cutStart,
            double cutEnd,
            Color c,
            double w,
            boolean head) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) {
            return new Group();
        }
        double ux = dx / len;
        double uy = dy / len;
        double sx = x1 + ux * cutStart;
        double sy = y1 + uy * cutStart;
        double ex = x2 - ux * cutEnd;
        double ey = y2 - uy * cutEnd;
        Group g = new Group(line(sx, sy, ex, ey, c, w));
        if (head) {
            double hs = 5 + w * 1.5;
            Polygon p = new Polygon(
                    ex,
                    ey,
                    ex - ux * hs - uy * hs * 0.6,
                    ey - uy * hs + ux * hs * 0.6,
                    ex - ux * hs + uy * hs * 0.6,
                    ey - uy * hs - ux * hs * 0.6);
            p.setFill(c);
            g.getChildren().add(p);
        }
        return g;
    }

    /** Chữ nhỏ màu nhạt (chỉ số, nhãn), căn giữa quanh {@code centerX}. */
    public static Text label(String s, double centerX, double y, double w) {
        Text t = new Text(s);
        t.getStyleClass().add("viz-index");
        t.setTextAlignment(TextAlignment.CENTER);
        t.setWrappingWidth(w);
        t.setX(centerX - w / 2);
        t.setY(y);
        return t;
    }

    /** Tiêu đề bộ vẽ (tên biến) ở góc trên trái. */
    public static Text title(String s) {
        Text t = new Text(s);
        t.getStyleClass().add("viz-index");
        t.setFont(Font.font(Theme.CODE_FONT, FontWeight.BOLD, 13));
        t.setX(4);
        t.setY(14);
        return t;
    }
}
