package labcast.client.views.codebien;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import labcast.common.trace.TraceEvent;

/**
 * Trạng thái của bộ vẽ chạy từng dòng, thuần Java để test: code, dòng đang chạy ({@code line}), bảng biến của
 * khung đang chạy ({@code vars}, cả bảng mỗi lần đổi) và các biến vừa đổi giá trị.
 */
public final class CodeVarModel {
    private final List<String> lines = new ArrayList<>();
    private int line;
    private String func = "";
    private final Map<String, String> vars = new LinkedHashMap<>();
    private final Set<String> changed = new HashSet<>();

    /** {@code {"kieu": "code-bien", "src": code, "dong": n, "ham": tên, "bien": [[tên, giá trị]…]}}. */
    public void reset(JsonNode s) {
        lines.clear();
        vars.clear();
        changed.clear();
        line = 0;
        func = "";
        if (s == null || !s.isObject()) {
            return;
        }
        lines.addAll(List.of(s.path("src").asText("").split("\n", -1)));
        line = s.path("dong").asInt(0);
        func = s.path("ham").asText("");
        s.path("bien").forEach(a -> vars.put(a.get(0).asText(), a.get(1).asText()));
    }

    /** @return true nếu có gì đổi cần vẽ lại */
    public boolean apply(TraceEvent e) {
        switch (e.t()) {
            case "line" -> {
                line = e.i("n");
                changed.clear();
                return true;
            }
            case "vars" -> {
                Map<String, String> next = new LinkedHashMap<>();
                e.get("d").forEach(a -> next.put(a.get(0).asText(), a.get(1).asText()));
                String f = e.text("f");
                boolean sameFrame = f != null && f.equals(func);
                changed.clear();
                next.forEach((k, v) -> {
                    if (sameFrame && !v.equals(vars.get(k))) {
                        changed.add(k);
                    }
                });
                vars.clear();
                vars.putAll(next);
                func = f == null ? "" : f;
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public List<String> lines() {
        return lines;
    }

    public int line() {
        return line;
    }

    public String func() {
        return func;
    }

    public Map<String, String> vars() {
        return vars;
    }

    public boolean changed(String name) {
        return changed.contains(name);
    }

    /** Trạng thái dạng chữ để test (không gồm "vừa đổi": chỉ là màu tạm). */
    public String describe() {
        return "dòng " + line + (func.isEmpty() ? "" : " trong " + func) + " " + vars;
    }
}
