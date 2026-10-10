package labcast.server.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import labcast.server.TestPaths;
import org.junit.jupiter.api.Test;

/** Chạy runner thật. Bỏ qua phần cần Python hoặc Docker khi máy không có. */
class SandboxTest {
    static final String NOI_BOT =
            """
            import viz

            def sap_xep(a):
                n = len(a)
                for i in range(n - 1):
                    for j in range(n - 1 - i):
                        if a[j] > a[j + 1]:
                            a[j], a[j + 1] = a[j + 1], a[j]

            a = viz.Mang(list(map(int, input().split())), ten="a")
            sap_xep(a)
            print(*a)
            """;

    static Path runnerDir() {
        return TestPaths.repoRoot().resolve("runner");
    }

    static Sandbox local() {
        String py = LocalPythonSandbox.detectPython();
        assumeTrue(py != null, "máy không có Python 3");
        return new LocalPythonSandbox(py, runnerDir());
    }

    @Test
    void local_noiBot_okVaCoTrace() throws Exception {
        List<String> lines = new ArrayList<>();
        Outcome o = local().run(new Job(NOI_BOT, "5 1 4 2\n", true, 2_000), lines::add);
        assertTrue(o.ok(), o.error());
        assertEquals("1 2 4 5\n", o.stdout());
        assertTrue(lines.size() > 5);
        assertTrue(lines.get(0).contains("\"hdr\""));
        assertFalse(o.killed());
    }

    @Test
    void local_vongLapVoHan_biGietDungHan() throws Exception {
        long t0 = System.nanoTime();
        Outcome o = local().run(new Job("while True:\n    pass\n", "", false, 500), l -> {});
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertTrue(o.killed());
        assertFalse(o.ok());
        assertTrue(ms < 4_000, "mất " + ms + " ms");
    }

    @Test
    void local_loiRuntime_coThongBao() throws Exception {
        Outcome o = local().run(new Job("print(1/0)\n", "", false, 2_000), l -> {});
        assertFalse(o.ok());
        assertTrue(o.error().contains("ZeroDivisionError"), o.error());
    }

    @Test
    void docker_khongCoMang_vaBiGietKhiQuaGio() throws Exception {
        assumeTrue(DockerSandbox.available(), "Docker không chạy");
        assumeTrue(
                DockerSandbox.imageAvailable(DockerSandbox.DEFAULT_IMAGE),
                "chưa có image: docker build -t labcast-runner:py3.12 runner");
        var d = new DockerSandbox(DockerSandbox.DEFAULT_IMAGE);
        Outcome net = d.run(
                new Job("import socket\nsocket.create_connection((\"1.1.1.1\", 80), 2)\n", "", false, 5_000), l -> {});
        assertFalse(net.ok());
        long t0 = System.nanoTime();
        Outcome loop = d.run(new Job("while True:\n    pass\n", "", false, 500), l -> {});
        assertTrue(loop.killed());
        assertTrue((System.nanoTime() - t0) / 1_000_000 < 8_000);
    }

    @Test
    void runService_chayTrenPoolRieng() throws Exception {
        try (var rs = new RunService(local(), 2)) {
            Outcome o = rs.run(1, new Job("print('chào')", "", false, 2_000), l -> {})
                    .get();
            assertEquals("chào\n", o.stdout());
        }
    }
}
