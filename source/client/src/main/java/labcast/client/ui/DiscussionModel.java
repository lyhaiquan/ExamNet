package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Các bình luận của một bài, gom thành luồng (bình luận gốc kèm trả lời). Nhận cả danh sách ({@code
 * DISCUSS_DATA}) lẫn từng mục mới, vừa ẩn hay vừa được thích ({@code DISCUSS_NEW}); thuần Java để test không cần
 * JavaFX.
 */
final class DiscussionModel {
    record Thread(JsonNode root, List<JsonNode> replies) {
        /** Có bình luận của mình trong luồng (mục "Theo dõi"). */
        boolean following() {
            if (root.path("mine").asBoolean()) return true;
            return replies.stream().anyMatch(r -> r.path("mine").asBoolean());
        }

        int likes() {
            return root.path("likes").asInt()
                    + replies.stream().mapToInt(r -> r.path("likes").asInt()).sum();
        }

        long lastId() {
            return replies.isEmpty()
                    ? root.path("id").asLong()
                    : replies.get(replies.size() - 1).path("id").asLong();
        }
    }

    enum Sort {
        NEWEST,
        USEFUL
    }

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

    /** Trả true nếu là bình luận mới (chưa có trong danh sách). */
    boolean upsert(JsonNode item) {
        long id = item.path("id").asLong();
        if (id <= 0) return false;
        boolean fresh = !items.containsKey(id);
        if (!staff && item.path("hidden").asBoolean()) {
            items.remove(id);
            return false;
        }
        items.put(id, item);
        return fresh;
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

    /**
     * Luồng để hiện: {@code onlyFollowing} chỉ giữ luồng có bình luận của mình; mới nhất theo hoạt động cuối
     * của luồng, hữu ích nhất theo tổng lượt thích (bằng nhau thì mới hơn trước).
     */
    List<Thread> threads(boolean onlyFollowing, Sort sort) {
        List<Thread> out = new ArrayList<>(threads());
        if (onlyFollowing) out.removeIf(t -> !t.following());
        Comparator<Thread> newest = Comparator.comparingLong(Thread::lastId).reversed();
        out.sort(
                sort == Sort.USEFUL
                        ? Comparator.comparingInt(Thread::likes).reversed().thenComparing(newest)
                        : newest);
        return out;
    }
}
