package labcast.server.practice;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.ProtocolException;
import labcast.server.ServerContext;
import labcast.server.ServerModule;
import labcast.server.grade.Grader;
import labcast.server.grade.Verdict;
import labcast.server.journal.CodeStore;
import labcast.server.lesson.Lesson;
import labcast.server.lesson.LessonPreparer;
import labcast.server.lesson.LessonRepository;
import labcast.server.net.Ctx;
import labcast.server.net.Priority;
import labcast.server.run.RunService;
import labcast.server.run.Sandbox;
import labcast.server.run.ScriptRunner;
import labcast.server.session.Session;

/**
 * Nộp bài (spec §8.2, task 3.8). Chủ: Người 3.
 *
 * <p>{@code SUBMIT_REQ(str lessonId, i64 codeSeq)} → {@code SUBMIT_RESULT JSON{verdict, message, score,
 * cooldownMs, mastered, unlocked, failedTest, tests:[{id, hidden, verdict, timeMs, error?}]}}. Test ẩn chỉ
 * có id, verdict, thời gian.
 *
 * <ul>
 *   <li>Đang phải chờ sau lần nộp sai → {@code ERROR(COOLDOWN, retryAfterMs)}.
 *   <li>Lượt nộp trước của chính bài đó chưa chấm xong → {@code ERROR(RATE_LIMITED)}: gửi dồn 10 lượt cùng
 *       lúc không lọt qua được kiểm thời gian chờ.
 *   <li>Lỗi hạ tầng (Docker, Python, bài hỏng) → {@code ERROR(SANDBOX_DOWN)}, KHÔNG tính là nộp sai.
 * </ul>
 *
 * <p>Chưa gọi {@code CodeStore.checkpoint} (task 1.7) và {@code ClassEvents.publish} (task 2.12) vì hai phần đó
 * chưa có; kết quả đã nằm trong bảng {@code submissions} để hai task đó đọc lại.
 */
public final class SubmitModule implements ServerModule {
    private static final Logger LOG = Logger.getLogger(SubmitModule.class.getName());

    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private ServerContext ctx;
    private ProgressService progress;
    private LessonPreparer preparer;
    private Grader grader;

    @Override
    public void install(ServerContext ctx) {
        this.ctx = ctx;
        this.progress = new ProgressService(ctx.db());
        ScriptRunner scripts = ctx.require(ScriptRunner.class);
        Sandbox sandbox = ctx.require(RunService.class).sandbox();
        this.preparer = new LessonPreparer(scripts, cacheDir(ctx));
        this.grader = new Grader(scripts, sandbox);
        ctx.register(ProgressService.class, progress);
        ctx.register(LessonPreparer.class, preparer);
        ctx.router().on(MessageType.SUBMIT_REQ, this::submit);
    }

