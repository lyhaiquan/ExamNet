package labcast.server.lesson;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.ProtocolException;
import labcast.server.ServerContext;
import labcast.server.ServerModule;
import labcast.server.journal.CodeStore;
import labcast.server.journal.StarterCode;
import labcast.server.net.Ctx;
import labcast.server.net.Priority;
import labcast.server.run.Job;
import labcast.server.run.Outcome;
import labcast.server.run.RunService;
import labcast.server.sqlviz.SqlLab;
import labcast.server.store.Database;

/** Danh sách bài, nội dung một bài và trace giảng giải. Chủ: Người 3. */
public final class LessonModule implements ServerModule {
    private static final Logger LOG = Logger.getLogger(LessonModule.class.getName());

    private ServerContext ctx;
    private LessonRepository repo;
    /** Trace giảng giải: chạy lời giải mẫu với demo.in lần đầu được hỏi, rồi giữ trong bộ nhớ. */
    private final Map<String, CompletableFuture<List<String>>> demoTraces = new ConcurrentHashMap<>();

    @Override
    public void install(ServerContext ctx) {
        this.ctx = ctx;
        LessonRepository all = LessonRepository.load(ctx.config().content());
        String mon = ctx.config().mon();
        this.repo = mon == null ? all : all.chiMon(mon);
        LOG.info("Đã nạp " + repo.all().size() + " bài" + (mon == null ? "" : " môn " + mon.toUpperCase()) + ", "
                + repo.errors().size() + " bài lỗi");
        try {
            syncToDb(ctx.db(), repo);
        } catch (SQLException e) {
            throw new IllegalStateException("Không ghi được bảng lessons", e);
        }
        ctx.register(LessonRepository.class, repo);
        ctx.register(StarterCode.class, repo::khung);
        try {
            // Database của bài SQL (dữ liệu mẫu, bản sao từng lượt chạy) nằm ở thư mục tạm riêng
            ctx.register(SqlLab.class, new SqlLab(repo, Files.createTempDirectory("labcast-sql")));
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được thư mục tạm cho bài SQL", e);
        }
        ctx.register(
                DemoTraces.class,
                id -> repo.get(id).map(this::demoTrace).orElseGet(() -> CompletableFuture.completedFuture(List.of())));

        ctx.router().on(MessageType.LESSON_LIST, this::list).on(MessageType.LESSON_FETCH, this::fetch);
    }

