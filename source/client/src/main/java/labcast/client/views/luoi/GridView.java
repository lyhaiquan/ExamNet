package labcast.client.views.luoi;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.Animation;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import labcast.client.player.View;
import labcast.client.player.VizPalette;
import labcast.client.ui.Theme;
import labcast.common.trace.TraceEvent;

/**
 * Bộ vẽ {@code luoi} (spec §7.4, task 3.18): bảng 2 chiều cho quy hoạch động, lưới, ma trận. Ô đang đọc tô
 * vàng, ô vừa ghi tô xanh dương và có mũi tên từ các ô nó phụ thuộc (các ô vừa đọc), ô đã điền tô xanh
 * nhạt, ô được đánh dấu ({@code danh_dau}, ví dụ đường truy vết) tô xanh lá và giữ màu. Hai trục ghi chỉ
 * số, và nhãn nếu bài khai báo (ví dụ hai xâu của bài LCS). Ô co giãn theo bề ngang khung.
 */
public final class GridView implements View {
    private static final double MAX_CELL = 46;
    private static final double MIN_CELL = 18;
    private static final double MAX_GRID_H = 520;
    private static final double TOP = 22;

    private static final Color CELL = Color.web("#f8fafc");
    private static final Color CELL_DONE_BEFORE = Color.web("#dbeafe");
    private static final Color BORDER = Color.web("#cbd5e1");
    private static final Color TEXT = Color.web("#1e293b");
    private static final Color ARROW = Color.web("#d97706");

    private final String name;
    private final GridModel model = new GridModel();
    private final VBox root = new VBox(4);
    private final Pane canvas = new Pane();
    private final Text title = new Text();

    private Rectangle[][] rects = new Rectangle[0][0];
    private Text[][] texts = new Text[0][0];
    private final List<Node> transient_ = new ArrayList<>();
    private final List<int[]> highlighted = new ArrayList<>();
    private final List<Animation> running = new ArrayList<>();

    private double cell = MAX_CELL;
    private double x0;
    private double y0;

    public GridView(String name) {
        this.name = name;
        title.setText(name);
        title.setFont(Font.font(Theme.CODE_FONT, FontWeight.BOLD, 13));
        title.getStyleClass().add("viz-index");
        canvas.setMaxWidth(Double.MAX_VALUE);
        canvas.widthProperty().addListener((obs, o, n) -> rebuild());
        root.getChildren().add(canvas);
        root.setFillWidth(true);
    }

    @Override
    public String kind() {
        return "luoi";
    }

    @Override
    public Node node() {
        return root;
    }

    @Override
    public void reset(JsonNode snapshot) {
        model.reset(snapshot);
        rebuild();
    }

    @Override
    public void apply(TraceEvent e, boolean animate, Duration d) {
        List<GridModel.Change> changes = model.apply(e);
        if (!animate) {
            rebuild();
            return;
        }
        clearTransient();
        for (GridModel.Change c : changes) {
            if (c instanceof GridModel.Read r) {
                highlight(r.i(), r.j(), VizPalette.CURRENT);
            } else if (c instanceof GridModel.Write w) {
                for (GridModel.Cell dep : w.deps()) {
                    highlight(dep.i(), dep.j(), VizPalette.CURRENT);
                    arrow(dep, w.i(), w.j());
                }
                setText(w.i(), w.j(), w.text());
                highlight(w.i(), w.j(), VizPalette.WRITTEN);
                pulse(w.i(), w.j(), d);
            } else if (c instanceof GridModel.Mark m) {
                restyle(m.i(), m.j());
                pulse(m.i(), m.j(), d);
            }
        }
    }

    // ---------------- bố trí ----------------