    /** {@code .labcast-cache} cạnh file database; database trong bộ nhớ (test) thì thư mục tạm. */
    static Path cacheDir(ServerContext ctx) {
        String db = ctx.config().db();
        try {
            if (db == null || db.startsWith(":memory:")) {
                return Files.createTempDirectory("labcast-cache");
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        Path parent = Path.of(db).toAbsolutePath().getParent();
        return (parent == null ? Path.of(".") : parent).resolve(".labcast-cache");
    }

    private void submit(Ctx c, Frame f) throws Exception {
        PayloadReader r = f.reader();
        String lessonId = r.str();
        long codeSeq = r.i64();
        Session s = c.session();
        LessonRepository repo = ctx.require(LessonRepository.class);
        Lesson l = repo.get(lessonId)
                .orElseThrow(() -> new ProtocolException(ErrorCode.NOT_FOUND, "Không có bài " + lessonId));

        // Giữ chỗ TRƯỚC rồi mới đọc thời gian chờ: đọc trước thì một lượt có thể thấy "không phải chờ" trong
        // lúc lượt trước còn đang chấm, rồi lọt vào ngay khi lượt trước vừa nhả chỗ (trước khi kịp ghi chờ).
        String key = s.userId() + "|" + lessonId;
        if (!inFlight.add(key)) {
            c.error(ErrorCode.RATE_LIMITED, "Lượt nộp trước của bài này đang được chấm", 2_000);
            return;
        }
        try {
            long left = progress.cooldownLeft(s.userId(), lessonId, System.currentTimeMillis());
            if (left > 0) {
                inFlight.remove(key);
                c.error(ErrorCode.COOLDOWN, "Chờ " + (left + 999) / 1000 + " s nữa mới được nộp lại", (int) left);
                return;
            }
            // Chấm đúng phiên bản code học viên đang thấy: delta có thể còn trên đường tới, chờ tối đa 1 s.
            CodeStore store = ctx.require(CodeStore.class);
            CodeStore.Snapshot snap = store.current(s.userId(), lessonId);
            for (int i = 0; i < 20 && snap.seq() < codeSeq; i++) {
                Thread.sleep(50);
                snap = store.current(s.userId(), lessonId);
            }
            String code = snap.code();
            long seq = snap.seq();
            ctx.require(RunService.class)
                    .submit(() -> {
                        LessonPreparer.Prepared prep = preparer.prepare(l);
                        return grader.grade(l, code, tests(repo, l, prep), prep.tb());
                    })
                    .whenComplete((res, err) -> {
                        try {
                            if (err != null) {
                                Throwable cause = err instanceof CompletionException && err.getCause() != null
                                        ? err.getCause()
                                        : err;
                                LOG.log(Level.WARNING, "Không chấm được bài " + lessonId, cause);
                                s.send(
                                        new ErrorMessage(
                                                        ErrorCode.SANDBOX_DOWN,
                                                        "Server chưa chấm được bài (không tính lượt nộp): "
                                                                + cause.getMessage(),
                                                        0)
                                                .toFrame(),
                                        Priority.NORMAL);
                                return;
                            }
                            reply(s, l, seq, res);
                        } finally {
                            inFlight.remove(key);
                        }
                    });
        } catch (Exception e) {
            inFlight.remove(key);
            throw e;
        }
    }

    /** Test cố định (xem được nếu nằm trong {@code test_xem}, còn lại ẩn) rồi test ẩn sinh từ {@code sinh_test.py}. */
    static List<Grader.TestCase> tests(LessonRepository repo, Lesson l, LessonPreparer.Prepared prep) {
        List<Grader.TestCase> out = new ArrayList<>();
        for (String id : repo.fixedTestIds(l.id())) {
            out.add(new Grader.TestCase(
                    id,
                    repo.testInput(l.id(), id).orElse(""),
                    repo.testOutput(l.id(), id).orElse(""),
                    !l.testXem().contains(id)));
        }
        for (LessonPreparer.HiddenTest t : prep.tests()) {
            out.add(new Grader.TestCase(t.id(), t.input(), t.output(), true));
        }
        return out;
    }

    private void reply(Session s, Lesson l, long seq, Grader.Result res) {
        ProgressService.SubmitOutcome out;
        try {
            out = progress.record(s.userId(), l, seq, res, System.currentTimeMillis());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Không ghi được tiến độ", e);
            s.send(new ErrorMessage(ErrorCode.INTERNAL, "Không ghi được kết quả nộp", 0).toFrame(), Priority.NORMAL);
            return;
        }
        LessonRepository repo = ctx.require(LessonRepository.class);
        ObjectNode n = JsonPayload.MAPPER.createObjectNode();
        n.put("lessonId", l.id());
        n.put("verdict", res.verdict().name());
        n.put("message", res.message());
        n.put("score", out.score());
        n.put("cooldownMs", out.cooldownMs());
        n.put("mastered", out.mastered());
        n.put("failedTest", res.failedTest());
        ArrayNode unlocked = n.putArray("unlocked");
        if (res.verdict() == Verdict.OK) {
            ProgressService.unlocked(l, id -> repo.get(id).isPresent()).forEach(unlocked::add);
        }
        ArrayNode tests = n.putArray("tests");
        for (Grader.TestResult t : res.tests()) {
            ObjectNode tn = tests.addObject()
                    .put("id", t.id())
                    .put("hidden", t.hidden())
                    .put("verdict", t.verdict().name())
                    .put("timeMs", t.timeMs());
            if (!t.hidden() && t.error() != null) {
                tn.put("error", t.error());
            }
        }
        s.send(JsonPayload.frame(MessageType.SUBMIT_RESULT, n), Priority.NORMAL);
    }
}
