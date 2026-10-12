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

    @Override
    protected List<String> scriptCommand(String name, String script, Path lessonDir) {
        String py = runPy.resolveSibling(script + ".py").toString();
        return lessonDir == null
                ? List.of(python, py)
                : List.of(python, py, lessonDir.toAbsolutePath().toString());
    }

    /** run.py không mở được (sai thư mục runner) là lỗi cấu hình server, không phải lỗi bài làm. */
    @Override
    protected java.io.IOException infrastructureFailure(int exitCode, String stderr) {
        if (stderr.contains("can't open file") || stderr.contains("No module named 'viz'")) {
            return new java.io.IOException("Không chạy được runner ở " + runPy + ": " + stderr);
        }
        return null;
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
