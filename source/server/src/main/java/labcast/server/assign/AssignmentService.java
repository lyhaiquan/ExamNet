package labcast.server.assign;

import java.util.List;
import labcast.common.protocol.ErrorCode;

/**
 * Bài giao cho lớp: thực hành, kiểm tra, khung giờ, thời gian làm bài. Chủ: B.
 *
 * <p>Hợp đồng cho C (sidebar 4 nhóm) và D (màn giao bài). Mọi kiểm tra giờ dùng giờ server
 * ({@code now} do bên gọi truyền vào để test được), không tin giờ máy học viên.
 */
public interface AssignmentService {

    /** Bốn nhóm của sidebar. Giá trị JSON: {@code mau}, {@code luyen-tap}, {@code thuc-hanh}, {@code kiem-tra}. */
    enum Nhom {
        MAU("mau"),
        LUYEN_TAP("luyen-tap"),
        THUC_HANH("thuc-hanh"),
        KIEM_TRA("kiem-tra");

        private final String json;

        Nhom(String json) {
            this.json = json;
        }

        public String json() {
            return json;
        }
    }

    /**
     * Một bài giao.
     *
     * @param phut thời gian làm tính từ lúc học viên bắt đầu; {@code null} là làm tới {@code dongLuc}
     * @param bat giáo viên đang bật hay tắt
     */
    record Assignment(
            long id, long classId, String lessonId, Nhom nhom, long moLuc, long dongLuc, Integer phut, boolean bat) {}

    /** Kết quả {@link #canSubmit}: {@code ok} thì {@code code} là null. */
    record Check(boolean ok, ErrorCode code, String why) {
        public static final Check OK = new Check(true, null, null);
    }

    /** Bài giao học viên lớp này đang thấy lúc {@code now}: đang bật và chưa qua {@code dongLuc}. */
    List<Assignment> visible(long classId, long now);

    /** Mọi bài giao của lớp, cả bài đã tắt hay đã đóng, cho giáo viên xem. */
    List<Assignment> all(long classId);

    /**
     * Học viên có được nộp bài này lúc {@code now} không. Bài không thuộc bài giao nào (bài mẫu, luyện
     * tập) luôn {@link Check#OK}. Ngoài khung giờ hoặc bị tắt: {@link ErrorCode#NOT_OPEN}; hết thời gian
     * làm: {@link ErrorCode#TIME_UP}.
     *
     * @param lessonId mã bài, có thể kèm đuôi ngôn ngữ {@code #cpp}
     */
    Check canSubmit(long userId, String lessonId, long now);
}
