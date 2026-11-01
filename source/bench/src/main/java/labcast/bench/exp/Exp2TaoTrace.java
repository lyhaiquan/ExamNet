package labcast.bench.exp;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import labcast.bench.Repo;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.lesson.DemoTraces;
import labcast.server.lesson.LessonModule;
import labcast.server.run.RunModule;
import labcast.server.store.Database;

/**
 * Chuẩn bị cho TN2: chạy lời giải bài giảng giải một lần (Python cục bộ) rồi ghi trace ra file, để máy
 * giáo viên lúc đo chỉ cần Java. Chạy lại khi bài đổi: {@code Exp2TaoTrace dsa.sap-xep.noi-bot out.trace}.
 */
public final class Exp2TaoTrace {
    public static void main(String[] args) throws Exception {
        String bai = args.length > 0 ? args[0] : Exp2Chieu.BAI;
        Path out = Path.of(args.length > 1 ? args[1] : "tn2.trace");
        var cfg = ServerConfig.forTest()
                .withContent(Repo.root().resolve("content"))
                .withKhongSandbox(true);
        try (Database db = Database.open(":memory:");
                var server = new LabCastServer(cfg, db, List.of(new LessonModule(), new RunModule()))) {
            server.start();
            List<String> lines =
                    server.context().require(DemoTraces.class).demoTrace(bai).get(60, TimeUnit.SECONDS);
            if (lines.isEmpty()) {
                throw new IllegalStateException("Bài " + bai + " không ra trace (không phải bài giảng giải?)");
            }
            Files.write(out, lines, StandardCharsets.UTF_8);
            System.out.println("Đã ghi " + lines.size() + " dòng trace vào " + out.toAbsolutePath());
        }
    }
}
