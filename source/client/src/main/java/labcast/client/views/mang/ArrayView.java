package labcast.client.views.mang;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import labcast.client.player.View;
import labcast.client.player.VizPalette;
import labcast.common.trace.TraceEvent;

/**
 * Bộ vẽ {@code mang}: mỗi phần tử một cột cao theo giá trị, giá trị ghi trên đầu cột, chỉ số ghi dưới
 * chân, con trỏ ({@code con_tro}) là mũi tên dưới chỉ số. Màu theo {@link VizPalette}. Đổi chỗ thì hai
 * cột trượt qua nhau cùng lúc. Cột co giãn theo bề ngang khung và luôn nằm giữa.
 */
public final class ArrayView implements View {
    private static final double MAX_BAR_W = 46;
    private static final double MIN_BAR_W = 8;
    private static final double MAX_GAP = 12;
    /** Chỗ cho tên biến và số ghi trên đầu cột cao nhất. */
    private static final double TOP = 42;

    private static final double MAX_H = 180;
    private static final double BASE_Y = TOP + MAX_H;
    private static final double POINTER_ROW = 30;
    /** Cột hẹp hơn mức này thì không ghi số, chữ sẽ đè lên nhau. */
    private static final double MIN_LABEL_W = 20;

    private final String name;
    private final ArrayModel model = new ArrayModel();
    private final VBox root = new VBox(4);
    private final Pane canvas = new Pane();
    private final Label title = new Label();
    private final List<Group> bars = new ArrayList<>();
    /** Chỉ số dưới chân cột: là VỊ TRÍ trong mảng nên đứng yên khi hai cột trượt qua nhau lúc đổi chỗ. */
    private final List<Text> indexLabels = new ArrayList<>();
    /** Tên con trỏ → chỉ số đang trỏ, giữ thứ tự xuất hiện để mỗi con trỏ một hàng. */
    private final Map<String, Integer> pointerAt = new LinkedHashMap<>();

    private final Map<String, Text> pointers = new LinkedHashMap<>();

    private double barW = MAX_BAR_W;
    private double gap = MAX_GAP;
    private double offsetX;

    public ArrayView(String name) {
        this.name = name;
        title.setText(name);
        title.getStyleClass().add("viz-title");
        double h = BASE_Y + POINTER_ROW + 2 * 16;
        canvas.setPrefHeight(h);
        canvas.setMinHeight(h);
        canvas.setMaxWidth(Double.MAX_VALUE);
        canvas.widthProperty().addListener((obs, o, n) -> rebuild());
        canvas.getChildren().add(title);
        root.getChildren().add(canvas);
        root.setFillWidth(true);
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
        pointerAt.clear();
        rebuild();
    }

    @Override
    public void apply(TraceEvent e, boolean animate, Duration d) {
        List<ArrayModel.Change> changes = model.apply(e);
        for (ArrayModel.Change c : changes) {
            if (c instanceof ArrayModel.Pointer p) {
                pointerAt.put(p.name(), p.i());
            }
        }
        if (!animate) {
            rebuild();
            return;
        }
        clearHighlights();
        // Java 17 chưa có switch theo kiểu (pattern matching) nên dùng instanceof
        for (ArrayModel.Change c : changes) {
            if (c instanceof ArrayModel.Read r) {
                color(r.i(), VizPalette.CURRENT);
            } else if (c instanceof ArrayModel.Set s) {
                redrawBar(s.i(), s.value());
                color(s.i(), VizPalette.WRITTEN);
            } else if (c instanceof ArrayModel.Swap s) {
                animateSwap(s.i(), s.j(), d);
            }
        }
        placePointers();
    }

    /** Tính bề ngang cột theo khung, dựng lại toàn bộ cột và con trỏ (không animation). */
    private void rebuild() {
        List<Integer> vs = model.values();
        int n = Math.max(1, vs.size());
        double width = canvas.getWidth() > 0 ? canvas.getWidth() : 480;
        double step = width / n;
        barW = Math.max(MIN_BAR_W, Math.min(MAX_BAR_W, step * 0.78));
        gap = Math.max(2, Math.min(MAX_GAP, step - barW));
        double total = vs.size() * barW + Math.max(0, vs.size() - 1) * gap;
        offsetX = Math.max(0, (width - total) / 2);
        title.relocate(offsetX, 0); // tên biến đứng ngay trên cột đầu

        canvas.getChildren().removeAll(bars);
        canvas.getChildren().removeAll(indexLabels);
        bars.clear();
        indexLabels.clear();
        for (int i = 0; i < vs.size(); i++) {
            Group g = new Group();
            bars.add(g);
            canvas.getChildren().add(g);
            redrawBar(i, vs.get(i));
            if (barW >= MIN_LABEL_W || i % 5 == 0) {
                Text idx = centered(String.valueOf(i), "viz-index", BASE_Y + 15);
                idx.setLayoutX(xOf(i));
                indexLabels.add(idx);
                canvas.getChildren().add(idx);
            }
        }
        placePointers();
    }

