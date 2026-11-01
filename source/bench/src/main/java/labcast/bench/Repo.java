package labcast.bench;

import java.nio.file.Files;
import java.nio.file.Path;

/** Tìm thư mục gốc repo (có {@code content/} và {@code runner/}) từ thư mục đang chạy. */
public final class Repo {
    private Repo() {}

    public static Path root() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.exists(p.resolve("runner/run.py"))) {
            p = p.getParent();
        }
        if (p == null) throw new IllegalStateException("Không tìm thấy gốc repo (thiếu runner/run.py)");
        return p;
    }
}
