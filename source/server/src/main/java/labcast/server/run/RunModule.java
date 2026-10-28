package labcast.server.run;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.Language;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.ProtocolException;
import labcast.server.ServerContext;
import labcast.server.ServerModule;
import labcast.server.journal.CodeStore;
import labcast.server.lesson.Lesson;
import labcast.server.lesson.LessonModule;
import labcast.server.lesson.LessonRepository;
import labcast.server.net.Ctx;
import labcast.server.net.Priority;
import labcast.server.practice.PracticeModule;
import labcast.server.session.Session;
import labcast.server.sqlviz.SqlLab;

/**
 * Chạy thử code học viên với một test xem được, gửi trace về để học viên xem
 * chính code mình chạy. Chủ: Người 3.
 */
public final class RunModule implements ServerModule {
    private static final Logger LOG = Logger.getLogger(RunModule.class.getName());
    static final int LINES_PER_CHUNK = 200;

    private final AtomicLong runIds = new AtomicLong();
    private ServerContext ctx;
    private RunService runs;

    @Override
    public void install(ServerContext ctx) {
        this.ctx = ctx;
        Sandbox sb = sandbox(ctx);
        this.runs = new RunService(sb);
        ctx.register(RunService.class, runs);
        // chấm bài nộp (practice.SubmitModule) chạy script khác của runner trên cùng
        // sandbox
        ctx.register(ScriptRunner.class, (ScriptRunner) sb);
        ctx.router().on(MessageType.RUN_REQ, this::run);
        ctx.router().on(MessageType.PLAYGROUND_RUN_REQ, this::runPlayground);
    }

    private static Sandbox sandbox(ServerContext ctx) {
        if (!ctx.config().khongSandbox()) {
            return new DockerSandbox(DockerSandbox.DEFAULT_IMAGE);
        }
        String py = LocalPythonSandbox.detectPython();
        if (py == null) {
            LOG.warning("--khong-sandbox nhưng không tìm thấy Python 3 — mọi lượt chạy sẽ báo SANDBOX_DOWN");
            return new Unavailable("Không có Python 3 trên máy server");
        }
        LOG.warning("CHẾ ĐỘ KHÔNG SANDBOX: code học viên chạy thẳng trên máy này. Chỉ dùng khi phát triển.");
        return new LocalPythonSandbox(py, ctx.config().runnerDir());
    }

