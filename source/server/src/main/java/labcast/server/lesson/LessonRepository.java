package labcast.server.lesson;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;
import labcast.common.protocol.Language;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Nạp các bài trong {@code content/<môn>/<chương>/<bài>/} (spec §7.1). Bài lỗi thì bỏ qua và ghi vào
 * {@link #errors()}, không làm hỏng cả server — nội dung là dữ liệu, không phải code (spec §1 R4).
 */
public final class LessonRepository {
    private static final Logger LOG = Logger.getLogger(LessonRepository.class.getName());
    private static final Set<String> MON = Set.of("dsa", "sql");
    private static final Set<String> LOAI = Set.of("giang-giai", "bai-tap", "luyen-tap");

    private final Map<String, Lesson> lessons;
    private final List<String> errors;

    private LessonRepository(Map<String, Lesson> lessons, List<String> errors) {
        this.lessons = lessons;
        this.errors = errors;
    }

    public static LessonRepository load(Path contentRoot) {
        Map<String, Lesson> out = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        if (!Files.isDirectory(contentRoot)) {
            errors.add("Không có thư mục nội dung " + contentRoot.toAbsolutePath());
            return new LessonRepository(out, errors);
        }
        for (Path dir : lessonDirs(contentRoot)) {
            String rel = contentRoot.relativize(dir).toString().replace('\\', '/');
            try {
                Lesson l = parse(dir, rel);
                if (out.putIfAbsent(l.id(), l) != null) {
                    errors.add(rel + ": trùng id " + l.id());
                }
            } catch (IOException | RuntimeException e) {
                errors.add(rel + ": " + e.getMessage());
            }
        }
        errors.forEach(e -> LOG.warning("Bài lỗi — " + e));
        return new LessonRepository(out, errors);
    }

    /** Chỉ các bài của một môn: server của lớp DSA không đưa ra bài SQL và ngược lại. Giữ nguyên danh sách lỗi. */
    public LessonRepository chiMon(String mon) {
        Map<String, Lesson> out = new LinkedHashMap<>();
        lessons.forEach((id, l) -> {
            if (l.mon().equals(mon)) {
                out.put(id, l);
            }
        });
        return new LessonRepository(out, errors);
    }

    public synchronized Optional<Lesson> get(String id) {
        return Optional.ofNullable(lessons.get(id));
    }

    public synchronized List<Lesson> all() {
        return List.copyOf(lessons.values());
    }

    public synchronized void addOrUpdate(Lesson l) {
        lessons.put(l.id(), l);
    }

    public synchronized boolean remove(String id) {
        return lessons.remove(id) != null;
    }

    public List<String> errors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Code ban đầu của một khoá tài liệu: {@code khung.py} (bài SQL: {@code khung.sql}); khoá theo ngôn ngữ
     * ({@code dsa.x#cpp}) đọc {@code khung.cpp}, thiếu thì khung mặc định của ngôn ngữ. Bài không có khung, hay
     * ngôn ngữ lạ, thì chuỗi rỗng.
     */
    public String khung(String docKey) {
        Language lang = Language.ofDocKey(docKey);
        return get(Language.lessonOf(docKey))
                .map(l -> {
                    if (l.isSql()) {
                        return readOr(l.dir().resolve("khung.sql"), "SELECT \n");
                    }
                    if (lang == null) {
                        return "";
                    }
                    if (lang == Language.PYTHON) {
                        return readOr(l.dir().resolve("khung.py"), "");
                    }
                    return readOr(l.dir().resolve("khung." + lang.extension()), lang.starter());
                })
                .orElse("");
    }

    /**
     * Lời giải mẫu bằng C/C++/Java ({@code loi_giai.c}, {@code loi_giai.cpp}, {@code loi_giai.java}) để học viên
     * đọc ở bài giảng giải; chỉ các file người soạn bài thêm vào. {@code loi_giai.py} không đưa ra: bài nào cũng
     * có (để sinh test), đưa ra là lộ đáp án ở bài vừa xem vừa code.
     */
    public Map<String, String> loiGiaiKhac(String id) {
        Map<String, String> out = new LinkedHashMap<>();
        get(id).ifPresent(l -> {
            for (Language lang : l.ngonNgu()) {
                Path f = l.dir().resolve("loi_giai." + lang.extension());
                if (lang != Language.PYTHON && Files.exists(f)) {
                    out.put(lang.code(), readOr(f, ""));
                }
            }
        });
        return out;
    }

    /** Một file của bài SQL ({@code schema.sql}, {@code du_lieu_mau.sql}, {@code dap_an.sql}), UTF-8. */
    public Optional<String> sqlFile(String id, String name) {
        return get(id).map(l -> l.dir().resolve(name)).filter(Files::exists).map(p -> readOr(p, ""));
    }

    public String de(String id) {
        return get(id).map(l -> readOr(l.dir().resolve("de.md"), "")).orElse("");
    }

    public Optional<String> loiGiai(String id) {
        return get(id).map(l -> l.dir().resolve("loi_giai.py"))
                .filter(Files::exists)
                .map(p -> readOr(p, ""));
    }

    public Optional<String> demoInput(String id) {
        return get(id).map(l -> l.dir().resolve("demo.in"))
                .filter(Files::exists)
                .map(p -> readOr(p, ""));
    }

    /** Mã mọi test cố định của bài ({@code tests/*.in} có {@code .out} đi kèm), sắp theo tên. */
    public List<String> fixedTestIds(String id) {
        return get(id).map(l -> {
                    try (var files = Files.list(l.dir().resolve("tests"))) {
                        return files.map(p -> p.getFileName().toString())
                                .filter(n -> n.endsWith(".in"))
                                .map(n -> n.substring(0, n.length() - 3))
                                .filter(t ->
                                        Files.exists(l.dir().resolve("tests").resolve(t + ".out")))
                                .sorted()
                                .toList();
                    } catch (IOException e) {
                        return List.<String>of();
                    }
                })
                .orElse(List.of());
    }

    /** Input của test cố định {@code tests/<testId>.in}. */
    public Optional<String> testInput(String id, String testId) {
        return testFile(id, testId, ".in");
    }

    public Optional<String> testOutput(String id, String testId) {
        return testFile(id, testId, ".out");
    }

    private Optional<String> testFile(String id, String testId, String ext) {
        if (!testId.matches("[a-z0-9-]+")) { // không cho "../" lọt vào đường dẫn
            return Optional.empty();
        }
        return get(id).map(l -> l.dir().resolve("tests").resolve(testId + ext))
                .filter(Files::exists)
                .map(p -> readOr(p, ""));
    }

    /** Thư mục ở độ sâu 3 (môn/chương/bài), bỏ thư mục bắt đầu bằng "_" hay ".". */
    private static List<Path> lessonDirs(Path root) {
        try (Stream<Path> s = Files.walk(root, 3)) {
            return s.filter(Files::isDirectory)
                    .filter(p -> root.relativize(p).getNameCount() == 3)
                    .filter(p -> {
                        for (Path part : root.relativize(p)) {
                            String n = part.toString();
                            if (n.startsWith("_") || n.startsWith(".")) {
                                return false;
                            }
                        }
                        return true;
                    })
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Lesson parseLesson(Path dir, String rel) throws IOException {
        return parse(dir, rel);
    }

    @SuppressWarnings("unchecked")
    private static Lesson parse(Path dir, String rel) throws IOException {
        Path yaml = dir.resolve("lesson.yaml");
        if (!Files.exists(yaml)) {
            throw new IOException("thiếu lesson.yaml");
        }
        Map<String, Object> y;
        try (InputStream in = Files.newInputStream(yaml)) {
            Object o = new Yaml(new SafeConstructor(new LoaderOptions())).load(in);
            if (!(o instanceof Map)) {
                throw new IOException("lesson.yaml phải là một object");
            }
            y = (Map<String, Object>) o;
        } catch (RuntimeException e) {
            throw new IOException("lesson.yaml không đọc được: " + e.getMessage());
        }
        String id = str(y, "id");
        String expected = rel.replace('/', '.');
        if (!id.equals(expected)) {
            throw new IOException("id phải là " + expected + " (theo đường dẫn), đang là " + id);
        }
        String mon = str(y, "mon");
        if (!MON.contains(mon)) {
            throw new IOException("mon phải là dsa hoặc sql");
        }
        String loai = str(y, "loai");
        if (!LOAI.contains(loai)) {
            throw new IOException("loai phải là một trong " + LOAI);
        }
        List<ViewSpec> views = new ArrayList<>();
        for (Object o : list(y, "khung_nhin")) {
            if (o instanceof Map<?, ?> m) {
                views.add(new ViewSpec(String.valueOf(m.get("bien")), String.valueOf(m.get("kieu"))));
            }
        }
        if (!loai.equals("giang-giai") && views.stream().anyMatch(v -> v.kieu().equals("code-bien"))) {
            // trace lời giải mẫu của bộ vẽ chạy từng dòng chứa nguyên code lời giải
            throw new IOException("code-bien hiện nguyên code lời giải trong animation mẫu: chỉ dùng ở bài giang-giai");
        }
        if (mon.equals("sql") && views.isEmpty()) {
            views.add(new ViewSpec("sql", "bang-sql")); // bài SQL nào cũng vẽ bằng bảng
        }
        String loaiCau =
                y.get("loai_cau") == null ? "select" : y.get("loai_cau").toString();
        if (!loaiCau.equals("select") && !loaiCau.equals("dml")) {
            throw new IOException("loai_cau phải là select hoặc dml");
        }
        if (mon.equals("sql")) {
            for (String f : List.of("schema.sql", "du_lieu_mau.sql", "dap_an.sql")) {
                if (!Files.exists(dir.resolve(f))) {
                    throw new IOException("bài SQL thiếu " + f);
                }
            }
        }
        int ms = 1000;
        if (y.get("gioi_han") instanceof Map<?, ?> g && g.get("thoi_gian_ms") instanceof Number n) {
            ms = n.intValue();
        }
        List<Language> ngonNgu = Lesson.ngonNguMacDinh(mon);
        if (y.containsKey("ngon_ngu")) {
            List<Language> ds = new ArrayList<>();
            for (String m : strings(y, "ngon_ngu")) {
                Language l = Language.of(m);
                if (l == null) {
                    throw new IOException("ngon_ngu chỉ nhận python, c, cpp, java; có " + m);
                }
                ds.add(l);
            }
            if (mon.equals("sql") && !ds.isEmpty()) {
                throw new IOException("bài SQL không chọn ngôn ngữ (ngon_ngu)");
            }
            ngonNgu = List.copyOf(ds);
        }
        Map<String, List<String>> camTheoNgonNgu = new LinkedHashMap<>();
        if (y.get("cam_theo_ngon_ngu") instanceof Map<?, ?> cam) {
            for (var e : cam.entrySet()) {
                String m = String.valueOf(e.getKey());
                if (Language.of(m) == null || Language.of(m) == Language.PYTHON) {
                    throw new IOException("cam_theo_ngon_ngu chỉ cho c, cpp, java; có " + m);
                }
                List<String> ten = e.getValue() instanceof List<?> l
                        ? l.stream().map(String::valueOf).toList()
                        : List.of();
                camTheoNgonNgu.put(Language.of(m).code(), ten);
            }
        }
        return new Lesson(
                id,
                mon,
                str(y, "chuong"),
                str(y, "ten"),
                loai,
                strings(y, "bac"),
                List.copyOf(views),
                strings(y, "cam_dung"),
                ms,
                strings(y, "test_xem"),
                strings(y, "mo_khoa"),
                dir,
                hash(dir),
                strings(y, "cam_import"),
                strings(y, "cam_cu_phap"),
                y.get("so_test_an") instanceof Number so ? so.intValue() : 0,
                loaiCau,
                "co".equals(String.valueOf(y.get("thu_tu"))),
                ngonNgu,
                Map.copyOf(camTheoNgonNgu));
    }

    private static String str(Map<String, Object> y, String k) throws IOException {
        Object v = y.get(k);
        if (v == null || v.toString().isBlank()) {
            throw new IOException("thiếu trường " + k);
        }
        return v.toString();
    }

    private static List<?> list(Map<String, Object> y, String k) {
        return y.get(k) instanceof List<?> l ? l : List.of();
    }

    private static List<String> strings(Map<String, Object> y, String k) {
        return list(y, k).stream().map(String::valueOf).toList();
    }

    /** SHA-256 của mọi file trong thư mục bài, sắp theo tên: đổi nội dung thì đổi hash. */
    static String hash(Path dir) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (Path p : s.filter(Files::isRegularFile).sorted().toList()) {
                md.update(dir.relativize(p).toString().replace('\\', '/').getBytes(StandardCharsets.UTF_8));
                md.update(Files.readAllBytes(p));
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String readOr(Path p, String fallback) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return fallback;
        }
    }
}
