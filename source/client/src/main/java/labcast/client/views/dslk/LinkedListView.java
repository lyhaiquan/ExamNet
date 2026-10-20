package labcast.client.views.dslk;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javafx.animation.FadeTransition;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import labcast.client.player.View;
import labcast.client.player.VizPalette;
import labcast.client.views.VizStage;
import labcast.common.trace.TraceEvent;

/**
 * Bộ vẽ {@code dslk} (spec §7.4): mỗi nút là ô hai ngăn [giá trị | con trỏ tiếp], mũi tên từ ngăn con trỏ tới
 * nút sau; con trỏ None thì gạch chéo ngăn. Nút kề bên phải nối bằng mũi tên thẳng, còn lại (bỏ qua nút, trỏ
 * ngược khi đảo danh sách) bằng mũi tên cong bên dưới. Trên mỗi nút ghi "đầu" và tên các biến đang trỏ vào nó
 * ({@code prev}, {@code cur}…). Nút không còn ai trỏ tới (vừa bị xoá ra) vẽ mờ.
 */
public final class LinkedListView implements View {
    private static final Logger LOG = Logger.getLogger(LinkedListView.class.getName());

    private static final double NODE_Y = 96;
    private static final double BOX_H = 34;
    private static final double LABEL_H = 15;

    private final String name;
    private final LinkedListModel model = new LinkedListModel();
    private final VizStage stage = new VizStage(() -> draw(false, null, -1, null));
    private final Map<Integer, Double> lastX = new HashMap<>();

    public LinkedListView(String name) {
        this.name = name;
        draw(false, null, -1, null);
    }

    @Override
    public String kind() {
        return "dslk";
    }

    @Override
    public Node node() {
        return stage.node();
    }

    @Override
    public void reset(JsonNode snapshot) {
        model.reset(snapshot);
        lastX.clear();
        draw(false, null, -1, null);
    }

    @Override
    public void apply(TraceEvent e, boolean animate, Duration d) {
        List<LinkedListModel.Change> changes;
        try {
            changes = model.apply(e);
        } catch (IllegalStateException ex) {
            LOG.warning("Trace danh sách liên kết " + name + " sai: " + ex.getMessage());
            draw(false, null, -1, null);
            return;
        }
        if (changes.isEmpty()) {
            return;
        }
        boolean structure = false;
        int hi = -1;
        Color hiColor = null;
        for (LinkedListModel.Change c : changes) {
            if (c instanceof LinkedListModel.Structure s) {
                structure = true;
                if (e.t().equals("node")) {
                    hi = s.id();
                    hiColor = VizPalette.WRITTEN;
                }
            } else if (c instanceof LinkedListModel.Read r) {
                hi = r.id();
                hiColor = VizPalette.CURRENT;
            } else if (c instanceof LinkedListModel.Written w) {
                hi = w.id();
                hiColor = VizPalette.WRITTEN;
            }
        }
        draw(animate && structure, d, animate ? hi : -1, hiColor);
    }

