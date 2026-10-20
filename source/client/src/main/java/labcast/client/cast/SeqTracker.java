package labcast.client.cast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import labcast.common.cast.CastMessages;

/**
 * Theo dõi SEQ của một luồng chiếu: bỏ gói trùng, biết gói nào thiếu, hẹn giờ xin lại.
 *
 * <p><b>Không giữ gói đến sớm để chờ gói thiếu.</b> Mỗi gói điều khiển và khung khoá mang đủ trạng thái
 * phát, khúc dữ liệu có chỉ số riêng, nên app áp ngay gói vừa tới (gói điều khiển cũ hơn gói đã áp thì
 * bỏ — {@code CastSession} lo). Giữ để giao theo thứ tự thì một gói mất hẳn sẽ chặn mọi gói sau nó.
 *
 * <p>Gói thiếu: chờ {@link #FIRST_NACK_MS} (có thể nó chỉ đến muộn) rồi xin; chưa tới thì xin lại sau
 * {@link #RETRY_MS}; xin {@link #MAX_NACKS} lần không được thì bỏ — khung khoá kế tiếp (≤ 2 s) bù trạng
 * thái. Không an toàn đa luồng: {@code CastReceiver} gọi trong khối đồng bộ.
 */
public final class SeqTracker {
    static final long FIRST_NACK_MS = 30;
    static final long RETRY_MS = 200;
    static final int MAX_NACKS = 3;
    /** Bằng vòng đệm của server: gói cũ hơn thế server không còn để gửi lại. */
    static final int MAX_TRACKED = 4096;

    private static final class Miss {
        final long since;
        long lastNack;
        int nacks;

        Miss(long since) {
            this.since = since;
        }
    }

    private final int castId;
    /** Mọi SEQ ≤ floor đã nhận hoặc đã bỏ. */
    private int floor;

    private int highest;
    private final TreeSet<Integer> received = new TreeSet<>();
    private final TreeMap<Integer, Miss> missing = new TreeMap<>();
    private boolean any;
    private long duplicates;
    private long givenUp;

    /** {@code firstSeq}: gói đầu tiên máy này thấy — máy vào muộn không xin lại cả lịch sử. */
    public SeqTracker(int castId, int firstSeq) {
        this.castId = castId;
        this.floor = firstSeq - 1;
        this.highest = firstSeq - 1;
    }

    public int castId() {
        return castId;
    }

    /** Ghi nhận một gói. {@code true} nếu là gói mới; trùng, quá cũ hoặc của lần chạy server khác thì {@code false}. */
    public boolean accept(int castId, int seq, long now) {
        if (castId != this.castId) {
            return false;
        }
        if (seq <= floor || received.contains(seq)) {
            duplicates++;
            return false;
        }
        if (seq > highest) {
            int from = highest + 1;
            if (seq - MAX_TRACKED > from) {
                // Lỗ dài hơn vòng đệm của server: phần đầu server không còn để gửi lại, bỏ luôn
                from = seq - MAX_TRACKED;
                givenUp += missing.headMap(from).size() + (from - (highest + 1));
                missing.headMap(from).clear();
                received.headSet(from).clear();
                floor = from - 1;
            }
            for (int s = from; s < seq; s++) {
                missing.put(s, new Miss(now));
            }
            highest = seq;
        }
        missing.remove(seq);
        received.add(seq);
        any = true;
        advance();
        return true;
    }

    /** Các dải cần gửi {@code CAST_NACK} ngay (mỗi dải tối đa 256 gói); đồng thời bỏ các gói đã xin đủ lần. */
    public List<int[]> due(long now) {
        List<Integer> ask = new ArrayList<>();
        Iterator<Map.Entry<Integer, Miss>> it = missing.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Miss> e = it.next();
            Miss m = e.getValue();
            if (m.nacks == 0 ? now - m.since >= FIRST_NACK_MS : now - m.lastNack >= RETRY_MS) {
                if (m.nacks >= MAX_NACKS) {
                    it.remove();
                    givenUp++;
                    continue;
                }
                m.nacks++;
                m.lastNack = now;
                ask.add(e.getKey());
            }
        }
        advance();
        return ranges(ask);
    }

    /** Các khoảng [từ, đến] đang thiếu. */
    public List<int[]> gaps() {
        return ranges(new ArrayList<>(missing.keySet()));
    }

    /** SEQ lớn nhất đã thấy; −1 nếu chưa nhận gói nào. Gửi kèm heartbeat ({@code lastCastSeq}). */
    public int lastSeq() {
        return any ? highest : -1;
    }

    public long duplicates() {
        return duplicates;
    }

    public long givenUp() {
        return givenUp;
    }

    private void advance() {
        while (floor < highest && !missing.containsKey(floor + 1)) {
            floor++;
            received.remove(floor);
        }
    }

    private static List<int[]> ranges(List<Integer> sorted) {
        List<int[]> out = new ArrayList<>();
        int i = 0;
        while (i < sorted.size()) {
            int from = sorted.get(i);
            int to = from;
            while (i + 1 < sorted.size()
                    && sorted.get(i + 1) == to + 1
                    && to + 1 - from < CastMessages.MAX_NACK_RANGE) {
                to = sorted.get(++i);
            }
            out.add(new int[] {from, to});
            i++;
        }
        return out;
    }
}
