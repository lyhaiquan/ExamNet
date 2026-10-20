package labcast.client.player;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import labcast.common.trace.TraceEvent;

/**
 * Một bộ vẽ (spec §7.4). Chủ bộ vẽ viết lớp cài đặt; trình phát chỉ gọi hai hàm {@link #reset} và
 * {@link #apply}.
 */
public interface View {
    /** "mang", "cay", "code-bien"… — trùng {@code kieu} trong {@code lesson.yaml}. */
    String kind();

    javafx.scene.Node node();

    /** Dựng lại từ keyframe, không animation. {@code snapshot} null = trạng thái rỗng. */
    void reset(JsonNode snapshot);

    /** Áp một sự kiện. {@code animate} false khi đang tua: chỉ cập nhật, vẽ lại ngay. */
    void apply(TraceEvent e, boolean animate, Duration d);
}
