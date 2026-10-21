package labcast.server.run;

/**
 * Kết quả một lượt chạy. {@code killed}: bị giết vì quá thời gian. {@code truncated}: trace vượt trần
 * 10 000 sự kiện / 2 MB. {@code errorLine}: dòng lỗi trong code học viên (kể cả lỗi biên dịch), 0 nếu không
 * biết; {@code errorKind}: {@code bien-dich}, {@code chay} hay null.
 */
public record Outcome(
        boolean ok,
        String stdout,
        String error,
        boolean truncated,
        long timeMs,
        boolean killed,
        int errorLine,
        String errorKind) {

    /** Không biết dòng lỗi (lỗi hạ tầng, quá giờ, bài Python cũ). */
    public Outcome(boolean ok, String stdout, String error, boolean truncated, long timeMs, boolean killed) {
        this(ok, stdout, error, truncated, timeMs, killed, 0, null);
    }
}
