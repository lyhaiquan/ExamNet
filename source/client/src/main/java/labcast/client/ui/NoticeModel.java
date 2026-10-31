package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/** Nhãn và bộ lọc của trang thông báo bảo mật; thuần Java để test không cần JavaFX. */
final class NoticeModel {
    static final String ALL = "Tất cả";
    static final String FORGOT = "Quên mật khẩu";
    static final String CHANGED = "Đổi / đặt lại";
    static final String LOCKED = "Tài khoản bị khoá";
    static final List<String> GROUPS = List.of(ALL, FORGOT, CHANGED, LOCKED);

    private NoticeModel() {}

    /** Tên hiển thị của một hành động trong {@code audit_log}. */
    static String label(String action) {
        return switch (action) {
            case "PASSWORD_FORGOT" -> "Bấm Quên mật khẩu";
            case "PASSWORD_FORGOT_UNKNOWN" -> "Quên mật khẩu với tên không tồn tại";
            case "PASSWORD_CHANGE" -> "Tự đổi mật khẩu";
            case "PASSWORD_CHANGE_FAIL" -> "Đổi mật khẩu nhưng nhập sai mật khẩu cũ";
            case "PASSWORD_RESET" -> "Đặt lại mật khẩu";
            case "AUTH_LOCKED" -> "Đăng nhập vào tài khoản đang bị khoá";
            default -> action;
        };
    }

    /** Nhóm lọc của một hành động. */
    static String group(String action) {
        return switch (action) {
            case "PASSWORD_FORGOT", "PASSWORD_FORGOT_UNKNOWN" -> FORGOT;
            case "PASSWORD_CHANGE", "PASSWORD_CHANGE_FAIL", "PASSWORD_RESET" -> CHANGED;
            case "AUTH_LOCKED" -> LOCKED;
            default -> ALL;
        };
    }

    /** Một dòng mô tả: ai, ở đâu / làm gì. Hành động tên lạ có actor là IP, detail là tên đã gõ. */
    static String describe(JsonNode ev) {
        String action = ev.path("action").asText();
        String actor = ev.path("actor").asText();
        String detail = ev.path("detail").asText();
        if (action.equals("PASSWORD_FORGOT_UNKNOWN")) {
            return "Tên \"" + detail + "\" từ " + actor;
        }
        return detail.isBlank() ? actor : actor + "  ·  " + detail;
    }

    static List<JsonNode> filter(JsonNode events, String group) {
        List<JsonNode> out = new ArrayList<>();
        for (JsonNode e : events) {
            if (ALL.equals(group) || group.equals(group(e.path("action").asText()))) out.add(e);
        }
        return out;
    }
}
