package labcast.client.editor;

import java.util.function.Consumer;
import java.util.function.LongSupplier;
import labcast.common.protocol.CodeDelta;

/**
 * Gộp các lần gõ phím thành delta, gửi tối đa mỗi 100 ms (ĐG2). Gõ "abc" liền tay ra MỘT delta chứ
 * không phải ba frame.
 *
 * <p>Gộp được: các lần chèn liền nhau (lần sau bắt đầu đúng ở cuối lần trước); các lần xoá lùi
 * (Backspace) hoặc xoá tới (Delete) liền nhau. Thay đổi không liền thì xả delta đang gộp rồi mở delta
 * mới. Không an toàn đa luồng: chỉ gọi từ luồng giao diện.
 */
public final class DeltaBatcher {
    public static final long WINDOW_MS = 100;

    private final String lessonId;
    private final LongSupplier now;
    private final Consumer<CodeDelta> out;
    private long seq;

    // delta đang gộp
    private boolean open;
    private int pos;
    private int delLen;
    private StringBuilder ins = new StringBuilder();
    private long firstAt;

    public DeltaBatcher(String lessonId, LongSupplier nowMillis, Consumer<CodeDelta> out) {
        this.lessonId = lessonId;
        this.now = nowMillis;
        this.out = out;
    }

    /** Ô soạn báo một thay đổi: tại {@code pos} văn bản {@code removed} bị thay bằng {@code inserted}. */
    public void onChange(int at, String removed, String inserted) {
        boolean pureInsert = removed.isEmpty() && !inserted.isEmpty();
        boolean pureDelete = !removed.isEmpty() && inserted.isEmpty();
        if (open) {
            boolean openInsert = delLen == 0 && ins.length() > 0;
            boolean openDelete = delLen > 0 && ins.length() == 0;
            if (pureInsert && openInsert && at == pos + ins.length()) {
                ins.append(inserted);
                return;
            }
            if (pureDelete && openDelete && at + removed.length() == pos) { // Backspace
                pos = at;
                delLen += removed.length();
                return;
            }
            if (pureDelete && openDelete && at == pos) { // Delete
                delLen += removed.length();
                return;
            }
            flush();
        }
        if (removed.isEmpty() && inserted.isEmpty()) {
            return;
        }
        open = true;
        pos = at;
        delLen = removed.length();
        ins = new StringBuilder(inserted);
        firstAt = now.getAsLong();
    }

    /** Gọi định kỳ (mỗi 50 ms): delta đã gộp quá 100 ms thì xả. */
    public void tick() {
        if (open && now.getAsLong() - firstAt >= WINDOW_MS) {
            flush();
        }
    }

    /** Xả ngay, ví dụ trước khi bấm Chạy thử để server có đúng code đang thấy. */
    public void flush() {
        if (!open) {
            return;
        }
        open = false;
        out.accept(new CodeDelta(lessonId, ++seq, pos, delLen, ins.toString(), now.getAsLong()));
    }

    public long lastSeq() {
        return seq;
    }

    /** Bắt đầu đánh số tiếp từ {@code seq} server đã có. */
    public void setLastSeq(long seq) {
        this.seq = seq;
    }
}
