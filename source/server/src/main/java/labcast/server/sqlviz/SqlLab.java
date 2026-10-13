package labcast.server.sqlviz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import labcast.server.grade.Grader;
import labcast.server.grade.Verdict;
import labcast.server.lesson.Lesson;
import labcast.server.lesson.LessonRepository;

/**
 * Bài SQL trên server (spec §7.6): database dữ liệu mẫu của bài, trace giảng giải từ {@code dap_an.sql},
 * chạy thử câu của học viên (kết quả + trace + đúng/sai so với đáp án trên dữ liệu mẫu), chấm bài nộp trên
 * dữ liệu mẫu và các database ẩn. Mọi câu của học viên qua {@link SqlGuard} rồi chạy bằng {@link
 * SqliteEngine}; database ẩn và kết quả trên đó không bao giờ gửi cho app.
 */
public final class SqlLab {
    private static final Logger LOG = Logger.getLogger(SqlLab.class.getName());
    public static final Duration TIMEOUT = Duration.ofSeconds(2);
    /** Mã "test" của lượt chạy thử trên dữ liệu mẫu (khung chọn test của app hiện tên này). */
    public static final String SAMPLE_TEST = "du-lieu-mau";

    public record RunResult(boolean ok, String stdout, String error, List<String> trace, String verdict, long timeMs) {}

    private final LessonRepository repo;
    private final Path work;
    private final SqliteEngine engine = new SqliteEngine();
    private final Map<String, Path> samples = new ConcurrentHashMap<>();

    public SqlLab(LessonRepository repo, Path work) {
        this.repo = repo;
        this.work = work;
    }

    /** Database dữ liệu mẫu của bài, dựng một lần cho mỗi phiên bản nội dung; chỉ mở chỉ đọc. */
    synchronized Path sample(Lesson l) throws IOException, SqlRejected {
        String key = l.id() + "@" + l.contentHash();
        Path p = samples.get(key);
        if (p == null || !Files.exists(p)) {
            p = SqliteEngine.build(work, file(l, "schema.sql"), file(l, "du_lieu_mau.sql"));
            samples.put(key, p);
        }
        return p;
    }

    /** Trace giảng giải: đáp án chạy trên dữ liệu mẫu. Lỗi (lỗi của người soạn bài) thì danh sách rỗng. */
    public List<String> demoTrace(Lesson l) {
        try {
            SqlGuard.Checked c = SqlGuard.check(file(l, "dap_an.sql"), l.isDml());
            Path db = sample(l);
            if (c.kind() == SqlGuard.Kind.DML) {
                Path copy = SqliteEngine.copy(db, work);
                try {
                    return new StepPlanner(engine, db, TIMEOUT).explainDml(l.id(), c, copy);
                } finally {
                    Files.deleteIfExists(copy);
                }
            }
            return new StepPlanner(engine, db, TIMEOUT).explainSelect(l.id(), c);
        } catch (IOException | SqlRejected e) {
            LOG.log(Level.WARNING, "Đáp án bài " + l.id() + " không chạy được: " + e.getMessage());
            return List.of();
        }
    }

    /** Chạy thử câu của học viên trên dữ liệu mẫu. Câu lỗi/bị chặn thì {@code ok = false}, lời nhắn ở {@code error}. */
    public RunResult run(Lesson l, String sql) {
        long t0 = System.nanoTime();
        try {
            SqlGuard.Checked c = SqlGuard.check(sql, l.isDml());
            Path db = sample(l);
            if (c.kind() == SqlGuard.Kind.DML) {
                Path mine = SqliteEngine.copy(db, work);
                Path answer = SqliteEngine.copy(db, work);
                try {
                    List<String> trace = new StepPlanner(engine, db, TIMEOUT).explainDml(l.id(), c, mine);
                    engine.execute(
                            answer, SqlGuard.check(file(l, "dap_an.sql"), true).sql(), TIMEOUT);
                    String verdict = sameState(mine, answer) ? "OK" : "WRONG";
                    return new RunResult(true, stateText(mine, c), null, trace, verdict, ms(t0));
                } finally {
                    Files.deleteIfExists(mine);
                    Files.deleteIfExists(answer);
                }
            }
            QueryResult r = engine.query(db, c.sql(), TIMEOUT);
            List<String> trace = new StepPlanner(engine, db, TIMEOUT).explainSelect(l.id(), c);
            QueryResult expect = engine.query(
                    db, SqlGuard.check(file(l, "dap_an.sql"), false).sql(), TIMEOUT);
            String verdict = ResultComparer.same(r, expect, l.thuTu()) ? "OK" : "WRONG";
            return new RunResult(true, TextTable.render(r), null, trace, verdict, ms(t0));
        } catch (SqlRejected e) {
            return new RunResult(false, "", e.getMessage(), List.of(), verdict(e), ms(t0));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Bài " + l.id() + " lỗi dữ liệu", e);
            return new RunResult(
                    false,
                    "",
                    "Server lỗi khi dựng dữ liệu của bài: " + e.getMessage(),
                    List.of(),
                    "RUNTIME_ERROR",
                    ms(t0));
        }
    }

