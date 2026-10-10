package labcast.client.views.mang;

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
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import labcast.client.player.View;
import labcast.common.trace.TraceEvent;

/**
 * Bộ vẽ {@code mang}: mỗi phần tử một cột cao theo giá trị. Màu theo quy ước spec §7.4: vàng là đang
 * xét, xanh dương là vừa ghi. Đổi chỗ thì hai cột trượt qua nhau cùng lúc.
 */
public final class ArrayView implements View {
    static final Color NORMAL = Color.web("#9db4c0");
    static final Color READ = Color.web("#f2c14e");
    static final Color WRITTEN = Color.web("#4a90d9");

    private static final double BAR_W = 36;
    private static final double GAP = 8;
    private static final double MAX_H = 160;
    private static final double BASE_Y = 190;

    private final String name;
    private final ArrayModel model = new ArrayModel();
    private final Pane root = new Pane();
    private final Text title = new Text();
    private final List<Group> bars = new ArrayList<>();
    private final Map<String, Text> pointers = new HashMap<>();

    public ArrayView(String name) {
        this.name = name;
        root.setPrefHeight(230);
        root.setMinHeight(230);
        title.setText(name);
        title.setX(0);
        title.setY(14);
        root.getChildren().add(title);
    }

    @Override
    public String kind() {
        return "mang";
    }

    @Override
    public Node node() {
        return root;
    }

    @Override
    public void reset(JsonNode snapshot) {
        List<Integer> vs = new ArrayList<>();
        if (snapshot != null && snapshot.isArray()) {
            snapshot.forEach(n -> vs.add(n.asInt()));
        }
        model.reset(vs);
        rebuild();
    }

    @Override
    public void apply(TraceEvent e, boolean animate, Duration d) {
        List<ArrayModel.Change> changes = model.apply(e);
        if (!animate) {
            rebuild();
            return;
        }
        clearHighlights();
        // Java 17 chưa có switch theo kiểu (pattern matching) nên dùng instanceof
        for (ArrayModel.Change c : changes) {
            if (c instanceof ArrayModel.Read r) {
                color(r.i(), READ);
            } else if (c instanceof ArrayModel.Set s) {
                redrawBar(s.i(), s.value());
                color(s.i(), WRITTEN);
            } else if (c instanceof ArrayModel.Swap s) {
                animateSwap(s.i(), s.j(), d);
            } else if (c instanceof ArrayModel.Pointer p) {
                movePointer(p.name(), p.i());
            }
        }
    }

    private void rebuild() {
        root.getChildren().removeAll(bars);
        bars.clear();
        List<Integer> vs = model.values();
        for (int i = 0; i < vs.size(); i++) {
            Group g = new Group();
            bars.add(g);
            root.getChildren().add(g);
            redrawBar(i, vs.get(i));
        }
    }

    private void redrawBar(int i, int value) {
        if (i >= bars.size()) {
            rebuild();
            return;
        }
        int max = model.values().stream().mapToInt(Math::abs).max().orElse(1);
        double h = Math.max(4, MAX_H * Math.abs(value) / Math.max(1, max));
        Rectangle r = new Rectangle(BAR_W, h, NORMAL);
        r.setArcWidth(6);
        r.setArcHeight(6);
        r.setY(BASE_Y - h);
        Text label = new Text(String.valueOf(value));
        label.setX(BAR_W / 2 - label.getLayoutBounds().getWidth() / 2);
        label.setY(BASE_Y - h - 4);
        Text index = new Text(String.valueOf(i));
        index.setFill(Color.GRAY);
        index.setX(BAR_W / 2 - 3);
        index.setY(BASE_Y + 14);
        Group g = bars.get(i);
        g.getChildren().setAll(r, label, index);
        g.setTranslateX(0);
        g.setLayoutX(i * (BAR_W + GAP));
    }

    private void color(int i, Color c) {
        if (i >= 0 && i < bars.size() && bars.get(i).getChildren().get(0) instanceof Rectangle r) {
            r.setFill(c);
        }
    }

    private void clearHighlights() {
        for (int i = 0; i < bars.size(); i++) {
            color(i, NORMAL);
        }
    }

    private void animateSwap(int i, int j, Duration d) {
        if (i >= bars.size() || j >= bars.size()) {
            rebuild();
            return;
        }
        Group a = bars.get(i);
        Group b = bars.get(j);
        double dx = (j - i) * (BAR_W + GAP);
        TranslateTransition ta = new TranslateTransition(javafx.util.Duration.millis(d.toMillis()), a);
        ta.setByX(dx);
        TranslateTransition tb = new TranslateTransition(javafx.util.Duration.millis(d.toMillis()), b);
        tb.setByX(-dx);
        color(i, WRITTEN);
        color(j, WRITTEN);
        ParallelTransition p = new ParallelTransition(ta, tb);
        p.setOnFinished(ev -> {
            List<Integer> vs = model.values();
            if (i < vs.size() && j < vs.size()) {
                redrawBar(i, vs.get(i));
                redrawBar(j, vs.get(j));
                color(i, WRITTEN);
                color(j, WRITTEN);
            }
        });
        p.play();
    }

    private void movePointer(String pname, int i) {
        Text t = pointers.computeIfAbsent(pname, k -> {
            Text nt = new Text(k);
            nt.setFill(Color.web("#c0392b"));
            root.getChildren().add(nt);
            return nt;
        });
        int row = new ArrayList<>(pointers.keySet()).indexOf(pname);
        t.setX(i * (BAR_W + GAP) + BAR_W / 2 - 3);
        t.setY(BASE_Y + 30 + row * 14);
    }

    @Override
    public String toString() {
        return "ArrayView(" + name + ")";
    }
}
