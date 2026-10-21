package labcast.client.teacher;

import javafx.scene.Node;

/**
 * Chỗ gắn màn giáo viên vào {@code HomeScreen}. Chủ: C (Đinh Hoàng Vũ).
 *
 * <p>{@code HomeScreen} chỉ gọi interface này khi người đăng nhập là giáo viên. B (màn giao bài, lịch ca) và
 * D (màn upload, bật/tắt bài) mỗi người cài một {@code TeacherPanel} trong package của mình, không sửa file
 * của C.
 */
public interface TeacherPanel {

    /** Tên hiện trên thanh chuyển màn, ví dụ "Giao bài". */
    String title();

    /** Node của màn; gọi một lần, giữ lại dùng tiếp. */
    Node view();

    /** Lớp giáo viên vừa chọn ở thanh trên cùng. */
    void showClass(long classId);
}
