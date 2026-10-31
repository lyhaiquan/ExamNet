package labcast.bench;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ghi kết quả thí nghiệm ra {@code statics/results/<tên>.csv}, mỗi dòng một lần đo. */
public final class Csv implements AutoCloseable {
    private final PrintWriter out;
    private final Path file;

    public Csv(String name, String... header) throws IOException {
        Path dir = Repo.root().resolve("statics/results");
        Files.createDirectories(dir);
        file = dir.resolve(name + ".csv");
        out = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8));
        out.println(String.join(",", header));
    }

    public void row(Object... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(values[i]);
        }
        out.println(sb);
        out.flush();
    }

    public Path file() {
        return file;
    }

    @Override
    public void close() {
        out.close();
    }
}