    /**
     * {@code RUN_REQ(str lessonId, i64 codeSeq, str testId, bool lineTrace, str lang)}
     * → {@code RUN_QUEUED(i64
     * runId, u16 position)}, các
     * {@code TRACE_CHUNK(i64 runId, i32 chunkIndex, bool last, blob lines)}, rồi
     * {@code
     * RUN_RESULT JSON{runId, ok, stdout, error, truncated, timeMs, tests:[{id, verdict}], errorLine?, errorKind?}}.
     * {@code lineTrace} và {@code lang} có thể thiếu (client cũ): tắt chạy từng
     * dòng, Python. Code chạy là bản
     * đang lưu của khoá tài liệu {@code lessonId} ở ngôn ngữ đó.
     */
    private void run(Ctx c, Frame f) throws Exception {
        PayloadReader r = f.reader();
        String lessonId = r.str();
        long codeSeq = r.i64();
        String testId = r.str();
        boolean lineTrace = r.hasRemaining() && r.bool(); // client cũ không gửi cờ: tắt tracing từng dòng
        String langCode = r.hasRemaining() ? r.str() : "";
        Session s = c.session();
        LessonRepository repo = ctx.require(LessonRepository.class);
        Lesson l = repo.get(lessonId)
                .orElseThrow(() -> new ProtocolException(ErrorCode.NOT_FOUND, "Không có bài " + lessonId));
        PracticeModule.requireOpen(ctx, c, lessonId);
        Language lang = language(l, langCode);
        String docKey = Language.docKey(lessonId, lang);
        if (testId.isEmpty()) {
            testId = l.testXem().isEmpty() ? "" : l.testXem().get(0);
        }
        String input = repo.testInput(lessonId, testId).orElse("");
        String expected = repo.testOutput(lessonId, testId).orElse(null);

        // Chạy đúng phiên bản code học viên đang thấy: delta có thể còn trên đường tới,
        // chờ tối đa 1 s.
        CodeStore store = ctx.require(CodeStore.class);
        CodeStore.Snapshot snap = store.current(s.userId(), docKey);
        for (int i = 0; i < 20 && snap.seq() < codeSeq; i++) {
            Thread.sleep(50);
            snap = store.current(s.userId(), docKey);
        }

        long runId = runIds.incrementAndGet();
        c.reply(MessageType.RUN_QUEUED, new PayloadWriter().i64(runId).u16(0).toBytes());
        if (l.isSql()) {
            runSql(s, l, runId, snap);
            return;
        }

        // C/C++/Java không có animation: chỉ ghi trace khi chạy từng dòng
        boolean trace = lang == Language.PYTHON || lineTrace;
        List<Map<String, String>> views = lang == Language.PYTHON ? LessonModule.views(l) : List.of();
        Job job = new Job(snap.code(), input, trace, l.thoiGianMs(), lessonId, views, lineTrace, lang);
        Chunker chunker = new Chunker(runId, s);
        String tid = testId;
        long seqRun = snap.seq();
        runs.run(s.userId(), job, chunker).whenComplete((o, err) -> {
            if (RunService.isReplaced(err)) {
                replaced(s, runId);
                return;
            }
            chunker.finish();
            if (err != null) {
                LOG.log(Level.WARNING, "Sandbox lỗi", err);
                s.send(
                        new ErrorMessage(ErrorCode.SANDBOX_DOWN, "Không chạy được code: " + err.getMessage(), 0)
                                .toFrame(),
                        Priority.NORMAL);
                return;
            }
            String verdict = verdict(o, expected, job.effectiveTimeLimitMs());
            record(s.userId(), lessonId, seqRun, verdict);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("runId", runId);
            res.put("ok", o.ok());
            res.put("stdout", o.stdout());
            res.put("error", o.error());
            res.put("truncated", o.truncated());
            res.put("timeMs", o.timeMs());
            res.put("tests", List.of(Map.of("id", tid, "verdict", verdict)));
            if (o.errorLine() > 0) {
                res.put("errorLine", o.errorLine());
                res.put("errorKind", o.errorKind());
            }
            s.send(JsonPayload.frame(MessageType.RUN_RESULT, res), Priority.NORMAL);
        });
    }

