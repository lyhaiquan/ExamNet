package labcast.client.editor;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import labcast.common.protocol.CodeDelta;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

/**
 * Ô soạn code Python (RichTextFX): tô màu từ khoá, số dòng, Tab thành 4 dấu cách. Mỗi thay đổi đi qua
 * {@link DeltaBatcher} để gộp thành delta rồi giao cho {@code sink} (thường là {@code JournalClient}).
 */
public final class CodeEditor {
    private static final String[] KEYWORDS = {
        "False", "None", "True", "and", "as", "assert", "break", "class", "continue", "def", "del", "elif", "else",
        "except", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal", "not", "or",
        "pass", "raise", "return", "try", "while", "with", "yield"
    };
    /** Hàm có sẵn hay gặp trong bài DSA, tô khác từ khoá để học viên phân biệt. */
    private static final String[] BUILTINS = {
        "abs",
        "bool",
        "dict",
        "enumerate",
        "float",
        "input",
        "int",
        "len",
        "list",
        "map",
        "max",
        "min",
        "print",
        "range",
        "reversed",
        "set",
        "sorted",
        "str",
        "sum",
        "tuple",
        "zip"
    };

    private static final Pattern PATTERN = Pattern.compile("(?<KEYWORD>\\b(" + String.join("|", KEYWORDS) + ")\\b)"
            + "|(?<BUILTIN>\\b(" + String.join("|", BUILTINS) + ")\\b(?=\\s*\\())"
            + "|(?<STRING>\"([^\"\\\\\\n]|\\\\.)*\"|'([^'\\\\\\n]|\\\\.)*')"
            + "|(?<COMMENT>#[^\\n]*)"
            + "|(?<NUMBER>\\b\\d+(\\.\\d+)?\\b)");

    private final GuardedArea area = new GuardedArea();
    private final VirtualizedScrollPane<CodeArea> pane = new VirtualizedScrollPane<>(area);
    private DeltaBatcher batcher;
    private boolean loading;

    public CodeEditor() {
        area.setParagraphGraphicFactory(LineNumberFactory.get(area));
        area.getStylesheets().add(CodeEditor.class.getResource("python.css").toExternalForm());
        area.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB) {
                area.replaceSelection("    ");
                e.consume();
            }
        });
        // Kéo thả chữ từ app khác vào (trình duyệt, Word…): khi khoá thì bỏ, như dán từ ngoài
        area.addEventFilter(DragEvent.ANY, e -> {
            if (area.guarded && e.getGestureSource() == null) {
                e.consume();
            }
        });
        area.plainTextChanges().subscribe(ch -> {
            if (!loading && batcher != null) {
                batcher.onChange(ch.getPosition(), ch.getRemoved(), ch.getInserted());
            }
        });
        area.textProperty().addListener((obs, o, n) -> area.setStyleSpans(0, highlight(n)));
        Timeline ticker = new Timeline(new KeyFrame(javafx.util.Duration.millis(50), e -> {
            if (batcher != null) {
                batcher.tick();
            }
        }));
        ticker.setCycleCount(Timeline.INDEFINITE);
        ticker.play();
    }

    /** Nạp code của một bài (không sinh delta), đánh số tiếp từ {@code seq} server đang có. */
    public void load(String lessonId, String code, long seq, Consumer<CodeDelta> sink) {
        if (batcher != null) {
            batcher.flush();
        }
        loading = true;
        try {
            area.replaceText(code);
            area.moveTo(0);
            area.getUndoManager().forgetHistory();
        } finally {
            loading = false;
        }
        batcher = new DeltaBatcher(lessonId, System::currentTimeMillis, sink);
        batcher.setLastSeq(seq);
    }

    /** Xả delta đang gộp ngay, ví dụ trước khi bấm Chạy thử. Trả seq cuối đã gửi. */
    public long flush() {
        if (batcher == null) {
            return 0;
        }
        batcher.flush();
        return batcher.lastSeq();
    }

    public String text() {
        return area.getText();
    }

    public Node node() {
        return pane;
    }

    public void setEditable(boolean editable) {
        area.setEditable(editable);
    }

    /**
     * Bật khi học viên làm bài: copy/cắt/dán chỉ trong ô code (xem {@link ClipboardGuard}). Giáo viên
     * mở bài để giảng thì không bật.
     */
    public void setPasteGuard(boolean on) {
        area.guarded = on;
    }

    /** Gọi khi học viên thử dán chữ lấy từ ngoài ô code (luồng giao diện). */
    public void onBlockedPaste(Runnable r) {
        area.onBlocked = r;
    }

    /**
     * CodeArea có copy/cắt/dán đi qua {@link ClipboardGuard} khi bật khoá. RichTextFX gọi đúng ba hàm
     * này cho Ctrl+C/X/V, Ctrl+Insert, Shift+Delete, Shift+Insert — UiSmokeTest kiểm bằng phím thật.
     */
    private static final class GuardedArea extends CodeArea {
        private final ClipboardGuard guard = new ClipboardGuard();
        private boolean guarded;
        private Runnable onBlocked = () -> {};

        @Override
        public void copy() {
            if (!guarded) {
                super.copy();
                return;
            }
            guard.copied(getSelectedText());
        }

        @Override
        public void cut() {
            if (!guarded) {
                super.cut();
                return;
            }
            guard.copied(getSelectedText());
            replaceSelection("");
        }

        @Override
        public void paste() {
            if (!guarded) {
                super.paste();
                return;
            }
            Clipboard cb = Clipboard.getSystemClipboard();
            if (guard.isExternal(cb.hasString() ? cb.getString() : null)) {
                onBlocked.run();
            }
            String s = guard.pasteText();
            if (!s.isEmpty()) {
                replaceSelection(s);
            }
        }
    }

    static StyleSpans<Collection<String>> highlight(String text) {
        Matcher m = PATTERN.matcher(text);
        StyleSpansBuilder<Collection<String>> b = new StyleSpansBuilder<>();
        int last = 0;
        while (m.find()) {
            String cls = m.group("KEYWORD") != null
                    ? "keyword"
                    : m.group("BUILTIN") != null
                            ? "builtin"
                            : m.group("STRING") != null ? "string" : m.group("COMMENT") != null ? "comment" : "number";
            b.add(Collections.emptyList(), m.start() - last);
            b.add(Collections.singleton(cls), m.end() - m.start());
            last = m.end();
        }
        b.add(Collections.emptyList(), text.length() - last);
        return b.create();
    }
}
