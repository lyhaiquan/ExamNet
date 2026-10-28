package labcast.client.ui;

import java.util.regex.Pattern;

/** Kiểm tra form thêm / sửa tài khoản trên app trước khi gửi (server kiểm lại y hệt). Thuần Java để test. */
final class AccountForm {
    /** Khớp {@code AccountAdminModule.USERNAME} của server. */
    static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._-]{2,32}");

    static final int MAX_NAME = 80;

    private AccountForm() {}

    /** {@code null} nếu gửi được; ngược lại là câu báo lỗi. */
    static String check(
            boolean creating,
            boolean student,
            String username,
            String fullName,
            long classId,
            String newClass,
            String password) {
        if (creating
                && !USERNAME.matcher(username == null ? "" : username.strip()).matches()) {
            return "Tên đăng nhập 2–32 ký tự, chỉ gồm chữ không dấu, số và . _ -";
        }
        String name = fullName == null ? "" : fullName.strip();
        if (name.isEmpty()) return "Nhập họ và tên";
        if (name.length() > MAX_NAME) return "Họ tên dài quá " + MAX_NAME + " ký tự";
        String cls = newClass == null ? "" : newClass.strip();
        if (cls.length() > 100) return "Tên lớp dài quá 100 ký tự";
        if (creating && student && classId <= 0) return "Chọn lớp cho học viên";
        if (creating && !student && cls.isEmpty()) return "Nhập tên lớp giáo viên này dạy";
        if (creating) return PasswordRules.check(password, null);
        return null;
    }
}
