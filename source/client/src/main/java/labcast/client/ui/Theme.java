package labcast.client.ui;

import java.io.IOException;
import java.io.InputStream;
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

    public static final String CSS = Theme.class.getResource("labcast.css").toExternalForm();

    private Theme() {}

    /** Gọi sớm (ví dụ đầu {@code start}) để chắc font đã nạp trước khi dựng màn đầu tiên. */
    public static void loadFonts() {
        // nạp trong khối static; gọi hàm này chỉ để lớp được nạp
    }

    public static <T extends Parent> T apply(T root) {
        if (!root.getStylesheets().contains(CSS)) {
            root.getStylesheets().add(CSS);
        }
        if (!root.getStyleClass().contains("lc-root")) {
            root.getStyleClass().add("lc-root");
        }
        return root;
    }
}