    /**
     * Workspace tự do: code do người dùng nhập, không chọn bài và không ghi
     * submissions. {@code
     * PLAYGROUND_RUN_REQ JSON{code, input, trace, lang?}}; thiếu {@code lang} là
     * Python.
     */
    private void runPlayground(Ctx c, Frame f) throws Exception {
        var request = JsonPayload.tree(f);
        String code = request.path("code").asText("");
        String input = request.path("input").asText("");
        boolean lineTrace = request.path("trace").asBoolean(false);
        Language lang = Language.of(request.path("lang").asText(""));
        if (lang == null) {
            throw new ProtocolException(
                    ErrorCode.BAD_FRAME, "Code tự do không chạy được ngôn ngữ " + request.path("lang"));
        }
        if (code.isBlank() || code.length() > 30_000 || input.length() > 20_000) {
            throw new ProtocolException(
                    ErrorCode.BAD_FRAME, "Code tự do yêu cầu 1-30000 ký tự và stdin tối đa 20000 ký tự");
        }
        Session s = c.session();
        long runId = runIds.incrementAndGet();
        c.reply(
                MessageType.PLAYGROUND_RUN_QUEUED,
                new PayloadWriter().i64(runId).toBytes());
        // Giới hạn thời gian, dung lượng và quyền truy cập giống Chạy thử Python.
        Job job = new Job(code, input, lineTrace, 3000, "playground", List.of(), lineTrace, lang);
        List<String> pending = new ArrayList<>();
        int[] part = { 0 };
        Consumer<Boolean> sendChunk = last -> {
            byte[] lines;
            synchronized (pending) {
                lines = String.join("\n", pending).getBytes(StandardCharsets.UTF_8);
                pending.clear();
            }
            s.send(
                    Frame.of(
                            MessageType.PLAYGROUND_TRACE_CHUNK,
                            new PayloadWriter()
                                    .i64(runId)
                                    .i32(part[0]++)
                                    .bool(last)
                                    .blob(lines)
                                    .toBytes()),
                    Priority.NORMAL);
        };
        Consumer<String> onLine = line -> {
            boolean flush;
            synchronized (pending) {
                pending.add(line);
                flush = pending.size() >= LINES_PER_CHUNK;
            }
            if (flush)
                sendChunk.accept(false);
        };
        runs.run(s.userId(), job, onLine).whenComplete((out, err) -> {
            sendChunk.accept(true);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("runId", runId);
            if (err != null) {
                String message = RunService.isReplaced(err)
                        ? "Lượt chạy đã bị một lượt mới thay thế"
                        : "Không chạy được Python Runner: " + err.getMessage();
                result.put("ok", false);
                result.put("stdout", "");
                result.put("error", message);
                result.put("timeMs", 0);
                result.put("truncated", false);
            } else {
                result.put("ok", out.ok());
                result.put("stdout", out.stdout());
                result.put("error", out.error());
                result.put("timeMs", out.timeMs());
                result.put("truncated", out.truncated());
            }
            s.send(JsonPayload.frame(MessageType.PLAYGROUND_RUN_RESULT, result), Priority.NORMAL);
        });
    }

    /**
     * Bài SQL: câu của học viên chạy bằng bộ máy SQL trên dữ liệu mẫu (không qua
     * sandbox Python), cùng các
     * thông điệp như bài DSA — trace các bước gửi bằng {@code TRACE_CHUNK}, kết quả
     * dạng bảng chữ ở {@code
     * stdout}, đúng/sai so với đáp án ở test {@code du-lieu-mau}.
     */
    private void runSql(Session s, Lesson l, long runId, CodeStore.Snapshot snap) {
        SqlLab lab = ctx.require(SqlLab.class);
        String code = snap.code();
        long seqRun = snap.seq();
        runs.tryRun(s.userId(), () -> lab.run(l, code)).whenComplete((res, err) -> {
            if (RunService.isReplaced(err)) {
                replaced(s, runId);
                return;
            }
            if (err != null) {
                LOG.log(Level.WARNING, "Chạy SQL lỗi", err);
                s.send(
                        new ErrorMessage(ErrorCode.INTERNAL, "Server không chạy được câu SQL: " + err.getMessage(), 0)
                                .toFrame(),
                        Priority.NORMAL);
                return;
            }
            Chunker chunker = new Chunker(runId, s);
            res.trace().forEach(chunker);
            chunker.finish();
            record(s.userId(), l.id(), seqRun, res.verdict());
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("runId", runId);
            out.put("ok", res.ok());
            out.put("stdout", res.stdout());
            out.put("error", res.error());
            out.put("truncated", false);
            out.put("timeMs", res.timeMs());
            out.put("tests", List.of(Map.of("id", SqlLab.SAMPLE_TEST, "verdict", res.verdict())));
            s.send(JsonPayload.frame(MessageType.RUN_RESULT, out), Priority.NORMAL);
        });
    }

