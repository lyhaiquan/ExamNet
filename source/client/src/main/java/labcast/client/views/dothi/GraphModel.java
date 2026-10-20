package labcast.client.views.dothi;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import labcast.client.player.VizText;
import labcast.common.trace.TraceEvent;

/**
 * Trạng thái của bộ vẽ đồ thị, thuần Java để test (spec §7.3): {@code edge} thêm cạnh {@code a}–{@code b}
 * (trọng số {@code x}), {@code read} xét đỉnh ({@code id}) hay xét cạnh ({@code a}, {@code b}), {@code mark}
 * tô màu đỉnh hay cạnh, {@code set} ghi chữ dưới đỉnh, {@code pos} đặt chỗ đỉnh.
 */
public final class GraphModel {

    public sealed interface Change permits Structure, ReadVertex, ReadEdge, Marked, Label {}

    /** Thêm cạnh hay đổi chỗ đỉnh: vẽ lại hình. */
    public record Structure() implements Change {}

    public record ReadVertex(String id) implements Change {}

    public record ReadEdge(String a, String b) implements Change {}

    public record Marked() implements Change {}

    public record Label(String id) implements Change {}

    /** Một cạnh theo thứ tự thêm; {@code weight} null là không trọng số. */
    public static final class E {
        public final String a;
        public final String b;
        public final String weight;
        public String mark;

        E(String a, String b, String weight) {
            this.a = a;
            this.b = b;
            this.weight = weight;
        }
    }

    private final List<String> vertices = new ArrayList<>();
    private final List<E> edges = new ArrayList<>();
    private final Map<String, String> marks = new LinkedHashMap<>();
    private final Map<String, String> labels = new LinkedHashMap<>();
    private final Map<String, double[]> positions = new LinkedHashMap<>();
    private boolean directed;

    /** Ảnh chụp do {@code DoThi.trang_thai()} ghi; null là đồ thị rỗng. */
    public void reset(JsonNode s) {
        vertices.clear();
        edges.clear();
        marks.clear();
        labels.clear();
        positions.clear();
        directed = false;
        if (s == null || !s.isObject()) {
            return;
        }
        directed = s.path("co_huong").asBoolean(false);
        s.path("dinh").forEach(v -> vertices.add(id(v)));
        for (JsonNode c : s.path("canh")) {
            edges.add(new E(id(c.get(0)), id(c.get(1)), weight(c.get(2))));
        }
        s.path("mau").forEach(m -> marks.put(id(m.get(0)), m.get(1).asText()));
        for (JsonNode m : s.path("mau_canh")) {
            E e = find(id(m.get(0)), id(m.get(1)));
            if (e != null) {
                e.mark = m.get(2).asText();
            }
        }
        s.path("nhan").forEach(m -> labels.put(id(m.get(0)), m.get(1).asText()));
        s.path("vt")
                .forEach(m -> positions.put(
                        id(m.get(0)),
                        new double[] {m.get(1).asDouble(), m.get(2).asDouble()}));
    }

    public List<Change> apply(TraceEvent e) {
        switch (e.t()) {
            case "edge" -> {
                String a = id(e.get("a"));
                String b = id(e.get("b"));
                addVertex(a);
                addVertex(b);
                edges.add(new E(a, b, weight(e.get("x"))));
                return List.of(new Structure());
            }
            case "read" -> {
                if (e.data().has("id")) {
                    return List.of(new ReadVertex(id(e.get("id"))));
                }
                return List.of(new ReadEdge(id(e.get("a")), id(e.get("b"))));
            }
            case "mark" -> {
                JsonNode m = e.get("m");
                String mark = m == null || m.isNull() ? null : m.asText();
                if (e.data().has("id")) {
                    String v = id(e.get("id"));
                    if (mark == null) {
                        marks.remove(v);
                    } else {
                        marks.put(v, mark);
                    }
                } else {
                    E ed = find(id(e.get("a")), id(e.get("b")));
                    if (ed != null) {
                        ed.mark = mark;
                    }
                }
                return List.of(new Marked());
            }
            case "set" -> {
                String v = id(e.get("id"));
                JsonNode x = e.get("x");
                if (x == null || x.isNull()) {
                    labels.remove(v);
                } else {
                    labels.put(v, x.asText());
                }
                return List.of(new Label(v));
            }
            case "pos" -> {
                positions.put(
                        id(e.get("id")),
                        new double[] {e.get("x").asDouble(), e.get("y").asDouble()});
                return List.of(new Structure());
            }
            default -> {
                return List.of();
            }
        }
    }

    /** Cạnh a–b (vô hướng thì không kể chiều); null nếu không có. */
    public E find(String a, String b) {
        for (E e : edges) {
            if ((e.a.equals(a) && e.b.equals(b)) || (!directed && e.a.equals(b) && e.b.equals(a))) {
                return e;
            }
        }
        return null;
    }

    /**
     * Chỗ đỉnh trong khung 0..1: chỗ người soạn bài đặt, còn lại xếp đều quanh vòng tròn theo thứ tự đỉnh, đỉnh
     * đầu ở trên cùng, đi theo chiều kim đồng hồ.
     */
    public Map<String, double[]> layout() {
        Map<String, double[]> out = new LinkedHashMap<>();
        int n = vertices.size();
        for (int k = 0; k < n; k++) {
            String v = vertices.get(k);
            double[] p = positions.get(v);
            if (p == null) {
                double ang = -Math.PI / 2 + 2 * Math.PI * k / Math.max(1, n);
                p = n == 1
                        ? new double[] {0.5, 0.5}
                        : new double[] {0.5 + 0.45 * Math.cos(ang), 0.5 + 0.45 * Math.sin(ang)};
            }
            out.put(v, p);
        }
        return out;
    }

    public List<String> vertices() {
        return List.copyOf(vertices);
    }

    public List<E> edges() {
        return List.copyOf(edges);
    }

    public String mark(String v) {
        return marks.get(v);
    }

    public String label(String v) {
        return labels.get(v);
    }

    public boolean directed() {
        return directed;
    }

    /** Trạng thái dạng chữ để test. */
    public String describe() {
        StringBuilder sb =
                new StringBuilder(directed ? "có hướng " : "").append(vertices).append(' ');
        for (E e : edges) {
            sb.append(e.a).append(directed ? ">" : "-").append(e.b);
            if (e.weight != null) {
                sb.append('(').append(e.weight).append(')');
            }
            if (e.mark != null) {
                sb.append(':').append(e.mark);
            }
            sb.append(' ');
        }
        sb.append(marks).append(' ').append(labels);
        return sb.toString();
    }

    private void addVertex(String v) {
        if (!vertices.contains(v)) {
            vertices.add(v);
        }
    }

    private static String id(JsonNode n) {
        return VizText.of(n);
    }

    private static String weight(JsonNode n) {
        return n == null || n.isNull() ? null : VizText.of(n);
    }
}
