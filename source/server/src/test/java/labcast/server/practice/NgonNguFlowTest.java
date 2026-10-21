package labcast.server.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import labcast.common.protocol.CodeDelta;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.TestClient;
import labcast.server.TestPaths;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.journal.JournalModule;
import labcast.server.lesson.LessonModule;
import labcast.server.run.LocalPythonSandbox;
import labcast.server.run.RunModule;
import labcast.server.store.Database;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Đa ngôn ngữ đi trọn đường thật (spec đa ngôn ngữ): khoá tài liệu theo ngôn ngữ → chạy thử, chạy từng dòng, nộp
 * bằng C++ và Java. Runner chạy cục bộ (chế độ không sandbox); bỏ qua nếu máy thiếu Python, g++ hay JDK.
 */
class NgonNguFlowTest {
    static final String BAI = "dsa.sap-xep.noi-bot";

    static final String NOI_BOT_CPP =
            """
            #include <bits/stdc++.h>
            using namespace std;

            int main() {
                vector<int> a;
                int x;
                while (cin >> x) a.push_back(x);
                int n = a.size();
                for (int i = 0; i < n - 1; i++) {
                    for (int j = 0; j < n - 1 - i; j++) {
                        if (a[j] > a[j + 1]) {
                            swap(a[j], a[j + 1]);
                        }
                    }
                }
                for (int i = 0; i < n; i++) cout << a[i] << (i + 1 < n ? " " : "\\n");
            }
            """;

    static final String DUNG_SORT_CPP =
            """
            #include <bits/stdc++.h>
            using namespace std;
            int main() {
                vector<int> a;
                int x;
                while (cin >> x) a.push_back(x);
                sort(a.begin(), a.end());
                for (int v : a) cout << v << " ";
            }
            """;

    static final String NOI_BOT_JAVA =
            """
            import java.io.*;
            import java.util.*;

            public class Main {
                public static void main(String[] args) throws IOException {
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    String[] p = br.readLine().trim().split("\\\\s+");
                    int n = p.length;
                    int[] a = new int[n];
                    for (int i = 0; i < n; i++) {
                        a[i] = Integer.parseInt(p[i]);
                    }
                    for (int i = 0; i < n - 1; i++) {
                        for (int j = 0; j < n - 1 - i; j++) {
                            if (a[j] > a[j + 1]) {
                                int t = a[j];
                                a[j] = a[j + 1];
                                a[j + 1] = t;
                            }
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < n; i++) {
                        sb.append(a[i]).append(i + 1 < n ? " " : "\\n");
                    }
                    System.out.print(sb);
                }
            }
            """;

    static Database db;
    static LabCastServer server;
    static UserRepository users;

    @BeforeAll
    static void bat() throws Exception {
        assumeTrue(LocalPythonSandbox.detectPython() != null, "máy không có Python 3");
        assumeTrue(coLenh("g++", "--version") && coLenh("javac", "-version"), "máy thiếu g++ hoặc JDK");
        db = Database.open(":memory:");
        server = new LabCastServer(
                ServerConfig.forTest()
                        .withContent(TestPaths.repoRoot().resolve("content"))
                        .withKhongSandbox(true),
                db,
                List.of(
                        new AccountModule(),
                        new LessonModule(),
                        new RunModule(),
                        new JournalModule(),
                        new SubmitModule()));
        server.start();
        users = new UserRepository(db);
    }

    @AfterAll
    static void tat() throws Exception {
        if (server != null) {
            server.close();
            db.close();
        }
    }

