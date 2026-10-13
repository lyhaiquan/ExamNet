package labcast.client.views.bangsql;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import labcast.common.trace.TraceEvent;

/**
 * Trạng thái bộ vẽ {@code bang-sql}, thuần Java để test. Giữ "bảng đang làm việc" (cột, dòng, dấu từng
 * dòng, nhóm của từng dòng) và bảng phụ bên phải khi ghép. Mỗi sự kiện {@code sql.*} đổi trạng thái
 * ngay, đầy đủ; thay đổi trả về chỉ để bộ vẽ chọn hiệu ứng. Áp lại từ đầu luôn ra cùng trạng thái, nên
 * trình phát tua được.
 *
 * <p>Sự kiện do {@code StepPlanner} phía server sinh; chỉ số dòng luôn là chỉ số trong bảng hiện tại.
 */
public final class SqlTableModel {

    /** Dấu của một dòng, ứng với màu chung của mọi bộ vẽ (spec §7.4). */
    public enum Mark {
        NONE,
        /** Thoả điều kiện — xanh lá. */
        OK,
        /** Không thoả, sắp bị loại — đỏ. */
        BAD,
        /** Dòng vừa thêm (INSERT) — xanh lá. */
        NEW,
        /** Dòng vừa sửa (UPDATE), ô đổi tô xanh dương. */
        CHANGED,
        /** Dòng vừa xoá (DELETE) — đỏ, gạch bỏ. */
        DELETED
    }

    public record Table(String title, List<String> cols, List<List<Object>> rows) {}

    public sealed interface Change permits Redraw, Remove, Reorder {}

    /** Vẽ lại; {@code fresh}: bảng mới hẳn (hiện dần). */
    public record Redraw(boolean fresh) implements Change {}

    /** Các dòng {@code removed} (chỉ số trước khi loại) mờ dần rồi biến mất. */
    public record Remove(List<Integer> removed) implements Change {}

    /** Dòng mới thứ i đến từ dòng cũ {@code from.get(i)} — trượt về chỗ mới. */
    public record Reorder(List<Integer> from) implements Change {}

    private String title = "";
    private List<String> cols = new ArrayList<>();
    private List<List<Object>> rows = new ArrayList<>();
    private List<Mark> marks = new ArrayList<>();
    private List<Integer> groups = new ArrayList<>();
    private List<Set<Integer>> changed = new ArrayList<>();
    private Table side;
    private final List<int[]> pairs = new ArrayList<>();
    private boolean result;

    public void reset() {
        title = "";
        cols = new ArrayList<>();
        rows = new ArrayList<>();
        marks = new ArrayList<>();
        groups = new ArrayList<>();
        changed = new ArrayList<>();
        side = null;
        pairs.clear();
        result = false;
    }

    public List<Change> apply(TraceEvent e) {
        JsonNode d = e.data();
        switch (e.t()) {
            case "sql.table" -> {
                Table t = new Table(name(d), strings(d.path("cols")), rows(d.path("rows")));
                if (d.path("side").asBoolean(false)) {
                    side = t;
                    pairs.clear();
                    return List.of(new Redraw(false));
                }
                setMain(t.title(), t.cols(), t.rows());
                return List.of(new Redraw(true));
            }
            case "sql.rename" -> {
                cols = strings(d.path("cols"));
                return List.of(new Redraw(false));
            }
            case "sql.pair" -> {
                pairs.add(new int[] {d.path("l").asInt(), d.path("r").asInt()});
                return List.of(new Redraw(false));
            }
            case "sql.join" -> {
                String t = title + " ⨝ " + (side == null ? "" : side.title());
                setMain(t, strings(d.path("cols")), rows(d.path("rows")));
                return List.of(new Redraw(true));
            }
            case "sql.mark" -> {
                JsonNode rs = d.path("rows");
                JsonNode ok = d.path("ok");
                for (int i = 0; i < rs.size(); i++) {
                    int r = rs.get(i).asInt();
                    if (r >= 0 && r < marks.size()) {
                        marks.set(r, ok.get(i).asBoolean() ? Mark.OK : Mark.BAD);
                    }
                }
                return List.of(new Redraw(false));
            }
            case "sql.keep" -> {
                return List.of(keep(ints(d.path("rows"))));
            }
            case "sql.group" -> {
                List<Integer> from = new ArrayList<>();
                List<Integer> g = new ArrayList<>();
                JsonNode gs = d.path("groups");
                for (int k = 0; k < gs.size(); k++) {
                    for (JsonNode i : gs.get(k)) {
                        from.add(i.asInt());
                        g.add(k);
                    }
                }
                reorder(from);
                groups = g;
                return List.of(new Reorder(from));
            }
            case "sql.agg" -> {
                List<List<Object>> rs = rows(d.path("rows"));
                setMain("Mỗi nhóm một dòng", strings(d.path("cols")), rs);
                for (int i = 0; i < rs.size(); i++) {
                    groups.set(i, i);
                }
                return List.of(new Redraw(true));
            }
            case "sql.project" -> {
                List<Integer> g = groups;
                setMain(title, strings(d.path("cols")), rows(d.path("rows")));
                if (g.size() == groups.size()) {
                    groups = g;
                }
                return List.of(new Redraw(true));
            }
            case "sql.distinct" -> {
                Set<Integer> dup = new HashSet<>(ints(d.path("dup")));
                List<Integer> keep = new ArrayList<>();
                for (int i = 0; i < rows.size(); i++) {
                    if (!dup.contains(i)) {
                        keep.add(i);
                    }
                }
                return List.of(keep(keep));
            }
            case "sql.order" -> {
                List<Integer> from = ints(d.path("perm"));
                reorder(from);
                return List.of(new Reorder(from));
            }
            case "sql.limit" -> {
                int off = d.path("offset").asInt();
                int count = d.path("count").asInt();
                List<Integer> keep = new ArrayList<>();
                for (int i = off; i < Math.min(rows.size(), off + count); i++) {
                    keep.add(i);
                }
                return List.of(keep(keep));
            }
            case "sql.result" -> {
                setMain("Kết quả", strings(d.path("cols")), rows(d.path("rows")));
                result = true;
                return List.of(new Redraw(true));
            }
            case "sql.setop" -> {
                setMain(d.path("op").asText(), strings(d.path("cols")), rows(d.path("rows")));
                return List.of(new Redraw(true));
            }
            case "sql.diff" -> {
                for (JsonNode c : d.path("changes")) {
                    switch (c.path("kind").asText()) {
                        case "sua" -> {
                            int i = c.path("i").asInt();
                            rows.set(i, row(c.path("row")));
                            marks.set(i, Mark.CHANGED);
                            changed.set(i, new HashSet<>(ints(c.path("cells"))));
                        }
                        case "xoa" -> marks.set(c.path("i").asInt(), Mark.DELETED);
                        case "them" -> {
                            rows.add(row(c.path("row")));
                            marks.add(Mark.NEW);
                            groups.add(-1);
                            changed.add(new HashSet<>());
                        }
                        default -> {}
                    }
                }
                return List.of(new Redraw(false));
            }
            default -> {
                return List.of();
            }
        }
    }

