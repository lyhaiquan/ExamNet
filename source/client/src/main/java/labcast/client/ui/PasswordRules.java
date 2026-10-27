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

    /** Bỏ các chữ dễ nhầm khi đọc cho nhau nghe: 0/o, 1/l/i. */
    private static final String LETTERS = "abcdefghjkmnpqrstuvwxyz";

    private static final String DIGITS = "23456789";
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

    /** Mật khẩu ngẫu nhiên dễ đọc cho nhau nghe, dạng {@code abcd-2345}. */
    public static String suggest() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) sb.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        sb.append('-');
        for (int i = 0; i < 4; i++) sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        return sb.toString();
    }
}
