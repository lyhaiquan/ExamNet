package labcast.client.views.dothi;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import labcast.client.player.View;
import labcast.client.player.VizPalette;
import labcast.client.views.VizStage;
import labcast.common.trace.TraceEvent;

/**
 * Bộ vẽ {@code do-thi} (spec §7.4): đỉnh xếp quanh vòng tròn (hoặc chỗ người soạn bài đặt), cạnh vô hướng là
 * đoạn thẳng, có hướng là mũi tên, trọng số ghi giữa cạnh. Đỉnh đang xét ({@code g.ke(u)}) tô vàng, cạnh đang
 * xét tô vàng và dày lên; màu đánh dấu giữ nguyên; chữ dưới đỉnh là nhãn ({@code g.nhan}), ví dụ khoảng cách.
 */
public final class GraphView implements View {
    private static final double TOP = 26;
    private static final double MAX_H = 330;

    private final String name;
    private final GraphModel model = new GraphModel();
    private final VizStage stage = new VizStage(() -> draw(null, null));

    public GraphView(String name) {
        this.name = name;
        draw(null, null);
    }

    @Override
    public String kind() {
        return "do-thi";
    }

    @Override
    public Node node() {
        return stage.node();
    }

    @Override
    public void reset(JsonNode snapshot) {
        model.reset(snapshot);
        draw(null, null);
    }

    @Override
    public void apply(TraceEvent e, boolean animate, Duration d) {
        List<GraphModel.Change> changes = model.apply(e);
        if (changes.isEmpty()) {
            return;
        }
        String hiVertex = null;
        GraphModel.ReadEdge hiEdge = null;
        if (animate) {
            for (GraphModel.Change c : changes) {
                if (c instanceof GraphModel.ReadVertex r) {
                    hiVertex = r.id();
                } else if (c instanceof GraphModel.ReadEdge r) {
                    hiEdge = r;
                } else if (c instanceof GraphModel.Label l) {
                    hiVertex = l.id();
                }
            }
        }
        draw(hiVertex, hiEdge);
    }

    /** Vẽ lại cả đồ thị; {@code hiVertex}/{@code hiEdge}: đỉnh, cạnh đang xét (tô vàng tạm). */
    private void draw(String hiVertex, GraphModel.ReadEdge hiEdge) {
        stage.clear();
        stage.add(VizStage.title(name + (model.directed() ? "  (đồ thị có hướng)" : "  (đồ thị)")));
        Map<String, double[]> pos = model.layout();
        double width = stage.width();
        double h = Math.min(MAX_H, Math.max(220, width * 0.6));
        double size = Math.min(width - 60, h - 40);
        double ox = (width - size) / 2;
        double oy = TOP + 10;
        stage.height(oy + size + 34);
        if (pos.isEmpty()) {
            stage.add(VizStage.label("(chưa có đỉnh)", width / 2, TOP + 30, 140));
            return;
        }
        int n = pos.size();
        double r = Math.max(11, Math.min(20, size / (n * 1.4)));
        Group edges = new Group();
        Group nodes = new Group();
        stage.add(edges);
        stage.add(nodes);
        for (GraphModel.E e : model.edges()) {
            double[] a = pos.get(e.a);
            double[] b = pos.get(e.b);
            if (a == null || b == null) {
                continue;
            }
            boolean hot = hiEdge != null
                    && ((hiEdge.a().equals(e.a) && hiEdge.b().equals(e.b))
                            || (!model.directed()
                                    && hiEdge.a().equals(e.b)
                                    && hiEdge.b().equals(e.a)));
            Color c = hot ? VizPalette.CURRENT : e.mark != null ? VizStage.mark(e.mark) : VizStage.OUTLINE;
            double w = hot || e.mark != null ? 3.5 : 2;
            double x1 = ox + a[0] * size;
            double y1 = oy + a[1] * size;
            double x2 = ox + b[0] * size;
            double y2 = oy + b[1] * size;
            if (e.a.equals(e.b)) {
                continue; // khuyên: bỏ qua cho gọn
            }
            edges.getChildren().add(VizStage.arrow(x1, y1, x2, y2, r, r + 1, c, w, model.directed()));
            if (e.weight != null) {
                Text t = VizStage.label(e.weight, (x1 + x2) / 2, (y1 + y2) / 2 - 4, 40);
                t.setFill(Color.web("#334155"));
                edges.getChildren().add(t);
            }
        }
        for (Map.Entry<String, double[]> en : pos.entrySet()) {
            String v = en.getKey();
            double x = ox + en.getValue()[0] * size;
            double y = oy + en.getValue()[1] * size;
            Group g = VizStage.circle(v, r);
            String m = model.mark(v);
            Color c = v.equals(hiVertex) ? VizPalette.CURRENT : VizStage.mark(m);
            VizStage.fill(g, c);
            if (hiEdge != null && v.equals(hiEdge.b())) {
                ((javafx.scene.shape.Circle) g.getChildren().get(0)).setStroke(VizPalette.CURRENT);
                ((javafx.scene.shape.Circle) g.getChildren().get(0)).setStrokeWidth(3);
            }
            g.setLayoutX(x);
            g.setLayoutY(y);
            nodes.getChildren().add(g);
            String lab = model.label(v);
            if (lab != null) {
                Text t = VizStage.label(lab, x, y + r + 14, 60);
                t.setFill(VizStage.TEXT_DARK);
                nodes.getChildren().add(t);
            }
        }
    }

    @Override
    public String toString() {
        return "GraphView(" + name + ")";
    }
}
