package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import labcast.client.player.Player;
import labcast.common.trace.Trace;
import labcast.common.trace.TraceEvent;

/**
 * Bảng chạy từng dòng: tái phát các sự kiện line/vars từ runner (Python, hay debugger của C/C++/Java), đồng bộ với Animation. Bảng chỉ hiện
 * các bước đã chạy tới vị trí của trình phát: Phát/Sau thêm dòng mới ở cuối, Trước/tua lùi bớt dòng.
 */
public final class TraceTablePane extends BorderPane {
    /** Mốc output là byte UTF-8 trước khi thực hiện dòng; -1 nếu runner cũ không gửi. */
    private record Step(
            int number,
            int eventIndex,
            int line,
            String code,
            String function,
            String vars,
            long outputOffset,
            String printed) {
        Step withVars(int idx, String name, String variables) {
            return new Step(number, idx, line, code, name, variables, outputOffset, printed);
        }

        Step withPrinted(String text) {
            return new Step(number, eventIndex, line, code, function, vars, outputOffset, text);
        }
    }

    private final TableView<Step> table = new TableView<>();
    private final Button previous = new Button("◀ Trước");
    private final Button next = new Button("Sau ▶");
    private final Button play = new Button("▶ Phát");
    private final Label position = new Label("0 / 0");
    private final List<Step> steps = new ArrayList<>();
    /** Phần đầu của {@code steps} đã chạy tới; là dữ liệu của bảng. */
    private final ObservableList<Step> shown = FXCollections.observableArrayList();

    private final Label placeholder = new Label("Chưa có dữ liệu chạy từng dòng.");
    /** Chỉ số bước đang đứng trong {@code steps}, -1 khi chưa chạy bước nào. */
    private int current = -1;

    private Player player;
    private IntConsumer onLine = line -> {};
    private boolean changingSelection;

