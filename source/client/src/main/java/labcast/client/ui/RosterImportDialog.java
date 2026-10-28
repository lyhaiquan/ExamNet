package labcast.client.ui;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
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
import javafx.stage.FileChooser;
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
                                var r = f.reader();
                                String msg = r.str();
                                // server cũ không gửi danh sách tài khoản: không xuất file được
                                Set<String> created = r.hasRemaining() ? lines(r.str()) : null;
                                Set<String> enrolled = r.hasRemaining() ? lines(r.str()) : Set.of();
                                d.close();
                                done(owner, className, msg, List.copyOf(current), created, enrolled);
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

    /**
     * Nhập xong: báo kết quả và cho xuất file Excel tài khoản + mật khẩu tạm để gửi giáo viên (học viên mới chưa
     * biết mật khẩu thì không vào lớp được). Mật khẩu tạm chỉ có ở máy này lúc này: server chỉ giữ bản băm.
     */
    static Dialog<ButtonType> done(
            Window owner,
            String className,
            String message,
            List<RosterSheet.Entry> entries,
            Set<String> created,
            Set<String> enrolled) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Đã nhập danh sách lớp");
        d.setHeaderText("Đã nhập danh sách lớp " + className);
        if (owner != null) d.initOwner(owner);
        Theme.apply(d.getDialogPane());
        Label result = new Label(message);
        result.setWrapText(true);
        result.setMinHeight(Region.USE_PREF_SIZE);
        Label why = new Label("Học viên mới chưa biết mật khẩu nên chưa vào lớp được. Xuất file Excel (tên đăng nhập,"
                + " mật khẩu tạm) gửi giáo viên phụ trách để phát cho học viên. Server chỉ lưu bản băm của mật khẩu,"
                + " đóng hộp thoại này rồi thì không xuất lại được mật khẩu tạm (khi đó dùng \"Đặt lại mật khẩu\").");
        why.setWrapText(true);
        why.setMinHeight(Region.USE_PREF_SIZE);
        why.getStyleClass().add("muted");
        Label saved = new Label();
        saved.setWrapText(true);
        saved.setMinHeight(Region.USE_PREF_SIZE);
        saved.getStyleClass().add("roster-saved");
        VBox content = new VBox(12, result, why, saved);
        content.setPrefWidth(560);
        d.getDialogPane().setPadding(new Insets(4));
        d.getDialogPane().setContent(content);
        ButtonType export = new ButtonType("Xuất file Excel cho giáo viên…", ButtonBar.ButtonData.OTHER);
        d.getDialogPane().getButtonTypes().addAll(export, ButtonType.CLOSE);
        ((Button) d.getDialogPane().lookupButton(ButtonType.CLOSE)).setText("Đóng");
        Button exportBtn = (Button) d.getDialogPane().lookupButton(export);
        exportBtn.setId("roster-export");
        ButtonBar.setButtonUniformSize(exportBtn, false);
        exportBtn.getStyleClass().add("workspace-primary-button");
        if (created == null) {
            exportBtn.setDisable(true);
            saved.setText("Server đang chạy bản cũ, chưa gửi danh sách tài khoản vừa tạo nên chưa xuất file được.");
        } else if (created.isEmpty() && enrolled.isEmpty()) {
            exportBtn.setDisable(true);
        }
        exportBtn.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume(); // xuất xong vẫn để hộp thoại, còn đọc đường dẫn file
            FileChooser fc = new FileChooser();
            fc.setTitle("Lưu file tài khoản lớp " + className);
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
            fc.setInitialFileName(fileName(className));
            File file = fc.showSaveDialog(d.getDialogPane().getScene().getWindow());
            if (file == null) return;
            Path out = file.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")
                    ? file.toPath()
                    : file.toPath().resolveSibling(file.getName() + ".xlsx");
            try {
                export(out, className, entries, created, enrolled);
                saved.setText("✓ Đã lưu " + out + "\nGửi file này cho giáo viên lớp " + className + ".");
            } catch (Exception ex) {
                saved.setText("Không lưu được file: " + ex.getMessage());
            }
        });
        d.show();
        return d;
    }

    static void export(
            Path out, String className, List<RosterSheet.Entry> entries, Set<String> created, Set<String> enrolled)
            throws java.io.IOException {
        XlsxWriter.write(
                out,
                "Tài khoản",
                RosterSheet.ACCOUNT_WIDTHS,
                RosterSheet.accountSheet(className, entries, created, enrolled, LocalDateTime.now()));
    }

    /** "DSA Nâng cao - K23" → "tai-khoan-DSA-Nang-cao-K23.xlsx". */
    static String fileName(String className) {
        String s = java.text.Normalizer.normalize(className, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return "tai-khoan-" + (s.isEmpty() ? "lop" : s) + ".xlsx";
    }

    private static Set<String> lines(String s) {
        Set<String> out = new HashSet<>();
        for (String x : s.split("\n")) if (!x.isBlank()) out.add(x.strip());
        return out;
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
