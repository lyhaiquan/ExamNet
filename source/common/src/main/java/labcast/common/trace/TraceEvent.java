package labcast.common.trace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Một sự kiện trace (spec §7.3), ví dụ {@code {"t":"set","v":"a","i":0,"x":1}}. {@code data} là toàn
 * bộ object JSON của dòng đó.
 */
public record TraceEvent(String t, ObjectNode data) {

    /** Tên biến (trường {@code v}); null nếu sự kiện không thuộc biến nào (say, key, line…). */
    public String var() {
        JsonNode v = data.get("v");
        return v == null ? null : v.asText();
    }

    /** Trường số nguyên, ví dụ {@code i(“i”)}; thiếu trường thì −1. */
    public int i(String field) {
        JsonNode v = data.get(field);
        return v == null || !v.canConvertToInt() ? -1 : v.asInt();
    }

    public String text(String field) {
        JsonNode v = data.get(field);
        return v == null ? null : v.asText();
    }

    public JsonNode get(String field) {
        return data.get(field);
    }
}
