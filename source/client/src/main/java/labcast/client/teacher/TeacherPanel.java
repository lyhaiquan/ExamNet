package labcast.client.teacher;

import javafx.scene.Node;

/**
 * Chỗ gắn màn giáo viên vào {@code HomeScreen}. Chủ: D.
 *
 * <p>{@code HomeScreen} (của C) chỉ gọi interface này khi người đăng nhập là giáo viên; D làm màn giao bài,
 * chọn lớp, lịch ca bên trong mà không sửa file của C.
 */
public interface TeacherPanel {

    /** Tên hiện trên thanh chuyển màn, ví dụ "Giao bài". */
    String title();

    /** Node của màn; gọi một lần, giữ lại dùng tiếp. */
    Node view();

    /** Lớp giáo viên vừa chọn ở thanh trên cùng. */
    void showClass(long classId);
}
