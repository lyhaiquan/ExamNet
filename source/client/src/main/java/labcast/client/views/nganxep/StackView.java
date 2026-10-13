package labcast.client.views.nganxep;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import labcast.client.player.View;
import labcast.client.player.VizPalette;
import labcast.common.trace.TraceEvent;

/**
 * Bộ vẽ {@code ngan-xep} (spec §7.4, task 4.8): ngăn xếp vẽ đứng trong một cái hộp hở miệng, đỉnh ở trên;
 * hàng đợi vẽ nằm ngang trong một ống, đầu bên trái, cuối bên phải. Vào thì ô mới trượt vào (xanh dương),
 * lấy ra thì ô trượt ra rồi mờ đi, giá trị vừa lấy ra ghi bên cạnh. Quá nhiều phần tử thì gộp phần giữa
 * thành một ô "…", luôn thấy đỉnh (ngăn xếp) hoặc hai đầu (hàng đợi).
 */
public final class StackView implements View {
    private static final Logger LOG = Logger.getLogger(StackView.class.getName());

    private static final double BOX_H = 30;
    private static final double GAP = 4;
    /** Ngăn xếp: số ô vẽ được, kể cả ô "…". */
    private static final int MAX_STACK = 8;

    private static final double TOP = 26;
    private static final double STACK_BASE = TOP + MAX_STACK * (BOX_H + GAP) + 6;
    private static final double QUEUE_Y = TOP + 16;
    private static final double SLIDE = 46;
    private static final Color OUTLINE = Color.web("#94a3b8");
    private static final Color TEXT_DARK = Color.web("#1e293b");

    private final String name;
    private final boolean queueHint;
    private final StackModel model = new StackModel();
    private final VBox root = new VBox(4);
    private final Pane canvas = new Pane();
    private final Text title = new Text();
    private final Text lastOut = new Text();

    /** Ô đang vẽ, theo chỉ số phần tử; null = phần tử nằm trong ô "…" nên không vẽ riêng. */
    private final List<Group> boxOf = new ArrayList<>();

    private final List<Node> drawn = new ArrayList<>();
    private final List<Animation> running = new ArrayList<>();
    private final List<Node> ghosts = new ArrayList<>();

    private double boxW = 60;
    private double width = 480;

    /** @param queueHint bài khai báo {@code kieu: hang-doi}: vẽ ngang ngay từ đầu, kể cả khi còn rỗng */
    public StackView(String name, boolean queueHint) {
        this.name = name;
        this.queueHint = queueHint;
        title.getStyleClass().add("viz-index");
        title.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        lastOut.getStyleClass().add("viz-pointer");
        canvas.setMaxWidth(Double.MAX_VALUE);
        canvas.widthProperty().addListener((obs, o, n) -> rebuild());
        root.getChildren().add(canvas);
        root.setFillWidth(true);
        sizeCanvas();
    }

    @Override
    public String kind() {
        return "ngan-xep";
    }

    @Override
    public Node node() {
        return root;
    }

    @Override
    public void reset(JsonNode snapshot) {
        stopAll();
        model.reset(snapshot, queueHint);
        lastOut.setText("");
        rebuild();
    }

    @Override
    public void apply(TraceEvent e, boolean animate, Duration d) {
        stopAll();
        List<StackModel.Change> changes;
        try {
            changes = model.apply(e);
        } catch (IllegalStateException ex) {
            LOG.warning("Trace ngăn xếp " + name + " sai: " + ex.getMessage());
            rebuild();
            return;
        }
        for (StackModel.Change c : changes) {
            if (c instanceof StackModel.Pop p) {
                lastOut.setText("vừa lấy ra: " + p.text());
            } else if (c instanceof StackModel.Push) {
                lastOut.setText("");
            }
        }
        if (!animate) {
            rebuild();
            return;
        }
        for (StackModel.Change c : changes) {
            if (c instanceof StackModel.Push p) {
                rebuild();
                Group g = box(p.index());
                if (g != null) {
                    fill(g, VizPalette.WRITTEN);
                    slideIn(g, d);
                }
            } else if (c instanceof StackModel.Pop p) {
                double[] from = slotXY(
                        model.queue() ? 0 : model.items().size(), model.items().size() + 1);
                rebuild();
                ghostOut(p.text(), from, d);
                if (model.queue()) {
                    shiftQueueLeft(d);
                }
            } else if (c instanceof StackModel.Read r) {
                clearHighlights();
                Group g = box(r.i());
                if (g != null) {
                    fill(g, VizPalette.CURRENT);
                }
            }
        }
    }