    private void draw(boolean slide, Duration d, int hi, Color hiColor) {
        stage.clear();
        stage.add(VizStage.title(name + "  (danh sách liên kết)"));
        List<Integer> order = model.order();
        int n = order.size();
        double width = stage.width();
        double step = Math.min(110, (width - 30) / Math.max(1, n));
        double boxW = Math.max(30, step * 0.62);
        double ptrW = Math.max(10, Math.min(18, boxW * 0.28));
        double valW = boxW - ptrW;
        double x0 = (width - step * n) / 2 + (step - boxW) / 2;
        int maxLabels = 1;
        Map<Integer, List<String>> labels = new HashMap<>();
        for (int id : order) {
            List<String> l = new ArrayList<>();
            if (model.head() != null && model.head() == id) {
                l.add("đầu");
            }
            model.pointers().forEach((k, v) -> {
                if (v == id) {
                    l.add(k);
                }
            });
            labels.put(id, l);
            maxLabels = Math.max(maxLabels, l.size());
        }
        double nodeY = Math.max(NODE_Y, 34 + maxLabels * LABEL_H + 14);
        stage.height(nodeY + BOX_H + 30 + Math.min(70, 16 + 12.0 * n));
        if (n == 0) {
            stage.add(
                    VizStage.label(model.head() == null ? "đầu → None  (danh sách rỗng)" : "", width / 2, nodeY, 220));
            lastX.clear();
            return;
        }
        Map<Integer, Double> xOf = new HashMap<>();
        Map<Integer, Integer> idx = new HashMap<>();
        for (int k = 0; k < n; k++) {
            xOf.put(order.get(k), x0 + k * step);
            idx.put(order.get(k), k);
        }
        Group arrows = new Group();
        Group boxes = new Group();
        stage.add(arrows);
        stage.add(boxes);
        for (int id : order) {
            LinkedListModel.N nd = model.node(id);
            double x = xOf.get(id);
            Group box = nodeBox(nd.text, valW, ptrW, nd.next == null);
            Color c = id == hi && hiColor != null ? hiColor : VizStage.mark(nd.mark);
            VizStage.fill(box, c);
            box.setLayoutX(x);
            box.setLayoutY(nodeY);
            if (model.orphan(id)) {
                box.setOpacity(0.4);
            }
            boxes.getChildren().add(box);
            Double old = lastX.get(id);
            if (slide && old != null) {
                stage.slide(box, old - x, 0, d);
            }
            // tên biến trỏ vào nút, xếp chồng phía trên, mũi tên nhỏ chỉ xuống
            List<String> l = labels.get(id);
            for (int k = 0; k < l.size(); k++) {
                Text t = VizStage.label(l.get(k), x + valW / 2, nodeY - 12 - (l.size() - 1 - k) * LABEL_H, 70);
                t.setFill(k == 0 && l.get(0).equals("đầu") ? VizStage.TEXT_DARK : Color.web("#4f46e5"));
                boxes.getChildren().add(t);
            }
            if (!l.isEmpty()) {
                boxes.getChildren()
                        .add(VizStage.arrow(
                                x + valW / 2, nodeY - 9, x + valW / 2, nodeY, 0, 0, VizStage.EDGE, 1.5, true));
            }
            // mũi tên tới nút sau
            if (nd.next != null && xOf.containsKey(nd.next)) {
                double sx = x + valW + ptrW / 2;
                double sy = nodeY + BOX_H / 2;
                double tx = xOf.get(nd.next);
                int gap = idx.get(nd.next) - idx.get(id);
                Color ec = model.orphan(id) ? VizStage.OUTLINE : VizStage.EDGE;
                if (gap == 1) {
                    arrows.getChildren().add(VizStage.arrow(sx, sy, tx, sy, 0, 1, ec, 2, true));
                } else {
                    arrows.getChildren()
                            .add(curve(
                                    sx,
                                    nodeY + BOX_H,
                                    tx + valW / 2,
                                    nodeY + BOX_H,
                                    nodeY + BOX_H + 14 + 9 * Math.abs(gap),
                                    ec));
                }
            }
        }
        if (slide) {
            FadeTransition f = new FadeTransition(VizStage.fx(d), arrows);
            f.setFromValue(0);
            f.setToValue(1);
            stage.play(f);
        }
        lastX.clear();
        lastX.putAll(xOf);
    }

    /** Ô [giá trị | con trỏ]; phần tử 0 là ngăn giá trị (để tô màu), 1 là chữ. Con trỏ None thì gạch chéo. */
    private static Group nodeBox(String text, double valW, double ptrW, boolean nullNext) {
        Group g = VizStage.box(text, valW, BOX_H);
        Rectangle ptr = new Rectangle(valW, 0, ptrW, BOX_H);
        ptr.setFill(Color.web("#e2e8f0"));
        ptr.setStroke(Color.web("#94a3b8"));
        ptr.setArcWidth(6);
        ptr.setArcHeight(6);
        g.getChildren().add(ptr);
        if (nullNext) {
            Line slash = new Line(valW + 3, BOX_H - 4, valW + ptrW - 3, 4);
            slash.setStroke(Color.web("#64748b"));
            slash.setStrokeWidth(1.5);
            g.getChildren().add(slash);
        } else {
            javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(valW + ptrW / 2, BOX_H / 2, 2.5);
            dot.setFill(VizStage.EDGE);
            g.getChildren().add(dot);
        }
        return g;
    }

    /** Mũi tên cong bên dưới hàng nút, từ (x1, y) tới (x2, y), đáy cong ở {@code low}. */
    private static Group curve(double x1, double y, double x2, double y2, double low, Color c) {
        QuadCurve q = new QuadCurve(x1, y, (x1 + x2) / 2, low + (low - y) * 0.6, x2, y2 + 1);
        q.setFill(null);
        q.setStroke(c);
        q.setStrokeWidth(2);
        // đầu mũi tên chỉ lên vào đáy nút đích
        double hs = 7;
        Polygon head = new Polygon(x2, y2 + 1, x2 - hs * 0.6, y2 + hs + 1, x2 + hs * 0.6, y2 + hs + 1);
        head.setFill(c);
        return new Group(q, head);
    }

    @Override
    public String toString() {
        return "LinkedListView(" + name + ")";
    }
}
