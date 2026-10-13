package labcast.client.cast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;
import labcast.client.clock.Clock;
import labcast.client.player.Player;
import labcast.common.cast.CastMessages;
import labcast.common.protocol.Frame;
import labcast.common.protocol.ProtocolException;
import labcast.common.trace.Trace;
import labcast.common.trace.TraceReader;

/**
 * Một buổi chiếu nhìn từ app (ĐG1): ghép các khúc {@code CAST_DATA} thành trace, áp gói điều khiển và
 * khung khoá mới nhất (theo SEQ — gói cũ đến sau thì bỏ), cho trình phát theo giờ hẹn của server.
 *
 * <p>Máy vào muộn chỉ cần một khung khoá: biết trace nào, bao nhiêu khúc, khúc đầu ở SEQ nào, rồi xin
 * các khúc thiếu bằng {@code CAST_NACK}. Đủ khúc thì hiện và nhảy tới đúng bước cả lớp đang ở.
 *
 * <p>Không an toàn đa luồng: app gọi mọi hàm trên luồng giao diện.
 */
public final class CastSession {
    private static final Logger LOG = Logger.getLogger(CastSession.class.getName());

    /** Nơi hiện buổi chiếu: dựng bộ vẽ theo trace và trả trình phát. */
    public interface Screen {
        Player show(String traceId, Trace trace);

        /** Giáo viên dừng chiếu. */
        void stopped();
    }

    private record Segment(int step, long at, int speedMilli, boolean playing) {}

    private final Clock clock;
    private final Consumer<CastMessages.Nack> nack;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private Screen screen;
    private Player player;
    private int castId;
    private String traceId = "";
    private int firstDataSeq = -1;
    private byte[][] chunks;
    private int have;
    private Trace trace;
    private Segment segment;
    private int lastControlSeq;
    private boolean active;
    private boolean following = true;

    public CastSession(Clock clock, Consumer<CastMessages.Nack> nack) {
        this.clock = clock;
        this.nack = nack;
    }

    /** Một gói chiếu đã qua {@link CastReceiver} (multicast hoặc TCP). Gói hỏng thì bỏ. */
    public void onFrame(Frame f) {
        try {
            switch (f.type()) {
                case CAST_DATA -> onData(f.seq(), CastMessages.Data.decode(f.payload()));
                case CAST_CONTROL -> onControl(f.seq(), CastMessages.Control.decode(f.payload()));
                case CAST_KEYFRAME -> onKeyframe(f.seq(), CastMessages.Keyframe.decode(f.payload()));
                default -> {}
            }
        } catch (ProtocolException e) {
            LOG.fine("Gói chiếu hỏng: " + e.getMessage());
        }
    }

    private void onData(int seq, CastMessages.Data d) {
        adopt(d.castId());
        int first = seq - d.chunkIndex();
        if (first <= 0) {
            return; // SEQ luồng chiếu bắt đầu từ 1: khúc này bịa
        }
        if (first != firstDataSeq) {
            if (first < firstDataSeq) {
                return; // khúc của lần nạp trước, đến trễ
            }
            startAssembly(d.traceId(), first, d.totalChunks());
        }
        if (trace != null || d.totalChunks() != chunks.length || chunks[d.chunkIndex()] != null) {
            return;
        }
        chunks[d.chunkIndex()] = d.bytes();
        have++;
        if (have == chunks.length) {
            assemble();
        }
    }

    private void onControl(int seq, CastMessages.Control c) {
        if (!newer(c.castId(), seq)) {
            return;
        }
        if (c.action().equals(CastMessages.STOP)) {
            active = false;
            segment = null;
            if (player != null) {
                player.unfollow();
            }
            if (screen != null) {
                screen.stopped();
            }
            fire();
            return;
        }
        active = true;
        if (!c.traceId().equals(traceId)) {
            // Lệnh cho trace chưa thấy khúc nào: chờ khung khoá để biết khúc ở SEQ nào
            startAssembly(c.traceId(), -1, 0);
        }
        segment = new Segment(c.step(), c.at(), c.speedMilli(), c.playing());
        if (c.action().equals(CastMessages.LOAD)) {
            following = true; // bài mới: mọi máy về theo giáo viên
            requestMissing();
        }
        follow();
        fire();
    }

