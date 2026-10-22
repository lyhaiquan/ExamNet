package labcast.server.ops;

import java.util.Map;

/**
 * Xếp mỗi dòng log và mỗi sự kiện {@code audit_log} vào một loại để dashboard lọc được: giáo viên tìm
 * "ai đăng nhập sai" hay "chấm bài lỗi gì" mà không phải đọc cả dòng chảy log.
 *
 * <p>Log xếp theo package của logger ({@code labcast.server.<package>.Lop}), không theo tên lớp: lớp mới
 * trong một package có sẵn tự vào đúng loại. Logger ngoài LabCast (JDK, thư viện) là {@link #HE_THONG}.
 */
public final class LogCategory {
    public static final String KET_NOI = "ket-noi";
    public static final String DANG_NHAP = "dang-nhap";
    public static final String BAI_HOC = "bai-hoc";
    public static final String CHAM_BAI = "cham-bai";
    public static final String CHIEU = "chieu";
    public static final String CODE = "code";
    public static final String GIAN_LAN = "gian-lan";
    public static final String QUAN_TRI = "quan-tri";
    public static final String HE_THONG = "he-thong";

    private static final String PREFIX = "labcast.server.";

    private static final Map<String, String> BY_PACKAGE = Map.ofEntries(
            Map.entry("net", KET_NOI),
            Map.entry("session", KET_NOI),
            Map.entry("discovery", KET_NOI),
            Map.entry("presence", KET_NOI),
            Map.entry("sync", KET_NOI),
            Map.entry("account", DANG_NHAP),
            Map.entry("lesson", BAI_HOC),
            Map.entry("run", CHAM_BAI),
            Map.entry("practice", CHAM_BAI),
            Map.entry("grade", CHAM_BAI),
            Map.entry("sqlviz", CHAM_BAI),
            Map.entry("cast", CHIEU),
            Map.entry("journal", CODE),
            Map.entry("mirror", CODE),
            Map.entry("classroom", QUAN_TRI));

    private static final Map<String, String> BY_ACTION = Map.ofEntries(
            Map.entry("AUTH_OK", DANG_NHAP),
            Map.entry("AUTH_FAIL", DANG_NHAP),
            Map.entry("LOGGED_IN_ELSEWHERE", DANG_NHAP),
            Map.entry("APP_LEAVE", GIAN_LAN),
            Map.entry("APP_RETURN", GIAN_LAN),
            Map.entry("PASTE_FLAG", GIAN_LAN),
            Map.entry("BAD_FRAME", KET_NOI),
            Map.entry("TOO_MANY_MESSAGES", KET_NOI),
            Map.entry("CAST_VIA_TCP", CHIEU));

    private LogCategory() {}

    /** Loại của một dòng log, theo tên đầy đủ của logger. */
    public static String ofLogger(String loggerName) {
        if (loggerName == null || !loggerName.startsWith(PREFIX)) {
            return HE_THONG;
        }
        String rest = loggerName.substring(PREFIX.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? HE_THONG : BY_PACKAGE.getOrDefault(rest.substring(0, dot), HE_THONG);
    }

    /** Loại của một sự kiện trong {@code audit_log}, theo cột {@code action}. */
    public static String ofAction(String action) {
        if (action == null) {
            return HE_THONG;
        }
        if (action.startsWith("ADMIN_")) {
            return QUAN_TRI;
        }
        return BY_ACTION.getOrDefault(action, HE_THONG);
    }
}
