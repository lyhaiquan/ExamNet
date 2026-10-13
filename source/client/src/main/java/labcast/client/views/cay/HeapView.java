package labcast.client.views.cay;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import labcast.client.player.View;
import labcast.client.player.VizPalette;
import labcast.client.views.VizStage;
import labcast.client.views.mang.ArrayModel;
import labcast.common.trace.TraceEvent;

/**
 * Bộ vẽ {@code heap} (spec §7.4: "heap = list vẽ thành cây"): một {@code viz.Mang} vẽ thành cây nhị phân đầy
 * đủ (con của ô i là 2i+1 và 2i+2), bên dưới là chính mảng đó. Cùng sự kiện với bộ vẽ mảng: đọc tô vàng, ghi
 * tô xanh dương, đổi chỗ thì hai nút trượt qua nhau (thấy được vun lên, vun xuống), thêm/bớt ô thì vẽ lại.
 *
 * <p>Con trỏ ({@code h.con_tro("n", k)}) là ranh giới heap: chỉ k ô đầu là heap, vẽ trên cây; các ô từ k trở
 * đi (ví dụ phần đã sắp của sắp xếp vun đống tại chỗ) chỉ còn ở mảng, tô xanh lá.
 */
public final class HeapView implements View {
    private static final double TOP = 34;
    private static final double LEVEL = 56;
    private static final double CELL = 34;

    private final String name;
    private final ArrayModel model = new ArrayModel();
    private final VizStage stage = new VizStage(() -> draw(Map.of()));
    private final Map<Integer, Group> nodeOf = new HashMap<>();
    private final Map<Integer, Group> cellOf = new HashMap<>();
    private final Map<Integer, double[]> nodeXY = new HashMap<>();
    /** Số ô đầu thuộc heap; −1 là cả mảng. */
    private int boundary = -1;

    public HeapView(String name) {
        this.name = name;
        draw(Map.of());
    }

    @Override
    public String kind() {
        return "heap";
    }

    @Override
    public Node node() {
        return stage.node();
    }

    @Override
    public void reset(JsonNode snapshot) {
        List<Integer> init = new ArrayList<>();
        if (snapshot != null && snapshot.isArray()) {
            snapshot.forEach(x -> init.add(x.asInt()));
        }
        model.reset(init);
        boundary = -1;
        draw(Map.of());
    }

    @Override
    public void apply(TraceEvent e, boolean animate, Duration d) {
        List<ArrayModel.Change> changes = model.apply(e);
        if (changes.isEmpty()) {
            return;
        }
        for (ArrayModel.Change c : changes) {
            if (c instanceof ArrayModel.Pointer p) {
                boundary = p.i();
            }
        }
        if (!animate) {
            draw(Map.of());
            return;
        }
        Map<Integer, Color> colors = new HashMap<>();
        ArrayModel.Swap swap = null;
        for (ArrayModel.Change c : changes) {
            if (c instanceof ArrayModel.Read r) {
                colors.put(r.i(), VizPalette.CURRENT);
            } else if (c instanceof ArrayModel.Set s) {
                colors.put(s.i(), VizPalette.WRITTEN);
            } else if (c instanceof ArrayModel.Swap s) {
                swap = s;
                colors.put(s.i(), VizPalette.WRITTEN);
                colors.put(s.j(), VizPalette.WRITTEN);
            } else if (c instanceof ArrayModel.Resized r && r.i() >= 0) {
                colors.put(r.i(), VizPalette.WRITTEN);
            }
        }
        draw(colors);
        if (swap != null) {
            swapSlide(swap.i(), swap.j(), d);
        }
    }

    private void draw(Map<Integer, Color> colors) {
        stage.clear();
        nodeOf.clear();
        cellOf.clear();
        nodeXY.clear();
        stage.add(VizStage.title(name + "  (heap)"));
        List<Integer> vs = model.values();
        int n = vs.size();
        int treeN = heapSize(n, boundary);
        double width = stage.width();
        int levels = treeN == 0 ? 1 : 32 - Integer.numberOfLeadingZeros(treeN);
        double leafStep = (width - 30) / Math.max(1, 1 << (levels - 1));
        double r = Math.max(9, Math.min(18, leafStep * 0.42));
        double treeH = TOP + levels * LEVEL;
        double cell = Math.max(18, Math.min(CELL, (width - 30) / Math.max(1, n)));
        double arrayY = treeH + 6;
        stage.height(arrayY + cell + 24);
        if (n == 0) {
            stage.add(VizStage.label("(rỗng)", width / 2, TOP + 24, 80));
            return;
        }
        Group edges = new Group();
        stage.add(edges);
        for (int i = 0; i < treeN; i++) {
            int depth = 31 - Integer.numberOfLeadingZeros(i + 1);
            int posInLevel = i + 1 - (1 << depth);
            double x = 15 + (posInLevel + 0.5) * (width - 30) / (1 << depth);
            double y = TOP + r + depth * LEVEL;
            nodeXY.put(i, new double[] {x, y});
            if (i > 0) {
                double[] p = nodeXY.get((i - 1) / 2);
                edges.getChildren().add(VizStage.line(p[0], p[1], x, y, VizStage.EDGE, 2));
            }
        }
        double ax = (width - cell * n) / 2;
        for (int i = 0; i < n; i++) {
            Color c = colors.getOrDefault(i, i < treeN ? VizPalette.NORMAL : VizPalette.DONE);
            double[] xy = nodeXY.get(i);
            if (xy != null) {
                Group g = VizStage.circle(String.valueOf(vs.get(i)), r);
                VizStage.fill(g, c);
                g.setLayoutX(xy[0]);
                g.setLayoutY(xy[1]);
                stage.add(g);
                nodeOf.put(i, g);
            }
            Group box = VizStage.box(String.valueOf(vs.get(i)), cell - 2, cell - 2);
            VizStage.fill(box, c);
            box.setLayoutX(ax + i * cell);
            box.setLayoutY(arrayY);
            stage.add(box);
            cellOf.put(i, box);
            stage.add(VizStage.label(String.valueOf(i), ax + i * cell + cell / 2 - 1, arrayY + cell + 12, cell));
        }
    }

    /** Số ô vẽ trên cây: {@code boundary} ô đầu (giới hạn trong mảng), hoặc cả mảng khi chưa có ranh giới. */
    static int heapSize(int n, int boundary) {
        return boundary < 0 ? n : Math.min(n, boundary);
    }

    /** Hai nút (và hai ô mảng) vừa đổi chỗ trượt từ chỗ của nhau về chỗ mới. */
    private void swapSlide(int i, int j, Duration d) {
        List<TranslateTransition> moves = new ArrayList<>();
        for (Map<Integer, Group> m : List.of(nodeOf, cellOf)) {
            Group a = m.get(i);
            Group b = m.get(j);
            if (a == null || b == null) {
                continue;
            }
            double dx = b.getLayoutX() - a.getLayoutX();
            double dy = b.getLayoutY() - a.getLayoutY();
            moves.add(move(a, dx, dy, d));
            moves.add(move(b, -dx, -dy, d));
        }
        if (!moves.isEmpty()) {
            stage.play(new ParallelTransition(moves.toArray(new TranslateTransition[0])));
        }
    }

    private static TranslateTransition move(Group g, double fromX, double fromY, Duration d) {
        TranslateTransition t = new TranslateTransition(VizStage.fx(d), g);
        t.setFromX(fromX);
        t.setFromY(fromY);
        t.setToX(0);
        t.setToY(0);
        return t;
    }

    @Override
    public String toString() {
        return "HeapView(" + name + ")";
    }
}
