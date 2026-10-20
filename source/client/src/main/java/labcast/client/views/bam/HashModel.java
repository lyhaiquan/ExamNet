package labcast.client.views.bam;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import labcast.client.player.VizText;
import labcast.common.trace.TraceEvent;

/**
 * Trạng thái của bộ vẽ bảng băm, thuần Java để test (spec §7.3). Dây chuyền: {@code bucket} ô {@code i} với
 * {@code op} {@code add} (thêm {@code x} vào cuối), {@code del} (bỏ phần tử {@code k}), {@code read} (so phần tử
 * {@code k}), {@code set}. Địa chỉ mở: {@code read}/{@code set} ô {@code i} như mảng; ô trống là None.
 */
public final class HashModel {

    public sealed interface Change permits Read, Written, Removed {}

    /** Đang so phần tử {@code k} của ô {@code i} (địa chỉ mở: k = 0). */
    public record Read(int i, int k) implements Change {}

    public record Written(int i, int k) implements Change {}

    /** Phần tử {@code k} của ô {@code i} vừa bị bỏ, giá trị {@code text}. */
    public record Removed(int i, int k, String text) implements Change {}

    private final List<List<String>> chains = new ArrayList<>();
    private boolean open;

    /** {@code {"kieu": "bam", "mo": …, "o": [...]}}: dây chuyền thì mỗi ô là một mảng, địa chỉ mở thì giá trị hoặc null. */
    public void reset(JsonNode s) {
        chains.clear();
        open = false;
        if (s == null || !s.isObject()) {
            return;
        }
        open = s.path("mo").asBoolean(false);
        for (JsonNode c : s.path("o")) {
            List<String> l = new ArrayList<>();
            if (open) {
                if (!c.isNull()) {
                    l.add(VizText.of(c));
                }
            } else {
                c.forEach(x -> l.add(VizText.of(x)));
            }
            chains.add(l);
        }
    }

    public List<Change> apply(TraceEvent e) {
        int i = e.i("i");
        if (i < 0 || i >= chains.size()) {
            return List.of();
        }
        List<String> c = chains.get(i);
        switch (e.t()) {
            case "bucket" -> {
                int k = e.data().has("k") ? e.i("k") : -1;
                switch (e.text("op")) {
                    case "add" -> {
                        c.add(VizText.of(e.get("x")));
                        return List.of(new Written(i, c.size() - 1));
                    }
                    case "del" -> {
                        if (k >= 0 && k < c.size()) {
                            return List.of(new Removed(i, k, c.remove(k)));
                        }
                        return List.of();
                    }
                    case "set" -> {
                        if (k >= 0 && k < c.size()) {
                            c.set(k, VizText.of(e.get("x")));
                            return List.of(new Written(i, k));
                        }
                        return List.of();
                    }
                    case "read" -> {
                        return List.of(new Read(i, k));
                    }
                    default -> {
                        return List.of();
                    }
                }
            }
            case "read" -> {
                return List.of(new Read(i, 0));
            }
            case "set" -> {
                c.clear();
                JsonNode x = e.get("x");
                if (x != null && !x.isNull()) {
                    c.add(VizText.of(x));
                }
                return List.of(new Written(i, 0));
            }
            default -> {
                return List.of();
            }
        }
    }

    public boolean open() {
        return open;
    }

    public int size() {
        return chains.size();
    }

    public List<String> chain(int i) {
        return List.copyOf(chains.get(i));
    }

    /** Trạng thái dạng chữ để test: "0:[] 1:[4, 7] …" (địa chỉ mở: ô trống là "-"). */
    public String describe() {
        StringBuilder sb = new StringBuilder(open ? "mở " : "");
        for (int i = 0; i < chains.size(); i++) {
            sb.append(i).append(':');
            if (open) {
                sb.append(chains.get(i).isEmpty() ? "-" : chains.get(i).get(0));
            } else {
                sb.append(chains.get(i));
            }
            sb.append(' ');
        }
        return sb.toString().strip();
    }
}
