package labcast.common.protocol;

import java.util.Locale;

/**
 * Ngôn ngữ học viên viết bài (spec đa ngôn ngữ §1). Mã ({@link #code()}) đi trong {@code RUN_REQ}, {@code
 * SUBMIT_REQ}, {@code LESSON_DATA} và khoá tài liệu code; runner nhận cùng mã ở khoá {@code "ngon_ngu"}.
 */
public enum Language {
    PYTHON("python", "Python 3.12", "main.py", 1, 0, ""),
    C(
            "c",
            "C (gcc)",
            "main.c",
            1,
            6_000,
            """
            #include <stdio.h>

            int main(void) {
                // Đọc input bằng scanf, in kết quả bằng printf
                return 0;
            }
            """),
    CPP(
            "cpp",
            "C++17",
            "main.cpp",
            1,
            6_000,
            """
            #include <bits/stdc++.h>
            using namespace std;

            int main() {
                ios::sync_with_stdio(false);
                cin.tie(nullptr);
                // Đọc input bằng cin, in kết quả bằng cout
                return 0;
            }
            """),
    JAVA(
            "java",
            "Java 17",
            "Main.java",
            2,
            8_000,
            """
            import java.io.*;
            import java.util.*;

            public class Main {
                public static void main(String[] args) throws IOException {
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    // Đọc input bằng br.readLine(), in kết quả bằng System.out.println
                }
            }
            """);

    /** Ngăn cách mã bài và mã ngôn ngữ trong khoá tài liệu: {@code dsa.cay.heap-them#cpp}. */
    public static final char DOC_KEY_SEPARATOR = '#';

    private final String code;
    private final String label;
    private final String fileName;
    private final int timeFactor;
    private final int compileBudgetMs;
    private final String starter;

    Language(String code, String label, String fileName, int timeFactor, int compileBudgetMs, String starter) {
        this.code = code;
        this.label = label;
        this.fileName = fileName;
        this.timeFactor = timeFactor;
        this.compileBudgetMs = compileBudgetMs;
        this.starter = starter;
    }

    public String code() {
        return code;
    }

    /** Tên hiện trên ô chọn ngôn ngữ. */
    public String label() {
        return label;
    }

    /** Tên file của học viên, hiện cạnh ô code và trong thông báo lỗi biên dịch. */
    public String fileName() {
        return fileName;
    }

    /** Đuôi file khung / lời giải trong thư mục bài: {@code khung.cpp}, {@code loi_giai.java}. */
    public String extension() {
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    /** Giới hạn thời gian của bài nhân lên bao nhiêu (JVM khởi động chậm). */
    public int timeFactor() {
        return timeFactor;
    }

    /** Thời gian biên dịch (và dựng debugger) cộng vào hạn chờ của lượt chạy; không tính vào giới hạn của bài. */
    public int compileBudgetMs() {
        return compileBudgetMs;
    }

    /** Khung mặc định khi bài không có {@code khung.<đuôi>}; Python dùng {@code khung.py} của bài. */
    public String starter() {
        return starter;
    }

    /** Mã rỗng hay null (client cũ không gửi) là Python; mã lạ trả null. */
    public static Language of(String code) {
        if (code == null || code.isEmpty()) {
            return PYTHON;
        }
        for (Language l : values()) {
            if (l.code.equals(code.toLowerCase(Locale.ROOT))) {
                return l;
            }
        }
        return null;
    }

    /** Khoá tài liệu code của một bài ở một ngôn ngữ. Python giữ nguyên mã bài: code đã lưu trước đây không mất. */
    public static String docKey(String lessonId, Language l) {
        return l == PYTHON ? lessonId : lessonId + DOC_KEY_SEPARATOR + l.code;
    }

    /** Mã bài trong khoá tài liệu. */
    public static String lessonOf(String docKey) {
        int i = docKey.indexOf(DOC_KEY_SEPARATOR);
        return i < 0 ? docKey : docKey.substring(0, i);
    }

    /** Ngôn ngữ trong khoá tài liệu; khoá không có phần ngôn ngữ là Python, phần ngôn ngữ lạ trả null. */
    public static Language ofDocKey(String docKey) {
        int i = docKey.indexOf(DOC_KEY_SEPARATOR);
        return i < 0 ? PYTHON : of(docKey.substring(i + 1));
    }
}
