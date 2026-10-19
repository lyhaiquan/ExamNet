package labcast.client.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;
import java.util.logging.Logger;
import javafx.scene.Parent;
import javafx.scene.text.Font;

/**
 * Gắn {@code labcast.css} vào gốc một màn. Mỗi màn tự gắn trong constructor nên Scene nào chứa màn đó
 * (app thật hay test tự dựng Scene) cũng có cùng giao diện.
 *
 * <p>Font chữ đóng gói trong app ({@code labcast/client/fonts}, giấy phép OFL): Inter cho giao diện và đề
 * bài, JetBrains Mono cho code. Nạp một lần khi lớp này được dùng lần đầu, trước khi CSS nào cần tới, nên
 * máy học viên không cần cài font.
 */
public final class Theme {
    private static final Logger LOG = Logger.getLogger(Theme.class.getName());

    /** Họ font chữ giao diện và họ font code, dùng khi bộ vẽ đặt font bằng Java thay vì CSS. */
    public static final String UI_FONT = "Inter";

    public static final String CODE_FONT = "JetBrains Mono";

    static {
        for (String f : new String[] {
            "Inter-Regular.ttf", "Inter-Bold.ttf", "JetBrainsMono-Regular.ttf", "JetBrainsMono-Bold.ttf"
        }) {
            try (InputStream in = Theme.class.getResourceAsStream("/labcast/client/fonts/" + f)) {
                if (in == null || Font.loadFont(in, 13) == null) {
                    LOG.warning("Không nạp được font " + f + ", dùng font hệ thống thay thế");
                }
            } catch (IOException e) {
                LOG.warning("Không đọc được font " + f + ": " + e.getMessage());
            }
        }
    }

// Hai giao dien luu trong file resource; CSS doi ngay khi bam nut, khong can restart.
    private static final String LIGHT_CSS = Theme.class.getResource("labcast-light.css").toExternalForm();
    private static final String DARK_CSS = Theme.class.getResource("labcast-dark.css").toExternalForm();
    private static final String LIGHT_EDITOR = Theme.class.getResource("/labcast/client/editor/python-light.css").toExternalForm();
    private static final String DARK_EDITOR = Theme.class.getResource("/labcast/client/editor/python-dark.css").toExternalForm();
    private static final Preferences PREFS = Preferences.userNodeForPackage(Theme.class);
    private static final Set<Parent> SCREENS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<Parent> EDITORS = Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean dark = PREFS.getBoolean("darkTheme", true);
    // Tuong thich voi code cu neu cho nao do su dung Theme.CSS.
    public static final String CSS = DARK_CSS;

    private Theme() {}

    /** Gọi sớm (ví dụ đầu {@code start}) để chắc font đã nạp trước khi dựng màn đầu tiên. */
    public static void loadFonts() {
        // nạp trong khối static; gọi hàm này chỉ để lớp được nạp
    }

    public static boolean isDark() {
        return dark;
    }

    /** Doi ngay theme tat ca man hinh (ke ca man dang mo va RichTextFX editor). */
    public static void setDark(boolean enabled) {
        if (dark == enabled) return;
        dark = enabled;
        PREFS.putBoolean("darkTheme", enabled);
        for (Parent p : SCREENS) switchSheet(p, LIGHT_CSS, DARK_CSS, dark ? DARK_CSS : LIGHT_CSS);
        for (Parent p : EDITORS) switchSheet(p, LIGHT_EDITOR, DARK_EDITOR, dark ? DARK_EDITOR : LIGHT_EDITOR);
    }

    private static void switchSheet(Parent root, String light, String darkCss, String next) {
        root.getStylesheets().removeAll(light, darkCss);
        root.getStylesheets().add(next);
    }

    /** Moi man JavaFX dang ky de cung thay doi khi nguoi dung bat/tat dark mode. */
    public static <T extends Parent> T apply(T root) {
        SCREENS.add(root);
        switchSheet(root, LIGHT_CSS, DARK_CSS, dark ? DARK_CSS : LIGHT_CSS);
        if (!root.getStyleClass().contains("lc-root")) {
            root.getStyleClass().add("lc-root");
        }
        return root;
    }

    /** Theme rieng cho RichTextFX (mau code, so dong, nen editor). */
    public static <T extends Parent> T applyEditor(T editor) {
        EDITORS.add(editor);
        switchSheet(editor, LIGHT_EDITOR, DARK_EDITOR, dark ? DARK_EDITOR : LIGHT_EDITOR);
        return editor;
    }
}
