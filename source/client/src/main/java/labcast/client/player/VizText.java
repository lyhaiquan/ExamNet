package labcast.client.player;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Giá trị trong trace thành chữ để vẽ, gần với cách Python in ra: chữ giữ nguyên, {@code true} →
 * {@code True}, {@code null} → {@code None}, mảng JSON (tuple, list của Python) → {@code (2, 3)}. Bộ vẽ ngăn xếp và bảng 2 chiều dùng chung, vì giá trị của chúng không chỉ là số
 * (dấu ngoặc, xâu nhị phân "101", ô lưới "#", toạ độ (hàng, cột)…).
 */
public final class VizText {
    private VizText() {}

    public static String of(JsonNode v) {
        if (v == null || v.isNull() || v.isMissingNode()) {
            return "None";
        }
        if (v.isTextual()) {
            return v.asText();
        }
        if (v.isBoolean()) {
            return v.asBoolean() ? "True" : "False";
        }
        if (v.isIntegralNumber()) {
            return v.bigIntegerValue().toString();
        }
        if (v.isNumber()) {
            double d = v.asDouble();
            // như Python: 2.0 in là "2.0", 0.5 là "0.5"
            return d == Math.rint(d) && Math.abs(d) < 1e15 ? (long) d + ".0" : String.valueOf(d);
        }
        if (v.isArray()) {
            List<String> parts = new ArrayList<>();
            v.forEach(x -> parts.add(of(x)));
            return "(" + String.join(", ", parts) + ")";
        }
        return v.toString();
    }
}