    public TraceTablePane() {
        getStyleClass().add("trace-table-pane");
        TableColumn<Step, Number> number = new TableColumn<>("Bước");
        number.setCellValueFactory(
                v -> new ReadOnlyObjectWrapper<Number>(v.getValue().number()));
        number.setPrefWidth(56);
        number.setMinWidth(56);
        number.setMaxWidth(64);
        TableColumn<Step, Number> line = new TableColumn<>("Dòng");
        line.setCellValueFactory(
                v -> new ReadOnlyObjectWrapper<Number>(v.getValue().line()));
        line.setPrefWidth(56);
        line.setMinWidth(56);
        line.setMaxWidth(64);
        TableColumn<Step, String> command = new TableColumn<>("Lệnh đang chạy");
        command.setCellValueFactory(v -> new ReadOnlyStringWrapper(v.getValue().code()));
        command.setPrefWidth(195);
        TableColumn<Step, String> vars = new TableColumn<>("Biến tại bước này");
        vars.setCellValueFactory(v -> new ReadOnlyStringWrapper(v.getValue().vars()));
        vars.setPrefWidth(210);
        TableColumn<Step, String> printed = new TableColumn<>("Kết quả in ra");
        printed.setCellValueFactory(v -> new ReadOnlyStringWrapper(v.getValue().printed()));
        printed.setPrefWidth(190);
        printed.setMinWidth(130);
        printed.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : (value == null || value.isEmpty() ? "—" : value));
                getStyleClass().remove("trace-output-cell");
                if (!empty && value != null && !value.isEmpty() && !"—".equals(value)) {
                    getStyleClass().add("trace-output-cell");
                    setTooltip(new Tooltip(value));
                } else {
                    setTooltip(null);
                }
            }
        });
        table.getColumns().add(number);
        table.getColumns().add(line);
        table.getColumns().add(command);
        table.getColumns().add(vars);
        table.getColumns().add(printed);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(shown);
        table.setPlaceholder(placeholder);
        table.getStyleClass().add("trace-table");
        table.setFixedCellSize(38);
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Step step, boolean empty) {
                super.updateItem(step, empty);
                getStyleClass().remove("trace-current-step");
                if (!empty
                        && step != null
                        && current >= 0
                        && current < steps.size()
                        && step.number() == steps.get(current).number()) {
                    getStyleClass().add("trace-current-step");
                }
            }
        });

        previous.setOnAction(e -> move(-1));
        next.setOnAction(e -> move(1));
        play.setOnAction(e -> {
            if (player == null) return;
            if (player.playing()) {
                player.pause();
                play.setText("▶ Phát");
            } else {
                player.play();
                play.setText("⏸ Dừng");
            }
        });
        previous.setTooltip(new Tooltip("Tới dòng đã thực thi trước đó"));
        next.setTooltip(new Tooltip("Tới dòng được thực thi tiếp theo"));
        HBox controls = new HBox(8, play, previous, next, position);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setMinHeight(48);
        controls.setPrefHeight(48);
        controls.getStyleClass().add("trace-controls");
        previous.getStyleClass().add("trace-control-button");
        next.getStyleClass().add("trace-control-button");
        play.getStyleClass().addAll("trace-control-button", "trace-play-button");
        // Hiện cả khi chưa có bước Trace: disabled chỉ khóa thao tác.
        // Nút Phát đứng đầu hàng để không bị khuất khi tab hẹp.
        play.setMinWidth(88);
        play.setPrefWidth(88);
        play.setMinHeight(34);
        position.getStyleClass().add("trace-progress-pill");
        // Chỉ hiển thị nút điều khiển và bảng, không có tiêu đề/mô tả/hướng dẫn dư.
        setTop(controls);
        setCenter(table);
        table.getSelectionModel().selectedIndexProperty().addListener((obs, old, value) -> {
            if (changingSelection || player == null) return;
            int i = value.intValue();
            if (i >= 0 && i < shown.size()) {
                player.pause();
                play.setText("▶ Phát");
                player.seek(steps.get(i).eventIndex());
            }
        });
        refreshControls();
    }

    public boolean hasSteps() {
        return !steps.isEmpty();
    }

    public void clear() {
        // Player trước đó có thể còn callback position nhưng đã không còn thuộc bảng.
        player = null;
        steps.clear();
        current = -1;
        changingSelection = true;
        shown.clear();
        table.getSelectionModel().clearSelection();
        changingSelection = false;
        placeholder.setText("Chưa có dữ liệu chạy từng dòng.");
        refreshControls();
    }

    /** Tương thích nơi gọi cũ; không bịa kết quả khi chưa nhận stdout từ runner. */
    public void load(Trace trace, String source, Player nextPlayer, IntConsumer lineHighlighter) {
        load(trace, source, "", nextPlayer, lineHighlighter);
    }

    /** stdout là kết quả thực tế từ cùng lượt chạy, KHÔNG phải output của lần chạy khác. */
    public void load(Trace trace, String source, String stdout, Player nextPlayer, IntConsumer lineHighlighter) {
        clear();
        player = nextPlayer;
        onLine = lineHighlighter == null ? line -> {} : lineHighlighter;
        String[] lines = source.split("\\R", -1);
        String vars = "—";
        String function = "";
        for (int index = 0; index < trace.events().size(); index++) {
            TraceEvent event = trace.events().get(index);
            if ("line".equals(event.t())) {
                int lineNo = event.i("n");
                if (lineNo <= 0) continue;
                String code = lineNo <= lines.length ? lines[lineNo - 1].strip() : "";
                long offset = event.get("o") != null && event.get("o").canConvertToLong()
                        ? Math.max(0, event.get("o").asLong())
                        : -1;
                steps.add(new Step(steps.size() + 1, index, lineNo, code, function, vars, offset, ""));
            } else if ("vars".equals(event.t())) {
                function = event.text("f");
                vars = formatVariables(event.get("d"), function);
                // sys.settrace phát 'line' trước 'vars'. Gắn bảng biến vào đúng dòng này.
                if (!steps.isEmpty()) {
                    Step last = steps.remove(steps.size() - 1);
                    steps.add(last.withVars(index, function, vars));
                }
            }
        }
        boolean mappedOutput = assignStdout(stdout, trace);
        boolean pythonStdout =
                "python".equalsIgnoreCase(trace.header().path("lang").asText("python"));
        placeholder.setText(
                steps.isEmpty() ? "Chưa có dữ liệu chạy từng dòng." : "Bấm ▶ Phát hoặc Sau ▶ để chạy từng dòng.");
        nextPlayer.onPosition(p -> {
            if (player == nextPlayer) syncWithPlayer(p);
        });
        syncWithPlayer(nextPlayer.position());
        refreshControls();
    }

    /**
     * Gán khoảng stdout giữa hai breakpoint cho dòng ở breakpoint đầu: dòng `print(x)`
     * nhận chính kết quả in ra của nó, không phải toàn bộ Console. Mốc runner dùng byte UTF-8.
     *
     * Với C/C++/Java, stdout có thể bị đệm cho tới lúc kết thúc. Không gán dữ liệu
     * không có mốc thời gian vào một dòng bất kỳ (tránh kết quả sai trong vòng lặp).
     */
    private boolean assignStdout(String stdout, Trace trace) {
        if (steps.isEmpty() || stdout == null) return false;
        byte[] bytes = stdout.getBytes(StandardCharsets.UTF_8);
        boolean python = "python".equalsIgnoreCase(trace.header().path("lang").asText("python"));
        boolean foundOffsets = false;
        for (int i = 0; i < steps.size(); i++) {
            Step currentStep = steps.get(i);
            if (currentStep.outputOffset() < 0) continue;
            foundOffsets = true;
            long end = i + 1 < steps.size() ? steps.get(i + 1).outputOffset() : -1;
            // Dòng cuối: Python StringIO theo dõi trực tiếp, nên output cuối có thể gán.
            // Với debugger native, stdout có thể chỉ được flush lúc thoát: không suy diễn.
            if (end < 0 && python && !trace.truncated()) end = bytes.length;
            if (end < currentStep.outputOffset() || end < 0) continue;
            int startByte = (int) Math.min(bytes.length, currentStep.outputOffset());
            int endByte = (int) Math.min(bytes.length, end);
            if (endByte <= startByte) continue;
            String piece = new String(bytes, startByte, endByte - startByte, StandardCharsets.UTF_8);
            String displayed = piece.replace("\r", "")
                    .replace("\n", " ↵ ")
                    .replace("\t", " ⇥ ")
                    .strip();
            if (displayed.length() > 260) displayed = displayed.substring(0, 259) + "…";
            steps.set(i, currentStep.withPrinted(displayed));
        }
        return foundOffsets;
    }

    private static String formatVariables(JsonNode data, String function) {
        List<String> values = new ArrayList<>();
        if (data != null && data.isArray()) {
            for (JsonNode item : data) {
                if (item.isArray() && item.size() > 1) {
                    values.add(item.get(0).asText() + "=" + item.get(1).asText());
                }
            }
        }
        String result = values.isEmpty() ? "—" : String.join("  •  ", values);
        return function == null || function.isBlank() ? result : function + "(): " + result;
    }

    private void move(int direction) {
        if (steps.isEmpty() || player == null) return;
        int target = current < 0 ? 0 : Math.max(0, Math.min(steps.size() - 1, current + direction));
        player.pause();
        play.setText("▶ Phát");
        player.seek(steps.get(target).eventIndex());
    }

    private void syncWithPlayer(int eventIndex) {
        int found = -1;
        int lo = 0, hi = steps.size() - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (steps.get(mid).eventIndex() <= eventIndex) {
                found = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        current = found;
        changingSelection = true;
        // bảng giữ đúng các bước 0..found: tua tới thì nối thêm, tua lùi thì cắt bớt ở cuối
        if (shown.size() > found + 1) shown.remove(found + 1, shown.size());
        else if (shown.size() < found + 1) shown.addAll(steps.subList(shown.size(), found + 1));
        if (found < 0) table.getSelectionModel().clearSelection();
        else if (table.getSelectionModel().getSelectedIndex() != found) {
            table.getSelectionModel().select(found);
            table.scrollTo(found);
        }
        changingSelection = false;
        table.refresh();
        if (found >= 0) onLine.accept(steps.get(found).line());
        play.setText(player != null && player.playing() ? "⏸ Dừng" : "▶ Phát");
        refreshControls();
    }

    private void refreshControls() {
        int idx = current;
        position.setText(steps.isEmpty() ? "0 / 0" : (Math.max(0, idx + 1) + " / " + steps.size()));
        previous.setDisable(steps.isEmpty() || idx <= 0);
        next.setDisable(steps.isEmpty() || idx >= steps.size() - 1);
        play.setDisable(steps.isEmpty());
    }
}
