package labcast.server.cast;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import labcast.common.cast.CastMessages;
import labcast.common.trace.Trace;
import labcast.common.trace.TraceReader;

/**
 * Buổi chiếu đang diễn ra: trace đã nén, SEQ của khúc đầu, và "đoạn phát" hiện tại — từ thời điểm
 * {@code anchorAt} (đồng hồ server) ở bước {@code step}, nếu đang chạy thì tiến đều theo tốc độ. Cách
 * tính vị trí giống hệt {@code Player} phía app, nên server biết mọi máy đang ở bước nào mà không cần hỏi.
 *
 * <p>Giữ trace nên dựng lại được bất kỳ khúc {@code CAST_DATA} nào, kể cả khi đã trôi khỏi vòng đệm
 * (máy vào lớp muộn xin lại cả trace).
 */
final class CastState {
    /** Lệnh của giáo viên có hiệu lực sau ngần này ms: đủ để gói tới mọi máy trước giờ hẹn. */
    static final long LEAD_MS = 300;

    /** Trace đã chuẩn bị để chiếu. {@code size} là số sự kiện, để chặn vị trí ở cuối. */
    record Loaded(String traceId, byte[] compressed, int chunkSize, int totalChunks, int size) {
        CastMessages.Data data(int castId, int i) {
            int from = i * chunkSize;
            byte[] part = Arrays.copyOfRange(compressed, from, Math.min(compressed.length, from + chunkSize));
            return new CastMessages.Data(castId, traceId, i, totalChunks, part);
        }
    }

    static Loaded prepare(String traceId, List<String> lines) throws IOException {
        Trace t = TraceReader.append(TraceReader.empty(), lines);
        byte[] z = CastMessages.compress(lines);
        int chunk = CastMessages.chunkSize(traceId);
        return new Loaded(traceId, z, chunk, Math.max(1, (z.length + chunk - 1) / chunk), t.size());
    }

    private Loaded loaded;
    private int firstDataSeq;
    private int step = -1;
    private boolean playing;
    private int speedMilli = 1000;
    private long anchorAt;

    synchronized void load(Loaded l, int firstDataSeq) {
        this.loaded = l;
        this.firstDataSeq = firstDataSeq;
    }

    synchronized Loaded loaded() {
        return loaded;
    }

    synchronized int firstDataSeq() {
        return firstDataSeq;
    }

    /** Vị trí ở thời điểm {@code t}: cùng công thức với {@code Player.followAt} phía app. */
    synchronized int position(long t) {
        return position(t, step, playing, speedMilli, anchorAt, loaded == null ? 0 : loaded.size());
    }

    static int position(long t, int step, boolean playing, int speedMilli, long anchorAt, int size) {
        if (!playing || t <= anchorAt) {
            return step;
        }
        long advance = (t - anchorAt) * speedMilli / (1000L * Trace.STEP_MS);
        return (int) Math.min(size - 1L, step + advance);
    }

    /**
     * Áp một lệnh của giáo viên ({@code LOAD} đã {@link #load} trước), trả gói điều khiển để phát. Lệnh có
     * hiệu lực ở {@code at = now + LEAD_MS}. Dừng và đổi tốc độ lấy vị trí do server tính ở đúng {@code
     * at}, nên cả lớp dừng ở cùng một bước dù giáo viên bấm lúc nào.
     */
    synchronized CastMessages.Control apply(int castId, CastMessages.Request r, long now) {
        long at = Math.max(now + LEAD_MS, anchorAt + 1); // giờ hẹn không lùi
        int last = loaded.size() - 1;
        switch (r.action()) {
            case CastMessages.LOAD -> {
                step = -1;
                playing = false;
                speedMilli = speed(r.speedMilli(), 1000);
            }
            case CastMessages.PLAY -> {
                step = clamp(r.step(), last);
                if (step >= last) {
                    step = -1; // đã hết thì phát lại từ đầu, như nút Phát của app
                }
                playing = true;
                speedMilli = speed(r.speedMilli(), speedMilli);
            }
            case CastMessages.PAUSE -> {
                step = position(at);
                playing = false;
            }
            case CastMessages.SEEK -> {
                step = clamp(r.step(), last);
            }
            case CastMessages.SPEED -> {
                step = position(at);
                speedMilli = speed(r.speedMilli(), speedMilli);
            }
            case CastMessages.STOP -> {
                step = position(at);
                playing = false;
            }
            default -> throw new IllegalArgumentException("Lệnh chiếu lạ: " + r.action());
        }
        anchorAt = at;
        return new CastMessages.Control(castId, r.action(), loaded.traceId(), step, speedMilli, at, playing);
    }

    synchronized CastMessages.Keyframe keyframe(int castId, int lastSeq) {
        return new CastMessages.Keyframe(
                castId,
                loaded.traceId(),
                loaded.totalChunks(),
                firstDataSeq,
                step,
                playing,
                speedMilli,
                anchorAt,
                lastSeq);
    }

    /** Hết buổi chiếu: không phát khung khoá nữa. */
    synchronized void clear() {
        loaded = null;
        playing = false;
        step = -1;
    }

    /** SEQ đó là khúc dữ liệu của trace đang chiếu thì trả chỉ số khúc, không thì −1. */
    synchronized int chunkIndexOf(int seq) {
        if (loaded == null || seq < firstDataSeq || seq >= firstDataSeq + loaded.totalChunks()) {
            return -1;
        }
        return seq - firstDataSeq;
    }

    private static int clamp(int step, int last) {
        return Math.max(-1, Math.min(step, last));
    }

    /** 0,25× … 8×; giá trị không hợp lệ thì giữ tốc độ cũ. */
    private static int speed(int milli, int current) {
        return milli <= 0 ? current : Math.max(250, Math.min(8000, milli));
    }
}
