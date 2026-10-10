package labcast.client.ui;

import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import labcast.client.net.DiscoveryClient.ServerInfo;

/**
 * Màn đăng nhập: danh sách lớp tự tìm thấy trong LAN (không phải gõ IP), ô nhập tay khi phòng máy
 * chặn broadcast, tên đăng nhập, mật khẩu.
 */
public final class LoginView extends VBox {
    private final ListView<ServerInfo> servers = new ListView<>();
    private final TextField manual = new TextField();
    private final TextField user = new TextField();
    private final PasswordField pass = new PasswordField();
    private final Button loginBtn = new Button("Đăng nhập");
    private final Label status = new Label();

    public LoginView(Consumer<LoginController.LoggedIn> onLoggedIn) {
        super(10);
        setPadding(new Insets(24));
        setMaxWidth(460);
        setAlignment(Pos.TOP_LEFT);

        Label title = new Label("LabCast");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");
        Label sub = new Label("Phòng thực hành DSA và SQL");

        servers.setPrefHeight(120);
        servers.setPlaceholder(new Label("Chưa thấy lớp nào. Bấm \"Tìm lại\" hoặc nhập địa chỉ máy giáo viên."));
        servers.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ServerInfo s, boolean empty) {
                super.updateItem(s, empty);
                setText(
                        empty || s == null
                                ? null
                                : s.className() + (s.teacherName().isEmpty() ? "" : " — " + s.teacherName()) + "  ("
                                        + s.host() + ":" + s.port() + ")");
            }
        });
        Button refresh = new Button("Tìm lại");
        refresh.setOnAction(e -> discover());
        manual.setPromptText("hoặc nhập địa chỉ, ví dụ 192.168.1.10:7000");
        user.setPromptText("Tên đăng nhập");
        pass.setPromptText("Mật khẩu");
        loginBtn.setDefaultButton(true);
        loginBtn.setOnAction(e -> login(onLoggedIn));
        status.setWrapText(true);

        getChildren()
                .addAll(
                        title,
                        sub,
                        new HBox(8, new Label("Lớp trong mạng:"), refresh),
                        servers,
                        manual,
                        user,
                        pass,
                        loginBtn,
                        status);
        discover();
    }

    private void discover() {
        status.setText("Đang tìm lớp trong mạng…");
        LoginController.discover()
                .thenAccept(list -> Platform.runLater(() -> {
                    servers.getItems().setAll(list);
                    if (!list.isEmpty()) {
                        servers.getSelectionModel().select(0);
                    }
                    status.setText(list.isEmpty() ? "Không tìm thấy lớp nào trong mạng." : "");
                }));
    }

    private void login(Consumer<LoginController.LoggedIn> onLoggedIn) {
        String host;
        int port = 7000;
        String m = manual.getText().strip();
        if (!m.isEmpty()) {
            int colon = m.lastIndexOf(':');
            host = colon > 0 ? m.substring(0, colon) : m;
            if (colon > 0) {
                try {
                    port = Integer.parseInt(m.substring(colon + 1));
                } catch (NumberFormatException e) {
                    status.setText("Cổng phải là số");
                    return;
                }
            }
        } else if (servers.getSelectionModel().getSelectedItem() != null) {
            ServerInfo s = servers.getSelectionModel().getSelectedItem();
            host = s.host();
            port = s.port();
        } else {
            status.setText("Chọn một lớp hoặc nhập địa chỉ máy giáo viên.");
            return;
        }
        String h = host;
        int p = port;
        String u = user.getText().strip();
        String pw = pass.getText();
        loginBtn.setDisable(true);
        status.setText("Đang đăng nhập…");
        Thread t = new Thread(
                () -> {
                    try {
                        LoginController.LoggedIn li = LoginController.login(h, p, u, pw);
                        Platform.runLater(() -> onLoggedIn.accept(li));
                    } catch (LoginController.LoginFailed ex) {
                        Platform.runLater(() -> {
                            status.setText(ex.getMessage());
                            loginBtn.setDisable(false);
                        });
                    }
                },
                "dang-nhap");
        t.setDaemon(true);
        t.start();
    }
}
