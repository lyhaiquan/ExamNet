package labcast.server.ops;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cho chữ tiếng Việt server in ra hiện đúng. Gọi ĐẦU TIÊN trong {@code main}.
 *
 * <p>Java 17 in ra stdout/stderr bằng bảng mã của hệ thống: Windows là Cp1252 (hoặc bảng mã của cửa sổ cmd,
 * thường 437), nên ớ, ư, ạ… thành '?' ngay trong Java, trước khi tới cửa sổ. Lớp này:
 *
 * <ul>
 *   <li>đổi System.out, System.err và log sang UTF-8 — đúng cả khi output ghi ra file, ống, Docker;
 *   <li>trên Windows, khi đang chạy trong cửa sổ lệnh, đổi bảng mã của cửa sổ sang UTF-8 (như gõ
 *       {@code chcp 65001}) để cửa sổ hiểu đúng các byte UTF-8, rồi trả lại bảng mã cũ lúc server tắt
 *       (Ctrl+C cũng chạy shutdown hook) để các lệnh gõ sau trong cửa sổ đó không bị ảnh hưởng.
 * </ul>
 *
 * <p>Bảng mã là thuộc tính của cửa sổ lệnh, mọi tiến trình gắn vào cửa sổ dùng chung, nên chạy {@code chcp}
 * ở một tiến trình con là đổi được cho chính cửa sổ của server — với điều kiện stdin của {@code chcp} là
 * cửa sổ lệnh (kế thừa từ server). stdin là ống (mặc định của ProcessBuilder) thì {@code chcp} chạy xong
 * với mã 0 mà KHÔNG đổi gì; đã đo trong cửa sổ cmd thật ngày 12/10/2026.
 */
public final class ConsoleUtf8 {
    static final String UTF8_CODE_PAGE = "65001";
    private static final Pattern CODE_PAGE = Pattern.compile("(\\d+)\\D*$");

    private ConsoleUtf8() {}

    public static void install() {
        if (isWindows() && System.console() != null) {
            Optional<String> old = activeCodePage();
            if (old.isPresent() && !old.get().equals(UTF8_CODE_PAGE) && setCodePage(UTF8_CODE_PAGE)) {
                String back = old.get();
                Runtime.getRuntime().addShutdownHook(new Thread(() -> setCodePage(back), "tra-bang-ma"));
            }
        }
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
        // Handler log mặc định ghi ra stderr bằng bảng mã hệ thống: đổi sang UTF-8 cho khớp
        for (Handler h : Logger.getLogger("").getHandlers()) {
            if (h instanceof ConsoleHandler) {
                try {
                    h.setEncoding(StandardCharsets.UTF_8.name());
                } catch (Exception ignored) {
                    // giữ như cũ
                }
            }
        }
    }

    /** Số bảng mã trong output của {@code chcp}, ví dụ "Active code page: 437" → 437 (mọi ngôn ngữ Windows). */
    static Optional<String> parseCodePage(String chcpOutput) {
        if (chcpOutput == null) {
            return Optional.empty();
        }
        Matcher m = CODE_PAGE.matcher(chcpOutput.strip());
        return m.find() ? Optional.of(m.group(1)) : Optional.empty();
    }

    private static Optional<String> activeCodePage() {
        try {
            Process p = new ProcessBuilder("cmd", "/c", "chcp")
                    .redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .redirectErrorStream(true)
                    .start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.ISO_8859_1);
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0 ? parseCodePage(out) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static boolean setCodePage(String cp) {
        try {
            Process p = new ProcessBuilder("cmd", "/c", "chcp " + cp)
                    .redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
    }
}
