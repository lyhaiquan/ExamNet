package labcast.client.views.nganxep;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import labcast.client.player.VizText;
import labcast.common.trace.TraceEvent;

/**
 * Trạng thái của bộ vẽ ngăn xếp / hàng đợi, thuần Java để test. Cùng một model cho cả hai (task 4.8):
 * ngăn xếp thêm và lấy ở cuối ({@code push}, {@code pop}), hàng đợi thêm ở cuối, lấy ở đầu ({@code enq},
 * {@code deq}). Phần tử giữ dạng chữ ({@link VizText}) vì có thể là dấu ngoặc, xâu, toạ độ.
 */
public final class StackModel {

    public sealed interface Change permits Push, Pop, Read {}

    /** Phần tử {@code text} vào ô {@code index} (luôn là ô cuối). */
    public record Push(int index, String text) implements Change {}

    /** Phần tử {@code text} rời ô {@code index}: ô cuối với ngăn xếp, ô 0 với hàng đợi. */
    public record Pop(int index, String text) implements Change {}

    /** Đang xem ô i (đỉnh ngăn xếp, đầu hàng đợi). */
    public record Read(int i) implements Change {}

    private final List<String> items = new ArrayList<>();
    private boolean queue;

    /**
     * {@code snapshot}: trạng thái do {@code trang_thai()} của Python ghi, {@code {"kieu": "ngan-xep" |
     * "hang-doi", "ds": [...]}}; mảng trơn cũng nhận. {@code queueHint}: bài khai báo {@code hang-doi}.
     */
    public void reset(JsonNode snapshot, boolean queueHint) {
        items.clear();
        queue = queueHint;
        JsonNode ds = snapshot;
        if (snapshot != null && snapshot.isObject()) {
            queue = queueHint || "hang-doi".equals(snapshot.path("kieu").asText());
            ds = snapshot.get("ds");
        }
        if (ds != null && ds.isArray()) {
            ds.forEach(x -> items.add(VizText.of(x)));
        }
    }

    /** @throws IllegalStateException lấy ra khi đang rỗng: trace sai, bộ vẽ phải dựng lại */
    public List<Change> apply(TraceEvent e) {
        switch (e.t()) {
            case "push", "enq" -> {
                if (e.t().equals("enq")) {
                    queue = true;
                }
                String x = VizText.of(e.get("x"));
                items.add(x);
                return List.of(new Push(items.size() - 1, x));
            }
            case "pop", "deq" -> {
                if (items.isEmpty()) {
                    throw new IllegalStateException(e.t() + " khi " + (queue ? "hàng đợi" : "ngăn xếp") + " rỗng");
                }
                boolean front = e.t().equals("deq");
                if (front) {
                    queue = true;
                }
                int idx = front ? 0 : items.size() - 1;
                return List.of(new Pop(idx, items.remove(idx)));
            }
            case "read" -> {
                int i = e.i("i");
                return i >= 0 && i < items.size() ? List.of(new Read(i)) : List.of();
            }
            default -> {
                return List.of();
            }
        }
    }

    public List<String> items() {
        return List.copyOf(items);
    }

    /** Vẽ nằm ngang (hàng đợi) hay đứng (ngăn xếp). */
    public boolean queue() {
        return queue;
    }
}
