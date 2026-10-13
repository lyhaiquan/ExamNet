package labcast.client.ui;

import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import labcast.client.net.DiscoveryClient.ServerInfo;

/**
 * Màn đăng nhập: danh sách lớp tự tìm thấy trong LAN (không phải gõ IP), ô nhập tay khi phòng máy
 * chặn broadcast, tên đăng nhập, mật khẩu. Tất cả nằm trong một thẻ giữa nền màu.
 */
public final class LoginView extends StackPane {
    private final ListView<ServerInfo> servers = new ListView<>();
    private final TextField manual = new TextField();
    private final TextField user = new TextField();
    private final PasswordField pass = new PasswordField();
    private final Button loginBtn = new Button("Đăng nhập");
    private final Label status = new Label();

    public LoginView(Consumer<LoginController.LoggedIn> onLoggedIn) {
        Theme.apply(this);
        getStyleClass().add("login-bg");

        Label mark = new Label("LC");
        mark.getStyleClass().addAll("brand-mark", "large");
        Label title = new Label("LabCast");
        title.getStyleClass().add("login-title");
        Label sub = new Label("Phòng thực hành DSA và SQL");
        sub.getStyleClass().add("muted");
        HBox brand = new HBox(12, mark, new VBox(0, title, sub));
        brand.setAlignment(Pos.CENTER_LEFT);

        Label classes = new Label("LỚP TRONG MẠNG");
        classes.getStyleClass().add("section-title");
        Button refresh = new Button("↻ Tìm lại");
        refresh.getStyleClass().add("ghost");
        refresh.setOnAction(e -> discover());
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox classesHeader = new HBox(classes, gap, refresh);
        classesHeader.setAlignment(Pos.CENTER_LEFT);

        servers.getStyleClass().add("server-list");
        servers.setPrefHeight(132);
        Label empty = new Label("Chưa thấy lớp nào.\nBấm \"Tìm lại\" hoặc nhập địa chỉ máy giáo viên bên dưới.");
        empty.getStyleClass().add("placeholder");
        empty.setWrapText(true);
        servers.setPlaceholder(empty);
        servers.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ServerInfo s, boolean empty) {
                super.updateItem(s, empty);
                setText(null);
                setGraphic(empty || s == null ? null : serverCell(s));
            }
        });

        manual.setPromptText("Địa chỉ máy giáo viên, ví dụ 192.168.1.10:7000");
        // Chọn lớp trong danh sách hay gõ địa chỉ tay: chỉ một trong hai có hiệu lực, nên làm cái này thì xoá
        // cái kia (trước đây ô nhập tay còn chữ thì lớp đã chọn bị bỏ qua mà không báo gì).
        servers.setOnMouseClicked(e -> {
            if (servers.getSelectionModel().getSelectedItem() == null) {
                return;
            }
            manual.clear();
            if (e.getClickCount() >= 2
                    && !user.getText().isBlank()
                    && !pass.getText().isEmpty()) {
                login(onLoggedIn);
            } else {
                (user.getText().isBlank() ? user : pass).requestFocus();
            }
        });
        manual.textProperty().addListener((obs, o, n) -> {
            if (!n.isBlank()) {
                servers.getSelectionModel().clearSelection();
            }
        });
        user.setPromptText("Tên đăng nhập");
        pass.setPromptText("Mật khẩu");
        loginBtn.getStyleClass().add("primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setDefaultButton(true);
        loginBtn.setOnAction(e -> login(onLoggedIn));
        status.getStyleClass().add("form-status");
        status.setWrapText(true);

        VBox card = new VBox(
                12,
                brand,
                spacer(4),
                classesHeader,
                servers,
                divider("hoặc nhập tay"),
                manual,
                spacer(2),
                user,
                pass,
                spacer(2),
                loginBtn,
                status);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(440);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        getChildren().add(card);
        discover();
    }

    private static HBox serverCell(ServerInfo s) {
        Circle dot = new Circle(4);
        dot.getStyleClass().add("server-dot");
        Label name = new Label(s.className());
        name.getStyleClass().add("server-name");
        String teacher = s.teacherName().isEmpty() ? "" : s.teacherName() + "  ·  ";
        Label meta = new Label(teacher + s.host() + ":" + s.port());
        meta.getStyleClass().add("server-meta");
        HBox row = new HBox(10, dot, new VBox(1, name, meta));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static HBox divider(String text) {
        Region l = new Region();
        Region r = new Region();
        l.getStyleClass().add("divider-line");
        r.getStyleClass().add("divider-line");
        HBox.setHgrow(l, Priority.ALWAYS);
        HBox.setHgrow(r, Priority.ALWAYS);
        Label t = new Label(text);
        t.getStyleClass().add("divider-text");
        HBox box = new HBox(10, l, t, r);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private static Region spacer(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }

    private void setStatus(String text, boolean error) {
        status.setText(text);
        status.getStyleClass().remove("error");
        if (error) {
            status.getStyleClass().add("error");
        }
    }

    private void discover() {
        setStatus("Đang tìm lớp trong mạng…", false);
        LoginController.discover()
                .thenAccept(list -> Platform.runLater(() -> {
                    servers.getItems().setAll(list);
                    if (!list.isEmpty() && manual.getText().isBlank()) {
                        servers.getSelectionModel().select(0);
                        user.requestFocus(); // đã có lớp: việc tiếp theo là gõ tên đăng nhập
                    }
                    setStatus(list.isEmpty() ? "Không tìm thấy lớp nào trong mạng." : "", false);
                }));
    }

    private void login(Consumer<LoginController.LoggedIn> onLoggedIn) {
        String host;
        int port = 7000;
        String m = manual.getText().strip();
        if (user.getText().isBlank() || pass.getText().isEmpty()) {
            setStatus("Nhập tên đăng nhập và mật khẩu rồi bấm Đăng nhập.", true);
            (user.getText().isBlank() ? user : pass).requestFocus();
            return;
        }
        if (!m.isEmpty() && !m.contains(".") && !m.contains(":") && !m.equalsIgnoreCase("localhost")) {
            // hay gặp: gõ tên đăng nhập nhầm vào ô địa chỉ
            setStatus(
                    "\"" + m + "\" không giống địa chỉ máy giáo viên (ví dụ 192.168.1.10:7000). Đã chọn lớp ở trên"
                            + " thì để trống ô này.",
                    true);
            manual.requestFocus();
            return;
        }
        if (!m.isEmpty()) {
            int colon = m.lastIndexOf(':');
            host = colon > 0 ? m.substring(0, colon) : m;
            if (colon > 0) {
                try {
                    port = Integer.parseInt(m.substring(colon + 1));
                } catch (NumberFormatException e) {
                    setStatus("Cổng phải là số", true);
                    return;
                }
            }
        } else if (servers.getSelectionModel().getSelectedItem() != null) {
            ServerInfo s = servers.getSelectionModel().getSelectedItem();
            host = s.host();
            port = s.port();
        } else {
            setStatus("Chọn một lớp hoặc nhập địa chỉ máy giáo viên.", true);
            return;
        }
        String h = host;
        int p = port;
        String u = user.getText().strip();
        String pw = pass.getText();
        loginBtn.setDisable(true);
        setStatus("Đang đăng nhập…", false);
        Thread t = new Thread(
                () -> {
                    try {
                        LoginController.LoggedIn li = LoginController.login(h, p, u, pw);
                        Platform.runLater(() -> onLoggedIn.accept(li));
                    } catch (LoginController.LoginFailed ex) {
                        Platform.runLater(() -> {
                            setStatus(ex.getMessage(), true);
                            loginBtn.setDisable(false);
                        });
                    }
                },
                "dang-nhap");
        t.setDaemon(true);
        t.start();
    }
}
