package labcast.client.player;

import java.util.List;
import javafx.scene.paint.Color;

/**
 * Quy ước màu dùng chung cho mọi bộ vẽ (spec §7.4): vàng là đang xét, xanh lá là đã xong hoặc đúng
 * chỗ, xanh dương là vừa ghi, đỏ là quay lui hoặc bị loại, xám là bị cắt hoặc bỏ qua. Bộ vẽ nào cũng
 * lấy màu ở đây để chú thích màu dưới trình phát luôn khớp với hình.
 */
public final class VizPalette {
    /** Phần tử chưa có gì đặc biệt. */
    public static final Color NORMAL = Color.web("#a5b4cb");

    public static final Color CURRENT = Color.web("#f5b82e");
    public static final Color DONE = Color.web("#2fb67c");
    public static final Color WRITTEN = Color.web("#3b82f6");
    public static final Color REJECTED = Color.web("#ef4444");
    public static final Color SKIPPED = Color.web("#d5dae2");

    public record Entry(Color color, String label) {}

    /** Thứ tự hiện trong chú thích màu. */
    public static final List<Entry> LEGEND = List.of(
            new Entry(CURRENT, "Đang xét"),
            new Entry(WRITTEN, "Vừa ghi"),
            new Entry(DONE, "Xong / đúng chỗ"),
            new Entry(REJECTED, "Quay lui / bị loại"),
            new Entry(SKIPPED, "Bỏ qua"));

    private VizPalette() {}
}
