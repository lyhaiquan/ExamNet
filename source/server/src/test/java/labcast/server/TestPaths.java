package labcast.server;

import java.nio.file.Files;
import java.nio.file.Path;

/** Đường dẫn trong repo cho test chạy thật (runner/, content/). */
public final class TestPaths {
    private TestPaths() {}

    /** Thư mục gốc repo: tìm ngược lên từ thư mục đang chạy test tới chỗ có runner/run.py. */
    public static Path repoRoot() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null) {
            if (Files.exists(p.resolve("runner/run.py"))) {
                return p;
            }
            p = p.getParent();
        }
        throw new IllegalStateException("Không thấy runner/run.py");
    }
}
