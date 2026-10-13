package labcast.client.views.bangsql;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import labcast.client.player.View;
import labcast.client.player.VizPalette;
import labcast.client.ui.Theme;
import labcast.common.trace.TraceEvent;

/**
 * Bộ vẽ {@code bang-sql}: bảng đang làm việc của câu SQL, bảng ghép bên phải khi JOIN, đường nối từng cặp
 * dòng, dấu ✓/✗ khi xét WHERE/HAVING, màu theo nhóm khi GROUP BY. Chỉ vẽ theo {@link SqlTableModel};
 * hiệu ứng (mờ dần khi loại dòng, trượt khi sắp xếp/gom nhóm) chỉ để nhìn — sự kiện mới tới thì dừng hiệu
 * ứng cũ và vẽ đúng trạng thái.
 */
public final class SqlTableView implements View {
    static final double ROW_H = 26;
    static final double HEAD_H = 28;
    static final double TITLE_H = 26;
    static final double PAD = 10;
    static final double MARK_W = 26;
    static final double GAP = 120;

    private static final Font FONT = Font.font(Theme.UI_FONT, 12.5);
    private static final Font BOLD = Font.font(Theme.UI_FONT, FontWeight.BOLD, 12.5);
    private static final Font TITLE = Font.font(Theme.UI_FONT, FontWeight.BOLD, 13.5);
    private static final Font NULL_FONT = Font.font(Theme.UI_FONT, FontPosture.ITALIC, 12);

    private static final Color TEXT = Color.web("#1e293b");
    private static final Color MUTED = Color.web("#94a3b8");
    private static final Color LINE = Color.web("#e2e8f0");
    private static final Color BORDER = Color.web("#cbd5e1");
    private static final Color HEAD = Color.web("#eef2f7");
    private static final Color OK_BG = Color.web("#dcfce7");
    private static final Color BAD_BG = Color.web("#fee2e2");
    private static final Color CUR_BG = Color.web("#fef3c7");
    private static final Color WRITE_BG = Color.web("#dbeafe");
    /** Màu nền các nhóm GROUP BY. */
    private static final Color[] GROUP = {
        Color.web("#e0e7ff"), Color.web("#fce7f3"), Color.web("#d1fae5"), Color.web("#fef9c3"),
        Color.web("#e0f2fe"), Color.web("#ffe4e6"), Color.web("#ede9fe"), Color.web("#ccfbf1")
    };

    private final SqlTableModel model = new SqlTableModel();
    private final StackPane root = new StackPane();
    private final Pane content = new Pane();
    private final List<Group> mainRows = new ArrayList<>();
    private Animation running;
    private double contentW = 1;

    public SqlTableView() {
        Group holder = new Group(content);
        root.getChildren().add(holder);
        StackPane.setAlignment(holder, Pos.TOP_CENTER);
        root.setMinHeight(120);
        // Bảng rộng hơn khung thì thu nhỏ cho vừa, không bị cắt
        root.widthProperty().addListener((obs, o, n) -> fit());
    }

    @Override
    public String kind() {
        return "bang-sql";
    }

    @Override
    public Node node() {
        return root;
    }

    @Override
    public void reset(JsonNode snapshot) {
        stop();
        model.reset();
        rebuild();
    }

    @Override
    public void apply(TraceEvent e, boolean animate, Duration d) {
        stop();
        List<Integer> before = new ArrayList<>();
        for (int i = 0; i < model.rows().size(); i++) {
            before.add(i);
        }
        List<SqlTableModel.Change> changes = model.apply(e);
        if (changes.isEmpty()) {
            return;
        }
        SqlTableModel.Change c = changes.get(0);
        javafx.util.Duration dur = javafx.util.Duration.millis(Math.max(120, d.toMillis() * 0.85));
        if (!animate) {
            rebuild();
            return;
        }
        if (c instanceof SqlTableModel.Remove r && !r.removed().isEmpty()) {
            fadeOutThen(r.removed(), dur, kept(before.size(), r.removed()));
        } else if (c instanceof SqlTableModel.Reorder re) {
            rebuild();
            slide(re.from(), dur);
        } else if (c instanceof SqlTableModel.Redraw rd && rd.fresh()) {
            rebuild();
            FadeTransition f = new FadeTransition(dur, content);
            f.setFromValue(0.2);
            f.setToValue(1);
            play(f);
        } else {
            rebuild();
        }
    }

