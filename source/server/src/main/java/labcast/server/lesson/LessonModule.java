package labcast.server.lesson;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.Language;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadReader;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.ProtocolException;
import labcast.common.protocol.Role;
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
    /** Danh sách id các bài tạm tắt (inactive) */
    private final Set<String> inactiveLessons = ConcurrentHashMap.newKeySet();

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

        loadInactiveLessons();

        ctx.router()
                .on(MessageType.LESSON_LIST, this::list)
                .on(MessageType.LESSON_FETCH, this::fetch)
                .on(MessageType.ADMIN_LESSON_DELETE, Set.of(Role.TEACHER), this::deleteLesson)
                .on(MessageType.ADMIN_LESSON_TOGGLE, Set.of(Role.TEACHER), this::toggleLesson)
                .on(MessageType.ADMIN_LESSON_UPLOAD, Set.of(Role.TEACHER), this::uploadLesson);
    }

    /**
     * Danh sách bài kèm tiến độ của CHÍNH tài khoản đang đăng nhập:
     * MASTERED = nộp đúng; OPEN = đã nộp nhưng chưa đạt;
     * NOT_STARTED = chưa có dòng progress (có thể đã lưu code nháp).
     */
    private void list(Ctx c, Frame f) throws SQLException {
        boolean isTeacher = c.session() != null && c.session().role() == Role.TEACHER;
        Map<String, Map<String, Object>> progress = new HashMap<>();
        ctx.db().read(dbConn -> {
            try (PreparedStatement ps =
                    dbConn.prepareStatement("SELECT lesson_id, status, score FROM progress WHERE user_id = ?")) {
                ps.setLong(1, c.session().userId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("status", rs.getString("status"));
                        int score = rs.getInt("score");
                        if (!rs.wasNull()) data.put("score", score);
                        progress.put(rs.getString("lesson_id"), data);
                    }
                }
            }
            return null;
        });
        List<Map<String, Object>> items = new ArrayList<>();
        for (Lesson l : repo.all()) {
            boolean active = !inactiveLessons.contains(l.id());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.id());
            m.put("mon", l.mon());
            m.put("chuong", l.chuong());
            m.put("ten", l.ten());
            m.put("loai", l.loai());
            m.put("active", active);
            Map<String, Object> state = progress.get(l.id());
            m.put("status", !active && !isTeacher ? "LOCKED" : (state == null ? "NOT_STARTED" : state.get("status")));
            if (state != null && state.containsKey("score")) m.put("score", state.get("score"));
            items.add(m);
        }
        c.reply(JsonPayload.frame(MessageType.LESSON_DATA, Map.of("kind", "list", "lessons", items)), Priority.NORMAL);
    }

    /**
     * {@code LESSON_FETCH(str lessonId)} → {@code LESSON_DATA JSON{kind: "lesson", id, ten, loai, de, khung,
     * khungNhin, testXem, trace, code, codeSeq, ngonNgu, loiGiai}}. {@code code}/{@code codeSeq} là code Python
     * học viên đang có trên server, để mở lại bài thì làm tiếp đúng chỗ (ngôn ngữ khác: {@code CODE_FETCH}).
     * {@code ngonNgu}: mã các ngôn ngữ được chọn; {@code loiGiai}: lời giải C/C++/Java của bài giảng giải.
     */
    private void fetch(Ctx c, Frame f) throws Exception {
        String id = f.reader().str();
        boolean isTeacher = c.session() != null && c.session().role() == Role.TEACHER;
        if (!isTeacher && inactiveLessons.contains(id)) {
            throw new ProtocolException(ErrorCode.FORBIDDEN, "Bài học này hiện chưa được mở");
        }
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
        m.put("ngonNgu", l.ngonNgu().stream().map(Language::code).toList());
        if (l.loai().equals("giang-giai")) {
            m.put("loiGiai", repo.loiGiaiKhac(id));
        }
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

    /**
     * Ghi danh sách bài vào bảng {@code lessons}. UPSERT, không xoá dòng cũ vì code_deltas trỏ tới nó. Mỗi ngôn ngữ
     * khác Python có thêm một dòng mang khoá tài liệu ({@code dsa.x#cpp}): code_deltas và checkpoints lưu code
     * theo khoá này, khoá ngoại sang {@code lessons(id)} vẫn đúng mà không phải đổi schema.
     */
    private static void syncToDb(Database db, LessonRepository repo) throws SQLException {
        db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO lessons (id, mon, chuong, ten, loai, path, content_hash) VALUES (?, ?, ?, ?, ?, ?, ?)"
                            + " ON CONFLICT(id) DO UPDATE SET mon = excluded.mon, chuong = excluded.chuong,"
                            + " ten = excluded.ten, loai = excluded.loai, path = excluded.path,"
                            + " content_hash = excluded.content_hash")) {
                for (Lesson l : repo.all()) {
                    for (Language lang : l.isSql() ? List.of(Language.PYTHON) : l.ngonNgu()) {
                        ps.setString(1, Language.docKey(l.id(), lang));
                        ps.setString(2, l.mon());
                        ps.setString(3, l.chuong());
                        ps.setString(4, l.ten());
                        ps.setString(5, l.loai());
                        ps.setString(6, l.dir().toString());
                        ps.setString(7, l.contentHash());
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
            return null;
        });
    }

    /** {@code ADMIN_LESSON_TOGGLE(str lessonId, bool active)} → {@code ADMIN_OK(str message)}. */
    private void toggleLesson(Ctx c, Frame f) throws Exception {
        PayloadReader r = f.reader();
        String lessonId = r.str();
        boolean active = r.bool();
        if (active) {
            inactiveLessons.remove(lessonId);
        } else {
            inactiveLessons.add(lessonId);
        }
        saveInactiveLessons();
        String msg = (active ? "Đã mở kích hoạt bài: " : "Đã tạm ẩn bài: ") + lessonId;
        ctx.audit().record(c.session().username(), "ADMIN_LESSON_TOGGLE", msg);
        c.reply(MessageType.ADMIN_OK, new PayloadWriter().str(msg).toBytes());
    }

    /** {@code ADMIN_LESSON_UPLOAD(str filename, blob zipBytes)} → {@code ADMIN_OK(str message)}. */
    @SuppressWarnings("unchecked")
    private void uploadLesson(Ctx c, Frame f) throws Exception {
        PayloadReader r = f.reader();
        String filename = r.str();
        byte[] zipBytes = r.blob();
        if (zipBytes.length == 0) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "File zip rỗng");
        }
        Path tempDir = Files.createTempDirectory("labcast-upload-");
        try {
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName().replace('\\', '/');
                    if (name.contains("..") || name.startsWith("/")) {
                        throw new ProtocolException(ErrorCode.BAD_FRAME, "Đường dẫn không hợp lệ trong zip: " + name);
                    }
                    Path target = tempDir.resolve(name);
                    if (entry.isDirectory()) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                }
            }

            Path lessonYaml;
            try (var stream = Files.walk(tempDir)) {
                lessonYaml = stream.filter(p -> p.getFileName().toString().equals("lesson.yaml"))
                        .findFirst()
                        .orElseThrow(() -> new ProtocolException(
                                ErrorCode.BAD_FRAME, "Không tìm thấy file lesson.yaml trong file zip"));
            }

            Path lessonDir = lessonYaml.getParent();
            Map<String, Object> y;
            try (var in = Files.newInputStream(lessonYaml)) {
                Object o = new org.yaml.snakeyaml.Yaml(new org.yaml.snakeyaml.constructor.SafeConstructor(
                                new org.yaml.snakeyaml.LoaderOptions()))
                        .load(in);
                if (!(o instanceof Map)) {
                    throw new ProtocolException(ErrorCode.BAD_FRAME, "lesson.yaml phải là một map/object");
                }
                y = (Map<String, Object>) o;
            }
            String id = String.valueOf(y.get("id"));
            String mon = String.valueOf(y.get("mon"));
            String chuong = String.valueOf(y.get("chuong"));
            if (id == null || id.isBlank() || mon == null || chuong == null) {
                throw new ProtocolException(ErrorCode.BAD_FRAME, "lesson.yaml thiếu id, mon hoặc chuong");
            }
            String[] parts = id.split("\\.");
            if (parts.length != 3 || !parts[0].equals(mon) || !parts[1].equals(chuong)) {
                throw new ProtocolException(
                        ErrorCode.BAD_FRAME,
                        "id phải có định dạng <mon>.<chuong>.<ten-bai>, ví dụ: " + mon + "." + chuong + ".bai-1");
            }

            Path contentRoot = ctx.config().content();
            Path destDir = contentRoot.resolve(parts[0]).resolve(parts[1]).resolve(parts[2]);
            Files.createDirectories(destDir);

            try (var files = Files.walk(lessonDir)) {
                for (Path src : files.toList()) {
                    Path relPath = lessonDir.relativize(src);
                    Path dst = destDir.resolve(relPath.toString());
                    if (Files.isDirectory(src)) {
                        Files.createDirectories(dst);
                    } else {
                        Files.createDirectories(dst.getParent());
                        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            String rel = contentRoot.relativize(destDir).toString().replace('\\', '/');
            Lesson l = LessonRepository.parseLesson(destDir, rel);
            repo.addOrUpdate(l);
            syncSingleLessonToDb(ctx.db(), l);

            String msg = "Tải lên thành công bài: " + l.ten() + " (" + l.id() + ")";
            ctx.audit().record(c.session().username(), "ADMIN_LESSON_UPLOAD", msg);
            c.reply(MessageType.ADMIN_OK, new PayloadWriter().str(msg).toBytes());
        } finally {
            try (var s = Files.walk(tempDir)) {
                s.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                    }
                });
            } catch (Exception ignored) {
            }
        }
    }

    /** {@code ADMIN_LESSON_DELETE(str lessonId)} → {@code ADMIN_OK(str message)}. */
    private void deleteLesson(Ctx c, Frame f) throws Exception {
        PayloadReader r = f.reader();
        String lessonId = r.str();
        if (lessonId == null || lessonId.isBlank()) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "Mã bài học không hợp lệ");
        }
        Optional<Lesson> opt = repo.get(lessonId);
        if (opt.isEmpty()) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "Không tìm thấy bài học: " + lessonId);
        }
        Lesson l = opt.get();
        Path lessonDir = l.dir();

        Path contentRoot = ctx.config().content().toAbsolutePath().normalize();
        Path targetDir = lessonDir.toAbsolutePath().normalize();
        if (!targetDir.startsWith(contentRoot) || targetDir.equals(contentRoot)) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "Đường dẫn bài học không hợp lệ hoặc nằm ngoài content");
        }

        if (Files.exists(targetDir)) {
            try (var s = Files.walk(targetDir)) {
                s.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                    }
                });
            }
        }

        repo.remove(lessonId);
        synchronized (inactiveLessons) {
            if (inactiveLessons.remove(lessonId)) {
                saveInactiveLessons();
            }
        }

        try {
            ctx.db().tx(conn -> {
                try (var ps = conn.prepareStatement("DELETE FROM lessons WHERE id = ? OR id LIKE ?")) {
                    ps.setString(1, lessonId);
                    ps.setString(2, lessonId + "#%");
                    ps.executeUpdate();
                }
                return null;
            });
        } catch (Exception ex) {
            LOG.warning("Không xoá được dòng bài học trong db: " + ex.getMessage());
        }

        String msg = "Đã xoá bài học: " + l.ten() + " (" + lessonId + ")";
        ctx.audit().record(c.session().username(), "ADMIN_LESSON_DELETE", msg);
        c.reply(MessageType.ADMIN_OK, new PayloadWriter().str(msg).toBytes());
    }

    private void loadInactiveLessons() {
        Path p = ctx.config().content().resolve(".inactive_lessons.json");
        if (Files.exists(p)) {
            try {
                byte[] bytes = Files.readAllBytes(p);
                JsonNode arr = JsonPayload.MAPPER.readTree(bytes);
                if (arr != null && arr.isArray()) {
                    for (JsonNode n : arr) {
                        inactiveLessons.add(n.asText());
                    }
                }
            } catch (Exception e) {
                LOG.warning("Không đọc được file .inactive_lessons.json: " + e.getMessage());
            }
        }
    }

    private synchronized void saveInactiveLessons() {
        Path p = ctx.config().content().resolve(".inactive_lessons.json");
        try {
            List<String> list = new ArrayList<>(inactiveLessons);
            Collections.sort(list);
            byte[] bytes = JsonPayload.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(list);
            Files.write(p, bytes);
        } catch (Exception e) {
            LOG.warning("Không ghi được file .inactive_lessons.json: " + e.getMessage());
        }
    }

    private static void syncSingleLessonToDb(Database db, Lesson l) throws SQLException {
        db.tx(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO lessons (id, mon, chuong, ten, loai, path, content_hash) VALUES (?, ?, ?, ?, ?, ?, ?)"
                            + " ON CONFLICT(id) DO UPDATE SET mon = excluded.mon, chuong = excluded.chuong,"
                            + " ten = excluded.ten, loai = excluded.loai, path = excluded.path,"
                            + " content_hash = excluded.content_hash")) {
                for (Language lang : l.isSql() ? List.of(Language.PYTHON) : l.ngonNgu()) {
                    ps.setString(1, Language.docKey(l.id(), lang));
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