    private double xOf(int i) {
        return offsetX + i * (barW + gap);
    }

    private void redrawBar(int i, int value) {
        if (i >= bars.size()) {
            rebuild();
            return;
        }
        int max = model.values().stream().mapToInt(Math::abs).max().orElse(1);
        double h = Math.max(4, MAX_H * Math.abs(value) / Math.max(1, max));
        Rectangle r = new Rectangle(barW, h, VizPalette.NORMAL);
        r.setArcWidth(Math.min(8, barW / 3));
        r.setArcHeight(Math.min(8, barW / 3));
        r.setY(BASE_Y - h);
        Group g = bars.get(i);
        g.getChildren().setAll(r);
        if (barW >= MIN_LABEL_W) {
            g.getChildren().add(centered(String.valueOf(value), "viz-value", BASE_Y - h - 6));
        }
        g.setTranslateX(0);
        g.setLayoutX(xOf(i));
    }

    /** Chữ căn giữa theo bề ngang cột (để CSS đổi cỡ chữ vẫn giữa). */
    private Text centered(String s, String styleClass, double baselineY) {
        Text t = new Text(s);
        t.getStyleClass().add(styleClass);
        t.setTextAlignment(TextAlignment.CENTER);
        t.setWrappingWidth(Math.max(barW, 1));
        t.setY(baselineY);
        return t;
    }

    private void color(int i, Color c) {
        if (i >= 0 && i < bars.size() && bars.get(i).getChildren().get(0) instanceof Rectangle r) {
            r.setFill(c);
        }
    }

    private void clearHighlights() {
        for (int i = 0; i < bars.size(); i++) {
            color(i, VizPalette.NORMAL);
        }
    }

    private void animateSwap(int i, int j, Duration d) {
        if (i >= bars.size() || j >= bars.size()) {
            rebuild();
            return;
        }
        Group a = bars.get(i);
        Group b = bars.get(j);
        double dx = (j - i) * (barW + gap);
        TranslateTransition ta = new TranslateTransition(javafx.util.Duration.millis(d.toMillis()), a);
        ta.setByX(dx);
        TranslateTransition tb = new TranslateTransition(javafx.util.Duration.millis(d.toMillis()), b);
        tb.setByX(-dx);
        color(i, VizPalette.WRITTEN);
        color(j, VizPalette.WRITTEN);
        ParallelTransition p = new ParallelTransition(ta, tb);
        p.setOnFinished(ev -> {
            List<Integer> vs = model.values();
            if (i < vs.size() && j < vs.size() && i < bars.size() && j < bars.size()) {
                redrawBar(i, vs.get(i));
                redrawBar(j, vs.get(j));
                color(i, VizPalette.WRITTEN);
                color(j, VizPalette.WRITTEN);
            }
        });
        p.play();
    }

    /** Mỗi con trỏ một hàng dưới chỉ số: mũi tên rồi tên, ví dụ "▲ j". */
    private void placePointers() {
        canvas.getChildren().removeAll(pointers.values());
        pointers.clear();
        int row = 0;
        for (Map.Entry<String, Integer> e : pointerAt.entrySet()) {
            int i = e.getValue();
            if (i >= 0 && i < model.values().size()) {
                Text t = new Text("▲ " + e.getKey());
                t.getStyleClass().add("viz-pointer");
                t.setTextAlignment(TextAlignment.CENTER);
                double w = Math.max(barW, 40);
                t.setWrappingWidth(w);
                t.setX(xOf(i) + barW / 2 - w / 2);
                t.setY(BASE_Y + POINTER_ROW + row * 16);
                pointers.put(e.getKey(), t);
                canvas.getChildren().add(t);
            }
            row++;
        }
    }

    @Override
    public String toString() {
        return "ArrayView(" + name + ")";
    }
}
