package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Các bình luận của một bài, gom thành luồng (bình luận gốc kèm trả lời). Nhận cả danh sách ({@code
 * DISCUSS_DATA}) lẫn từng mục mới hoặc vừa ẩn ({@code DISCUSS_NEW}); thuần Java để test không cần JavaFX.
 */
final class DiscussionModel {
    record Thread(JsonNode root, List<JsonNode> replies) {}

    private final boolean staff;
    private final Map<Long, JsonNode> items = new TreeMap<>();

    /** {@code staff}: giáo viên/quản trị vẫn thấy mục đã ẩn (mờ); học viên thì mục ẩn bị gỡ. */
    DiscussionModel(boolean staff) {
        this.staff = staff;
    }

    void reset(JsonNode list) {
        items.clear();
        list.forEach(this::upsert);
    }

    void upsert(JsonNode item) {
        long id = item.path("id").asLong();
        if (id <= 0) return;
        if (!staff && item.path("hidden").asBoolean()) items.remove(id);
        else items.put(id, item);
    }

    int size() {
        return items.size();
    }

    /** Luồng theo thứ tự đăng; trả lời mà mất bình luận gốc (gốc bị ẩn) thì không hiện. */
    List<Thread> threads() {
        List<Thread> out = new ArrayList<>();
        Map<Long, Thread> byRoot = new TreeMap<>();
        for (JsonNode n : items.values()) {
            long parent = n.path("parentId").asLong(0);
            if (parent == 0) {
                Thread t = new Thread(n, new ArrayList<>());
                byRoot.put(n.path("id").asLong(), t);
                out.add(t);
            } else if (byRoot.containsKey(parent)) {
                byRoot.get(parent).replies().add(n);
            }
        }
        return out;
    }
}
