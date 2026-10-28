package labcast.client.ui;

import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import labcast.client.net.LcpClient;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;

/**
 * Hộp thoại đổi mật khẩu của chính mình và đặt lại mật khẩu cho người khác (giáo viên: học viên lớp mình, quản
 * trị: mọi người). Server mới là nơi quyết định có được phép không; app chỉ kiểm tra trước cho đỡ một lượt gửi.
 */
final class PasswordDialogs {
    private PasswordDialogs() {}

    /** Tự đổi: mật khẩu hiện tại, mật khẩu mới, nhập lại. */
    static Dialog<ButtonType> change(Window owner, LcpClient client) {
        PasswordField current = new PasswordField();
        PasswordField fresh = new PasswordField();
        PasswordField again = new PasswordField();
        current.setPromptText("Mật khẩu đang dùng");
        fresh.setPromptText("Ít nhất " + PasswordRules.MIN + " ký tự");
        again.setPromptText("Gõ lại mật khẩu mới");
        GridPane form = form(
                new Label("Mật khẩu hiện tại"), current,
                new Label("Mật khẩu mới"), fresh,
                new Label("Nhập lại mật khẩu mới"), again);
        Label error = errorLabel();
        Dialog<ButtonType> d =
                dialog(owner, "Đổi mật khẩu", "Đổi mật khẩu đăng nhập của bạn", new VBox(12, form, error));
        Platform.runLater(current::requestFocus);
        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume(); // tự đóng khi server trả lời xong
            String bad = current.getText().isEmpty()
                    ? "Nhập mật khẩu hiện tại"
                    : PasswordRules.check(fresh.getText(), again.getText());
            if (bad != null) {
                error.setText(bad);
                return;
            }
            submit(client, ok, error, MessageType.PASSWORD_CHANGE, current.getText(), fresh.getText(), msg -> {
                d.close();
                info(owner, msg + ". Lần đăng nhập sau dùng mật khẩu mới.");
            });
        });
        d.show();
        return d;
    }

    /**
     * Đặt lại cho người khác. {@code username} {@code null}: cho gõ tên đăng nhập (quản trị đặt lại cho giáo viên,
     * quản trị khác); có sẵn thì khoá ô đó (bấm từ dòng học viên).
     */
    static Dialog<ButtonType> reset(Window owner, LcpClient client, String username, String fullName) {
        return reset(owner, client, username, fullName, null);
    }

    /** Như trên; {@code onDone} chạy sau khi đặt lại thành công (tải lại danh sách yêu cầu…). */
    static Dialog<ButtonType> reset(Window owner, LcpClient client, String username, String fullName, Runnable onDone) {
        TextField user = new TextField(username == null ? "" : username);
        user.setPromptText("Tên đăng nhập, ví dụ sv01");
        user.setEditable(username == null);
        // hiện rõ mật khẩu: giáo viên cần đọc lại cho học viên
        TextField fresh = new TextField(PasswordRules.suggest());
        fresh.setPromptText("Ít nhất " + PasswordRules.MIN + " ký tự");
        Button another = new Button("Số khác");
        another.setOnAction(e -> fresh.setText(PasswordRules.suggest()));
        HBox pwdRow = new HBox(8, fresh, another);
        HBox.setHgrow(fresh, Priority.ALWAYS);
        GridPane form = form(new Label("Tài khoản"), user, new Label("Mật khẩu tạm"), pwdRow);
        Label hint = new Label("Đọc mật khẩu tạm này cho người dùng. Lần đăng nhập tới app sẽ bắt họ đặt mật khẩu"
                + " của riêng mình, dễ nhớ với họ.");
        hint.setWrapText(true);
        hint.getStyleClass().add("muted");
        Label error = errorLabel();
        String who = username == null ? "" : " cho " + (fullName == null || fullName.isBlank() ? username : fullName);
        Dialog<ButtonType> d =
                dialog(owner, "Đặt lại mật khẩu", "Đặt lại mật khẩu" + who, new VBox(12, form, hint, error));
        Platform.runLater(username == null ? user::requestFocus : fresh::requestFocus);
        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume();
            String name = user.getText().strip();
            String bad = name.isEmpty() ? "Nhập tên đăng nhập" : PasswordRules.check(fresh.getText(), null);
            if (bad != null) {
                error.setText(bad);
                return;
            }
            String pwd = fresh.getText();
            submit(client, ok, error, MessageType.PASSWORD_RESET, name, pwd, msg -> {
                d.close();
                info(owner, msg + ".\nMật khẩu tạm: " + pwd + "\nLần đăng nhập tới họ sẽ tự đặt mật khẩu mới.");
                if (onDone != null) onDone.run();
            });
        });
        d.show();
        return d;
    }

    /**
     * Đăng nhập bằng mật khẩu tạm: bắt đặt mật khẩu của riêng mình trước khi vào app. Mật khẩu cũ là mật khẩu vừa gõ
     * ở màn đăng nhập nên không hỏi lại. {@code onDone} khi đổi xong, {@code onCancel} khi bấm Huỷ (về màn đăng
     * nhập).
     */
    static Dialog<ButtonType> chooseOwn(
            Window owner, LcpClient client, String tempPassword, Runnable onDone, Runnable onCancel) {
        PasswordField fresh = new PasswordField();
        PasswordField again = new PasswordField();
        fresh.setPromptText("Ít nhất " + PasswordRules.MIN + " ký tự, dễ nhớ với bạn");
        again.setPromptText("Gõ lại mật khẩu mới");
        GridPane form = form(new Label("Mật khẩu mới"), fresh, new Label("Nhập lại"), again);
        Label hint = new Label("Bạn đang dùng mật khẩu tạm giáo viên đưa. Đặt một mật khẩu của riêng bạn để lần sau"
                + " đăng nhập bằng mật khẩu đó.");
        hint.setWrapText(true);
        hint.setMaxWidth(380);
        hint.getStyleClass().add("muted");
        Label error = errorLabel();
        Dialog<ButtonType> d =
                dialog(owner, "Đặt mật khẩu của bạn", "Đặt mật khẩu của riêng bạn", new VBox(12, hint, form, error));
        Platform.runLater(fresh::requestFocus);
        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume();
            String bad = PasswordRules.check(fresh.getText(), again.getText());
            if (bad == null && fresh.getText().equals(tempPassword)) bad = "Chọn mật khẩu khác mật khẩu tạm";
            if (bad != null) {
                error.setText(bad);
                return;
            }
            submit(client, ok, error, MessageType.PASSWORD_CHANGE, tempPassword, fresh.getText(), msg -> {
                d.setResult(ButtonType.OK);
                d.close();
                onDone.run();
            });
        });
        d.setOnHidden(e -> {
            if (d.getResult() != ButtonType.OK) onCancel.run();
        });
        d.show();
        return d;
    }

    private static void submit(
            LcpClient client,
            Button ok,
            Label error,
            MessageType type,
            String a,
            String b,
            java.util.function.Consumer<String> onOk) {
        ok.setDisable(true);
        error.setText("");
        var reply = client.expect(MessageType.ADMIN_OK, MessageType.ERROR);
        client.send(type, new PayloadWriter().str(a).str(b).toBytes());
        reply.orTimeout(15, TimeUnit.SECONDS)
                .whenComplete((Frame f, Throwable err) -> Platform.runLater(() -> {
                    ok.setDisable(false);
                    try {
                        if (err != null) {
                            error.setText("Server không trả lời, thử lại sau");
                        } else if (f.type() == MessageType.ADMIN_OK) {
                            onOk.accept(f.reader().str());
                        } else {
                            error.setText(ErrorMessage.from(f).message());
                        }
                    } catch (Exception ex) {
                        error.setText(ex.getMessage());
                    }
                }));
    }

    private static GridPane form(Node... labelAndField) {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        for (int i = 0; i + 1 < labelAndField.length; i += 2) {
            g.add(labelAndField[i], 0, i / 2);
            g.add(labelAndField[i + 1], 1, i / 2);
            GridPane.setHgrow(labelAndField[i + 1], Priority.ALWAYS);
            if (labelAndField[i + 1] instanceof TextField t) t.setPrefColumnCount(22);
        }
        return g;
    }

    private static Label errorLabel() {
        Label l = new Label();
        l.setWrapText(true);
        l.setStyle("-fx-text-fill: #EF4444;");
        return l;
    }

    private static Dialog<ButtonType> dialog(Window owner, String title, String header, Node content) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(title);
        d.setHeaderText(header);
        if (owner != null) {
            d.initOwner(owner);
            if (owner.getScene() != null)
                d.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        d.getDialogPane().setPadding(new Insets(4));
        d.getDialogPane().setContent(content);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) d.getDialogPane().lookupButton(ButtonType.OK)).setText("Lưu");
        ((Button) d.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Huỷ");
        ButtonBar.setButtonUniformSize(d.getDialogPane().lookupButton(ButtonType.OK), false);
        return d;
    }

    private static void info(Window owner, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setHeaderText(null);
        if (owner != null) a.initOwner(owner);
        a.show();
    }
}
