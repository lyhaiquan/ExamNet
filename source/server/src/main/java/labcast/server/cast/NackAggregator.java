package labcast.server.cast;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/**
 * Gộp NACK (spec §12): nhiều máy cùng thiếu một gói — thường do nghẽn ở switch hay sóng Wi-Fi — thì
 * phát lại gói đó <b>một lần</b> qua multicast thay vì gửi riêng hàng chục lần qua TCP.
 *
 * <p>Đếm số máy xin cùng một SEQ trong cửa sổ {@link #WINDOW_MS} tính từ lần xin đầu. Đạt ngưỡng 20%
 * số máy trong lớp (ít nhất 2 máy) thì phát lại cho cả lớp; các máy xin sau trong cùng cửa sổ không cần
 * gì thêm vì sẽ nhận bản phát lại đó.
 */
final class NackAggregator {
    static final long WINDOW_MS = 50;
    private static final int MAX_WINDOWS = 8192;

    enum Mode {
        /** Gửi riêng máy này qua TCP. */
        UNICAST,
        /** Phát lại cho cả lớp qua multicast. */
        MULTICAST,
        /** Đã phát lại trong cửa sổ này: máy này sẽ nhận bản đó. */
        NONE
    }

    private static final class Window {
        final long start;
        final Set<Long> users = new HashSet<>();
        boolean multicastDone;

        Window(long start) {
            this.start = start;
        }
    }

    private final IntSupplier classSize;
    private final LongSupplier now;
    private final TreeMap<Integer, Window> windows = new TreeMap<>();

    NackAggregator(IntSupplier classSize, LongSupplier now) {
        this.classSize = classSize;
        this.now = now;
    }

    synchronized Map<Integer, Mode> onNack(long userId, int from, int to) {
        long t = now.getAsLong();
        int threshold = Math.max(2, (int) Math.ceil(classSize.getAsInt() * 0.2));
        Map<Integer, Mode> out = new LinkedHashMap<>();
        for (int seq = from; seq <= to; seq++) {
            Window w = windows.get(seq);
            if (w == null || t - w.start >= WINDOW_MS) {
                w = new Window(t);
                windows.put(seq, w);
            }
            if (w.multicastDone) {
                out.put(seq, Mode.NONE);
                continue;
            }
            w.users.add(userId);
            if (w.users.size() >= threshold) {
                w.multicastDone = true;
                out.put(seq, Mode.MULTICAST);
            } else {
                out.put(seq, Mode.UNICAST);
            }
        }
        if (windows.size() > MAX_WINDOWS) {
            Iterator<Window> it = windows.values().iterator();
            while (it.hasNext()) {
                if (t - it.next().start >= WINDOW_MS) {
                    it.remove();
                }
            }
        }
        return out;
    }
}
