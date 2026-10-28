package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import labcast.client.net.LcpClient;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;

/**
 * Khung "Yêu cầu quên mật khẩu" cho giáo viên, quản trị: ai bấm Quên mật khẩu ở màn đăng nhập thì hiện ở đây,
 * kèm nút đặt mật khẩu tạm. Không có yêu cầu nào thì ẩn hẳn.
 */
final class PasswordRequestsBanner extends VBox {
    private final LcpClient client;
    private final Runnable afterReset;
    private final Label title = new Label();
    private final VBox rows = new VBox(6);
    private boolean loading;

    PasswordRequestsBanner(LcpClient client, Runnable afterReset) {
        super(8);
        this.client = client;
        this.afterReset = afterReset;
        title.getStyleClass().add("pwd-requests-title");
        Label hint = new Label("Đặt mật khẩu tạm rồi đọc cho họ; lần đăng nhập tới họ sẽ tự đặt mật khẩu mới.");
        hint.getStyleClass().add("pwd-requests-hint");
        hint.setWrapText(true);
        getChildren().addAll(title, hint, rows);
        getStyleClass().add("pwd-requests");
        managedProperty().bind(visibleProperty());
        setVisible(false);
    }

    /** Hỏi server danh sách yêu cầu đang chờ. Server cũ không biết loại này thì khung cứ ẩn. */
    void reload() {
        if (loading) return;
        loading = true;
        var reply = client.expect(MessageType.PASSWORD_REQUESTS_DATA, MessageType.ERROR);
        client.send(MessageType.PASSWORD_REQUESTS_REQ, new byte[0]);
        reply.orTimeout(10, TimeUnit.SECONDS).whenComplete((f, err) -> {
            JsonNode list = null;
            if (err == null && f.type() == MessageType.PASSWORD_REQUESTS_DATA) {
                try {
                    list = JsonPayload.tree(f).path("requests");
                } catch (Exception ignored) {
                    // để trống
                }
            }
            JsonNode l = list;
            Platform.runLater(() -> {
                loading = false;
                show(l);
            });
        });
    }

    private void show(JsonNode list) {
        rows.getChildren().clear();
        if (list == null || list.size() == 0) {
            setVisible(false);
            return;
        }
        title.setText("Yêu cầu quên mật khẩu (" + list.size() + ")");
        for (JsonNode r : list) {
            String username = r.path("username").asText();
            String fullName = r.path("fullName").asText(username);
            Label name = new Label(fullName + "  ·  " + username);
            name.getStyleClass().add("pwd-requests-name");
            String role = r.path("role").asText();
            Label meta = new Label((role.equals("TEACHER") ? "Giáo viên · " : role.equals("ADMIN") ? "Quản trị · " : "")
                    + DashboardHome.ago(r.path("at").asLong()));
            meta.getStyleClass().add("pwd-requests-meta");
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            Button reset = new Button("Đặt mật khẩu tạm");
            reset.getStyleClass().add("pwd-requests-btn");
            reset.setOnAction(e -> PasswordDialogs.reset(
                    getScene() == null ? null : getScene().getWindow(), client, username, fullName, () -> {
                        reload();
                        if (afterReset != null) afterReset.run();
                    }));
            HBox row = new HBox(10, new VBox(1, name, meta), gap, reset);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("pwd-requests-row");
            rows.getChildren().add(row);
        }
        setVisible(true);
    }
}