    // ------------------------------------------------------------------ hiệu ứng

    private void play(Animation a) {
        running = a;
        a.setOnFinished(ev -> running = null);
        a.play();
    }

    /** Dừng hiệu ứng đang chạy và vẽ đúng trạng thái hiện tại của model. */
    private void stop() {
        if (running != null) {
            Animation a = running;
            running = null;
            a.setOnFinished(null);
            a.stop();
            rebuild();
        }
        content.setOpacity(1);
    }

    /** Dòng bị loại mờ dần, rồi vẽ lại; các dòng còn lại trượt lên chỗ mới. */
    private void fadeOutThen(List<Integer> removed, javafx.util.Duration dur, List<Integer> keptFrom) {
        ParallelTransition p = new ParallelTransition();
        for (int i : removed) {
            if (i < mainRows.size()) {
                FadeTransition f = new FadeTransition(dur, mainRows.get(i));
                f.setToValue(0.08);
                p.getChildren().add(f);
            }
        }
        p.setOnFinished(ev -> {
            running = null;
            rebuild();
            slide(keptFrom, dur.multiply(0.6));
        });
        running = p;
        p.play();
    }

    /** Dòng mới i đến từ dòng cũ {@code from.get(i)}: đặt ở chỗ cũ rồi trượt về chỗ mới. */
    private void slide(List<Integer> from, javafx.util.Duration dur) {
        ParallelTransition p = new ParallelTransition();
        for (int j = 0; j < Math.min(from.size(), mainRows.size()); j++) {
            double dy = (from.get(j) - j) * ROW_H;
            if (dy != 0) {
                Group g = mainRows.get(j);
                g.setTranslateY(dy);
                TranslateTransition t = new TranslateTransition(dur, g);
                t.setToY(0);
                t.setInterpolator(Interpolator.EASE_BOTH);
                p.getChildren().add(t);
            }
        }
        if (!p.getChildren().isEmpty()) {
            play(p);
        }
    }

