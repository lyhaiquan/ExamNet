package labcast.client.views.bam;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import labcast.client.player.View;
import labcast.client.player.VizPalette;
import labcast.client.views.VizStage;
import labcast.common.trace.TraceEvent;

/**
 * Bộ vẽ {@code bam} (spec §7.4): mỗi hàng là một ô của bảng, số thứ tự ô bên trái. Dây chuyền: các phần tử
 * của ô nối nhau sang phải bằng mũi tên; đang so tô vàng, vừa thêm tô xanh dương, vừa bỏ thì trượt lên rồi mờ
 * đi (đỏ). Địa chỉ mở: mỗi hàng một ô, ô dò tới sáng vàng, ô đã xoá ghi "XOA".
 */
public final class HashView implements View {
    private static final double TOP = 28;

    private final String name;
    private final HashModel model = new HashModel();
    private final VizStage stage = new VizStage(() -> draw(-1, -1, null));

    public HashView(String name) {
        this.name = name;
        draw(-1, -1, null);
    }

    @Override
    public String kind() {
        return "bam";
    }

    @Override
    public Node node() {
        return stage.node();
    }

    @Override
    public void reset(JsonNode snapshot) {
        model.reset(snapshot);
        draw(-1, -1, null);
    }

    @Override
    public void apply(TraceEvent e, boolean animate, Duration d) {
        List<HashModel.Change> changes = model.apply(e);
        if (changes.isEmpty()) {
            return;
        }
        if (!animate) {
            draw(-1, -1, null);
            return;
        }
        HashModel.Change c = changes.get(0);
        if (c instanceof HashModel.Read r) {
            draw(r.i(), r.k(), VizPalette.CURRENT);
        } else if (c instanceof HashModel.Written w) {
            draw(w.i(), w.k(), VizPalette.WRITTEN);
        } else if (c instanceof HashModel.Removed rm) {
            draw(-1, -1, null);
            ghost(rm, d);
        }
    }

    private double rowH() {
        return model.size() > 12 ? Math.max(18, 360.0 / model.size()) : 34;
    }

    private double cellW() {
        return model.open() ? 64 : 46;
    }

    private double x0() {
        return 62;
    }

    private double y(int i) {
        return TOP + i * (rowH() + 6);
    }

    /** Vẽ lại cả bảng; phần tử {@code k} của ô {@code hi} tô {@code color}. */
    private void draw(int hi, int hk, Color color) {
        stage.clear();
        stage.add(VizStage.title(name + (model.open() ? "  (bảng băm địa chỉ mở)" : "  (bảng băm dây chuyền)")));
        int m = model.size();
        double h = rowH();
        double w = cellW();
        double step = w + (model.open() ? 0 : 22);
        stage.height(TOP + m * (h + 6) + 10);
        for (int i = 0; i < m; i++) {
            double y = y(i);
            stage.add(VizStage.label(String.valueOf(i), 22, y + h / 2 + 4, 30));
            Rectangle slot = new Rectangle(42, y, 10, h);
            slot.setFill(Color.web("#e2e8f0"));
            slot.setStroke(Color.web("#94a3b8"));
            stage.add(slot);
            List<String> c = model.chain(i);
            if (model.open() && c.isEmpty()) {
                Group g = VizStage.box("", w, h);
                VizStage.fill(g, i == hi && color != null ? color : Color.web("#f1f5f9"));
                ((Rectangle) g.getChildren().get(0)).setStroke(Color.web("#cbd5e1"));
                g.setLayoutX(x0());
                g.setLayoutY(y);
                stage.add(g);
                continue;
            }
            for (int k = 0; k < c.size(); k++) {
                double x = x0() + k * step;
                Group g = VizStage.box(c.get(k), w, h);
                boolean deleted = model.open() && c.get(k).equals("XOA");
                Color fill =
                        i == hi && k == hk && color != null ? color : deleted ? VizPalette.SKIPPED : VizPalette.NORMAL;
                VizStage.fill(g, fill);
                g.setLayoutX(x);
                g.setLayoutY(y);
                stage.add(g);
                if (!model.open()) {
                    double ay = y + h / 2;
                    double from = k == 0 ? 52 : x - 22;
                    stage.add(VizStage.arrow(from, ay, x, ay, 0, 1, VizStage.EDGE, 1.5, true));
                }
            }
        }
    }

    /** Phần tử vừa bỏ khỏi dây chuyền: hiện lại ở chỗ cũ (đỏ), trượt lên và mờ đi. */
    private void ghost(HashModel.Removed rm, Duration d) {
        double x = x0() + rm.k() * (cellW() + 22);
        Group g = VizStage.box(rm.text(), cellW(), rowH());
        VizStage.fill(g, VizPalette.REJECTED);
        g.setLayoutX(x);
        g.setLayoutY(y(rm.i()));
        stage.add(g);
        TranslateTransition t = new TranslateTransition(VizStage.fx(d), g);
        t.setByY(-14);
        FadeTransition f = new FadeTransition(VizStage.fx(d), g);
        f.setFromValue(1);
        f.setToValue(0);
        ParallelTransition p = new ParallelTransition(t, f);
        p.setOnFinished(ev -> stage.canvas().getChildren().remove(g));
        stage.play(p);
    }

    @Override
    public String toString() {
        return "HashView(" + name + ")";
    }
}
