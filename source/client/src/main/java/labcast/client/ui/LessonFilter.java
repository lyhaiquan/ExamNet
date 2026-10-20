package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Lọc và sắp danh sách bài ở cột trái. Tìm theo tên bài, không dấu, không phân biệt hoa thường, đủ mọi từ đã
 * gõ ("duong di" ra "Đường đi ngắn nhất…"). Sắp theo bảng chữ cái tiếng Việt (a ă â b c d đ e ê …), dấu thanh
 * chỉ để phân thứ tự khi phần chữ giống hệt nhau.
 */
final class LessonFilter {
    enum Order {
        /** Thứ tự server gửi: theo chương, bài trước học trước. */
        CHUONG,
        /** A → Z theo tên bài. */
        CHU_CAI
    }

    private static final String ALPHABET = "aăâbcdđeêfghijklmnoôơpqrstuưvwxyz";
    /** Dấu đổi chữ cái: ă, â, ê, ô, ơ, ư. */
    private static final String MU = "̛̆̂";
    /** Dấu thanh theo thứ tự từ điển: huyền, hỏi, ngã, sắc, nặng. */
    private static final String THANH = "̣̀̉̃́";

    private LessonFilter() {}

    static List<JsonNode> apply(List<JsonNode> all, String query, Order order) {
        String[] words = Arrays.stream(fold(query == null ? "" : query).split("\\s+"))
                .filter(w -> !w.isEmpty())
                .toArray(String[]::new);
        List<JsonNode> out = new ArrayList<>();
        for (JsonNode n : all) {
            String ten = fold(n.path("ten").asText());
            if (Arrays.stream(words).allMatch(ten::contains)) {
                out.add(n);
            }
        }
        if (order == Order.CHU_CAI) {
            out.sort(Comparator.comparing((JsonNode n) -> sortKey(n.path("ten").asText()))
                    .thenComparing(n -> n.path("ten").asText()));
        }
        return out;
    }

    /** Bỏ mọi dấu, đ thành d, chữ thường: để so khi tìm. */
    static String fold(String s) {
        String d = Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        StringBuilder b = new StringBuilder(d.length());
        for (int i = 0; i < d.length(); i++) {
            char c = d.charAt(i);
            if (Character.getType(c) != Character.NON_SPACING_MARK) {
                b.append(c == 'đ' ? 'd' : c);
            }
        }
        return b.toString();
    }

    /**
     * Khoá sắp xếp: phần chữ (mỗi chữ cái tiếng Việt một ký tự theo đúng thứ tự bảng chữ cái, xếp sau số và
     * dấu câu), rồi phần dấu thanh của từng chữ.
     */
    static String sortKey(String s) {
        String d = Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        StringBuilder chu = new StringBuilder();
        StringBuilder thanh = new StringBuilder();
        int i = 0;
        while (i < d.length()) {
            char c = d.charAt(i++);
            char mu = 0;
            int t = 0;
            // NFD có thể đặt dấu nặng trước dấu mũ (ặ = a + nặng + trăng): gom hết dấu đi sau chữ cái
            while (i < d.length() && Character.getType(d.charAt(i)) == Character.NON_SPACING_MARK) {
                char m = d.charAt(i++);
                if (MU.indexOf(m) >= 0) {
                    mu = m;
                } else if (THANH.indexOf(m) >= 0) {
                    t = THANH.indexOf(m) + 1;
                }
            }
            String letter = mu == 0 ? String.valueOf(c) : Normalizer.normalize("" + c + mu, Normalizer.Form.NFC);
            int rank = ALPHABET.indexOf(letter);
            chu.append(rank >= 0 ? (char) (0xE000 + rank) : c);
            thanh.append((char) ('0' + t));
        }
        return chu + "\u0000" + thanh;
    }
}
