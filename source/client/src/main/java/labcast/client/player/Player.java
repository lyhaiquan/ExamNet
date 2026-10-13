package labcast.client.player;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongSupplier;
import labcast.client.clock.Clock;
import labcast.common.cast.CastMessages;
import labcast.common.trace.Trace;
import labcast.common.trace.TraceEvent;

/**
 * Trình phát trace: chạy, dừng, tua tới bước k qua keyframe, đổi tốc độ. Thuần Java — không đụng
 * JavaFX — để test bằng đồng hồ giả; {@code PlayerPane} gọi {@link #tick()} mỗi khung hình.
 *
 * <p>Vị trí là chỉ số sự kiện cuối cùng đã áp (−1 = chưa áp gì).
 */
public final class Player {
    /** Một bước ở tốc độ 1×. */
    public static final Duration STEP = Duration.ofMillis(Trace.STEP_MS);
    /** Một khung hình phải áp nhiều hơn ngần này bước thì áp không animation (tốc độ cao). */
    private static final int MAX_ANIMATED_PER_TICK = 3;

    private final Trace trace;
    private final Map<String, View> viewsByVar;
    private final LongSupplier now;
    private final List<Consumer<String>> narration = new CopyOnWriteArrayList<>();
    private final List<IntConsumer> positionListeners = new CopyOnWriteArrayList<>();

    private int position = -1;
    private boolean playing;
    private double speed = 1.0;
    private long lastTick;
    /** Thời gian phát đã tích (ms × tốc độ) chưa đủ một bước. Cộng mili giây để không lệch số thực. */
    private double progressMs;
    /** Khác null khi đang theo giáo viên. */
    private Clock followClock;

    private Segment segment;
    private Segment nextSegment;

    public Player(Trace trace, Map<String, View> viewsByVar, LongSupplier nowMillis) {
        this.trace = trace;
        this.viewsByVar = Map.copyOf(viewsByVar);
        this.now = nowMillis;
        resetViews(-1);
    }

    public void play() {
        if (position >= trace.size() - 1) {
            seek(-1); // hết rồi thì phát lại từ đầu
        }
        playing = true;
        lastTick = now.getAsLong();
        progressMs = 0;
    }

    public void pause() {
        playing = false;
    }

    public boolean playing() {
        return playing;
    }

    /** 0,25 … 8. */
    public void speed(double x) {
        this.speed = Math.max(0.25, Math.min(8, x));
    }

    public double speed() {
        return speed;
    }

    /** Dựng lại từ keyframe gần nhất trước k rồi áp tới k, KHÔNG animation. k = −1 là về đầu. */
    public void seek(int k) {
        k = Math.max(-1, Math.min(k, trace.size() - 1));
        // Ở vị trí đầu (−1) vẫn lấy keyframe ở chỉ số 0 nếu có: đó là trạng thái ban đầu của mảng,
        // không có nó thì màn hình trống tới khi bấm Phát.
        int s = trace.snapshotBefore(Math.max(k, 0));
        resetViews(s);
        for (int i = s + 1; i <= k; i++) {
            dispatch(trace.events().get(i), false, Duration.ZERO);
        }
        position = k;
        progressMs = 0;
        firePosition();
    }

    /** Nhảy tới sự kiện {@code key} kế tiếp; không còn thì giữ nguyên. */
    public void nextKeyStep() {
        for (int k : trace.keySteps()) {
            if (k > position) {
                seek(k);
                return;
            }
        }
    }

    /**
     * Theo giáo viên (ĐG1): từ thời điểm {@code atServerTime} (đồng hồ server) ở bước {@code step}, nếu
     * {@code playing} thì tiến đều theo tốc độ. Vị trí tính bằng {@link CastMessages#positionAt} — cùng
     * hàm server dùng — nên mọi máy cùng giờ server ra cùng một bước, dù gói điều khiển đến sớm hay muộn.
     *
     * <p>Trước giờ hẹn vẫn theo đoạn phát cũ (lần đầu thì đứng yên). Gọi lại với đoạn mới thay đoạn đang
     * chờ. Thoát chế độ này bằng {@link #unfollow()}.
     */
    public void followAt(int step, long atServerTime, int speedMilli, boolean playing, Clock clock) {
        this.followClock = clock;
        this.nextSegment = new Segment(step, atServerTime, speedMilli, playing);
        followTick();
    }

