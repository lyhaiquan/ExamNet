package labcast.server.run;

/**
 * Kết quả một lượt chạy. {@code killed}: bị giết vì quá thời gian. {@code truncated}: trace vượt trần
 * 10 000 sự kiện / 2 MB.
 */
public record Outcome(boolean ok, String stdout, String error, boolean truncated, long timeMs, boolean killed) {}
