package labcast.client.editor;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hỗ trợ gõ code, thuần Java để test không cần JavaFX: thụt dòng khi xuống dòng, tự đóng ngoặc, định dạng
 * câu SQL.
 */
public final class CodeAssist {
    private CodeAssist() {}

    /**
     * Chuỗi chèn khi bấm Enter ở cuối {@code before} (phần văn bản trước con trỏ): giữ thụt của dòng hiện tại,
     * thêm 4 dấu cách sau {@code :} (Python) hoặc sau {@code ( [ {}.
     */
    public static String newline(String before) {
        int start = before.lastIndexOf('\n') + 1;
        String line = before.substring(start);
        int n = 0;
        while (n < line.length() && line.charAt(n) == ' ') n++;
        String indent = " ".repeat(n);
        String trimmed = line.stripTrailing();
        boolean opens =
                trimmed.endsWith(":") || trimmed.endsWith("{") || trimmed.endsWith("(") || trimmed.endsWith("[");
        return "\n" + indent + (opens ? "    " : "");
    }

    /** Ngoặc mở → ngoặc đóng tự chèn theo. */
    public static String closing(char open) {
        return switch (open) {
            case '(' -> ")";
            case '[' -> "]";
            case '{' -> "}";
            case '"' -> "\"";
            case '\'' -> "'";
            default -> "";
        };
    }

    /** Gõ ký tự đóng đúng bằng ký tự ngay sau con trỏ thì chỉ bước qua, không chèn thêm. */
    public static boolean skipsOver(char typed, String after) {
        return !after.isEmpty() && after.charAt(0) == typed && ")]}\"'".indexOf(typed) >= 0;
    }

    private static final Set<String> KEYWORDS = Set.of(
            "select",
            "from",
            "where",
            "join",
            "inner",
            "left",
            "right",
            "full",
            "outer",
            "cross",
            "on",
            "group",
            "by",
            "having",
            "order",
            "asc",
            "desc",
            "limit",
            "offset",
            "as",
            "and",
            "or",
            "not",
            "null",
            "is",
            "in",
            "exists",
            "between",
            "like",
            "distinct",
            "union",
            "intersect",
            "except",
            "all",
            "case",
            "when",
            "then",
            "else",
            "end",
            "insert",
            "into",
            "values",
            "update",
            "set",
            "delete",
            "with",
            "natural");

    /** Mệnh đề bắt đầu dòng mới khi định dạng (so khớp không phân biệt hoa thường). */
    private static final Pattern CLAUSE = Pattern.compile(
            "(?<!\\b(?i:inner|left|right|full|cross|natural|outer))\\s+(?=(?i:from|where|group\\s+by|having|order\\s+by|limit|union|intersect|except|values|set"
                    + "|(?:(?:inner|left|right|full|cross|natural)\\s+(?:outer\\s+)?)?join)\\b)");

    private static final Pattern CONDITION = Pattern.compile("\\s+(?=(?i:and|or)\\b)");

    /**
     * Định dạng câu SQL: từ khoá viết hoa, mỗi mệnh đề một dòng, AND/OR trong điều kiện xuống dòng thụt 4.
     * Chuỗi trong nháy đơn và chú thích {@code --} giữ nguyên.
     */
    public static String formatSql(String sql) {
        StringBuilder out = new StringBuilder();
        // tách theo câu (dấu ;) ngoài chuỗi; mỗi câu định dạng riêng
        for (String stmt : splitStatements(sql)) {
            String s = stmt.strip();
            if (s.isEmpty()) continue;
            if (out.length() > 0) out.append("\n\n");
            out.append(formatOne(s));
        }
        return out.toString();
    }

    private static String formatOne(String stmt) {
        boolean hasSemicolon = stmt.endsWith(";");
        String body = hasSemicolon ? stmt.substring(0, stmt.length() - 1).strip() : stmt;
        // chú thích giữ nguyên dòng của nó, chỉ định dạng phần code
        StringBuilder masked = new StringBuilder();
        java.util.List<String> keep = new java.util.ArrayList<>();
        Matcher m = Pattern.compile("'([^']|'')*'|--[^\\n]*").matcher(body);
        int last = 0;
        while (m.find()) {
            masked.append(body, last, m.start())
                    .append("\u0000")
                    .append(keep.size())
                    .append("\u0000");
            keep.add(m.group());
            last = m.end();
        }
        masked.append(body.substring(last));
        String code = masked.toString().replaceAll("[ \\t]*\\n[ \\t]*", " ").replaceAll("[ \\t]+", " ");
        Matcher w = Pattern.compile("\\b[A-Za-z_]+\\b").matcher(code);
        StringBuilder up = new StringBuilder();
        while (w.find()) {
            String word = w.group();
            w.appendReplacement(
                    up,
                    KEYWORDS.contains(word.toLowerCase(Locale.ROOT))
                            ? word.toUpperCase(Locale.ROOT)
                            : Matcher.quoteReplacement(word));
        }
        w.appendTail(up);
        String lines = CLAUSE.matcher(up.toString()).replaceAll("\n");
        lines = CONDITION.matcher(lines).replaceAll("\n    ");
        // khôi phục chuỗi và chú thích; chú thích kết thúc dòng
        Matcher back = Pattern.compile("\u0000(\\d+)\u0000").matcher(lines);
        StringBuilder done = new StringBuilder();
        while (back.find()) {
            String piece = keep.get(Integer.parseInt(back.group(1)));
            back.appendReplacement(done, Matcher.quoteReplacement(piece.startsWith("--") ? piece + "\n" : piece));
        }
        back.appendTail(done);
        String result = done.toString()
                .replaceAll("(--[^\\n]*\\n) +", "$1")
                .replaceAll(" *\\n *(?=\\n)", "")
                .strip();
        return result + (hasSemicolon ? ";" : "");
    }

    private static java.util.List<String> splitStatements(String sql) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inString = false;
        boolean inComment = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            cur.append(c);
            if (inComment) {
                if (c == '\n') inComment = false;
            } else if (inString) {
                if (c == '\'') inString = false;
            } else if (c == '\'') {
                inString = true;
            } else if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                inComment = true;
            } else if (c == ';') {
                parts.add(cur.toString());
                cur.setLength(0);
            }
        }
        parts.add(cur.toString());
        return parts;
    }
}
