package labcast.client.views.dslk;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import labcast.client.player.VizText;
import labcast.common.trace.TraceEvent;

/**
 * Trạng thái của bộ vẽ danh sách liên kết, thuần Java để test (spec §7.3): {@code node} tạo nút, {@code link}
 * nối {@code id} → {@code w} ({@code id} rỗng là con trỏ đầu, {@code w} rỗng là cắt), {@code read}/{@code set}
 * giá trị, {@code mark}, {@code ptr} (các biến đang trỏ vào nút, cả danh sách mỗi lần đổi).
 *
 * <p>Thứ tự vẽ: khi đi từ đầu qua được mọi nút thì vẽ theo đúng thứ tự trong danh sách; còn lại (đang đảo
 * ngược dở, nút mới tạo chưa nối, nút vừa bị bỏ ra) thì giữ thứ tự tạo nút. Nhờ vậy lúc đảo ngược các nút đứng
 * yên, chỉ mũi tên quay đầu; chèn xong thì nút mới trượt vào đúng chỗ.
 */
public final class LinkedListModel {

    public sealed interface Change permits Structure, Read, Written, Marked, Pointers {}

    public record Structure(int id) implements Change {}

    public record Read(int id) implements Change {}

    public record Written(int id) implements Change {}

    public record Marked(int id) implements Change {}

    public record Pointers() implements Change {}

    public static final class N {
        public final int id;
        public String text;
        public Integer next;
        public String mark;

        N(int id, String text) {
            this.id = id;
            this.text = text;
        }
    }

    private final Map<Integer, N> nodes = new LinkedHashMap<>();
    private Integer head;
    /** Tên biến → id nút, theo thứ tự tên. */
    private final Map<String, Integer> pointers = new TreeMap<>();

    /** {@code {"kieu": "dslk", "dau": id, "nut": [[id, giá trị, tiếp, màu]…], "tro": [[tên, id]…]}}. */
    public void reset(JsonNode s) {
        nodes.clear();
        pointers.clear();
        head = null;
        if (s == null || !s.isObject()) {
            return;
        }
        head = intOrNull(s.get("dau"));
        for (JsonNode a : s.path("nut")) {
            N n = new N(a.get(0).asInt(), VizText.of(a.get(1)));
            n.next = intOrNull(a.get(2));
            n.mark = a.get(3) == null || a.get(3).isNull() ? null : a.get(3).asText();
            nodes.put(n.id, n);
        }
        readPointers(s.path("tro"));
    }

    public List<Change> apply(TraceEvent e) {
        switch (e.t()) {
            case "node" -> {
                int id = e.i("id");
                nodes.put(id, new N(id, VizText.of(e.get("x"))));
                return List.of(new Structure(id));
            }
            case "link" -> {
                Integer from = intOrNull(e.get("id"));
                Integer to = intOrNull(e.get("w"));
                if (from == null) {
                    head = to;
                } else {
                    N n = nodes.get(from);
                    if (n == null) {
                        throw new IllegalStateException("link từ nút chưa có: " + from);
                    }
                    n.next = to;
                }
                return List.of(new Structure(from == null ? -1 : from));
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
            case "ptr" -> {
                readPointers(e.get("p"));
                return List.of(new Pointers());
            }
            default -> {
                return List.of();
            }
        }
    }

    private void readPointers(JsonNode p) {
        pointers.clear();
        if (p != null) {
            p.forEach(a -> pointers.put(a.get(0).asText(), a.get(1).asInt()));
        }
    }

    /** Nút theo thứ tự đi từ đầu (dừng khi gặp vòng). */
    public List<Integer> chain() {
        List<Integer> out = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        Integer p = head;
        while (p != null && nodes.containsKey(p) && seen.add(p)) {
            out.add(p);
            p = nodes.get(p).next;
        }
        return out;
    }

    /** Thứ tự vẽ từ trái sang (xem chú thích lớp). */
    public List<Integer> order() {
        List<Integer> c = chain();
        if (c.size() == nodes.size()) {
            return c;
        }
        return new ArrayList<>(nodes.keySet());
    }

    /** Nút không ai trỏ tới (không phải đầu, không có mũi tên vào, không biến nào trỏ): đã bị bỏ ra. */
    public boolean orphan(int id) {
        if (head != null && head == id || pointers.containsValue(id)) {
            return false;
        }
        for (N n : nodes.values()) {
            if (n.next != null && n.next == id) {
                return false;
            }
        }
        return true;
    }

    public N node(int id) {
        return nodes.get(id);
    }

    public Integer head() {
        return head;
    }

    public Map<String, Integer> pointers() {
        return pointers;
    }

    /** Trạng thái dạng chữ để test: thứ tự vẽ, mỗi nút "giá trị→giá trị tiếp", đầu, biến trỏ. */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        for (int id : order()) {
            N n = nodes.get(id);
            sb.append(n.text)
                    .append("→")
                    .append(n.next == null || !nodes.containsKey(n.next) ? "None" : nodes.get(n.next).text);
            if (n.mark != null) {
                sb.append(':').append(n.mark);
            }
            sb.append(' ');
        }
        sb.append("đầu=").append(head == null ? "None" : nodes.get(head).text).append(' ');
        pointers.forEach((k, v) -> sb.append(k)
                .append('=')
                .append(nodes.containsKey(v) ? nodes.get(v).text : "?")
                .append(' '));
        return sb.toString().strip();
    }

    private static Integer intOrNull(JsonNode n) {
        return n == null || n.isNull() || n.isMissingNode() ? null : n.asInt();
    }
}