    static boolean coLenh(String... lenh) {
        try {
            Process p = new ProcessBuilder(lenh).redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor(30, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    static TestClient dangNhap(String user) throws Exception {
        users.create(user, user, Role.STUDENT, "1");
        TestClient c = TestClient.connect(server.port());
        assertEquals(MessageType.AUTH_OK, c.login(user, "1").type());
        return c;
    }

    /** CODE_FETCH khoá tài liệu rồi thay cả code bằng {@code code} (seq 1). */
    static void viet(TestClient c, String docKey, String code) throws Exception {
        c.send(MessageType.CODE_FETCH, new PayloadWriter().str(docKey).toBytes());
        PayloadReader r = c.readUntil(MessageType.CODE_DATA).reader();
        assertEquals(docKey, r.str());
        r.i64();
        String cu = r.str();
        c.send(new CodeDelta(docKey, 1, 0, cu.length(), code, 0).toFrame());
        PayloadReader ack = c.readUntil(MessageType.CODE_ACK).reader();
        assertEquals(docKey, ack.str());
        assertEquals(1, ack.i64());
    }

    static JsonNode chay(TestClient c, String lang, boolean tungDong, StringBuilder trace) throws Exception {
        c.send(
                MessageType.RUN_REQ,
                new PayloadWriter()
                        .str(BAI)
                        .i64(1)
                        .str("")
                        .bool(tungDong)
                        .str(lang)
                        .toBytes());
        while (true) {
            Frame f = c.readUntil(MessageType.TRACE_CHUNK, MessageType.RUN_RESULT, MessageType.ERROR);
            if (f.type() == MessageType.TRACE_CHUNK) {
                PayloadReader ch = f.reader();
                ch.i64();
                ch.i32();
                ch.bool();
                trace.append(new String(ch.blob(), StandardCharsets.UTF_8)).append('\n');
            } else if (f.type() == MessageType.ERROR) {
                throw new AssertionError("ERROR: " + ErrorMessage.from(f).message());
            } else {
                return JsonPayload.tree(f);
            }
        }
    }

    static JsonNode nop(TestClient c, String lang) throws Exception {
        c.send(
                MessageType.SUBMIT_REQ,
                new PayloadWriter().str(BAI).i64(1).str(lang).toBytes());
        Frame f = c.readUntil(MessageType.SUBMIT_RESULT, MessageType.ERROR);
        if (f.type() == MessageType.ERROR) {
            throw new AssertionError("ERROR: " + ErrorMessage.from(f).message());
        }
        return JsonPayload.tree(f);
    }

    @Test
    void baiCoDanhSachNgonNgu_vaKhungMacDinhRiengTungNgonNgu() throws Exception {
        try (var c = dangNhap("sv-khung")) {
            c.send(MessageType.LESSON_FETCH, new PayloadWriter().str(BAI).toBytes());
            JsonNode l = JsonPayload.tree(c.readUntil(MessageType.LESSON_DATA));
            List<String> ngonNgu = l.get("ngonNgu").findValuesAsText("");
            assertEquals(4, l.get("ngonNgu").size(), l.get("ngonNgu").toString());
            assertTrue(l.get("ngonNgu").toString().contains("\"cpp\""), ngonNgu.toString());

            c.send(MessageType.CODE_FETCH, new PayloadWriter().str(BAI + "#cpp").toBytes());
            PayloadReader r = c.readUntil(MessageType.CODE_DATA).reader();
            assertEquals(BAI + "#cpp", r.str());
            assertEquals(0, r.i64());
            assertTrue(r.str().contains("#include <bits/stdc++.h>"));

            // viết bản C++ không đụng bản Python
            viet(c, BAI + "#cpp", NOI_BOT_CPP);
            c.send(MessageType.CODE_FETCH, new PayloadWriter().str(BAI).toBytes());
            r = c.readUntil(MessageType.CODE_DATA).reader();
            r.str();
            assertEquals(0, r.i64());
            assertTrue(r.str().contains("import viz"), "bản Python vẫn là khung.py");
        }
    }

    @Test
    void chayThuCpp_vaChayTungDong() throws Exception {
        try (var c = dangNhap("sv-chay-cpp")) {
            viet(c, BAI + "#cpp", NOI_BOT_CPP);
            JsonNode r = chay(c, "cpp", false, new StringBuilder());
            assertTrue(r.get("ok").asBoolean(), r.toString());
            assertEquals("1 2 3\n", r.get("stdout").asText());
            assertEquals("OK", r.get("tests").get(0).get("verdict").asText());

            assumeTrue(coLenh("gdb", "--version"), "máy thiếu gdb: bỏ phần chạy từng dòng C++");
            StringBuilder trace = new StringBuilder();
            r = chay(c, "cpp", true, trace);
            assertTrue(r.get("ok").asBoolean(), r.toString());
            assertTrue(trace.toString().contains("\"t\": \"line\""), "chạy từng dòng phải có sự kiện line");
            assertTrue(trace.toString().contains("\"t\": \"vars\""), "chạy từng dòng phải có bảng biến");
        }
    }

    @Test
    void loiBienDichCoDong() throws Exception {
        try (var c = dangNhap("sv-loi-bd")) {
            viet(c, BAI + "#cpp", "int main() {\n    int x = ;\n}\n");
            JsonNode r = chay(c, "cpp", false, new StringBuilder());
            assertFalse(r.get("ok").asBoolean());
            assertEquals(2, r.get("errorLine").asInt(), r.toString());
            assertEquals("bien-dich", r.get("errorKind").asText());

            JsonNode n = nop(c, "cpp");
            assertEquals("COMPILE_ERROR", n.get("verdict").asText(), n.toString());
            assertEquals(0, n.get("tests").size());
        }
    }

    @Test
    void nopCppDung_quaHetTest_khongSoiLuatTracePython() throws Exception {
        try (var c = dangNhap("sv-nop-cpp")) {
            viet(c, BAI + "#cpp", NOI_BOT_CPP);
            JsonNode n = nop(c, "cpp");
            assertEquals("OK", n.get("verdict").asText(), n.toString());
            assertTrue(n.get("tests").size() > 10, "phải chạy cả test ẩn");
        }
    }

    @Test
    void nopCppDungSort_biBatHamCamSuyTuCamPython() throws Exception {
        try (var c = dangNhap("sv-sort-cpp")) {
            viet(c, BAI + "#cpp", DUNG_SORT_CPP);
            JsonNode n = nop(c, "cpp");
            assertEquals("FORBIDDEN_CALL", n.get("verdict").asText(), n.toString());
            assertTrue(n.get("message").asText().contains("sort"), n.toString());
        }
    }

    @Test
    void nopJavaDung() throws Exception {
        try (var c = dangNhap("sv-nop-java")) {
            viet(c, BAI + "#java", NOI_BOT_JAVA);
            JsonNode n = nop(c, "java");
            assertEquals("OK", n.get("verdict").asText(), n.toString());
        }
    }

    @Test
    void ngonNguLa_biTuChoi() throws Exception {
        try (var c = dangNhap("sv-rust")) {
            c.send(
                    MessageType.RUN_REQ,
                    new PayloadWriter()
                            .str(BAI)
                            .i64(0)
                            .str("")
                            .bool(false)
                            .str("rust")
                            .toBytes());
            Frame f = c.readUntil(MessageType.ERROR);
            assertEquals(ErrorCode.BAD_FRAME, ErrorMessage.from(f).code());
        }
    }
}