    /**
     * Chấm bài nộp: dữ liệu mẫu (test xem được) rồi từng database ẩn ({@code hiddenData}: script INSERT,
     * chạy sau {@code schema.sql}). Dừng ở database đầu tiên sai.
     */
    public Grader.Result grade(Lesson l, String sql, List<String> hiddenData) throws IOException {
        SqlGuard.Checked c;
        try {
            c = SqlGuard.check(sql, l.isDml());
        } catch (SqlRejected e) {
            return new Grader.Result(Verdict.RUNTIME_ERROR, e.getMessage(), List.of(), SAMPLE_TEST);
        }
        String answer;
        try {
            answer = SqlGuard.check(file(l, "dap_an.sql"), l.isDml()).sql();
        } catch (SqlRejected e) {
            throw new IOException("Đáp án bài " + l.id() + " không hợp lệ: " + e.getMessage());
        }
        List<Grader.TestResult> tests = new ArrayList<>();
        for (int i = 0; i <= hiddenData.size(); i++) {
            boolean hidden = i > 0;
            String id = hidden ? "an-" + i : SAMPLE_TEST;
            long t0 = System.nanoTime();
            Path db = null;
            try {
                db = hidden ? SqliteEngine.build(work, file(l, "schema.sql"), hiddenData.get(i - 1)) : sample(l);
                boolean same;
                if (c.kind() == SqlGuard.Kind.DML) {
                    Path mine = SqliteEngine.copy(db, work);
                    Path ans = SqliteEngine.copy(db, work);
                    try {
                        engine.execute(mine, c.sql(), TIMEOUT);
                        engine.execute(ans, answer, TIMEOUT);
                        same = sameState(mine, ans);
                    } finally {
                        Files.deleteIfExists(mine);
                        Files.deleteIfExists(ans);
                    }
                } else {
                    QueryResult mine = engine.query(db, c.sql(), TIMEOUT);
                    QueryResult expect = engine.query(db, answer, TIMEOUT);
                    same = ResultComparer.same(mine, expect, l.thuTu());
                }
                Verdict v = same ? Verdict.OK : Verdict.WRONG;
                tests.add(new Grader.TestResult(id, hidden, v, ms(t0), null));
                if (!same) {
                    return new Grader.Result(v, wrongMessage(hidden, i), tests, id);
                }
            } catch (SqlRejected e) {
                Verdict v = Verdict.valueOf(verdict(e));
                tests.add(new Grader.TestResult(id, hidden, v, ms(t0), hidden ? null : e.getMessage()));
                return new Grader.Result(
                        v, (hidden ? "Lỗi trên bộ dữ liệu ẩn " + i + ": " : "") + e.getMessage(), tests, id);
            } finally {
                if (hidden && db != null) {
                    Files.deleteIfExists(db);
                }
            }
        }
        String msg = hiddenData.isEmpty()
                ? "Đúng trên dữ liệu mẫu."
                : "Đúng trên dữ liệu mẫu và cả " + hiddenData.size() + " bộ dữ liệu ẩn.";
        return new Grader.Result(Verdict.OK, msg, tests, null);
    }

    private static String wrongMessage(boolean hidden, int i) {
        return hidden
                ? "Đúng với dữ liệu mẫu nhưng sai trên bộ dữ liệu ẩn " + i
                        + ". Bộ ẩn có NULL, dòng trùng và bảng rỗng: kiểm lại cách xử lý NULL (IS NULL, COUNT(cột)),"
                        + " DISTINCT, và câu có còn đúng khi bảng không có dòng nào."
                : "Kết quả trên dữ liệu mẫu khác đáp án (số cột, số dòng hoặc giá trị).";
    }

    /** Hai database có cùng nội dung mọi bảng (so như đa tập, bỏ qua rowid). */
    private boolean sameState(Path a, Path b) throws SqlRejected {
        QueryResult tables = engine.query(
                a,
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name",
                TIMEOUT);
        for (List<Object> t : tables.rows()) {
            String sql = "SELECT * FROM \"" + t.get(0) + "\"";
            if (!ResultComparer.same(engine.query(a, sql, TIMEOUT), engine.query(b, sql, TIMEOUT), false)) {
                return false;
            }
        }
        return true;
    }

    /** Bảng bị câu DML đụng tới, sau khi chạy. */
    private String stateText(Path db, SqlGuard.Checked c) throws SqlRejected {
        String sql = c.sql().strip();
        String[] words = sql.split("\\s+");
        String table = null;
        for (int i = 0; i + 1 < words.length && table == null; i++) {
            String w = words[i].toUpperCase();
            if (w.equals("UPDATE") || w.equals("INTO") || w.equals("FROM")) {
                table = words[i + 1].replaceAll("[^A-Za-z0-9_]", "");
            }
        }
        if (table == null || table.isEmpty()) {
            return "";
        }
        return "Bảng " + table + " sau khi chạy:\n"
                + TextTable.render(engine.query(db, "SELECT * FROM " + table, TIMEOUT));
    }

    private String file(Lesson l, String name) throws IOException {
        return repo.sqlFile(l.id(), name).orElseThrow(() -> new IOException("Bài " + l.id() + " thiếu " + name));
    }

    private static String verdict(SqlRejected e) {
        return e.getMessage() != null && e.getMessage().startsWith("Câu lệnh chạy quá")
                ? "TIME_LIMIT"
                : "RUNTIME_ERROR";
    }

    private static long ms(long t0) {
        return (System.nanoTime() - t0) / 1_000_000;
    }
}
