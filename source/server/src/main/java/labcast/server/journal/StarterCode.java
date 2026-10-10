package labcast.server.journal;

/**
 * Code ban đầu của một bài (nội dung {@code khung.py}). Module bài học (Người 3) đăng ký dịch vụ này;
 * {@link CodeStore} không đọc file bài học trực tiếp.
 */
@FunctionalInterface
public interface StarterCode {
    String of(String lessonId);
}
