package labcast.client.views.luoi;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import labcast.client.player.VizText;
import labcast.common.trace.TraceEvent;

/**
 * Trạng thái của bộ vẽ bảng 2 chiều {@code luoi} (task 3.18), thuần Java để test.
 *
 * <p>Các {@code cell_r} đứng ngay trước một {@code cell_w} là <b>các ô phụ thuộc</b> của ô vừa ghi: bộ vẽ
 * nối mũi tên từ chúng tới ô đó, nên thấy được công thức truy hồi, ví dụ {@code dp[i][j]} lấy từ {@code
 * dp[i-1][j]} và {@code dp[i][j-1]}. Không tính chính ô đang ghi ({@code dp[i][j] = max(dp[i][j], …)}) và
 * giữ tối đa {@link #MAX_DEPS} ô gần nhất. {@code mark} tô xanh lá và giữ màu (đường truy vết).
 */
public final class GridModel {
    public static final int MAX_DEPS = 6;

    public record Cell(int i, int j) {}

    public sealed interface Change permits Read, Write, Mark {}

    public record Read(int i, int j) implements Change {}

    public record Write(int i, int j, String text, List<Cell> deps) implements Change {}

    public record Mark(int i, int j) implements Change {}

    private String[][] cells = new String[0][0];
    private List<String> rowLabels = List.of();
    private List<String> colLabels = List.of();
    private final LinkedHashSet<Cell> readsSinceWrite = new LinkedHashSet<>();
    private final Set<Cell> written = new HashSet<>();
    private final Set<Cell> marked = new HashSet<>();

    /**
     * {@code snapshot}: trạng thái do {@code Bang.trang_thai()} ghi, {@code {"o": [[...]], "hang": [...],
     * "cot": [...], "ghi": [[i, j]…], "dau": [[i, j]…]}} (nhãn hai trục, ô đã ghi, ô đã đánh dấu đều tuỳ
     * chọn); mảng 2 chiều trơn cũng nhận.
     */
    public void reset(JsonNode snapshot) {
        readsSinceWrite.clear();
        written.clear();
        marked.clear();
        rowLabels = List.of();
        colLabels = List.of();
        JsonNode o = snapshot;
        if (snapshot != null && snapshot.isObject()) {
            o = snapshot.get("o");
            rowLabels = texts(snapshot.get("hang"));
            colLabels = texts(snapshot.get("cot"));
            cellsOf(snapshot.get("ghi"), written);
            cellsOf(snapshot.get("dau"), marked);
        }
        if (o == null || !o.isArray()) {
            cells = new String[0][0];
            return;
        }
        int rows = o.size();
        int cols = 0;
        for (JsonNode r : o) {
            cols = Math.max(cols, r.size());
        }
        cells = new String[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                JsonNode v = o.get(i).get(j);
                cells[i][j] = v == null ? "" : VizText.of(v);
            }
        }
    }

    public List<Change> apply(TraceEvent e) {
        int i = e.i("i");
        int j = e.i("j");
        switch (e.t()) {
            case "cell_r" -> {
                if (!inside(i, j)) {
                    return List.of();
                }
                Cell c = new Cell(i, j);
                readsSinceWrite.remove(c); // đọc lại thì xếp về cuối (gần nhất)
                readsSinceWrite.add(c);
                return List.of(new Read(i, j));
            }
            case "cell_w" -> {
                if (!inside(i, j)) {
                    readsSinceWrite.clear();
                    return List.of();
                }
                Cell self = new Cell(i, j);
                List<Cell> deps = new ArrayList<>(readsSinceWrite);
                deps.remove(self);
                if (deps.size() > MAX_DEPS) {
                    deps = new ArrayList<>(deps.subList(deps.size() - MAX_DEPS, deps.size()));
                }
                readsSinceWrite.clear();
                String text = VizText.of(e.get("x"));
                cells[i][j] = text;
                written.add(self);
                return List.of(new Write(i, j, text, List.copyOf(deps)));
            }
            case "mark" -> {
                if (!inside(i, j)) {
                    return List.of();
                }
                marked.add(new Cell(i, j));
                return List.of(new Mark(i, j));
            }
            default -> {
                return List.of();
            }
        }
    }

    public int rows() {
        return cells.length;
    }

    public int cols() {
        return cells.length == 0 ? 0 : cells[0].length;
    }

    public String text(int i, int j) {
        return cells[i][j];
    }

    /** Nhãn hàng i (ví dụ ký tự thứ i của xâu thứ nhất); null nếu bài không khai báo nhãn. */
    public String rowLabel(int i) {
        return i < rowLabels.size() ? rowLabels.get(i) : null;
    }

    public String colLabel(int j) {
        return j < colLabels.size() ? colLabels.get(j) : null;
    }

    public boolean hasLabels() {
        return !rowLabels.isEmpty() || !colLabels.isEmpty();
    }

    /** Ô đã từng được ghi (tô nhạt để thấy phần bảng đã điền). */
    public boolean written(int i, int j) {
        return written.contains(new Cell(i, j));
    }

    public boolean marked(int i, int j) {
        return marked.contains(new Cell(i, j));
    }

    private boolean inside(int i, int j) {
        return i >= 0 && j >= 0 && i < rows() && j < cols();
    }

    /** {@code [[i, j], …]} → tập ô. */
    private static void cellsOf(JsonNode a, Set<Cell> into) {
        if (a != null && a.isArray()) {
            for (JsonNode c : a) {
                if (c.isArray() && c.size() == 2) {
                    into.add(new Cell(c.get(0).asInt(), c.get(1).asInt()));
                }
            }
        }
    }

    private static List<String> texts(JsonNode a) {
        if (a == null || !a.isArray()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        a.forEach(x -> out.add(VizText.of(x)));
        return out;
    }
}
