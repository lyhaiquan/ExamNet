package labcast.client.ui;

import java.security.SecureRandom;

/**
 * Kiểm tra mật khẩu mới ngay trên app trước khi gửi (server kiểm tra lại y hệt), và gợi ý mật khẩu dễ đọc khi giáo
 * viên đặt lại cho học viên. Thuần Java để test không cần JavaFX.
 */
public final class PasswordRules {
    /** Khớp {@code AccountModule.MIN_PASSWORD} / {@code MAX_PASSWORD} của server. */
    public static final int MIN = 4;

    public static final int MAX = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordRules() {}

    /** {@code null} nếu dùng được; ngược lại là câu báo lỗi. {@code confirm} {@code null}: không cần nhập lại. */
    public static String check(String pwd, String confirm) {
        if (pwd == null || pwd.isBlank()) return "Nhập mật khẩu mới";
        if (pwd.length() < MIN) return "Mật khẩu mới cần ít nhất " + MIN + " ký tự";
        if (pwd.length() > MAX) return "Mật khẩu mới dài quá " + MAX + " ký tự";
        if (confirm != null && !pwd.equals(confirm)) return "Hai lần nhập mật khẩu mới không khớp";
        return null;
    }

    /**
     * Mật khẩu tạm khi giáo viên đặt lại: 6 chữ số, dễ đọc cho nhau nghe. Chỉ dùng một lần, lần đăng nhập sau người
     * dùng tự đặt mật khẩu của mình.
     */
    public static String suggest() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
