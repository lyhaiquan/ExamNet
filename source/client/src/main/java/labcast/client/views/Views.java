package labcast.client.views;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import labcast.client.player.View;
import labcast.client.views.bangsql.SqlTableView;
import labcast.client.views.luoi.GridView;
import labcast.client.views.mang.ArrayView;
import labcast.client.views.nganxep.StackView;

/** Dựng bộ vẽ theo khai báo {@code [{tên biến, kieu}]}. Bộ vẽ chưa có thì bỏ qua biến đó. */
public final class Views {
    private Views() {}

    /**
     * @param specs mảng khai báo: {@code khungNhin} của bài ({@code nameField = "bien"}) hoặc {@code views}
     *     trong sự kiện {@code hdr} của trace ({@code nameField = "v"})
     */
    public static Map<String, View> create(JsonNode specs, String nameField) {
        Map<String, View> m = new LinkedHashMap<>();
        for (JsonNode v : specs) {
            String name = v.path(nameField).asText();
            View view = create(v.path("kieu").asText(), name);
            if (view != null) {
                m.put(name, view);
            }
        }
        return m;
    }

    static View create(String kieu, String name) {
        return switch (kieu) {
            case "mang" -> new ArrayView(name);
            case "ngan-xep" -> new StackView(name, false);
                // hàng đợi dùng chung bộ vẽ ngăn xếp, vẽ nằm ngang (khai báo hang-doi, hoặc tự nhận khi gặp enq/deq)
            case "hang-doi" -> new StackView(name, true);
            case "luoi" -> new GridView(name);
            case "bang-sql" -> new SqlTableView();
            default -> null;
        };
    }
}
