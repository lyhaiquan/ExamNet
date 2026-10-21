package labcast.server.classroom;

import java.util.List;
import java.util.Optional;

/**
 * Danh bạ lớp của một server (đợt 2: một server nhiều lớp, ví dụ DSA1, DSA2). Chủ: A.
 *
 * <p>Hợp đồng cho B (bài giao theo lớp) và D (trang quản trị): chữ ký chốt trong PR hợp đồng, đổi
 * thì mở PR riêng cả 4 duyệt.
 */
public interface ClassDirectory {

    /** Một lớp. {@code siSo}: số học viên ghi danh. */
    record ClassInfo(long id, String ten, int siSo) {}

    /** Mọi lớp trên server, theo tên. */
    List<ClassInfo> all();

    Optional<ClassInfo> find(long classId);

    /** Lớp người này dạy (giáo viên) hoặc học (học viên), theo tên. Rỗng nếu chưa có lớp. */
    List<ClassInfo> classesOf(long userId);

    /** userId các học viên ghi danh vào lớp. */
    List<Long> studentsOf(long classId);

    /** userId các giáo viên phụ trách lớp. */
    List<Long> teachersOf(long classId);
}
