package labcast.server.journal;

/** Áp một delta lên văn bản. */
final class TextOps {
    private TextOps() {}

    /** Xoá {@code delLen} ký tự từ {@code pos} rồi chèn {@code ins}. Ngoài phạm vi → IllegalArgumentException. */
    static String apply(String text, int pos, int delLen, String ins) {
        if (pos < 0 || delLen < 0 || pos > text.length() || pos + delLen > text.length()) {
            throw new IllegalArgumentException(
                    "Delta ngoài phạm vi: pos=" + pos + ", delLen=" + delLen + ", độ dài=" + text.length());
        }
        return text.substring(0, pos) + (ins == null ? "" : ins) + text.substring(pos + delLen);
    }
}
