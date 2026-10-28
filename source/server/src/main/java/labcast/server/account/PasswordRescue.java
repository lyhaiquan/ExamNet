package labcast.server.account;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import labcast.server.store.Database;

/**
 * {@code --dat-lai-mat-khau user:mật khẩu mới}: đặt lại mật khẩu thẳng trong file database rồi thoát, không bật
 * server. Dùng khi chính tài khoản quản trị quên mật khẩu, không còn ai đăng nhập vào app để đặt lại giúp.
 * Chạy một lần, không phải khoá trong file cấu hình, để không lỡ đặt lại mật khẩu mỗi lần bật server.
 */
public final class PasswordRescue {
    public static final String FLAG = "--dat-lai-mat-khau";

    /** Tham số đã tách: {@code spec} là {@code user:mật khẩu}, {@code rest} là các tham số còn lại cho server. */
    public record Split(String spec, String[] rest) {}

    private PasswordRescue() {}

    /** Tách {@link #FLAG} và giá trị của nó khỏi tham số dòng lệnh; không có thì {@code spec} là {@code null}. */
    public static Split split(String[] args) {
        List<String> rest = new ArrayList<>();
        String spec = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(FLAG)) {
                if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                    throw new IllegalArgumentException("Thiếu giá trị sau " + FLAG + " (dạng user:mật khẩu mới)");
                }
                spec = args[++i];
            } else {
                rest.add(args[i]);
            }
        }
        return new Split(spec, rest.toArray(String[]::new));
    }

    /** Đặt lại mật khẩu trong {@code db}; trả câu báo kết quả. Sai dạng, mật khẩu không hợp lệ, không có tài khoản → lỗi. */
    public static String run(String spec, Database db) throws SQLException {
        int colon = spec.indexOf(':');
        if (colon <= 0) {
            throw new IllegalArgumentException(FLAG + " phải có dạng user:mật khẩu mới");
        }
        String username = spec.substring(0, colon).strip();
        String pwd = spec.substring(colon + 1);
        String bad = AccountModule.checkNewPassword(pwd);
        if (bad != null) {
            throw new IllegalArgumentException(bad);
        }
        UserRepository users = new UserRepository(db);
        User u = users.byUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không có tài khoản " + username + " trong database"));
        users.setPassword(u.id(), pwd);
        users.setMustChangePassword(u.id(), false); // quản trị tự chọn mật khẩu ở đây, không phải mật khẩu tạm
        users.clearPasswordRequest(u.id());
        return "Đã đặt lại mật khẩu cho " + u.username() + " (" + u.fullName() + ", " + u.role() + ")";
    }
}
