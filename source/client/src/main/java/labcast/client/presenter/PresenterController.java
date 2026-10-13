package labcast.client.presenter;

import java.util.function.Consumer;
import labcast.client.player.Player;
import labcast.client.player.PlayerControls;
import labcast.common.cast.CastMessages;
import labcast.common.cast.CastMessages.Request;

/**
 * Bảng điều khiển chiếu của giáo viên (ĐG1). Các nút của trình phát không điều khiển trình phát trên
 * máy giáo viên mà gửi {@code CAST_CONTROL} cho server. Server đóng dấu giờ hẹn rồi phát cho cả lớp,
 * kể cả máy giáo viên (qua TCP), nên giáo viên thấy đúng bước cả lớp đang thấy, không đi trước 300 ms.
 */
public final class PresenterController implements PlayerControls {
    private final Consumer<Request> send;
    private String traceId = "";
    private boolean casting;

    /** {@code send}: gửi payload {@code CAST_CONTROL} qua TCP. */
    public PresenterController(Consumer<Request> send) {
        this.send = send;
    }

    /** Bắt đầu chiếu bài {@code lessonId}: server phát trace giảng giải của bài cho cả lớp. */
    public void start(String lessonId) {
        traceId = lessonId;
        casting = true;
        send.accept(new Request(CastMessages.LOAD, traceId, -1, 1000));
    }

    public void stop() {
        if (!casting) {
            return;
        }
        casting = false;
        send.accept(new Request(CastMessages.STOP, traceId, 0, 0));
    }

    public boolean casting() {
        return casting;
    }

    public String traceId() {
        return traceId;
    }

    @Override
    public void play(Player p) {
        int step = p.position() >= p.trace().size() - 1 ? -1 : p.position();
        send.accept(new Request(CastMessages.PLAY, traceId, step, milli(p.speed())));
    }

    /** Server tự tính bước dừng ở giờ hẹn; bước gửi kèm chỉ để ghi nhật ký. */
    @Override
    public void pause(Player p) {
        send.accept(new Request(CastMessages.PAUSE, traceId, p.position(), 0));
    }

    @Override
    public void seek(Player p, int k) {
        send.accept(new Request(CastMessages.SEEK, traceId, k, 0));
    }

    @Override
    public void speed(Player p, double x) {
        send.accept(new Request(CastMessages.SPEED, traceId, p.position(), milli(x)));
    }

    @Override
    public void nextKey(Player p) {
        for (int k : p.trace().keySteps()) {
            if (k > p.position()) {
                send.accept(new Request(CastMessages.SEEK, traceId, k, 0));
                return;
            }
        }
    }

    private static int milli(double speed) {
        return (int) Math.round(speed * 1000);
    }
}