    private void rebuild() {
        clearTransient();
        canvas.getChildren().clear();
        int rows = model.rows();
        int cols = model.cols();
        boolean labels = model.hasLabels();
        double width = canvas.getWidth() > 0 ? canvas.getWidth() : 480;
        // trục: chỉ số, cộng một hàng / cột nhãn nếu bài có nhãn
        double headerW = labels ? 46 : 26;
        double headerH = labels ? 34 : 18;
        cell = Math.max(MIN_CELL, Math.min(MAX_CELL, (width - headerW - 24) / Math.max(1, cols)));
        if (rows * cell > MAX_GRID_H) {
            cell = Math.max(MIN_CELL, MAX_GRID_H / rows);
        }
        double gridW = cols * cell;
        x0 = Math.max(headerW, (width - gridW) / 2 + headerW / 2);
        y0 = TOP + headerH;
        double h = y0 + rows * cell + 12;
        canvas.setPrefHeight(h);
        canvas.setMinHeight(h);

        title.setX(x0 - headerW);
        title.setY(14);
        canvas.getChildren().add(title);

        for (int j = 0; j < cols; j++) {
            double cx = x0 + j * cell;
            canvas.getChildren().add(axisText(String.valueOf(j), cx, y0 - (labels ? 22 : 5), cell, false));
            String lab = model.colLabel(j);
            if (lab != null) {
                canvas.getChildren().add(axisText(lab, cx, y0 - 5, cell, true));
            }
        }
        for (int i = 0; i < rows; i++) {
            double cy = y0 + i * cell + cell / 2 + 4;
            canvas.getChildren().add(axisText(String.valueOf(i), x0 - headerW, cy, labels ? 22 : headerW - 4, false));
            String lab = model.rowLabel(i);
            if (lab != null) {
                canvas.getChildren().add(axisText(lab, x0 - 24, cy, 20, true));
            }
        }

        rects = new Rectangle[rows][cols];
        texts = new Text[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Rectangle r = new Rectangle(x0 + j * cell, y0 + i * cell, cell, cell);
                r.setStroke(BORDER);
                r.setStrokeWidth(1);
                rects[i][j] = r;
                Text t = new Text();
                t.setTextAlignment(TextAlignment.CENTER);
                t.setWrappingWidth(cell);
                t.setX(x0 + j * cell);
                t.setFill(TEXT);
                texts[i][j] = t;
                canvas.getChildren().addAll(r, t);
                setText(i, j, model.text(i, j));
                restyle(i, j);
            }
        }
    }

    private Text axisText(String s, double x, double baselineY, double w, boolean strong) {
        Text t = new Text(s);
        t.setTextAlignment(TextAlignment.CENTER);
        t.setWrappingWidth(w);
        t.setX(x);
        t.setY(baselineY);
        if (strong) {
            t.setFont(Font.font(Theme.CODE_FONT, FontWeight.BOLD, 14));
            t.setFill(TEXT);
        } else {
            t.getStyleClass().add("viz-index");
        }
        return t;
    }

    /** Chữ trong ô, co cỡ chữ cho vừa ô. */
    private void setText(int i, int j, String s) {
        if (!inside(i, j)) {
            return;
        }
        Text t = texts[i][j];
        double size = Math.max(9, Math.min(14, cell * 0.42));
        int len = Math.max(1, s.length());
        if (len > 2) {
            size = Math.max(8, Math.min(size, (cell - 4) / (0.62 * len)));
        }
        t.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, size));
        t.setText(s);
        t.setY(y0 + i * cell + cell / 2 + size * 0.36);
    }

    /** Màu nền giữ lâu dài của ô: đánh dấu > đã ghi > trống. */
    private void restyle(int i, int j) {
        if (!inside(i, j)) {
            return;
        }
        Color c = model.marked(i, j) ? VizPalette.DONE : model.written(i, j) ? CELL_DONE_BEFORE : CELL;
        rects[i][j].setFill(c);
    }

    private void highlight(int i, int j, Color c) {
        if (inside(i, j)) {
            rects[i][j].setFill(c);
            highlighted.add(new int[] {i, j});
        }
    }

    /** Mũi tên từ tâm ô phụ thuộc tới tâm ô vừa ghi, rút ngắn ở hai đầu để không che chữ. */
    private void arrow(GridModel.Cell from, int ti, int tj) {
        double fx = x0 + from.j() * cell + cell / 2;
        double fy = y0 + from.i() * cell + cell / 2;
        double tx = x0 + tj * cell + cell / 2;
        double ty = y0 + ti * cell + cell / 2;
        double dx = tx - fx;
        double dy = ty - fy;
        double len = Math.hypot(dx, dy);
        if (len < 1) {
            return;
        }
        double ux = dx / len;
        double uy = dy / len;
        double cut = Math.min(cell * 0.32, len / 3);
        double sx = fx + ux * cut;
        double sy = fy + uy * cut;
        double ex = tx - ux * cut;
        double ey = ty - uy * cut;
        Line l = new Line(sx, sy, ex, ey);
        l.setStroke(ARROW);
        l.setStrokeWidth(2);
        double head = Math.max(5, Math.min(9, cell * 0.2));
        Polygon p = new Polygon(
                ex,
                ey,
                ex - ux * head - uy * head * 0.6,
                ey - uy * head + ux * head * 0.6,
                ex - ux * head + uy * head * 0.6,
                ey - uy * head - ux * head * 0.6);
        p.setFill(ARROW);
        canvas.getChildren().addAll(l, p);
        transient_.add(l);
        transient_.add(p);
    }

    private void pulse(int i, int j, Duration d) {
        if (!inside(i, j)) {
            return;
        }
        ScaleTransition s =
                new ScaleTransition(javafx.util.Duration.millis(Math.max(1, d.toMillis() * 0.6)), texts[i][j]);
        s.setFromX(1.35);
        s.setFromY(1.35);
        s.setToX(1);
        s.setToY(1);
        running.add(s);
        s.setOnFinished(ev -> running.remove(s));
        s.play();
    }

    /** Bỏ mũi tên và màu tạm của bước trước, dừng hiệu ứng đang chạy. */
    private void clearTransient() {
        for (Animation a : List.copyOf(running)) {
            a.stop();
        }
        running.clear();
        canvas.getChildren().removeAll(transient_);
        transient_.clear();
        for (int[] c : highlighted) {
            restyle(c[0], c[1]);
        }
        highlighted.clear();
        for (Text[] row : texts) {
            for (Text t : row) {
                t.setScaleX(1);
                t.setScaleY(1);
            }
        }
    }

    private boolean inside(int i, int j) {
        return i >= 0 && j >= 0 && i < rects.length && j < rects[i].length;
    }

    @Override
    public String toString() {
        return "GridView(" + name + ")";
    }
}
