package labcast.client.views.caygoi;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.FadeTransition;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import labcast.client.player.View;
import labcast.client.player.VizPalette;
import labcast.client.views.VizStage;
import labcast.common.trace.TraceEvent;

/**
 * Bộ vẽ {@code cay-goi} (spec §7.4): mỗi lời gọi là một ô "fib(3)", con nằm dưới cha. Lời gọi đang chạy tô
 * vàng, các lời gọi đang chờ nó (trên ngăn) viền vàng, đã trả về thì xanh lá kèm kết quả "→ 2" bên dưới; nhánh
 * bị cắt xám, nhánh quay lui đỏ. Cây lớn thì ô nhỏ lại cho vừa bề ngang.
 */
public final class CallTreeView implements View {
    private static final double TOP = 30;
    private static final double LEVEL = 54;
    private static final double BOX_H = 26;

    private final String name;
    private final CallTreeModel model = new CallTreeModel();
    private final VizStage stage = new VizStage(() -> draw(false, null));
    private final Map<Integer, double[]> lastPos = new HashMap<>();

    public CallTreeView(String name) {
        this.name = name;
        draw(false, null);
    }

    @Override
    public String kind() {
        return "cay-goi";
    }

    @Override
    public Node node() {
        return stage.node();
    }

    @Override
    public void reset(JsonNode snapshot) {
        model.reset(snapshot);
        lastPos.clear();
        draw(false, null);
    }

    @Override
    public void apply(TraceEvent e, boolean animate, Duration d) {
        List<CallTreeModel.Change> changes = model.apply(e);
        if (changes.isEmpty()) {
            return;
        }
        draw(animate && changes.get(0) instanceof CallTreeModel.Called, d);
    }

    private void draw(boolean slide, Duration d) {
        stage.clear();
        Map<Integer, double[]> pos = model.layout();
        double cols = 1;
        double depth = 0;
        for (double[] p : pos.values()) {
            cols = Math.max(cols, p[0] + 1);
            depth = Math.max(depth, p[1]);
        }
        double width = stage.width();
        double step = Math.min(84, (width - 20) / cols);
        double boxW = Math.max(16, step * 0.92);
        // chữ nhỏ nhất của ô là cỡ 9 (VizStage.fontSize), mỗi ký tự rộng chừng 0,62 cỡ chữ
        int maxChars = (int) ((boxW - 4) / (0.62 * 9));
        boolean compact =
                pos.keySet().stream().anyMatch(id -> model.node(id).label.length() > maxChars);
        stage.add(VizStage.title("Cây lời gọi " + name + "(…)" + (compact ? "  · ô chỉ ghi tham số" : "")));
        double x0 = (width - step * cols) / 2 + step / 2;
        stage.height(TOP + (depth + 1) * LEVEL + 16);
        if (pos.isEmpty()) {
            stage.add(VizStage.label("(chưa gọi " + name + ")", width / 2, TOP + 24, 200));
            lastPos.clear();
            return;
        }
        Group edges = new Group();
        Group boxes = new Group();
        stage.add(edges);
        stage.add(boxes);
        Map<Integer, Group> groupOf = new HashMap<>();
        Map<Integer, double[]> now = new HashMap<>();
        int cur = model.current();
        for (Map.Entry<Integer, double[]> en : pos.entrySet()) {
            int id = en.getKey();
            CallTreeModel.N n = model.node(id);
            double x = x0 + en.getValue()[0] * step;
            double y = TOP + BOX_H / 2 + en.getValue()[1] * LEVEL;
            Group g = callBox(
                    n,
                    compact ? CallTreeModel.argsOnly(n.label, maxChars) : n.label,
                    id == cur,
                    model.onStack(id),
                    boxW);
            g.setLayoutX(x);
            g.setLayoutY(y);
            boxes.getChildren().add(g);
            groupOf.put(id, g);
            now.put(id, new double[] {x, y});
            double[] old = lastPos.get(id);
            if (slide && old != null) {
                stage.slide(g, old[0] - x, old[1] - y, d);
            } else if (slide) {
                FadeTransition f = new FadeTransition(VizStage.fx(d), g);
                f.setFromValue(0);
                f.setToValue(1);
                stage.play(f);
            }
        }
        for (Map.Entry<Integer, Group> en : groupOf.entrySet()) {
            CallTreeModel.N n = model.node(en.getKey());
            Group parent = n.parent == null ? null : groupOf.get(n.parent);
            if (parent != null) {
                Line l = new Line();
                l.setStroke(VizStage.OUTLINE);
                l.setStrokeWidth(1.5);
                Group child = en.getValue();
                l.startXProperty().bind(parent.layoutXProperty().add(parent.translateXProperty()));
                l.startYProperty().bind(parent.layoutYProperty().add(parent.translateYProperty()));
                l.endXProperty().bind(child.layoutXProperty().add(child.translateXProperty()));
                l.endYProperty().bind(child.layoutYProperty().add(child.translateYProperty()));
                edges.getChildren().add(l);
            }
        }
        lastPos.clear();
        lastPos.putAll(now);
    }

    /** Ô một lời gọi, toạ độ của Group là tâm ô; kết quả ghi dưới ô. */
    private static Group callBox(CallTreeModel.N n, String label, boolean current, boolean onStack, double w) {
        Group box = VizStage.box(label, w, BOX_H);
        Color fill;
        if (current) {
            fill = VizPalette.CURRENT;
        } else if (onStack) {
            fill = Color.WHITE;
        } else {
            fill = switch (n.state) {
                case "xong" -> VizPalette.DONE;
                case "bo" -> VizPalette.SKIPPED;
                case "loai" -> VizPalette.REJECTED;
                default -> VizPalette.NORMAL;};
        }
        VizStage.fill(box, fill);
        if (onStack && !current) {
            Rectangle r = (Rectangle) box.getChildren().get(0);
            r.setStroke(VizPalette.CURRENT);
            r.setStrokeWidth(2.5);
        }
        box.setLayoutX(-w / 2);
        box.setLayoutY(-BOX_H / 2);
        Group g = new Group(box);
        if (n.ret != null) {
            Text t = VizStage.label("→ " + n.ret, 0, BOX_H / 2 + 13, Math.max(w, 50));
            t.setFill(VizStage.TEXT_DARK);
            g.getChildren().add(t);
        }
        return g;
    }

    @Override
    public String toString() {
        return "CallTreeView(" + name + ")";
    }
}
