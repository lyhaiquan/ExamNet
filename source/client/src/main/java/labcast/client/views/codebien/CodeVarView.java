package labcast.client.views.codebien;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import labcast.client.player.View;
import labcast.client.player.VizPalette;
import labcast.client.ui.Theme;
import labcast.client.views.VizStage;
import labcast.common.trace.TraceEvent;

/**
 * Bộ vẽ {@code code-bien} (spec §7.4): bên trái là code với dòng đang chạy tô vàng (chỉ hiện một khúc quanh
 * dòng đó), bên phải là bảng biến của hàm đang chạy; biến vừa đổi giá trị in xanh dương đậm.
 */
public final class CodeVarView implements View {
    private static final double TOP = 26;
    private static final double LINE_H = 18;
    private static final int WINDOW = 13;

    private final String name;
    private final CodeVarModel model = new CodeVarModel();
    private final VizStage stage = new VizStage(this::draw);

    public CodeVarView(String name) {
        this.name = name;
        draw();
    }

    @Override
    public String kind() {
        return "code-bien";
    }

    @Override
    public Node node() {
        return stage.node();
    }

    @Override
    public void reset(JsonNode snapshot) {
        model.reset(snapshot);
        draw();
    }

    @Override
    public void apply(TraceEvent e, boolean animate, Duration d) {
        if (model.apply(e)) {
            draw();
        }
    }

    private void draw() {
        stage.clear();
        stage.add(VizStage.title("Chạy từng dòng"));
        double width = stage.width();
        double codeW = Math.max(200, width * 0.62);
        List<String> lines = model.lines();
        int n = lines.size();
        int cur = model.line();
        int from = Math.max(1, Math.min(cur - WINDOW / 2, n - WINDOW + 1));
        int to = Math.min(n, from + WINDOW - 1);
        stage.height(TOP + WINDOW * LINE_H + 14);
        Rectangle bg = new Rectangle(0, TOP, codeW, WINDOW * LINE_H + 8);
        bg.setFill(Color.web("#f8fafc"));
        bg.setStroke(Color.web("#e2e8f0"));
        bg.setArcWidth(8);
        bg.setArcHeight(8);
        stage.add(bg);
        Font mono = Font.font(Theme.CODE_FONT, 12);
        Font monoBold = Font.font(Theme.CODE_FONT, FontWeight.BOLD, 12);
        for (int k = from; k <= to; k++) {
            double y = TOP + 4 + (k - from) * LINE_H;
            if (k == cur) {
                Rectangle band = new Rectangle(1, y, codeW - 2, LINE_H);
                band.setFill(VizPalette.CURRENT.deriveColor(0, 1, 1, 0.45));
                stage.add(band);
            }
            Text num = new Text(String.format("%3d", k));
            num.setFont(mono);
            num.setFill(Color.web("#94a3b8"));
            num.setX(6);
            num.setY(y + 13);
            stage.add(num);
            String src = lines.get(k - 1).replace("\t", "    ");
            Text t = new Text(src.length() > 70 ? src.substring(0, 69) + "…" : src);
            t.setFont(k == cur ? monoBold : mono);
            t.setFill(VizStage.TEXT_DARK);
            t.setX(38);
            t.setY(y + 13);
            stage.add(t);
        }
        // bảng biến
        double vx = codeW + 14;
        double vw = width - vx - 4;
        Text head = new Text(model.func().isEmpty() ? "Biến" : "Biến trong " + model.func() + "()");
        head.setFont(Font.font(Theme.UI_FONT, FontWeight.BOLD, 12));
        head.setFill(Color.web("#64748b"));
        head.setX(vx);
        head.setY(TOP + 14);
        stage.add(head);
        int row = 0;
        for (Map.Entry<String, String> v : model.vars().entrySet()) {
            double y = TOP + 22 + row * LINE_H;
            if (y > TOP + WINDOW * LINE_H) {
                break;
            }
            Text t = new Text(v.getKey() + " = " + v.getValue());
            boolean hot = model.changed(v.getKey());
            t.setFont(hot ? monoBold : mono);
            t.setFill(hot ? VizPalette.WRITTEN : VizStage.TEXT_DARK);
            t.setWrappingWidth(Math.max(80, vw));
            t.setX(vx);
            t.setY(y + 13);
            stage.add(t);
            row++;
        }
        if (cur == 0 && n <= 1) {
            stage.add(VizStage.label("(chưa chạy dòng nào)", codeW / 2, TOP + 40, 200));
        }
    }

    @Override
    public String toString() {
        return "CodeVarView(" + name + ")";
    }
}
