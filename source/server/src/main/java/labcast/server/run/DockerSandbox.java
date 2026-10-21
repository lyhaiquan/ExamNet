package labcast.server.run;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Mỗi lượt chạy là một container dùng một lần, đủ cờ an toàn ở spec §8.1: không mạng, giới hạn RAM,
 * CPU, số tiến trình, hệ thống file chỉ đọc, không quyền root. Container KHÔNG được gắn thư mục bài
 * học: code học viên chỉ nhận code và input qua stdin, nên không đọc được lời giải hay test ẩn.
 */
// Không final: test thay lệnh docker bằng tiến trình giả để kiểm cách phân loại lỗi hạ tầng.
public class DockerSandbox extends ProcessSandbox {
    public static final String DEFAULT_IMAGE = "labcast-runner:py3.12";

    private final String image;

    public DockerSandbox(String image) {
        this.image = image;
    }

    @Override
    protected List<String> command(String name) {
        // image đã có ENTRYPOINT ["python", "/runner/run.py"], không cần ghi lệnh sau tên image
        List<String> cmd = new ArrayList<>(safeRun(name));
        cmd.add(image);
        return cmd;
    }

    /**
     * Cùng cờ an toàn, đổi điểm vào thành {@code runner/<script>.py}. Có {@code lessonDir} thì gắn vào
     * {@code /bai} CHỈ ĐỌC — chỉ cho script tin cậy (xem {@link ScriptRunner}).
     */
    @Override
    protected List<String> scriptCommand(String name, String script, Path lessonDir) {
        List<String> cmd = new ArrayList<>(safeRun(name));
        if (lessonDir != null) {
            cmd.add("--mount");
            cmd.add("type=bind,source=" + lessonDir.toAbsolutePath() + ",target=/bai,readonly");
        }
        cmd.addAll(List.of("--entrypoint", "python", image, "/runner/" + script + ".py"));
        if (lessonDir != null) {
            cmd.add("/bai");
        }
        return cmd;
    }

    /** {@code docker run} với đủ cờ an toàn ở spec §8.1, chưa có tên image. */
    private static List<String> safeRun(String name) {
        return List.of(
                "docker",
                "run",
                "--rm",
                "-i",
                "--name",
                name,
                "--network",
                "none",
                "--memory",
                "256m",
                "--memory-swap",
                "256m",
                "--cpus",
                "1",
                "--pids-limit",
                "64",
                "--read-only",
                "--tmpfs",
                // exec: bài C/C++ chạy file vừa biên dịch trong /tmp (tmpfs của Docker mặc định noexec)
                "/tmp:size=64m,exec",
                "--user",
                "65534:65534",
                "--cap-drop",
                "ALL",
                "--security-opt",
                "no-new-privileges");
    }

    /** Dựng container mất 1–2 s trên Windows; không tính khoảng này vào giới hạn của bài. */
    @Override
    protected long startupBudgetMs() {
        return 3_000;
    }

    /**
     * {@code docker run} tự báo lỗi (daemon không chạy, thiếu image) bằng mã 125–127 và dòng {@code
     * docker: …} trên stderr; khi đó code học viên chưa hề chạy.
     */
    @Override
    protected IOException infrastructureFailure(int exitCode, String stderr) {
        if (exitCode >= 125 && exitCode <= 127
                || stderr.startsWith("docker:")
                || stderr.contains("Unable to find image")) {
            return new IOException("Docker không chạy được lượt này (mã " + exitCode + "): " + firstLine(stderr)
                    + ". Bật Docker Desktop và build image: docker build -t " + image + " runner");
        }
        return null;
    }

    private static String firstLine(String s) {
        return s.lines().findFirst().orElse("");
    }

    /** Giết tiến trình {@code docker run} chưa đủ: container vẫn chạy. Phải {@code docker kill} theo tên. */
    @Override
    protected void kill(String name, Process p) {
        try {
            new ProcessBuilder("docker", "kill", name)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // container có thể đã tự thoát
        }
        p.destroyForcibly();
    }

    /** Image runner đã được build hoặc {@code docker load} trên máy này. */
    public static boolean imageAvailable(String image) {
        try {
            Process p = new ProcessBuilder("docker", "image", "inspect", image)
                    .redirectErrorStream(true)
                    .start();
            p.getInputStream().readAllBytes();
            return p.waitFor(15, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** {@code docker info} chạy được: Docker Desktop đang bật. */
    public static boolean available() {
        try {
            Process p = new ProcessBuilder("docker", "info")
                    .redirectErrorStream(true)
                    .start();
            p.getInputStream().readAllBytes();
            return p.waitFor(15, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
