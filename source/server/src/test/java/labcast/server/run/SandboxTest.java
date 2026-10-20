package labcast.server.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
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

    /**
     * Script chấm chạy trong container học viên; script tin cậy đọc được thư mục bài gắn chỉ đọc nhưng không
     * ghi được vào đó; container học viên không thấy thư mục bài.
     */
    @Test
    void docker_chamVaScriptTinCay() throws Exception {
        assumeTrue(DockerSandbox.available(), "Docker không chạy");
        assumeTrue(
                DockerSandbox.imageAvailable(DockerSandbox.DEFAULT_IMAGE),
                "chưa có image: docker build -t labcast-runner:py3.12 runner");
        var d = new DockerSandbox(DockerSandbox.DEFAULT_IMAGE);
        String job = "{\"k\":\"k1\",\"code\":"
                + labcast.common.protocol.JsonPayload.MAPPER.writeValueAsString(
                        NOI_BOT + "import os\nprint(os.path.exists('/bai'))\n")
                + ",\"time_limit_ms\":1000,\"tests\":[{\"id\":\"a\",\"input\":\"3 2 1\\n\"}],\"cam\":{}}";
        ScriptRunner.Result cham = d.runScript("cham", null, job, 20_000);
        String dong = String.join("\n", cham.lines());
        assertTrue(
                dong.contains("\"stdout\": \"1 2 3\\nFalse\\n\""), "container học viên không được thấy /bai: " + dong);

        Path bai = TestPaths.repoRoot().resolve("content/dsa/sap-xep/noi-bot");
        ScriptRunner.Result prep = d.runScript("chuan_bi", bai, "{\"so_test_an\": 2}", 60_000);
        assertTrue(prep.lines().get(prep.lines().size() - 1).contains("\"an-2\""), String.join("\n", prep.lines()));
    }

    /** Docker không chạy / thiếu image: không được chấm thành lỗi của học viên. */
    @Test
    void dockerBaoLoi_laLoiHaTang_khongPhaiLoiBaiLam() throws Exception {
        String py = LocalPythonSandbox.detectPython();
        assumeTrue(py != null, "máy không có Python 3");
        var gia = new DockerSandbox(DockerSandbox.DEFAULT_IMAGE) {
            @Override
            protected List<String> command(String name) {
                // giả lập `docker run` báo lỗi: in ra stderr rồi thoát mã 125
                return List.of(
                        py,
                        "-c",
                        "import sys; sys.stdin.read(); sys.stderr.write('docker: Error response from daemon');"
                                + " sys.exit(125)");
            }
        };
        var ex = assertThrows(IOException.class, () -> gia.run(new Job("print(1)", "", false, 1_000), l -> {}));
        assertTrue(ex.getMessage().contains("Docker"), ex.getMessage());
    }

    @Test
    void baiLamTuThoatMa3_vanLaLoiBaiLam() throws Exception {
        Outcome o = local().run(new Job("import os\nos._exit(3)\n", "", false, 2_000), l -> {});
        assertFalse(o.ok());
        assertFalse(o.killed());
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
