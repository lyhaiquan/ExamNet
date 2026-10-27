package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Lọc và sắp bài cho {@link ExerciseCatalogPane}; thuần Java để test không cần JavaFX. */
final class ExerciseCatalog {
    private ExerciseCatalog() {}

    /** Bộ lọc của bảng; {@code subject} rỗng là mọi môn, {@code chapter} null là mọi chương. */
    record Query(String subject, String chapter, Set<String> kinds, String status, String text) {}

    static final Comparator<JsonNode> ORDER = Comparator.comparing(
                    (JsonNode l) -> l.path("mon").asText())
            .thenComparingInt(l -> {
                int i = LessonScreen.CHAPTERS.indexOf(l.path("chuong").asText());
                return i < 0 ? Integer.MAX_VALUE : i;
            })
            .thenComparingInt(l -> LessonScreen.loaiRank(l.path("loai").asText()))
            .thenComparing(l -> l.path("ten").asText());

    /** Các môn có bài, viết hoa, theo thứ tự xuất hiện. */
    static List<String> subjects(List<JsonNode> all) {
        return all.stream()
                .map(l -> l.path("mon").asText("dsa").toUpperCase(Locale.ROOT))
                .distinct()
                .sorted()
                .toList();
    }

    static String normalize(String text) {
        String raw = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD);
        return raw.replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .strip();
    }

    static List<JsonNode> select(List<JsonNode> all, Query q, boolean student) {
        String text = normalize(q.text());
        List<JsonNode> out = new ArrayList<>();
        for (JsonNode l : all) {
            boolean active = l.path("active").asBoolean(true);
            if (student && !active) continue;
            if (!q.subject().isEmpty()
                    && !q.subject().equalsIgnoreCase(l.path("mon").asText())) continue;
            if (q.chapter() != null && !q.chapter().equals(l.path("chuong").asText())) continue;
            if (!q.kinds().isEmpty() && !q.kinds().contains(l.path("loai").asText())) continue;
            String st = l.path("status").asText("NOT_STARTED");
            boolean keep =
                    switch (q.status()) {
                        case "Đã giải" -> "MASTERED".equals(st);
                        case "Đang làm" -> "OPEN".equals(st);
                        case "Bài sai" -> isWrong(l);
                        case "Chưa làm" -> !isDone(l) && !isWrong(l);
                        case "Đang mở" -> active;
                        case "Đang ẩn" -> !active;
                        default -> true;
                    };
            if (!keep) continue;
            if (!text.isEmpty()) {
                String hay =
                        normalize(l.path("ten").asText() + " " + l.path("id").asText() + " "
                                + LessonScreen.chapterLabel(l.path("chuong").asText()));
                if (!hay.contains(text)) continue;
            }
            out.add(l);
        }
        return out;
    }

    /** Tên chương: tên giáo viên đặt ({@code chuongTen}) nếu có, không thì bảng tên sẵn. */
    static String chapterName(JsonNode l) {
        String own = l.path("chuongTen").asText("");
        return own.isBlank() ? LessonScreen.chapterLabel(l.path("chuong").asText()) : own;
    }

    /** Mã chương/bài từ tên tiếng Việt: bỏ dấu, chữ thường, nối bằng '-'. */
    static String slug(String name) {
        return normalize(name).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
    }

    /** Bài mẫu (giảng giải có animation) hay bài luyện (bài tập, luyện tập). */
    static boolean isSample(JsonNode l) {
        return "giang-giai".equals(l.path("loai").asText());
    }

    /** Đã nộp nhưng chưa đạt: server ghi OPEN sau lần nộp sai đầu tiên. */
    static boolean isWrong(JsonNode l) {
        return "OPEN".equals(l.path("status").asText())
                || (!"MASTERED".equals(l.path("status").asText())
                        && l.path("wrongSubmits").asInt(0) > 0);
    }

    static boolean isDone(JsonNode l) {
        return "MASTERED".equals(l.path("status").asText());
    }
}
