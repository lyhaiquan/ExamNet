package labcast.server.ops;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Giữ {@code capacity} dòng log gần nhất trong bộ nhớ để dashboard hiện. Gắn vào logger gốc lúc server
 * khởi động; vòng có giới hạn nên chạy cả buổi học cũng không tốn thêm bộ nhớ.
 */
public final class LogBuffer extends Handler {
    public record Line(long ts, String level, String logger, String message) {}

    private final int capacity;
    private final Deque<Line> lines = new ArrayDeque<>();
    private final SimpleFormatter formatter = new SimpleFormatter();

    public LogBuffer(int capacity) {
        this.capacity = capacity;
        setLevel(Level.INFO);
    }

    @Override
    public void publish(LogRecord r) {
        if (r == null || !isLoggable(r)) {
            return;
        }
        String msg = formatter.formatMessage(r);
        if (r.getThrown() != null) {
            msg += " — " + r.getThrown();
        }
        String logger = r.getLoggerName() == null ? "" : r.getLoggerName();
        Line line = new Line(r.getMillis(), r.getLevel().getName(), logger.substring(logger.lastIndexOf('.') + 1), msg);
        synchronized (lines) {
            lines.addLast(line);
            while (lines.size() > capacity) {
                lines.removeFirst();
            }
        }
    }

    /** Các dòng còn giữ, mới nhất trước. */
    public List<Line> recent() {
        synchronized (lines) {
            List<Line> out = new ArrayList<>(lines);
            java.util.Collections.reverse(out);
            return out;
        }
    }

    @Override
    public void flush() {}

    @Override
    public void close() {}
}
