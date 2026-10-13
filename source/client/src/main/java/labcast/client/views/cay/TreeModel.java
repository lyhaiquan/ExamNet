package labcast.client.views.cay;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import labcast.client.player.VizText;
import labcast.common.trace.TraceEvent;

/**
 * Trạng thái của bộ vẽ cây nhị phân, thuần Java để test (spec §7.3: {@code node}, {@code edge}, {@code read},
 * {@code set}, {@code mark}).
 *
 * <p>Ngoài cây chính (đi từ gốc) còn có các <b>mảnh rời</b>: nút vừa tạo chưa gắn vào đâu, hay một nhánh tạm
 * tách ra giữa phép xoay AVL ({@code y.trai = x.phai} làm {@code x} rời cây cho tới khi gán lại). Mảnh rời vẫn
 * được vẽ, bên phải cây chính, để phép xoay không làm nút biến mất giữa chừng. Nút đã từng nằm trong cây mà
 * giờ đứng một mình (đã bị xoá) thì không vẽ nữa.
 */
public final class TreeModel {

    public sealed interface Change permits Shape, Read, Written, Marked {}

    /** Cây đổi hình (thêm nút, đổi con, đổi gốc): vẽ lại, nút trượt về chỗ mới. */
    public record Shape(int id) implements Change {}

    /** Đang xét nút (đọc khoá). */
    public record Read(int id) implements Change {}

    /** Nút nhận khoá mới. */
    public record Written(int id) implements Change {}

    public record Marked(int id) implements Change {}

    /** Một nút; {@code left}/{@code right} là id con, null = không có. */
    public static final class N {
        public final int id;
        public String text;
        public Integer left;
        public Integer right;
        public String mark;
        /** Đã từng nằm trong cây (làm gốc hay làm con). */
        public boolean attached;

        N(int id, String text) {
            this.id = id;
            this.text = text;
        }
    }

    /** Vị trí vẽ: {@code col} là thứ tự từ trái sang (theo duyệt giữa), {@code depth} là tầng. */
    public record Pos(int col, int depth) {}

    private final Map<Integer, N> nodes = new LinkedHashMap<>();
    private Integer root;

    /** {@code {"kieu": "cay", "goc": id, "nut": [[id, khoá, trái, phải, màu, đã gắn], …]}}; null là cây rỗng. */
    public void reset(JsonNode snap) {
        nodes.clear();
        root = null;
        if (snap == null || !snap.isObject()) {
            return;
        }
        root = intOrNull(snap.get("goc"));
        for (JsonNode a : snap.path("nut")) {
            N n = new N(a.get(0).asInt(), VizText.of(a.get(1)));
            n.left = intOrNull(a.get(2));
            n.right = intOrNull(a.get(3));
            n.mark = a.get(4) == null || a.get(4).isNull() ? null : a.get(4).asText();
            n.attached = a.path(5).asBoolean(false);
            nodes.put(n.id, n);
        }
    }

    public List<Change> apply(TraceEvent e) {
        switch (e.t()) {
            case "node" -> {
                int id = e.i("id");
                nodes.put(id, new N(id, VizText.of(e.get("x"))));
                return List.of(new Shape(id));
            }
            case "edge" -> {
                Integer w = intOrNull(e.get("w"));
                Integer u = intOrNull(e.get("u"));
                if (u == null) {
                    root = w;
                } else {
                    N p = nodes.get(u);
                    if (p == null) {
                        throw new IllegalStateException("edge từ nút chưa có: " + u);
                    }
                    if ("L".equals(e.text("c"))) {
                        p.left = w;
                    } else {
                        p.right = w;
                    }
                }
                if (w != null && nodes.containsKey(w)) {
                    nodes.get(w).attached = true;
                }
                return List.of(new Shape(w == null ? -1 : w));
            }
            case "read" -> {
                int id = e.i("id");
                return nodes.containsKey(id) ? List.of(new Read(id)) : List.of();
            }
            case "set" -> {
                N n = nodes.get(e.i("id"));
                if (n == null) {
                    return List.of();
                }
                n.text = VizText.of(e.get("x"));
                return List.of(new Written(n.id));
            }
            case "mark" -> {
                N n = nodes.get(e.i("id"));
                if (n == null) {
                    return List.of();
                }
                JsonNode m = e.get("m");
                n.mark = m == null || m.isNull() ? null : m.asText();
                return List.of(new Marked(n.id));
            }
            default -> {
                return List.of();
            }
        }
    }

    public Integer root() {
        return root;
    }

    public N node(int id) {
        return nodes.get(id);
    }

    /**
     * Chỗ vẽ của mọi nút cần vẽ: cây chính trước, rồi từng mảnh rời theo thứ tự tạo. Cột theo duyệt giữa (với
     * cây nhị phân tìm kiếm thì đúng thứ tự khoá), tầng tính từ gốc của mảnh.
     */
    public Map<Integer, Pos> layout() {
        Map<Integer, Pos> out = new LinkedHashMap<>();
        Set<Integer> seen = new HashSet<>();
        int[] col = {0};
        if (root != null) {
            inorder(root, 0, out, seen, col);
        }
        Set<Integer> hasParent = new HashSet<>();
        for (N n : nodes.values()) {
            if (n.left != null) {
                hasParent.add(n.left);
            }
            if (n.right != null) {
                hasParent.add(n.right);
            }
        }
        for (N n : nodes.values()) {
            if (seen.contains(n.id) || hasParent.contains(n.id)) {
                continue;
            }
            boolean alone = n.left == null && n.right == null;
            if (alone && n.attached) {
                continue; // đã bị xoá khỏi cây
            }
            if (!out.isEmpty()) {
                col[0]++; // chừa một cột giữa các mảnh
            }
            inorder(n.id, 0, out, seen, col);
        }
        return out;
    }

    private void inorder(int id, int depth, Map<Integer, Pos> out, Set<Integer> seen, int[] col) {
        N n = nodes.get(id);
        if (n == null || !seen.add(id)) {
            return; // nút lạ, hoặc nút đã vẽ (giữa phép xoay một nút có thể tạm có hai cha)
        }
        if (n.left != null) {
            inorder(n.left, depth + 1, out, seen, col);
        }
        out.put(id, new Pos(col[0]++, depth));
        if (n.right != null) {
            inorder(n.right, depth + 1, out, seen, col);
        }
    }

    /** Cây dạng chữ để test: duyệt giữa, mỗi nút "khoá" hoặc "(trái khoá phải)". */
    public String shape() {
        return root == null ? "." : shape(root, new HashSet<>());
    }

    private String shape(Integer id, Set<Integer> seen) {
        if (id == null || !seen.add(id)) {
            return ".";
        }
        N n = nodes.get(id);
        if (n.left == null && n.right == null) {
            return n.text;
        }
        return "(" + shape(n.left, seen) + " " + n.text + " " + shape(n.right, seen) + ")";
    }

    public List<N> nodes() {
        return new ArrayList<>(nodes.values());
    }

    private static Integer intOrNull(JsonNode n) {
        return n == null || n.isNull() || n.isMissingNode() ? null : n.asInt();
    }
}
