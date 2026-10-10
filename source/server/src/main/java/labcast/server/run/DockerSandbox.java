package labcast.server.run;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Mỗi lượt chạy là một container dùng một lần, đủ cờ an toàn ở spec §8.1: không mạng, giới hạn RAM,
 * CPU, số tiến trình, hệ thống file chỉ đọc, không quyền root. Container KHÔNG được gắn thư mục bài
 * học: code học viên chỉ nhận code và input qua stdin, nên không đọc được lời giải hay test ẩn.
 */
public final class DockerSandbox extends ProcessSandbox {
    public static final String DEFAULT_IMAGE = "labcast-runner:py3.12";

    private final String image;

    public DockerSandbox(String image) {
        this.image = image;
    }

    @Override
    protected List<String> command(String name) {
        // image đã có ENTRYPOINT ["python", "/runner/run.py"], không cần ghi lệnh sau tên image
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
                "/tmp:size=16m",
                "--user",
                "65534:65534",
                "--cap-drop",
                "ALL",
                "--security-opt",
                "no-new-privileges",
                image);
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
