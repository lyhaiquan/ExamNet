package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Số liệu cho trang chủ, tính từ danh sách bài ({@code LESSON_DATA}) và danh sách học viên
 * ({@code STUDENT_OVERVIEW_DATA}) đã có sẵn: không thêm lệnh mới lên server. Tách khỏi {@link
 * DashboardHome} để test được không cần JavaFX.
 */
final class DashboardStats {
    record ChapterProgress(String mon, String chuong, int done, int total) {
        String label() {
            return LessonScreen.chapterLabel(chuong);
        }

        double ratio() {
            return total == 0 ? 0 : (double) done / total;
        }
    }

    record Slice(String label, int count) {}

    private final List<JsonNode> lessons;
    private final int done;
    private final int inProgress;
    private final int hidden;
    private final int xp;
    private final List<ChapterProgress> chapters;

    private DashboardStats(List<JsonNode> lessons, int hidden) {
        this.lessons = lessons;
        this.hidden = hidden;
        int d = 0;
        int p = 0;
        int x = 0;
        Map<String, int[]> byChapter = new LinkedHashMap<>();
        for (JsonNode l : lessons) {
            String status = l.path("status").asText("NOT_STARTED");
            int[] c = byChapter.computeIfAbsent(
                    l.path("mon").asText() + "|" + l.path("chuong").asText(), k -> new int[2]);
            c[1]++;
            if ("MASTERED".equals(status)) {
                d++;
                c[0]++;
                x += l.path("score").asInt(0);
            } else if ("OPEN".equals(status)) {
                p++;
            }
        }
        this.done = d;
        this.inProgress = p;
        this.xp = x;
        List<ChapterProgress> list = new ArrayList<>();
        byChapter.forEach((key, c) -> {
            String[] k = key.split("\\|", 2);
            list.add(new ChapterProgress(k[0], k[1], c[0], c[1]));
        });
        this.chapters = List.copyOf(list);
    }

    /** {@code student}: học viên không tính bài giáo viên đang ẩn. */
    static DashboardStats of(List<JsonNode> all, boolean student) {
        List<JsonNode> sorted = new ArrayList<>();
        int hidden = 0;
        for (JsonNode l : all) {
            boolean active = l.path("active").asBoolean(true);
            if (!active) hidden++;
            if (student && !active) continue;
            sorted.add(l);
        }
        sorted.sort(ORDER);
        return new DashboardStats(List.copyOf(sorted), hidden);
    }

    private static final Comparator<JsonNode> ORDER = Comparator.comparing(
                    (JsonNode l) -> l.path("mon").asText())
            .thenComparingInt(l -> {
                int i = LessonScreen.CHAPTERS.indexOf(l.path("chuong").asText());
                return i < 0 ? Integer.MAX_VALUE : i;
            })
            .thenComparingInt(l -> LessonScreen.loaiRank(l.path("loai").asText()));

    int total() {
        return lessons.size();
    }

    int done() {
        return done;
    }

    int inProgress() {
        return inProgress;
    }

    int hidden() {
        return hidden;
    }

    int percentDone() {
        return total() == 0 ? 0 : Math.round(done * 100f / total());
    }

    /** Điểm kinh nghiệm = tổng điểm các bài đã đạt. */
    int xp() {
        return xp;
    }

    /** Mỗi cấp 500 XP, bắt đầu từ cấp 1. */
    int level() {
        return xp / 500 + 1;
    }

    double levelProgress() {
        return (xp % 500) / 500.0;
    }

    List<ChapterProgress> chapters() {
        return chapters;
    }

    /** Huy hiệu: mỗi chương làm xong hết bài. */
    List<String> badges() {
        List<String> out = new ArrayList<>();
        for (ChapterProgress c : chapters) {
            if (c.total() > 0 && c.done() == c.total()) out.add("Xong chương " + c.label());
        }
        return out;
    }

    /** Bài nên làm tiếp: bài đang làm dở đầu tiên, không có thì bài chưa làm đầu tiên. */
    Optional<JsonNode> continueLesson() {
        for (JsonNode l : lessons) {
            if ("OPEN".equals(l.path("status").asText())) return Optional.of(l);
        }
        for (JsonNode l : lessons) {
            if (!"MASTERED".equals(l.path("status").asText())) return Optional.of(l);
        }
        return Optional.empty();
    }

    /** Bài đã có kết quả (đạt hoặc đang làm), theo thứ tự chương. */
    List<JsonNode> touched(int limit) {
        return lessons.stream()
                .filter(l -> !"NOT_STARTED".equals(l.path("status").asText("NOT_STARTED")))
                .limit(limit)
                .toList();
    }

    List<Slice> bySubject() {
        Map<String, Integer> count = new LinkedHashMap<>();
        for (JsonNode l : lessons) {
            count.merge(l.path("mon").asText("khac").toUpperCase(Locale.ROOT), 1, Integer::sum);
        }
        return count.entrySet().stream()
                .map(e -> new Slice(e.getKey(), e.getValue()))
                .toList();
    }

    /** Mũi tên tăng/giảm so với 30 ngày trước; kỳ trước bằng 0 thì ghi số mới. */
    static String trend(long current, long previous) {
        if (previous <= 0) return current > 0 ? "+" + current + " trong 30 ngày" : "chưa có thay đổi";
        long pct = Math.round((current - previous) * 100.0 / previous);
        return (pct >= 0 ? "↑ " : "↓ ") + Math.abs(pct) + "% so với 30 ngày trước";
    }

    /** Tỉ lệ hoàn thành của lớp: tổng bài đạt / (số học viên × số bài). */
    static int classCompletion(JsonNode students, int lessonCount) {
        if (lessonCount <= 0 || students.size() == 0) return 0;
        long sum = 0;
        for (JsonNode s : students) sum += s.path("completed").asInt();
        return (int) Math.round(sum * 100.0 / ((long) students.size() * lessonCount));
    }

    static int online(JsonNode students) {
        int n = 0;
        for (JsonNode s : students) if (s.path("online").asBoolean()) n++;
        return n;
    }

    /** Học viên có hoạt động gần nhất trước. */
    static List<JsonNode> recent(JsonNode students, int limit) {
        List<JsonNode> list = new ArrayList<>();
        students.forEach(list::add);
        list.sort(Comparator.comparingLong((JsonNode s) -> s.path("updatedAt").asLong())
                .reversed());
        return list.subList(0, Math.min(limit, list.size()));
    }
}
