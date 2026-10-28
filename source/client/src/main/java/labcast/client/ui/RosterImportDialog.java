package labcast.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import labcast.client.net.LcpClient;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;

/**
 * Màn xem trước khi nhập danh sách lớp từ Excel: chọn dòng tiêu đề và cột (đã đoán sẵn), xem từng học viên với tên
 * đăng nhập, mật khẩu tạm và dòng lỗi, rồi mới bấm tạo. Chỉ các dòng hợp lệ được gửi lên server.
 */
final class RosterImportDialog {
    private RosterImportDialog() {}

    /** Một lựa chọn trong ô chọn cột / dòng tiêu đề; {@code index} {@code -1} là "không có". */
    private record Choice(int index, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    static Dialog<ButtonType> show(
            Window owner, LcpClient client, String className, List<List<String>> rows, Runnable onDone) {
        RosterSheet.Columns guess = RosterSheet.guess(rows);
        ComboBox<Choice> header = new ComboBox<>();
        header.getItems().add(new Choice(-1, "Không có tiêu đề"));
        for (int r = 0; r < Math.min(rows.size(), 20); r++) {
            if (!rows.get(r).isEmpty())
                header.getItems().add(new Choice(r, "Dòng " + (r + 1) + ": " + preview(rows.get(r))));
        }
        ComboBox<Choice> id = new ComboBox<>();
        ComboBox<Choice> surname = new ComboBox<>();
        ComboBox<Choice> name = new ComboBox<>();
        ComboBox<Choice> birth = new ComboBox<>();
        header.setPrefWidth(320);
        for (ComboBox<Choice> c : List.of(id, surname, name, birth)) c.setPrefWidth(190);

        TableView<RosterSheet.Entry> table = new TableView<>();
        table.setId("roster-preview");
        table.getColumns()
                .addAll(List.of(
                        column("Dòng", 56, e -> String.valueOf(e.excelRow())),
                        column("Tên đăng nhập (mã SV)", 170, RosterSheet.Entry::username),
                        column("Họ tên", 210, RosterSheet.Entry::fullName),
                        column("Ngày sinh", 100, RosterSheet.Entry::birth),
                        column("Mật khẩu tạm", 110, RosterSheet.Entry::password),
                        column("Tình trạng", 240, e -> e.ok() ? "✓ Sẽ tạo" : e.problem())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Không có dòng học viên nào dưới dòng tiêu đề."));
        table.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(RosterSheet.Entry e, boolean empty) {
                super.updateItem(e, empty);
                getStyleClass().remove("roster-bad");
                if (!empty && e != null && !e.ok()) getStyleClass().add("roster-bad");
            }
        });
        VBox.setVgrow(table, Priority.ALWAYS);

        Label summary = new Label();
        summary.getStyleClass().add("roster-summary");
        Label hint = new Label("Tên đăng nhập là mã SV, mật khẩu tạm là ngày sinh dạng ngày tháng năm liền nhau (ví dụ"
                + " 05/08/2004 → 05082004). Lần đầu đăng nhập học viên phải tự đặt mật khẩu mới. Mã SV đã có tài khoản"
                + " thì chỉ được ghi danh thêm vào lớp, mật khẩu cũ giữ nguyên.");
        hint.setWrapText(true);
        hint.setMinHeight(Region.USE_PREF_SIZE); // không thì nhãn bị cắt còn một dòng
        hint.getStyleClass().add("muted");
        Label error = new Label();
        error.setWrapText(true);
        error.setStyle("-fx-text-fill: #EF4444;");

        FlowPane pickers = new FlowPane(14, 10);
        pickers.getChildren()
                .addAll(
                        labeled("Dòng tiêu đề", header),
                        labeled("Cột mã SV", id),
                        labeled("Cột họ đệm (nếu tách)", surname),
                        labeled("Cột họ tên / tên", name),
                        labeled("Cột ngày sinh", birth));

        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Nhập danh sách lớp từ Excel");
        d.setHeaderText("Nhập học viên vào lớp " + className);
        if (owner != null) {
            d.initOwner(owner);
            if (owner.getScene() != null)
                d.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        Theme.apply(d.getDialogPane()); // theme gắn vào gốc từng màn, không ở Scene: hộp thoại tự gắn
        d.setResizable(true);
        VBox content = new VBox(12, pickers, summary, table, hint, error);
        content.setPrefSize(980, 620);
        d.getDialogPane().setPadding(new Insets(4));
        d.getDialogPane().setContent(content);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        ((Button) d.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Huỷ");
        ButtonBar.setButtonUniformSize(ok, false);

        List<RosterSheet.Entry> current = new ArrayList<>();
        boolean[] filling = {false};
        Runnable refresh = () -> {
            if (filling[0]) return;
            var cols = new RosterSheet.Columns(pick(header), pick(id), pick(surname), pick(name), pick(birth));
            current.clear();
            current.addAll(RosterSheet.entries(rows, cols));
            table.getItems().setAll(current);
            long good = RosterSheet.valid(current);
            long bad = current.size() - good;
            summary.setText(current.size() + " dòng học viên · " + good + " sẽ tạo"
                    + (bad > 0 ? " · " + bad + " dòng lỗi, sẽ bỏ qua (sửa trong Excel rồi nhập lại)" : ""));
            ok.setText(good > 0 ? "Tạo " + good + " tài khoản" : "Không có dòng hợp lệ");
            ok.setDisable(good == 0);
        };
        // đổi dòng tiêu đề thì tên cột trong các ô chọn đổi theo
        Runnable fillColumns = () -> {
            filling[0] = true;
            int h = pick(header);
            List<String> titles = h >= 0 ? rows.get(h) : List.of();
            int width = rows.stream().mapToInt(List::size).max().orElse(0);
            for (ComboBox<Choice> c : List.of(id, surname, name, birth)) {
                int keep = pick(c);
                c.getItems().setAll(new Choice(-1, "— không có —"));
                for (int i = 0; i < width; i++) {
                    String t = i < titles.size() ? titles.get(i) : "";
                    c.getItems().add(new Choice(i, "Cột " + XlsxReader.columnName(i) + (t.isBlank() ? "" : " · " + t)));
                }
                select(c, keep);
            }
            filling[0] = false;
        };
        fillColumns.run();
        select(header, guess.header());
        fillColumns.run();
        select(id, guess.id());
        select(surname, guess.surname());
        select(name, guess.name());
        select(birth, guess.birth());
        header.setOnAction(e -> {
            fillColumns.run();
            refresh.run();
        });
        for (ComboBox<Choice> c : List.of(id, surname, name, birth)) c.setOnAction(e -> refresh.run());
        refresh.run();

        ok.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume(); // tự đóng khi server trả lời xong
            if (pick(id) < 0 || pick(name) < 0 || pick(birth) < 0) {
                error.setText("Chọn đủ cột mã SV, họ tên và ngày sinh");
                return;
            }
            ok.setDisable(true);
            error.setText("");
            var reply = client.expect(MessageType.ADMIN_OK, MessageType.ERROR);
            client.send(
                    MessageType.ADMIN_ROSTER_IMPORT,
                    new PayloadWriter().str(RosterSheet.csv(current)).toBytes());
            reply.orTimeout(60, TimeUnit.SECONDS)
                    .whenComplete((Frame f, Throwable err) -> Platform.runLater(() -> {
                        ok.setDisable(false);
                        try {
                            if (err != null) {
                                error.setText("Server không trả lời, thử lại sau");
                            } else if (f.type() == MessageType.ADMIN_OK) {
                                String msg = f.reader().str();
                                d.close();
                                Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
                                a.setHeaderText("Đã nhập danh sách lớp " + className);
                                if (owner != null) a.initOwner(owner);
                                a.show();
                                if (onDone != null) onDone.run();
                            } else {
                                error.setText(ErrorMessage.from(f).message());
                            }
                        } catch (Exception ex) {
                            error.setText(ex.getMessage());
                        }
                    }));
        });
        d.show();
        return d;
    }

    private static TableColumn<RosterSheet.Entry, String> column(
            String title, double width, java.util.function.Function<RosterSheet.Entry, String> value) {
        TableColumn<RosterSheet.Entry, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setSortable(false);
        c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(value.apply(cd.getValue())));
        return c;
    }

    private static VBox labeled(String text, ComboBox<Choice> box) {
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        return new VBox(4, l, box);
    }

    private static int pick(ComboBox<Choice> c) {
        return c.getValue() == null ? -1 : c.getValue().index();
    }

    private static void select(ComboBox<Choice> c, int index) {
        for (Choice ch : c.getItems()) {
            if (ch.index() == index) {
                c.setValue(ch);
                return;
            }
        }
        if (!c.getItems().isEmpty()) c.setValue(c.getItems().get(0));
    }

    private static String preview(List<String> row) {
        String s = String.join(" | ", row.stream().filter(x -> !x.isBlank()).toList());
        return s.length() > 60 ? s.substring(0, 57) + "…" : s;
    }
}
