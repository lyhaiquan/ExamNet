package labcast.common.trace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Một trace đã đọc. Vị trí trong trace là chỉ số trong {@link #events()} (không gồm {@code hdr} và
 * {@code end}). Sự kiện {@code snap} (keyframe) nằm trong danh sách để biết vị trí của nó.
 *
 * <p>Không an toàn đa luồng: {@link TraceReader#append} thêm sự kiện vào chính đối tượng này khi nhận
 * thêm khúc {@code TRACE_CHUNK}.
 */
public final class Trace {
    /**
     * Một bước ở tốc độ 1× (ms). Trình phát của app và server chiếu cùng tính vị trí theo số này, nên
     * mọi máy đang theo giáo viên ra cùng một bước.
     */
    public static final int STEP_MS = 400;

    private JsonNode header = JsonNodeFactory.instance.objectNode();
    private final List<TraceEvent> events = new ArrayList<>();
    private final List<Integer> snapshots = new ArrayList<>();
    private final List<Integer> keySteps = new ArrayList<>();
    private boolean truncated;
    private boolean ended;

    Trace() {}

    public JsonNode header() {
        return header;
    }

    public List<TraceEvent> events() {
        return Collections.unmodifiableList(events);
    }

    public int size() {
        return events.size();
    }

    /** Chỉ số sự kiện {@code snap} gần nhất ≤ k; −1 nếu không có. */
    public int snapshotBefore(int k) {
        int lo = 0;
        int hi = snapshots.size() - 1;
        int best = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (snapshots.get(mid) <= k) {
                best = snapshots.get(mid);
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return best;
    }

    /** Lời giải hay code học viên vượt trần 10 000 sự kiện / 2 MB (spec §7.3). */
    public boolean truncated() {
        return truncated;
    }

    /** Đã nhận sự kiện {@code end}. */
    public boolean ended() {
        return ended;
    }

    /** Chỉ số các sự kiện {@code key} (bước quan trọng). */
    public List<Integer> keySteps() {
        return Collections.unmodifiableList(keySteps);
    }

    void accept(TraceEvent e) {
        switch (e.t()) {
            case "hdr" -> header = e.data();
            case "end" -> {
                ended = true;
                JsonNode t = e.data().get("truncated");
                truncated = t != null && t.asBoolean();
            }
            default -> {
                if (e.t().equals("snap")) {
                    snapshots.add(events.size());
                } else if (e.t().equals("key")) {
                    keySteps.add(events.size());
                }
                events.add(e);
            }
        }
    }
}
