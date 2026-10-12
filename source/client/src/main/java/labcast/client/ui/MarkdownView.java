package labcast.client.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.ThematicBreak;
import org.commonmark.parser.Parser;

/**
 * Vẽ đề bài markdown ({@code de.md}) thành nút JavaFX: tiêu đề, đoạn văn có in đậm/nghiêng/code, danh
 * sách, khối code và bảng kiểu GitHub. Phân tích cú pháp do commonmark-java làm; lớp này chỉ dựng nút,
 * màu và cỡ chữ lấy từ {@code labcast.css} (các lớp {@code md-*}). Không dùng WebView để jar của học
 * viên khỏi kéo theo cả trình duyệt.
 */
public final class MarkdownView {
    private static final Parser PARSER =
            Parser.builder().extensions(List.of(TablesExtension.create())).build();

    private MarkdownView() {}

    /**
     * @param skipTitle bỏ tiêu đề cấp 1 đứng đầu đề bài — màn bài học đã hiện tên bài ở trên
     */
    public static VBox render(String markdown, boolean skipTitle) {
        VBox root = new VBox(10);
        root.getStyleClass().add("markdown");
        Node doc = PARSER.parse(markdown == null ? "" : markdown);
        Node first = doc.getFirstChild();
        if (skipTitle && first instanceof Heading h && h.getLevel() == 1) {
            first = first.getNext();
        }
        for (Node n = first; n != null; n = n.getNext()) {
            javafx.scene.Node b = block(n);
            if (b != null) {
                root.getChildren().add(b);
            }
        }
        return root;
    }

    private static javafx.scene.Node block(Node n) {
        if (n instanceof Heading h) {
            TextFlow f = flow(h, "md-h" + Math.min(3, h.getLevel()));
            f.getStyleClass().add("md-heading");
            return f;
        }
        if (n instanceof Paragraph p) {
            return flow(p);
        }
        if (n instanceof BulletList || n instanceof OrderedList) {
            return list(n);
        }
        if (n instanceof FencedCodeBlock c) {
            return pre(c.getLiteral());
        }
        if (n instanceof IndentedCodeBlock c) {
            return pre(c.getLiteral());
        }
        if (n instanceof BlockQuote q) {
            VBox box = children(q);
            box.getStyleClass().add("md-quote");
            return box;
        }
        if (n instanceof TableBlock t) {
            return table(t);
        }
        if (n instanceof ThematicBreak) {
            return new Separator();
        }
        if (n instanceof HtmlBlock html) {
            return pre(html.getLiteral());
        }
        return null;
    }

    private static VBox children(Node parent) {
        VBox box = new VBox(8);
        for (Node c = parent.getFirstChild(); c != null; c = c.getNext()) {
            javafx.scene.Node b = block(c);
            if (b != null) {
                box.getChildren().add(b);
            }
        }
        return box;
    }

    private static VBox list(Node listNode) {
        VBox box = new VBox(4);
        int number = listNode instanceof OrderedList o ? o.getStartNumber() : 0;
        for (Node item = listNode.getFirstChild(); item != null; item = item.getNext()) {
            if (!(item instanceof ListItem li)) {
                continue;
            }
            Text marker = new Text(listNode instanceof OrderedList ? (number++) + "." : "•");
            marker.getStyleClass().add("md-text");
            StackPane m = new StackPane(marker);
            m.setMinWidth(18);
            m.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
            VBox body = children(li);
            HBox.setHgrow(body, Priority.ALWAYS);
            box.getChildren().add(new HBox(8, m, body));
        }
        return box;
    }

    private static Label pre(String code) {
        Label l = new Label(code.stripTrailing());
        l.getStyleClass().add("md-pre");
        l.setMaxWidth(Double.MAX_VALUE);
        l.setWrapText(true);
        return l;
    }

    private static GridPane table(TableBlock t) {
        GridPane g = new GridPane();
        g.getStyleClass().add("md-table");
        g.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        int row = 0;
        int cols = 0;
        for (Node section = t.getFirstChild(); section != null; section = section.getNext()) {
            boolean head = section instanceof TableHead;
            if (!head && !(section instanceof TableBody)) {
                continue;
            }
            for (Node r = section.getFirstChild(); r != null; r = r.getNext()) {
                if (!(r instanceof TableRow)) {
                    continue;
                }
                int col = 0;
                for (Node c = r.getFirstChild(); c != null; c = c.getNext()) {
                    if (!(c instanceof TableCell cell)) {
                        continue;
                    }
                    TextFlow f = flow(cell);
                    StackPane box = new StackPane(f);
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    box.getStyleClass().add(head ? "md-th" : "md-td");
                    box.setMaxWidth(Double.MAX_VALUE);
                    g.add(box, col++, row);
                }
                cols = Math.max(cols, col);
                row++;
            }
        }
        for (int i = 0; i < cols; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setMinWidth(80);
            g.getColumnConstraints().add(cc);
        }
        return g;
    }

    /** Một đoạn chữ liền: mọi nút inline con của {@code parent} nối vào một TextFlow. */
    private static TextFlow flow(Node parent, String... styles) {
        TextFlow f = new TextFlow();
        f.setLineSpacing(3);
        inlines(parent, f, List.of(styles));
        return f;
    }

    private static void inlines(Node parent, TextFlow f, List<String> styles) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNext()) {
            if (n instanceof org.commonmark.node.Text t) {
                f.getChildren().add(text(t.getLiteral(), styles));
            } else if (n instanceof Emphasis) {
                inlines(n, f, with(styles, "md-em"));
            } else if (n instanceof StrongEmphasis) {
                inlines(n, f, with(styles, "md-strong"));
            } else if (n instanceof Link) {
                inlines(n, f, with(styles, "md-link"));
            } else if (n instanceof Code c) {
                Label l = new Label(c.getLiteral());
                l.getStyleClass().add("md-code");
                l.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                f.getChildren().add(l);
            } else if (n instanceof SoftLineBreak) {
                f.getChildren().add(text(" ", styles));
            } else if (n instanceof HardLineBreak) {
                f.getChildren().add(text("\n", styles));
            } else if (n instanceof Image img) {
                f.getChildren().add(text("[hình: " + img.getDestination() + "]", styles));
            } else if (n instanceof HtmlInline html) {
                f.getChildren().add(text(html.getLiteral(), styles));
            } else {
                inlines(n, f, styles);
            }
        }
    }

    private static Text text(String s, List<String> styles) {
        Text t = new Text(s);
        t.getStyleClass().add("md-text");
        t.getStyleClass().addAll(styles);
        return t;
    }

    private static List<String> with(List<String> styles, String extra) {
        List<String> out = new ArrayList<>(styles);
        out.add(extra);
        return out;
    }
}