    /** Thôi theo giáo viên: giữ nguyên vị trí, dừng, từ đây học viên tự điều khiển. */
    public void unfollow() {
        followClock = null;
        segment = null;
        nextSegment = null;
        playing = false;
    }

    public boolean following() {
        return followClock != null;
    }

    private record Segment(int step, long at, int speedMilli, boolean playing) {}

    private void followTick() {
        long t = followClock.serverNow();
        if (nextSegment != null && t >= nextSegment.at) {
            segment = nextSegment;
            nextSegment = null;
        }
        if (segment == null) {
            return; // chưa tới giờ hẹn đầu tiên: đứng yên
        }
        Segment s = segment;
        int target = Math.max(-1, CastMessages.positionAt(t, s.step, s.playing, s.speedMilli, s.at, trace.size()));
        speed = s.speedMilli / 1000.0;
        playing = s.playing && target < trace.size() - 1;
        int ahead = target - position;
        if (ahead == 0) {
            return;
        }
        if (ahead > 0 && ahead <= MAX_ANIMATED_PER_TICK) {
            Duration d = Duration.ofMillis((long) (STEP.toMillis() / speed));
            while (position < target) {
                position++;
                dispatch(trace.events().get(position), true, d);
            }
            firePosition();
        } else {
            seek(target); // lùi lại, hoặc cách xa (vào muộn, vừa theo lại): dựng từ keyframe
        }
    }

    /** Gọi mỗi khung hình. Tiến {@code (thời gian trôi × tốc độ) / STEP} bước. */
    public void tick() {
        if (followClock != null) {
            followTick();
            return;
        }
        long t = now.getAsLong();
        if (!playing) {
            lastTick = t;
            return;
        }
        progressMs += (t - lastTick) * speed;
        lastTick = t;
        int n = (int) (progressMs / STEP.toMillis());
        if (n <= 0) {
            return;
        }
        progressMs -= n * STEP.toMillis();
        boolean animate = n <= MAX_ANIMATED_PER_TICK;
        Duration d = Duration.ofMillis((long) (STEP.toMillis() / speed));
        for (int i = 0; i < n && position < trace.size() - 1; i++) {
            position++;
            dispatch(trace.events().get(position), animate, d);
        }
        if (position >= trace.size() - 1 && trace.ended()) {
            playing = false;
        }
        firePosition();
    }

    public int position() {
        return position;
    }

    public Trace trace() {
        return trace;
    }

    /** Lời thuyết minh ({@code say}). */
    public void onNarration(Consumer<String> l) {
        narration.add(l);
    }

    public void onPosition(IntConsumer l) {
        positionListeners.add(l);
    }

    private void resetViews(int snapIndex) {
        JsonNode state = snapIndex >= 0 ? trace.events().get(snapIndex).get("state") : null;
        for (Map.Entry<String, View> e : viewsByVar.entrySet()) {
            e.getValue().reset(state == null ? null : state.get(e.getKey()));
        }
    }

    private void dispatch(TraceEvent e, boolean animate, Duration d) {
        switch (e.t()) {
            case "snap" -> {
                // keyframe chỉ dùng khi tua
            }
                // key là bước quan trọng (nút ⏭ nhảy tới); lời ghi kèm cũng hiện như thuyết minh
            case "say", "key" -> {
                String s = e.text("s");
                if (s != null && !s.isEmpty()) {
                    narration.forEach(l -> l.accept(s));
                }
            }
            case "line", "vars" -> {
                for (View v : viewsByVar.values()) {
                    if (v.kind().equals("code-bien")) {
                        v.apply(e, animate, d);
                    }
                }
            }
            default -> {
                String var = e.var();
                View v = var == null ? null : viewsByVar.get(var);
                if (v != null) {
                    v.apply(e, animate, d);
                }
            }
        }
    }

    private void firePosition() {
        for (IntConsumer l : positionListeners) {
            l.accept(position);
        }
    }
}