    /**
     * Lượt còn chờ bị lượt chạy thử mới hơn của cùng người thay: trả
     * {@code RUN_RESULT} của chính lượt cũ với
     * verdict {@code REPLACED}, KHÔNG trả {@code ERROR} — ERROR không mang runId,
     * app sẽ tưởng lượt mới hỏng.
     */
    private static void replaced(Session s, long runId) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("runId", runId);
        res.put("ok", false);
        res.put("stdout", "");
        res.put("error", "Lượt chạy này bị lượt chạy thử mới hơn thay");
        res.put("truncated", false);
        res.put("timeMs", 0);
        res.put("tests", List.of(Map.of("id", "", "verdict", "REPLACED")));
        s.send(JsonPayload.frame(MessageType.RUN_RESULT, res), Priority.NORMAL);
    }

    /**
     * Sandbox khi máy server không chạy được code: mọi lượt báo lỗi hạ tầng
     * (SANDBOX_DOWN).
     */
    private record Unavailable(String reason) implements Sandbox, ScriptRunner {
        @Override
        public Outcome run(Job job, Consumer<String> onLine) throws java.io.IOException {
            throw new java.io.IOException(reason);
        }

        @Override
        public Result runScript(String script, java.nio.file.Path lessonDir, String stdin, long deadlineMs)
                throws java.io.IOException {
            throw new java.io.IOException(reason);
        }
    }

    /** Kết quả chấm của một test (spec §10.3). */
    /**
     * Ngôn ngữ của lượt chạy hay nộp bài: mã rỗng (client cũ) là Python; bài SQL bỏ
     * qua mã. Mã lạ hay ngôn ngữ bài
     * không cho chọn là frame sai.
     */
    public static Language language(Lesson l, String code) throws ProtocolException {
        if (l.isSql()) {
            return Language.PYTHON;
        }
        Language lang = Language.of(code);
        if (lang == null || !l.choPhep(lang)) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "Bài " + l.id() + " không cho viết bằng " + code);
        }
        return lang;
    }

    static String verdict(Outcome o, String expected, int timeLimitMs) {
        // time_ms đo bên trong runner, không gồm thời gian dựng container
        if (o.killed() || (o.ok() && o.timeMs() > timeLimitMs)) {
            return "TIME_LIMIT";
        }
        if (!o.ok()) {
            return "RUNTIME_ERROR";
        }
        if (expected == null) {
            return "NO_EXPECTED";
        }
        return normalize(o.stdout()).equals(normalize(expected)) ? "OK" : "WRONG";
    }

    /**
     * Bỏ khoảng trắng cuối mỗi dòng và các dòng trống ở cuối. Chấm bài nộp
     * ({@code grade}) dùng chung.
     */
    public static String normalize(String s) {
        List<String> lines = new ArrayList<>(List.of(s.replace("\r\n", "\n").split("\n", -1)));
        lines.replaceAll(String::stripTrailing);
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return String.join("\n", lines);
    }

    private void record(long userId, String lessonId, long codeSeq, String verdict) {
        try {
            ctx.db().read(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO submissions"
                        + " (user_id, lesson_id, code_seq, kind, verdict, created_at) VALUES (?, ?, ?, 'RUN', ?, ?)")) {
                    ps.setLong(1, userId);
                    ps.setString(2, lessonId);
                    ps.setLong(3, codeSeq);
                    ps.setString(4, verdict);
                    ps.setLong(5, System.currentTimeMillis());
                    return ps.executeUpdate();
                }
            });
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Không ghi được submissions", e);
        }
    }

    /**
     * Gom dòng trace thành khúc ≤ 200 dòng rồi gửi {@code TRACE_CHUNK}. Gọi trên
     * luồng chạy code.
     */
    private static final class Chunker implements Consumer<String> {
        private final long runId;
        private final Session s;
        private final List<String> buf = new ArrayList<>();
        private int index;

        Chunker(long runId, Session s) {
            this.runId = runId;
            this.s = s;
        }

        @Override
        public synchronized void accept(String line) {
            buf.add(line);
            if (buf.size() >= LINES_PER_CHUNK) {
                send(false);
            }
        }

        synchronized void finish() {
            send(true);
        }

        private void send(boolean last) {
            byte[] lines = String.join("\n", buf).getBytes(StandardCharsets.UTF_8);
            buf.clear();
            s.send(
                    Frame.of(
                            MessageType.TRACE_CHUNK,
                            new PayloadWriter()
                                    .i64(runId)
                                    .i32(index++)
                                    .bool(last)
                                    .blob(lines)
                                    .toBytes()),
                    Priority.NORMAL);
        }
    }
}
