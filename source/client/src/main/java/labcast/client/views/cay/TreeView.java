package labcast.client.views.cay;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javafx.animation.FadeTransition;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import labcast.client.player.View;
import labcast.client.player.VizPalette;
import labcast.client.views.VizStage;
import labcast.common.trace.TraceEvent;

/**
 * Bộ vẽ {@code cay} (spec §7.4): cây nhị phân, cột theo duyệt giữa, tầng theo độ sâu. Nút đang xét (vừa đọc
 * khoá) tô vàng, nút vừa tạo hay vừa đổi khoá tô xanh dương, màu đánh dấu ({@code T.danh_dau}) giữ nguyên. Cây
 * đổi hình (chèn, xoay AVL) thì các nút trượt từ chỗ cũ tới chỗ mới, cạnh bám theo nút.
 */
public final class TreeView implements View {
    private static final Logger LOG = Logger.getLogger(TreeView.class.getName());

    private static final double TOP = 34;
    private static final double LEVEL = 58;
    private static final double MAX_STEP = 56;

    private final String name;
    private final TreeModel model = new TreeModel();
    private final VizStage stage = new VizStage(() -> draw(false, null, -1, null));
    /** Chỗ vẽ lần trước của từng nút, để trượt từ đó tới chỗ mới. */
    private final Map<Integer, double[]> lastPos = new HashMap<>();

    public TreeView(String name) {
        this.name = name;
        draw(false, null, -1, null);
    }

    @Override
    public String kind() {
        return "cay";
    }

    @Override
    public Node node() {
        return stage.node();
    }

    @Override
    public void reset(JsonNode snapshot) {
        model.reset(snapshot);
        lastPos.clear();
        draw(false, null, -1, null);
    }

    @Override
    public void apply(TraceEvent e, boolean animate, Duration d) {
        List<TreeModel.Change> changes;
        try {
            changes = model.apply(e);
        } catch (IllegalStateException ex) {
            LOG.warning("Trace cây " + name + " sai: " + ex.getMessage());
            draw(false, null, -1, null);
            return;
        }
        if (changes.isEmpty()) {
            return;
        }
        boolean shape = false;
        int hi = -1;
        Color hiColor = null;
        for (TreeModel.Change c : changes) {
            if (c instanceof TreeModel.Shape s) {
                shape = true;
                hi = s.id();
                hiColor = VizPalette.WRITTEN;
            } else if (c instanceof TreeModel.Read r) {
                hi = r.id();
                hiColor = VizPalette.CURRENT;
            } else if (c instanceof TreeModel.Written w) {
                hi = w.id();
                hiColor = VizPalette.WRITTEN;
            }
        }
        draw(animate && shape, d, animate ? hi : -1, hiColor);
    }

    /**
     * Vẽ lại cả cây. {@code slide}: nút đã có trượt từ chỗ cũ (cây vừa đổi hình). {@code hi}: nút tô màu tạm
     * {@code hiColor} (đang xét, vừa ghi), −1 nếu không có.
     */
    private void draw(boolean slide, Duration d, int hi, Color hiColor) {
        stage.clear();
        stage.add(VizStage.title(name + "  (cây)"));
        Map<Integer, TreeModel.Pos> pos = model.layout();
        int cols = 0;
        int depth = 0;
        for (TreeModel.Pos p : pos.values()) {
            cols = Math.max(cols, p.col() + 1);
            depth = Math.max(depth, p.depth());
        }
        double width = stage.width();
        double step = Math.min(MAX_STEP, (width - 30) / Math.max(1, cols));
        double r = Math.max(9, Math.min(19, step * 0.4));
        double x0 = (width - step * cols) / 2 + step / 2;
        stage.height(TOP + (depth + 1) * LEVEL + 10);
        if (pos.isEmpty()) {
            stage.add(VizStage.label("(cây rỗng)", width / 2, TOP + 24, 120));
            lastPos.clear();
            return;
        }
        Group edges = new Group();
        Group nodes = new Group();
        stage.add(edges);
        stage.add(nodes);
        Map<Integer, Group> groupOf = new HashMap<>();
        Map<Integer, double[]> now = new HashMap<>();
        for (Map.Entry<Integer, TreeModel.Pos> en : pos.entrySet()) {
            int id = en.getKey();
            TreeModel.N n = model.node(id);
            double x = x0 + en.getValue().col() * step;
            double y = TOP + r + en.getValue().depth() * LEVEL;
            Group g = VizStage.circle(n.text, r);
            VizStage.fill(g, id == hi && hiColor != null ? hiColor : VizStage.mark(n.mark));
            g.setLayoutX(x);
            g.setLayoutY(y);
            nodes.getChildren().add(g);
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
        if (model.root() != null && groupOf.containsKey(model.root())) {
            double[] rp = now.get(model.root());
            stage.add(VizStage.label("gốc", rp[0], rp[1] - r - 6, 40));
        }
        for (Map.Entry<Integer, Group> en : groupOf.entrySet()) {
            TreeModel.N n = model.node(en.getKey());
            for (Integer c : new Integer[] {n.left, n.right}) {
                Group child = c == null ? null : groupOf.get(c);
                if (child != null) {
                    edges.getChildren().add(edge(en.getValue(), child));
                }
            }
        }
        lastPos.clear();
        lastPos.putAll(now);
    }

    /** Cạnh cha → con, đầu mút bám theo vị trí đang trượt của hai nút. */
    private static Line edge(Group from, Group to) {
        Line l = new Line();
        l.setStroke(VizStage.EDGE);
        l.setStrokeWidth(2);
        l.startXProperty().bind(from.layoutXProperty().add(from.translateXProperty()));
        l.startYProperty().bind(from.layoutYProperty().add(from.translateYProperty()));
        l.endXProperty().bind(to.layoutXProperty().add(to.translateXProperty()));
        l.endYProperty().bind(to.layoutYProperty().add(to.translateYProperty()));
        return l;
    }

    @Override
    public String toString() {
        return "TreeView(" + name + ")";
    }
}
