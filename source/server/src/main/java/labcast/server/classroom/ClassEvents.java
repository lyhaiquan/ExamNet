package labcast.server.classroom;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/**
 * Sự kiện lớp học mà sơ đồ lớp hiện cho giáo viên (spec §17.3). Chữ ký {@link #publish} và {@link #flagPaste}
 * chốt từ ngày 1 để Người 1 và Người 3 gọi được ngay. Chủ: Người 2.
 *
 * <p>Hiện giữ trạng thái mới nhất của từng học viên và các cờ "dán khối lớn" trong bộ nhớ; dashboard đọc
 * ra. Sơ đồ lớp trong app (task 2.12) nghe {@link #onChange} để đẩy {@code CLASS_STATE}.
 */
public final class ClassEvents {

    public enum StudentStatus {
        DANG_LAM,
        SAI_TEST,
        DA_QUA,
        GIO_TAY,
        HA_TAY
    }

    /** Trạng thái mới nhất. {@code failedTest}: id test sai đầu tiên khi {@code SAI_TEST}, còn lại null. */
    public record Status(String lessonId, StudentStatus status, String failedTest, long at) {}

    /**
     * Cờ "dán khối lớn" của một bài: giữ tới khi học viên nộp đúng bài đó.
     *
     * @param times số lần bị đánh dấu
     * @param chars số ký tự của lần lớn nhất
     * @param detail mô tả lần gần nhất, cho giáo viên đọc
     */
    public record PasteFlag(String lessonId, int times, int chars, String detail, long at) {}

    private final LongSupplier clock;
    private final Map<Long, Status> status = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, PasteFlag>> pastes = new ConcurrentHashMap<>();
    private final List<LongConsumer> listeners = new CopyOnWriteArrayList<>();

    public ClassEvents(LongSupplier clock) {
        this.clock = clock;
    }

    /** failedTest: id test sai đầu tiên khi status = SAI_TEST, còn lại null. Dùng cho thống kê "N bạn sai test X". */
    public void publish(long userId, String lessonId, StudentStatus st, String failedTest) {
        status.put(userId, new Status(lessonId, st, failedTest, clock.getAsLong()));
        if (st == StudentStatus.DA_QUA) {
            Map<String, PasteFlag> m = pastes.get(userId);
            if (m != null) {
                m.remove(lessonId);
            }
        }
        fire(userId);
    }

    /** Cờ dán khối lớn (task 1.14). Giữ tới khi học viên nộp đúng bài đó. */
    public void flagPaste(long userId, String lessonId) {
        flagPaste(userId, lessonId, 0, "");
    }

    /** Như {@link #flagPaste(long, String)}, kèm cỡ khối và mô tả để giáo viên tự xét. */
    public void flagPaste(long userId, String lessonId, int chars, String detail) {
        long now = clock.getAsLong();
        pastes.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .merge(
                        lessonId,
                        new PasteFlag(lessonId, 1, chars, detail, now),
                        (old, add) ->
                                new PasteFlag(lessonId, old.times() + 1, Math.max(old.chars(), chars), detail, now));
        fire(userId);
    }

    public Optional<Status> status(long userId) {
        return Optional.ofNullable(status.get(userId));
    }

    /** Các cờ dán còn giữ của học viên, mới nhất trước. */
    public List<PasteFlag> pasteFlags(long userId) {
        Map<String, PasteFlag> m = pastes.get(userId);
        if (m == null) {
            return List.of();
        }
        List<PasteFlag> out = new ArrayList<>(m.values());
        out.sort(Comparator.comparingLong(PasteFlag::at).reversed());
        return out;
    }

    /** Nghe mỗi lần trạng thái hay cờ của một học viên đổi (nhận userId). */
    public void onChange(LongConsumer listener) {
        listeners.add(listener);
    }

    private void fire(long userId) {
        listeners.forEach(l -> l.accept(userId));
    }
}
