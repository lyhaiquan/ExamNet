package labcast.client.assign;

import java.util.List;

/**
 * Dữ liệu sidebar 4 nhóm: Bài mẫu · Luyện tập · Thực hành · Kiểm tra. Chủ: B. Chỉ logic, không có Node
 * JavaFX; C vẽ từ đây.
 */
public interface SidebarModel {

    /** Thứ tự hiện trên sidebar. */
    enum Nhom {
        MAU("Bài mẫu"),
        LUYEN_TAP("Luyện tập"),
        THUC_HANH("Thực hành"),
        KIEM_TRA("Kiểm tra");

        private final String ten;

        Nhom(String ten) {
            this.ten = ten;
        }

        public String ten() {
            return ten;
        }
    }

    /**
     * Một dòng trong sidebar.
     *
     * @param hetHanLuc giờ server bài đóng hoặc hết thời gian làm; 0 nếu không giới hạn
     * @param khoa đã hết giờ hoặc chưa tới giờ: hiện mờ, không mở được
     */
    record Item(String lessonId, String ten, String chuong, Nhom nhom, long hetHanLuc, boolean khoa, boolean daQua) {}

    /** Các dòng của một nhóm, đúng thứ tự hiện. Thực hành và Kiểm tra rỗng khi lớp chưa được giao bài. */
    List<Item> items(Nhom nhom);

    /** Gọi lại (trên luồng bất kỳ) mỗi khi danh sách đổi, ví dụ giáo viên vừa giao bài. */
    void onChange(Runnable listener);
}