    private static List<Integer> kept(int n, List<Integer> removed) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!removed.contains(i)) {
                out.add(i);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ vẽ

    private void rebuild() {
        content.getChildren().clear();
        mainRows.clear();
        List<String> cols = model.cols();
        if (cols.isEmpty()) {
            contentW = 1;
            fit();
            return;
        }
        double[] w = widths(cols, model.rows());
        double mainW = sum(w) + MARK_W;
        int[] cur = model.currentPair();

        // Bảng chính
        Text title = text(model.title(), TITLE, model.isResult() ? VizPalette.DONE.darker() : TEXT);
        title.relocate(0, 2);
        content.getChildren().add(title);
        drawHeader(0, cols, w);
        for (int i = 0; i < model.rows().size(); i++) {
            Group g = new Group();
            g.setLayoutY(TITLE_H + HEAD_H + i * ROW_H);
            Color bg = rowColor(i);
            if (cur != null && cur[0] == i) {
                bg = CUR_BG;
            }
            Rectangle r = new Rectangle(mainW, ROW_H, bg);
            g.getChildren().add(r);
            double x = 0;
            List<Object> row = model.rows().get(i);
            SqlTableModel.Mark mark = model.marks().get(i);
            for (int k = 0; k < cols.size(); k++) {
                if (model.changedCells(i).contains(k)) {
                    Rectangle hl = new Rectangle(x, 0, w[k], ROW_H);
                    hl.setFill(WRITE_BG);
                    g.getChildren().add(hl);
                }
                Object v = k < row.size() ? row.get(k) : null;
                Text t = cell(v, model.changedCells(i).contains(k));
                if (mark == SqlTableModel.Mark.DELETED) {
                    t.setStrikethrough(true);
                    t.setFill(VizPalette.REJECTED.darker());
                }
                t.relocate(x + PAD, (ROW_H - t.getLayoutBounds().getHeight()) / 2);
                g.getChildren().add(t);
                x += w[k];
            }
            String sym =
                    switch (mark) {
                        case OK -> "✓";
                        case BAD -> "✗";
                        case NEW -> "+";
                        case CHANGED -> "✎";
                        case DELETED -> "−";
                        default -> "";
                    };
            if (!sym.isEmpty()) {
                Color sc = mark == SqlTableModel.Mark.OK || mark == SqlTableModel.Mark.NEW
                        ? VizPalette.DONE.darker()
                        : mark == SqlTableModel.Mark.CHANGED ? VizPalette.WRITTEN : VizPalette.REJECTED;
                Text s = text(sym, BOLD, sc);
                s.relocate(x + 8, (ROW_H - s.getLayoutBounds().getHeight()) / 2);
                g.getChildren().add(s);
            }
            Line sep = new Line(0, ROW_H, mainW, ROW_H);
            sep.setStroke(LINE);
            g.getChildren().add(sep);
            mainRows.add(g);
            content.getChildren().add(g);
        }
        double mainH = TITLE_H + HEAD_H + model.rows().size() * ROW_H;
        frame(0, mainW, mainH, model.isResult() ? VizPalette.DONE : BORDER);
        double totalW = mainW;

        // Bảng bên phải (JOIN, vế 2) và các đường nối cặp dòng
        SqlTableModel.Table side = model.side();
        if (side != null) {
            double sx = mainW + GAP;
            double[] sw = widths(side.cols(), side.rows());
            double sideW = sum(sw);
            Text st = text(side.title(), TITLE, TEXT);
            st.relocate(sx, 2);
            content.getChildren().add(st);
            drawHeaderAt(sx, side.cols(), sw);
            for (int i = 0; i < side.rows().size(); i++) {
                double y = TITLE_H + HEAD_H + i * ROW_H;
                Rectangle r = new Rectangle(sx, y, sideW, ROW_H);
                r.setFill(cur != null && cur[1] == i ? CUR_BG : Color.WHITE);
                content.getChildren().add(r);
                double x = sx;
                for (int k = 0; k < side.cols().size(); k++) {
                    Text t = cell(side.rows().get(i).get(k), false);
                    t.relocate(x + PAD, y + (ROW_H - t.getLayoutBounds().getHeight()) / 2);
                    content.getChildren().add(t);
                    x += sw[k];
                }
                Line sep = new Line(sx, y + ROW_H, sx + sideW, y + ROW_H);
                sep.setStroke(LINE);
                content.getChildren().add(sep);
            }
            frame(sx, sideW, TITLE_H + HEAD_H + side.rows().size() * ROW_H, BORDER);
            for (int[] p : model.pairs()) {
                boolean now = p == cur;
                Color c = now ? VizPalette.CURRENT : Color.web("#b6c2d4");
                if (p[0] >= 0 && p[1] >= 0) {
                    Line l = new Line(mainW + 2, center(p[0]), sx - 2, center(p[1]));
                    l.setStroke(c);
                    l.setStrokeWidth(now ? 2.6 : 1.4);
                    content.getChildren().add(l);
                } else if (p[0] >= 0) {
                    Text t = text("→ NULL", NULL_FONT, now ? VizPalette.REJECTED : MUTED);
                    t.relocate(mainW + 8, center(p[0]) - 8);
                    content.getChildren().add(t);
                } else if (p[1] >= 0) {
                    Text t = text("NULL ←", NULL_FONT, now ? VizPalette.REJECTED : MUTED);
                    t.relocate(sx - 56, center(p[1]) - 8);
                    content.getChildren().add(t);
                }
            }
            totalW = sx + sideW;
        }
        contentW = totalW;
        content.setPrefSize(
                totalW + 2,
                Math.max(
                                mainH,
                                side == null
                                        ? 0
                                        : TITLE_H + HEAD_H + side.rows().size() * ROW_H)
                        + 4);
        fit();
    }

    private void drawHeader(double x0, List<String> cols, double[] w) {
        drawHeaderAt(x0, cols, w);
        Rectangle markHead = new Rectangle(x0 + sum(w), TITLE_H, MARK_W, HEAD_H);
        markHead.setFill(HEAD);
        content.getChildren().add(markHead);
    }

    private void drawHeaderAt(double x0, List<String> cols, double[] w) {
        double x = x0;
        for (int k = 0; k < cols.size(); k++) {
            Rectangle h = new Rectangle(x, TITLE_H, w[k], HEAD_H);
            h.setFill(HEAD);
            content.getChildren().add(h);
            Text t = text(cols.get(k), BOLD, Color.web("#334155"));
            t.relocate(x + PAD, TITLE_H + (HEAD_H - t.getLayoutBounds().getHeight()) / 2);
            content.getChildren().add(t);
            x += w[k];
        }
    }

    private void frame(double x, double w, double h, Color c) {
        Rectangle f = new Rectangle(x, TITLE_H, w, h - TITLE_H);
        f.setFill(Color.TRANSPARENT);
        f.setStroke(c);
        f.setStrokeWidth(c == BORDER ? 1 : 2);
        f.setArcWidth(6);
        f.setArcHeight(6);
        content.getChildren().add(f);
    }

    private Color rowColor(int i) {
        return switch (model.marks().get(i)) {
            case OK, NEW -> OK_BG;
            case BAD, DELETED -> BAD_BG;
            default -> {
                int g = model.groups().get(i);
                yield g >= 0 ? GROUP[g % GROUP.length] : Color.WHITE;
            }
        };
    }

    private static double center(int row) {
        return TITLE_H + HEAD_H + row * ROW_H + ROW_H / 2;
    }

    private void fit() {
        double avail = root.getWidth() > 0 ? root.getWidth() - 16 : contentW;
        double s = Math.min(1, avail / Math.max(1, contentW));
        content.setScaleX(s);
        content.setScaleY(s);
    }

    /** Bề ngang mỗi cột theo chữ dài nhất (tiêu đề hoặc giá trị), trong khoảng 44–240 px. */
    private static double[] widths(List<String> cols, List<List<Object>> rows) {
        double[] w = new double[cols.size()];
        for (int k = 0; k < cols.size(); k++) {
            double m = measure(cols.get(k), BOLD);
            for (List<Object> r : rows) {
                if (k < r.size()) {
                    m = Math.max(m, measure(show(r.get(k)), FONT));
                }
            }
            w[k] = Math.max(44, Math.min(240, m + 2 * PAD));
        }
        return w;
    }

    private static double sum(double[] w) {
        double s = 0;
        for (double v : w) {
            s += v;
        }
        return s;
    }

    private static double measure(String s, Font f) {
        Text t = new Text(s);
        t.setFont(f);
        return t.getLayoutBounds().getWidth();
    }

    private static Text cell(Object v, boolean written) {
        if (v == null) {
            return text("NULL", NULL_FONT, MUTED);
        }
        return text(show(v), written ? BOLD : FONT, written ? VizPalette.WRITTEN.darker() : TEXT);
    }

    /** Số thực: tối đa 4 chữ số lẻ; số thực tròn vẫn ghi ".0" để thấy là REAL. */
    static String show(Object v) {
        if (v == null) {
            return "NULL";
        }
        if (v instanceof Double d) {
            if (d == Math.rint(d) && Math.abs(d) < 1e15) {
                return String.format(java.util.Locale.ROOT, "%.1f", d);
            }
            return new BigDecimal(d)
                    .setScale(4, RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString();
        }
        return v.toString();
    }

    private static Text text(String s, Font f, Color c) {
        Text t = new Text(s);
        t.setFont(f);
        t.setFill(c);
        return t;
    }

    /** Cho test: model đang vẽ. */
    SqlTableModel model() {
        return model;
    }
}
