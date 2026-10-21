package labcast.client.assign;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Thao tác giao bài của giáo viên, gửi {@code ASSIGN_*} và {@code SCHEDULE_*} lên server. Chủ: B; D gọi từ
 * màn giáo viên.
 */
public interface AssignController {

    /** Loại bài giao. Giá trị JSON: {@code thuc-hanh}, {@code kiem-tra}. */
    enum Loai {
        THUC_HANH,
        KIEM_TRA
    }

    /** Một bài giao như giáo viên thấy. {@code phut} null là làm tới {@code dongLuc}. */
    record Giao(long id, String lessonId, Loai loai, long moLuc, long dongLuc, Integer phut, boolean bat) {}

    /** Một ca học hằng tuần. {@code thu}: 1 = thứ 2 … 7 = chủ nhật; {@code tu}, {@code den}: phút trong ngày. */
    record Ca(int thu, int tu, int den) {}

    CompletableFuture<List<Giao>> list(long classId);

    CompletableFuture<Void> assign(
            long classId, List<String> lessonIds, Loai loai, long moLuc, long dongLuc, Integer phut);

    CompletableFuture<Void> open(long assignmentId);

    CompletableFuture<Void> close(long assignmentId);

    CompletableFuture<List<Ca>> schedule(long classId);

    CompletableFuture<Void> setSchedule(long classId, List<Ca> ca);
}