    // ---------------- bố trí ----------------

    private void sizeCanvas() {
        double h = model.queue() ? QUEUE_Y + BOX_H + 40 : STACK_BASE + 24;
        canvas.setPrefHeight(h);
        canvas.setMinHeight(h);
    }

    /** Dựng lại toàn bộ theo model, không animation. */
    private void rebuild() {
        stopAll();
        sizeCanvas();
        width = canvas.getWidth() > 0 ? canvas.getWidth() : 480;
        canvas.getChildren().removeAll(drawn);
        drawn.clear();
        boxOf.clear();
        List<String> items = model.items();
        boxW = boxWidth(items);
        title.setText(name + (model.queue() ? "  (hàng đợi)" : "  (ngăn xếp)"));
        add(title);
        if (model.queue()) {
            layoutQueue(items);
        } else {
            layoutStack(items);
        }
        add(lastOut);
    }

    private double boxWidth(List<String> items) {
        double w = 0;
        for (String s : items) {
            Text t = new Text(s);
            t.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, 15));
            w = Math.max(w, t.getLayoutBounds().getWidth());
        }
        return model.queue() ? clamp(w + 20, 40, 160) : clamp(w + 28, 64, 220);
    }

    private void layoutStack(List<String> items) {
        int n = items.size();
        double x0 = (width - boxW) / 2;
        title.setX(x0);
        title.setY(14);
        // hộp hở miệng: hai thành và đáy
        double left = x0 - 6;
        double right = x0 + boxW + 6;
        double top = TOP - 2;
        add(line(left, top, left, STACK_BASE));
        add(line(right, top, right, STACK_BASE));
        add(line(left, STACK_BASE, right, STACK_BASE));

        boolean folded = n > MAX_STACK;
        int hidden = folded ? n - (MAX_STACK - 1) : 0;
        for (int i = 0; i < n; i++) {
            boxOf.add(null);
        }
        if (folded) {
            Group more = boxNode("… " + hidden + " phần tử", true);
            place(more, slotXY(0, n));
            add(more);
        }
        for (int i = hidden; i < n; i++) {
            Group g = boxNode(items.get(i), false);
            int slot = folded ? i - hidden + 1 : i;
            place(g, xy(x0, slot));
            boxOf.set(i, g);
            add(g);
        }
        if (n > 0) {
            int topSlot = folded ? MAX_STACK - 1 : n - 1;
            Text arrow = new Text("đỉnh ➜");
            arrow.getStyleClass().add("viz-pointer");
            arrow.setX(x0 - 70);
            arrow.setY(xy(x0, topSlot)[1] + BOX_H / 2 + 5);
            add(arrow);
        } else {
            Text empty = new Text("(rỗng)");
            empty.getStyleClass().add("viz-index");
            empty.setX(x0 + boxW / 2 - 18);
            empty.setY(STACK_BASE - 12);
            add(empty);
        }
        lastOut.setX(right + 18);
        lastOut.setY(TOP + 14);
    }

    private double[] xy(double x0, int slot) {
        return new double[] {x0, STACK_BASE - 4 - (slot + 1) * (BOX_H + GAP) + GAP};
    }

    /** Toạ độ ô thứ {@code slot} khi đang có {@code n} phần tử (dùng cho ô ma lúc lấy ra). */
    private double[] slotXY(int slot, int n) {
        if (model.queue()) {
            int cap = queueCapacity();
            int s = n > cap ? Math.min(slot, cap - 1) : slot;
            return new double[] {queueX0(Math.min(n, cap)) + s * (boxW + GAP), QUEUE_Y};
        }
        int s = Math.min(slot, MAX_STACK - 1);
        return xy((width - boxW) / 2, s);
    }

    private int queueCapacity() {
        return Math.max(3, (int) ((width - 120) / (boxW + GAP)));
    }

    private double queueX0(int visible) {
        double total = visible * boxW + Math.max(0, visible - 1) * GAP;
        return Math.max(40, (width - total) / 2);
    }

    private void layoutQueue(List<String> items) {
        int n = items.size();
        int cap = queueCapacity();
        boolean folded = n > cap;
        int visible = folded ? cap : Math.max(n, 1);
        double x0 = queueX0(visible);
        title.setX(x0);
        title.setY(14);
        double tubeL = x0 - 12;
        double tubeR = x0 + visible * (boxW + GAP) - GAP + 12;
        add(line(tubeL, QUEUE_Y - 5, tubeR, QUEUE_Y - 5));
        add(line(tubeL, QUEUE_Y + BOX_H + 5, tubeR, QUEUE_Y + BOX_H + 5));
        for (int i = 0; i < n; i++) {
            boxOf.add(null);
        }
        if (n == 0) {
            Text empty = new Text("(rỗng)");
            empty.getStyleClass().add("viz-index");
            empty.setX(x0 + 4);
            empty.setY(QUEUE_Y + BOX_H / 2 + 5);
            add(empty);
        }
        // gộp phần giữa: giữ (cap − 2) phần tử đầu, ô "…", phần tử cuối
        int head = folded ? cap - 2 : n;
        for (int i = 0; i < head; i++) {
            Group g = boxNode(items.get(i), false);
            place(g, new double[] {x0 + i * (boxW + GAP), QUEUE_Y});
            boxOf.set(i, g);
            add(g);
        }
        if (folded) {
            Group more = boxNode("…" + (n - head - 1), true);
            place(more, new double[] {x0 + head * (boxW + GAP), QUEUE_Y});
            add(more);
            Group last = boxNode(items.get(n - 1), false);
            place(last, new double[] {x0 + (cap - 1) * (boxW + GAP), QUEUE_Y});
            boxOf.set(n - 1, last);
            add(last);
        }
        if (n > 0) {
            add(label("đầu", x0 + boxW / 2, QUEUE_Y + BOX_H + 22));
            double lastX = x0 + (folded ? cap - 1 : n - 1) * (boxW + GAP);
            if (n > 1) {
                add(label("cuối", lastX + boxW / 2, QUEUE_Y + BOX_H + 22));
            }
        }
        lastOut.setX(x0);
        lastOut.setY(QUEUE_Y + BOX_H + 38);
    }

    // ---------------- ô và hiệu ứng ----------------

    private Group boxNode(String text, boolean more) {
        Rectangle r = new Rectangle(boxW, BOX_H, more ? VizPalette.SKIPPED : VizPalette.NORMAL);
        r.setArcWidth(8);
        r.setArcHeight(8);
        if (more) {
            r.getStrokeDashArray().setAll(4.0, 3.0);
            r.setStroke(OUTLINE);
        }
        Text t = new Text(text);
        t.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, more ? 12 : 15));
        t.setFill(more ? Color.web("#64748b") : TEXT_DARK);
        t.setTextAlignment(TextAlignment.CENTER);
        t.setWrappingWidth(boxW);
        t.setY(BOX_H / 2 + (more ? 4 : 5));
        return new Group(r, t);
    }

    private static void place(Group g, double[] xy) {
        g.setLayoutX(xy[0]);
        g.setLayoutY(xy[1]);
    }

    private Group box(int i) {
        return i >= 0 && i < boxOf.size() ? boxOf.get(i) : null;
    }

    /** Tô nền ô; nền xanh đậm (vừa ghi, xong) thì chữ trắng cho dễ đọc. */
    private static void fill(Group g, Color c) {
        if (g.getChildren().get(0) instanceof Rectangle r) {
            r.setFill(c);
        }
        if (g.getChildren().size() > 1 && g.getChildren().get(1) instanceof Text t) {
            boolean dark = c.equals(VizPalette.WRITTEN) || c.equals(VizPalette.DONE);
            t.setFill(dark ? Color.WHITE : TEXT_DARK);
        }
    }

    private void clearHighlights() {
        for (Group g : boxOf) {
            if (g != null) {
                fill(g, VizPalette.NORMAL);
            }
        }
    }

    private void slideIn(Group g, Duration d) {
        TranslateTransition t = new TranslateTransition(fx(d), g);
        if (model.queue()) {
            t.setFromX(SLIDE);
        } else {
            t.setFromY(-SLIDE);
        }
        t.setToX(0);
        t.setToY(0);
        FadeTransition f = new FadeTransition(fx(d), g);
        f.setFromValue(0);
        f.setToValue(1);
        play(new ParallelTransition(t, f));
    }

    /** Ô vừa lấy ra trượt khỏi chỗ cũ (lên trên với ngăn xếp, sang trái với hàng đợi) rồi mờ đi. */
    private void ghostOut(String text, double[] from, Duration d) {
        Group g = boxNode(text, false);
        fill(g, VizPalette.CURRENT);
        place(g, from);
        canvas.getChildren().add(g);
        ghosts.add(g);
        TranslateTransition t = new TranslateTransition(fx(d), g);
        if (model.queue()) {
            t.setByX(-SLIDE);
        } else {
            t.setByY(-SLIDE);
        }
        FadeTransition f = new FadeTransition(fx(d), g);
        f.setFromValue(1);
        f.setToValue(0);
        ParallelTransition p = new ParallelTransition(t, f);
        p.setOnFinished(ev -> {
            canvas.getChildren().remove(g);
            ghosts.remove(g);
        });
        play(p);
    }

    /** Hàng đợi vừa ra một phần tử: các ô còn lại trượt sang trái một ô. */
    private void shiftQueueLeft(Duration d) {
        List<Animation> moves = new ArrayList<>();
        for (Group g : boxOf) {
            if (g != null) {
                TranslateTransition t = new TranslateTransition(fx(d), g);
                t.setFromX(boxW + GAP);
                t.setToX(0);
                moves.add(t);
            }
        }
        if (!moves.isEmpty()) {
            play(new ParallelTransition(moves.toArray(new Animation[0])));
        }
    }

    private void play(Animation a) {
        running.add(a);
        a.setOnFinished(withCleanup(a, a.getOnFinished()));
        a.play();
    }

    private javafx.event.EventHandler<javafx.event.ActionEvent> withCleanup(
            Animation a, javafx.event.EventHandler<javafx.event.ActionEvent> then) {
        return ev -> {
            running.remove(a);
            if (then != null) {
                then.handle(ev);
            }
        };
    }

    /** Tua hay dựng lại giữa chừng: dừng mọi hiệu ứng đang chạy, bỏ các ô ma, trả ô về đúng chỗ. */
    private void stopAll() {
        for (Animation a : List.copyOf(running)) {
            a.stop();
        }
        running.clear();
        canvas.getChildren().removeAll(ghosts);
        ghosts.clear();
        for (Group g : boxOf) {
            if (g != null) {
                g.setTranslateX(0);
                g.setTranslateY(0);
                g.setOpacity(1);
            }
        }
    }

    private void add(Node n) {
        if (!canvas.getChildren().contains(n)) {
            canvas.getChildren().add(n);
        }
        drawn.add(n);
    }

    private static Line line(double x1, double y1, double x2, double y2) {
        Line l = new Line(x1, y1, x2, y2);
        l.setStroke(OUTLINE);
        l.setStrokeWidth(2);
        return l;
    }

    private static Text label(String s, double centerX, double y) {
        Text t = new Text(s);
        t.getStyleClass().add("viz-index");
        t.setTextAlignment(TextAlignment.CENTER);
        t.setWrappingWidth(60);
        t.setX(centerX - 30);
        t.setY(y);
        return t;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static javafx.util.Duration fx(Duration d) {
        return javafx.util.Duration.millis(Math.max(1, d.toMillis()));
    }

    @Override
    public String toString() {
        return "StackView(" + name + ")";
    }
}
