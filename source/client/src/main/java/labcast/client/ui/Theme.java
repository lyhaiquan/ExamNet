package labcast.client.ui;

import javafx.scene.Parent;

/**
 * Gắn {@code labcast.css} vào gốc một màn. Mỗi màn tự gắn trong constructor nên Scene nào chứa màn đó
 * (app thật hay test tự dựng Scene) cũng có cùng giao diện.
 */
public final class Theme {
    public static final String CSS = Theme.class.getResource("labcast.css").toExternalForm();

    private Theme() {}

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