    /** {@code LESSON_LIST} → {@code LESSON_DATA JSON{kind: "list", lessons:[{id, mon, chuong, ten, loai}]}}. */
    private void list(Ctx c, labcast.common.protocol.Frame f) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Lesson l : repo.all()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.id());
            m.put("mon", l.mon());
            m.put("chuong", l.chuong());
            m.put("ten", l.ten());
            m.put("loai", l.loai());
            items.add(m);
        }
        c.reply(JsonPayload.frame(MessageType.LESSON_DATA, Map.of("kind", "list", "lessons", items)), Priority.NORMAL);
    }

    /**
     * {@code LESSON_FETCH(str lessonId)} → {@code LESSON_DATA JSON{kind: "lesson", id, ten, loai, de, khung,
     * khungNhin, testXem, trace, code, codeSeq}}. {@code code}/{@code codeSeq} là code học viên đang
     * có trên server, để mở lại bài thì làm tiếp đúng chỗ.
     */
    private void fetch(Ctx c, labcast.common.protocol.Frame f) throws Exception {
        String id = f.reader().str();
        Lesson l = repo.get(id).orElseThrow(() -> new ProtocolException(ErrorCode.NOT_FOUND, "Không có bài " + id));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kind", "lesson");
        m.put("id", l.id());
        m.put("ten", l.ten());
        m.put("loai", l.loai());
        m.put("mon", l.mon());
        m.put("de", repo.de(id));
        m.put("khung", repo.khung(id));
        m.put("khungNhin", l.khungNhin());
        List<Map<String, String>> tests = new ArrayList<>();
        for (String t : l.testXem()) {
            tests.add(Map.of("id", t, "input", repo.testInput(id, t).orElse("")));
        }
        if (l.isSql()) {
            // Bài SQL chạy trên dữ liệu mẫu: một "test" duy nhất, dữ liệu đã có trong đề
            tests.add(Map.of("id", SqlLab.SAMPLE_TEST, "input", ""));
        }
        m.put("testXem", tests);
        var store = ctx.find(CodeStore.class);
        if (store.isPresent()) {
            CodeStore.Snapshot s = store.get().current(c.session().userId(), id);
            m.put("code", s.code());
            m.put("codeSeq", s.seq());
        } else {
            m.put("code", repo.khung(id));
            m.put("codeSeq", 0);
        }
        demoTrace(l).whenComplete((lines, err) -> {
            m.put("trace", err == null ? lines : List.of());
            c.reply(JsonPayload.frame(MessageType.LESSON_DATA, m), Priority.NORMAL);
        });
    }

    private CompletableFuture<List<String>> demoTrace(Lesson l) {
        if (!l.loai().equals("giang-giai")) {
            return CompletableFuture.completedFuture(List.of());
        }
        if (l.isSql()) {
            // Đáp án chạy trên dữ liệu mẫu, tách bước: nhanh (vài chục ms) nên làm ngay, vẫn giữ đệm như bài DSA
            synchronized (demoTraces) {
                CompletableFuture<List<String>> cached = demoTraces.get(l.id());
                if (cached != null && !cached.join().isEmpty()) {
                    return cached;
                }
                List<String> lines =
                        ctx.find(SqlLab.class).map(lab -> lab.demoTrace(l)).orElse(List.of());
                CompletableFuture<List<String>> f = CompletableFuture.completedFuture(lines);
                demoTraces.put(l.id(), f);
                return f;
            }
        }
        var rs = ctx.find(RunService.class);
        var code = repo.loiGiai(l.id());
        var input = repo.demoInput(l.id());
        if (rs.isEmpty() || code.isEmpty() || input.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        synchronized (demoTraces) {
            CompletableFuture<List<String>> cached = demoTraces.get(l.id());
            // Lần trước chạy lỗi (Docker chưa bật…) thì kết quả là danh sách rỗng: chạy lại, không giữ lỗi mãi
            if (cached != null && !(cached.isDone() && cached.join().isEmpty())) {
                return cached;
            }
            List<String> lines = new ArrayList<>();
            Job job = new Job(code.get(), input.get(), true, Math.max(5_000, l.thoiGianMs()), l.id(), views(l));
            // Việc ưu tiên, không phải chạy thử: chạy thử của "người 0" thì lời giải mẫu các bài thay nhau mất
            RunService runs = rs.get();
            CompletableFuture<List<String>> f = runs.submit(() -> runs.sandbox().run(job, lines::add))
                    .handle((Outcome o, Throwable err) -> {
                        if (err != null || !o.ok()) {
                            LOG.warning("Lời giải mẫu " + l.id() + " chạy lỗi: "
                                    + (err != null ? err.getMessage() : o.error()));
                            return List.<String>of();
                        }
                        return List.copyOf(lines);
                    });
            demoTraces.put(l.id(), f);
            return f;
        }
    }

    /** {@code khung_nhin} của bài dưới dạng ghi vào sự kiện {@code hdr} của trace. */
    public static List<Map<String, String>> views(Lesson l) {
        List<Map<String, String>> out = new ArrayList<>();
        for (ViewSpec v : l.khungNhin()) {
            out.add(Map.of("v", v.bien(), "kieu", v.kieu()));
        }
        return out;
    }

    /** Ghi danh sách bài vào bảng {@code lessons}. UPSERT, không xoá dòng cũ vì code_deltas trỏ tới nó. */
    private static void syncToDb(Database db, LessonRepository repo) throws SQLException {
        db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO lessons (id, mon, chuong, ten, loai, path, content_hash) VALUES (?, ?, ?, ?, ?, ?, ?)"
                            + " ON CONFLICT(id) DO UPDATE SET mon = excluded.mon, chuong = excluded.chuong,"
                            + " ten = excluded.ten, loai = excluded.loai, path = excluded.path,"
                            + " content_hash = excluded.content_hash")) {
                for (Lesson l : repo.all()) {
                    ps.setString(1, l.id());
                    ps.setString(2, l.mon());
                    ps.setString(3, l.chuong());
                    ps.setString(4, l.ten());
                    ps.setString(5, l.loai());
                    ps.setString(6, l.dir().toString());
                    ps.setString(7, l.contentHash());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return null;
        });
    }
}