    private void onKeyframe(int seq, CastMessages.Keyframe k) {
        if (k.castId() == castId && k.firstDataSeq() < firstDataSeq) {
            return; // khung khoá của lần nạp trước
        }
        if (!newer(k.castId(), seq)) {
            return;
        }
        boolean wasActive = active;
        active = true;
        if (!k.traceId().equals(traceId) || k.firstDataSeq() != firstDataSeq) {
            startAssembly(k.traceId(), k.firstDataSeq(), k.totalChunks());
        }
        segment = new Segment(k.step(), k.anchorAt(), k.speedMilli(), k.playing());
        requestMissing(); // mỗi khung khoá (2 s) xin lại những khúc còn thiếu
        follow();
        if (!wasActive) {
            fire();
        }
    }

    /** Gói điều khiển/khung khoá có SEQ lớn hơn gói đã áp (cùng lần chạy server) thì mới áp. */
    private boolean newer(int id, int seq) {
        adopt(id);
        if (seq <= lastControlSeq) {
            return false;
        }
        lastControlSeq = seq;
        return true;
    }

    /** Gói của lần chạy server khác (server vừa khởi động lại): quên mọi thứ của lần trước. */
    private void adopt(int id) {
        if (id == castId) {
            return;
        }
        castId = id;
        lastControlSeq = 0;
        traceId = "";
        firstDataSeq = -1;
        chunks = null;
        have = 0;
        trace = null;
        segment = null;
    }

    private void startAssembly(String id, int first, int total) {
        boolean sameTrace = trace != null && id.equals(traceId) && first == firstDataSeq;
        if (sameTrace) {
            return;
        }
        traceId = id;
        firstDataSeq = first;
        chunks = new byte[Math.max(0, total)][];
        have = 0;
        trace = null;
    }

    /** Xin các khúc còn thiếu, theo dải SEQ liên tiếp. */
    private void requestMissing() {
        if (trace != null || firstDataSeq <= 0 || chunks == null) {
            return;
        }
        List<Integer> miss = missingChunks();
        int i = 0;
        while (i < miss.size()) {
            int from = miss.get(i);
            int to = from;
            while (i + 1 < miss.size() && miss.get(i + 1) == to + 1 && to + 1 - from < CastMessages.MAX_NACK_RANGE) {
                to = miss.get(++i);
            }
            nack.accept(new CastMessages.Nack(castId, firstDataSeq + from, firstDataSeq + to));
            i++;
        }
    }

    /** Chỉ số các khúc còn thiếu của trace đang chờ. */
    public List<Integer> missingChunks() {
        List<Integer> out = new ArrayList<>();
        if (chunks != null && trace == null) {
            for (int i = 0; i < chunks.length; i++) {
                if (chunks[i] == null) {
                    out.add(i);
                }
            }
        }
        return out;
    }

    private void assemble() {
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        for (byte[] c : chunks) {
            all.writeBytes(c);
        }
        try {
            trace = TraceReader.append(TraceReader.empty(), CastMessages.decompress(all.toByteArray()));
        } catch (IOException e) {
            LOG.warning("Trace chiếu hỏng, xin lại: " + e.getMessage());
            chunks = new byte[chunks.length][];
            have = 0;
            return;
        }
        show();
        fire();
    }

    private void show() {
        player = null;
        if (screen != null && trace != null) {
            player = screen.show(traceId, trace);
            follow();
        }
    }

    private void follow() {
        if (player != null && following && segment != null) {
            player.followAt(segment.step, segment.at, segment.speedMilli, segment.playing, clock);
        }
    }

    /** Gắn nơi hiện chiếu. Đã có trace thì hiện ngay và nhảy tới đúng bước. */
    public void attach(Screen s) {
        this.screen = s;
        show();
    }

    /** Gỡ nơi hiện (học viên đóng màn chiếu); vẫn nhận và ghép dữ liệu để mở lại là có ngay. */
    public void detachScreen() {
        if (player != null) {
            player.unfollow();
        }
        screen = null;
        player = null;
    }

    /** Học viên tự tua: thôi theo giáo viên cho tới khi bấm "Theo giáo viên". */
    public void detach() {
        following = false;
        if (player != null) {
            player.unfollow();
        }
        fire();
    }

    public void rejoin() {
        following = true;
        follow();
        fire();
    }

    /** Giáo viên đang chiếu (đã thấy lệnh hoặc khung khoá, chưa thấy STOP). */
    public boolean active() {
        return active;
    }

    /** Đã có đủ trace để hiện. */
    public boolean ready() {
        return trace != null;
    }

    public boolean following() {
        return following;
    }

    public String traceId() {
        return traceId;
    }

    /** Gọi khi bắt đầu/hết chiếu, có trace mới, đổi theo/tách. */
    public void onChange(Runnable l) {
        listeners.add(l);
    }

    private void fire() {
        for (Runnable l : listeners) {
            l.run();
        }
    }
}
