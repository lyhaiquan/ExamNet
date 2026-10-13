package labcast.client.views.caygoi;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import labcast.common.trace.TraceEvent;

/**
 * Trạng thái của bộ vẽ cây lời gọi, thuần Java để test (spec §7.3): {@code call} (nút {@code id}, cha
 * {@code p}, nhãn {@code a}) đẩy lời gọi lên ngăn, {@code ret} (kết quả {@code x}) lấy xuống, {@code mark}
 * đổi màu ({@code bo}: nhánh bị cắt, {@code loai}: quay lui).
 */
public final class CallTreeModel {

    public sealed interface Change permits Called, Returned, Marked {}

    public record Called(int id) implements Change {}

    public record Returned(int id) implements Change {}

    public record Marked(int id) implements Change {}

    /** Một lời gọi. {@code state}: {@code dang} (chưa trả về), {@code xong}, {@code bo}, {@code loai}. */
    public static final class N {
        public final int id;
        public final Integer parent;
        public final String label;
        public String ret;
        public String state = "dang";

        N(int id, Integer parent, String label) {
            this.id = id;
            this.parent = parent;
            this.label = label;
        }
    }

    private final Map<Integer, N> nodes = new LinkedHashMap<>();
    /** Các lời gọi đang chạy, ngoài cùng trước. */
    private final List<Integer> stack = new ArrayList<>();

    /** {@code {"kieu": "cay-goi", "nut": [[id, cha, nhãn, kết quả, trạng thái]…], "ngan": [id…]}}. */
    public void reset(JsonNode s) {
        nodes.clear();
        stack.clear();
        if (s == null || !s.isObject()) {
            return;
        }
        for (JsonNode a : s.path("nut")) {
            N n = new N(a.get(0).asInt(), intOrNull(a.get(1)), a.get(2).asText());
            n.ret = a.get(3) == null || a.get(3).isNull() ? null : a.get(3).asText();
            n.state = a.get(4).asText("dang");
            nodes.put(n.id, n);
        }
        s.path("ngan").forEach(x -> stack.add(x.asInt()));
    }

    public List<Change> apply(TraceEvent e) {
        switch (e.t()) {
            case "call" -> {
                int id = e.i("id");
                nodes.put(id, new N(id, intOrNull(e.get("p")), e.text("a")));
                stack.add(id);
                return List.of(new Called(id));
            }
            case "ret" -> {
                int id = e.i("id");
                N n = nodes.get(id);
                if (n == null) {
                    return List.of();
                }
                JsonNode x = e.get("x");
                n.ret = x == null || x.isNull() ? null : x.asText();
                if (n.state.equals("dang")) {
                    n.state = "xong";
                }
                stack.remove(Integer.valueOf(id));
                return List.of(new Returned(id));
            }
            case "mark" -> {
                N n = nodes.get(e.i("id"));
                if (n == null) {
                    return List.of();
                }
                n.state = e.text("m");
                return List.of(new Marked(n.id));
            }
            default -> {
                return List.of();
            }
        }
    }

    /** Lời gọi đang chạy trong cùng (đỉnh ngăn), −1 nếu không có. */
    public int current() {
        return stack.isEmpty() ? -1 : stack.get(stack.size() - 1);
    }

    public boolean onStack(int id) {
        return stack.contains(id);
    }

    public N node(int id) {
        return nodes.get(id);
    }

    public int size() {
        return nodes.size();
    }

    /**
     * Chỗ vẽ: {@code [cột, tầng]}. Lá lấy các cột liền nhau theo thứ tự gọi, cha nằm giữa con đầu và con cuối
     * (cây gọn, không chồng nhau). Nhiều gốc (hàm được gọi nhiều lần từ ngoài) xếp cạnh nhau.
     */
    public Map<Integer, double[]> layout() {
        Map<Integer, List<Integer>> children = new LinkedHashMap<>();
        List<Integer> roots = new ArrayList<>();
        for (N n : nodes.values()) {
            if (n.parent == null || !nodes.containsKey(n.parent)) {
                roots.add(n.id);
            } else {
                children.computeIfAbsent(n.parent, k -> new ArrayList<>()).add(n.id);
            }
        }
        Map<Integer, double[]> out = new LinkedHashMap<>();
        double[] next = {0};
        for (int r : roots) {
            place(r, 0, children, out, next);
        }
        return out;
    }

    private double place(
            int id, int depth, Map<Integer, List<Integer>> children, Map<Integer, double[]> out, double[] next) {
        List<Integer> cs = children.getOrDefault(id, List.of());
        double x;
        if (cs.isEmpty()) {
            x = next[0]++;
        } else {
            double first = place(cs.get(0), depth + 1, children, out, next);
            double last = first;
            for (int k = 1; k < cs.size(); k++) {
                last = place(cs.get(k), depth + 1, children, out, next);
            }
            x = (first + last) / 2;
        }
        out.put(id, new double[] {x, depth});
        return x;
    }

    /**
     * Nhãn khi cây rộng, ô hẹp không đủ chỗ: bỏ tên hàm ({@code "thu(2, 7)"} → {@code "2, 7"}; tên đã có ở tiêu
     * đề), vẫn dài hơn {@code maxChars} thì bỏ cả dấu cách. Không để chữ tự xuống dòng thành hai dòng khó đọc.
     */
    public static String argsOnly(String label, int maxChars) {
        String s = label;
        int k = s.indexOf('(');
        if (k > 0 && s.endsWith(")")) {
            s = s.substring(k + 1, s.length() - 1);
        }
        return s.length() <= maxChars ? s : s.replace(" ", "");
    }

    /** Trạng thái dạng chữ để test: mỗi lời gọi "nhãn=kết quả:trạng thái", rồi ngăn. */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        for (N n : nodes.values()) {
            sb.append(n.label);
            if (n.ret != null) {
                sb.append('=').append(n.ret);
            }
            sb.append(':').append(n.state).append(' ');
        }
        return sb.append("ngăn").append(stack).toString();
    }

    private static Integer intOrNull(JsonNode n) {
        return n == null || n.isNull() || n.isMissingNode() ? null : n.asInt();
    }
}
