package labcast.server.run;

import java.io.IOException;
import java.util.function.Consumer;

/** Nơi chạy code học viên. Bản thật là {@link DockerSandbox}; {@link LocalPythonSandbox} chỉ để phát triển. */
public interface Sandbox {
    /** Chạy job; mỗi dòng trace gọi {@code onLine} (không gồm dòng "result"). */
    Outcome run(Job job, Consumer<String> onLine) throws IOException, InterruptedException;
}