    private void setMain(String t, List<String> c, List<List<Object>> r) {
        title = t;
        cols = c;
        rows = r;
        marks = new ArrayList<>(Collections.nCopies(r.size(), Mark.NONE));
        groups = new ArrayList<>(Collections.nCopies(r.size(), -1));
        changed = new ArrayList<>();
        for (int i = 0; i < r.size(); i++) {
            changed.add(new HashSet<>());
        }
        side = null;
        pairs.clear();
        result = false;
    }

    private Remove keep(List<Integer> keep) {
        Set<Integer> k = new HashSet<>(keep);
        List<Integer> removed = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            if (!k.contains(i)) {
                removed.add(i);
            }
        }
        List<List<Object>> r = new ArrayList<>();
        List<Integer> g = new ArrayList<>();
        for (int i : keep) {
            r.add(rows.get(i));
            g.add(groups.get(i));
        }
        rows = r;
        groups = g;
        marks = new ArrayList<>(Collections.nCopies(r.size(), Mark.NONE));
        changed = new ArrayList<>();
        for (int i = 0; i < r.size(); i++) {
            changed.add(new HashSet<>());
        }
        return new Remove(removed);
    }

    private void reorder(List<Integer> from) {
        List<List<Object>> r = new ArrayList<>();
        List<Mark> m = new ArrayList<>();
        List<Integer> g = new ArrayList<>();
        List<Set<Integer>> c = new ArrayList<>();
        for (int i : from) {
            r.add(rows.get(i));
            m.add(marks.get(i));
            g.add(groups.get(i));
            c.add(changed.get(i));
        }
        rows = r;
        marks = m;
        groups = g;
        changed = c;
    }

    private static String name(JsonNode d) {
        String n = d.path("name").asText();
        String a = d.path("alias").asText("");
        return a.isEmpty() || a.equals(n) ? n : n + " " + a;
    }

    private static List<String> strings(JsonNode a) {
        List<String> out = new ArrayList<>();
        a.forEach(x -> out.add(x.asText()));
        return out;
    }

    private static List<Integer> ints(JsonNode a) {
        List<Integer> out = new ArrayList<>();
        a.forEach(x -> out.add(x.asInt()));
        return out;
    }

    private static List<List<Object>> rows(JsonNode a) {
        List<List<Object>> out = new ArrayList<>();
        a.forEach(r -> out.add(row(r)));
        return out;
    }

    private static List<Object> row(JsonNode r) {
        List<Object> out = new ArrayList<>();
        for (JsonNode v : r) {
            out.add(
                    v.isNull()
                            ? null
                            : v.isIntegralNumber() ? (Object) v.asLong() : v.isNumber() ? v.asDouble() : v.asText());
        }
        return out;
    }

    public String title() {
        return title;
    }

    public List<String> cols() {
        return Collections.unmodifiableList(cols);
    }

    public List<List<Object>> rows() {
        return Collections.unmodifiableList(rows);
    }

    public List<Mark> marks() {
        return Collections.unmodifiableList(marks);
    }

    /** Nhóm của từng dòng (chỉ số màu), −1 nếu chưa gom nhóm. */
    public List<Integer> groups() {
        return Collections.unmodifiableList(groups);
    }

    public Set<Integer> changedCells(int row) {
        return Collections.unmodifiableSet(changed.get(row));
    }

    /** Bảng bên phải khi đang ghép (JOIN) hoặc vế 2 của phép tập hợp; null nếu không có. */
    public Table side() {
        return side;
    }

    /** Các cặp dòng đã nối, mỗi cặp {chỉ số trái, chỉ số phải}; −1 là không có cặp. */
    public List<int[]> pairs() {
        return Collections.unmodifiableList(pairs);
    }

    /** Cặp vừa nối (đang xét), null nếu chưa có. */
    public int[] currentPair() {
        return pairs.isEmpty() ? null : pairs.get(pairs.size() - 1);
    }

    /** Bảng đang hiện là kết quả cuối. */
    public boolean isResult() {
        return result;
    }
}
