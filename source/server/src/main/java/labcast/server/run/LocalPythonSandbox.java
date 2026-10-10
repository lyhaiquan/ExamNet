package labcast.server.run;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Chạy {@code python run.py} bằng tiến trình con, KHÔNG cách ly: code học viên đọc được file và mạng
 * của máy server. Chỉ dùng khi phát triển ({@code --khong-sandbox}, spec §8.1).
 */
public final class LocalPythonSandbox extends ProcessSandbox {
    private final String python;
    private final Path runPy;

    public LocalPythonSandbox(Path runnerDir) {
        this(detectPython(), runnerDir);
    }

    public LocalPythonSandbox(String python, Path runnerDir) {
        this.python = python;
        this.runPy = runnerDir.resolve("run.py").toAbsolutePath();
    }

    @Override
    protected List<String> command(String name) {
        return List.of(python, runPy.toString());
    }

    /** {@code LABCAST_PYTHON} nếu có, rồi {@code python3}, rồi {@code python}. Không có thì null. */
    public static String detectPython() {
        String env = System.getenv("LABCAST_PYTHON");
        if (env != null && !env.isBlank()) {
            return env;
        }
        for (String cmd : List.of("python3", "python")) {
            try {
                Process p = new ProcessBuilder(cmd, "--version")
                        .redirectErrorStream(true)
                        .start();
                if (p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0) {
                    String v = new String(p.getInputStream().readAllBytes()).strip();
                    if (v.startsWith("Python 3")) {
                        return cmd;
                    }
                }
            } catch (Exception ignored) {
                // thử lệnh kế
            }
        }
        return null;
    }
}
