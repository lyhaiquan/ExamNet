package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import labcast.client.player.Player;
import labcast.common.trace.Trace;
import labcast.common.trace.TraceEvent;

/**
 * Bảng chạy từng dòng: tái phát các sự kiện line/vars từ runner (Python, hay debugger của C/C++/Java), đồng bộ với Animation. Bảng chỉ hiện
 * các bước đã chạy tới vị trí của trình phát: Phát/Sau thêm dòng mới ở cuối, Trước/tua lùi bớt dòng.
 */
public final class TraceTablePane extends BorderPane {
    private record Step(int number, int eventIndex, int line, String code, String function, String vars) {}

    private final TableView<Step> table = new TableView<>();
    private final Button previous = new Button("◀ Trước");
    private final Button next = new Button("Sau ▶");
    private final Button play = new Button("▶ Phát");
    private final Label position = new Label("Chưa có trace");
    private final Label hint = new Label("Bấm 'Trace từng dòng' để ghi lại các bước chạy.");
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
        // Không có cột lệnh: dòng đang chạy đã tô sáng trong editor, để chỗ cho cột biến
        TableColumn<Step, String> vars = new TableColumn<>("Biến tại bước này");
        vars.setCellValueFactory(v -> new ReadOnlyStringWrapper(v.getValue().vars()));
        vars.setPrefWidth(405);
        table.getColumns().add(number);
        table.getColumns().add(line);
        table.getColumns().add(vars);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(shown);
        table.setPlaceholder(placeholder);
        table.getStyleClass().add("trace-table");
        table.setFixedCellSize(31);

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
        HBox controls = new HBox(8, previous, play, next, position);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getStyleClass().add("trace-controls");
        hint.setWrapText(true);
        hint.getStyleClass().add("trace-hint");
        VBox top = new VBox(7, controls, hint);
        setTop(top);
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
        hint.setText("Bấm 'Trace từng dòng' để ghi lại các bước chạy.");
        refreshControls();
    }

    public void load(Trace trace, String source, Player nextPlayer, IntConsumer lineHighlighter) {
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
                steps.add(new Step(steps.size() + 1, index, lineNo, code, function, vars));
            } else if ("vars".equals(event.t())) {
                function = event.text("f");
                vars = formatVariables(event.get("d"), function);
                // sys.settrace phát 'line' trước 'vars'. Gắn bảng biến vào đúng dòng này.
                if (!steps.isEmpty()) {
                    Step last = steps.remove(steps.size() - 1);
                    steps.add(new Step(last.number(), index, last.line(), last.code(), function, vars));
                }
            }
        }
        placeholder.setText(
                steps.isEmpty() ? "Chưa có dữ liệu chạy từng dòng." : "Bấm ▶ Phát hoặc Sau ▶ để chạy từng dòng.");
        hint.setText(
                steps.isEmpty()
                        ? "Lượt chạy này chưa có sự kiện line/vars. Hãy dùng Trace từng dòng."
                        : "Mỗi dòng là trạng thái TRƯỚC khi chạy lệnh ở dòng đó; dòng mới hiện ra khi chạy tới. Nhấp một dòng để tua lại.");
        nextPlayer.onPosition(p -> {
            if (player == nextPlayer) syncWithPlayer(p);
        });
        syncWithPlayer(nextPlayer.position());
        refreshControls();
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
