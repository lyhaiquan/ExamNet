package labcast.server.practice;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.Role;
import labcast.server.net.Connection;
import labcast.server.net.FramePolicy;
import labcast.server.session.Session;

/**
 * Giới hạn tốc độ (spec §13, task 3.11), kiểm trên luồng đọc <b>trước khi</b> thông điệp vào pool nghiệp
 * vụ: lượt bị chặn không tốn luồng nghiệp vụ hay sandbox nào. Chủ: Người 3.
 *
 * <table>
 *   <tr><th>Loại</th><th>Thùng token</th><th>Đếm theo</th></tr>
 *   <tr><td>{@code RUN_REQ}</td><td>6 lượt/phút, đợt tối đa 3</td><td>người (nối lại không được thùng mới);
 *       giáo viên không giới hạn để chạy mẫu khi giảng</td></tr>
 *   <tr><td>{@code PREDICT_ANSWER}</td><td>1 lượt / 3 s</td><td>người</td></tr>
 *   <tr><td>{@code CAST_NACK}</td><td>100 / s, đợt 300; vượt thì bỏ im lặng</td><td>kết nối</td></tr>
 *   <tr><td>mọi loại khác</td><td>50 / s</td><td>kết nối</td></tr>
 * </table>
 *
 * <p>{@code CAST_NACK} có thùng riêng: trace lớn trên mạng mất gói nhiều thì app xin lại cả trăm dải gói
 * trong một giây (mỗi 10 ms một lượt), chặn ở 50 / s sẽ làm sai số liệu đo khả năng sửa gói của chiếu
 * multicast. NACK bị bỏ thì app tự xin lại sau 200 ms, quá 3 lần thì khung khoá kế tiếp bù. {@code
 * CAST_CONTROL} của giáo viên không bị đếm: tua bằng phím mũi tên giữ lâu gửi liên tục.
 *
 * <p>Không đếm {@code CODE_DELTA}: chặn một delta là đứt mạch {@code seq}, mọi delta sau phải chờ tới lần nối
 * lại kế tiếp (app chỉ gửi lại khi nối lại). Cũng không đếm {@code HEARTBEAT_ACK}, {@code TIME_SYNC_REQ},
 * {@code BYE}. {@code SUBMIT_REQ} giới hạn ở {@link SubmitModule} (thời gian chờ sau nộp sai, mỗi người một
 * lượt đang chấm), vì cần đọc database mà luồng đọc thì không được chờ database. Gửi dồn quá 500 thông điệp/s
 * suốt 5 s thì {@link Connection} đóng hẳn kết nối, kể cả trước khi đăng nhập.
 */
public final class RateLimitPolicy implements FramePolicy {

    /** {@code burst} lượt liền, rồi cứ {@code every} thêm một lượt. */
    public record Limit(int burst, Duration every) {}

    public static final Limit RUN = new Limit(3, Duration.ofSeconds(10));
    public static final Limit PREDICT = new Limit(1, Duration.ofSeconds(3));
    public static final Limit OTHER = new Limit(50, Duration.ofMillis(20));
    public static final Limit NACK = new Limit(300, Duration.ofMillis(10));

    private static final Set<MessageType> FREE =
            EnumSet.of(MessageType.CODE_DELTA, MessageType.HEARTBEAT_ACK, MessageType.TIME_SYNC_REQ, MessageType.BYE);
    private static final String ATTR = "gioi-han-toc-do";

    private final LongSupplier clock;
    private final Limit run;
    private final Limit predict;
    private final Limit other;
    private final Map<Long, PerUser> users = new ConcurrentHashMap<>();

    private record PerUser(TokenBucket run, TokenBucket predict) {}

    /** Chỉ luồng đọc của chính kết nối đó đụng tới. */
    private static final class PerConnection {
        final TokenBucket other;
        final TokenBucket nack;
        long notifiedAt = Long.MIN_VALUE / 2;

        PerConnection(TokenBucket other, TokenBucket nack) {
            this.other = other;
            this.nack = nack;
        }
    }

    public RateLimitPolicy(LongSupplier clock) {
        this(clock, RUN, PREDICT, OTHER);
    }

    public RateLimitPolicy(LongSupplier clock, Limit run, Limit predict, Limit other) {
        this.clock = clock;
        this.run = run;
        this.predict = predict;
        this.other = other;
    }

    @Override
    public Decision check(Connection c, MessageType t) {
        if (FREE.contains(t)) {
            return Decision.ALLOW;
        }
        long now = clock.getAsLong();
        PerConnection pc = (PerConnection) c.attr(ATTR);
        if (pc == null) {
            pc = new PerConnection(
                    new TokenBucket(other.burst(), other.every(), now),
                    new TokenBucket(NACK.burst(), NACK.every(), now));
            c.setAttr(ATTR, pc);
        }
        Session s = c.session();
        if (t == MessageType.CAST_NACK) {
            return pc.nack.tryTake(now) == 0 ? Decision.ALLOW : Decision.DROP;
        }
        if (t == MessageType.CAST_CONTROL && s != null && s.role() == Role.TEACHER) {
            return Decision.ALLOW;
        }
        long wait = pc.other.tryTake(now);
        if (wait > 0) {
            // Báo một lần mỗi giây; báo theo từng frame thì gửi dồn bao nhiêu server trả lại bấy nhiêu.
            if (now - pc.notifiedAt < 1_000) {
                return Decision.DROP;
            }
            pc.notifiedAt = now;
            return Decision.deny(ErrorCode.RATE_LIMITED, (int) wait, "Gửi quá nhiều thông điệp, server tạm bỏ qua");
        }
        return perUser(s, t, now);
    }

    @Override
    public Decision check(Session s, MessageType t) {
        return FREE.contains(t) ? Decision.ALLOW : perUser(s, t, clock.getAsLong());
    }

    private Decision perUser(Session s, MessageType t, long now) {
        if (s == null) {
            return Decision.ALLOW; // chưa đăng nhập: Router tự chặn mọi loại cần đăng nhập
        }
        if (t == MessageType.RUN_REQ && s.role() != Role.TEACHER) {
            long wait = user(s, now).run.tryTake(now);
            if (wait > 0) {
                return Decision.deny(
                        ErrorCode.RATE_LIMITED,
                        (int) wait,
                        "Chạy thử nhanh quá: chờ " + (wait + 999) / 1000 + " s rồi chạy lại (tối đa " + perMinute(run)
                                + " lượt mỗi phút)");
            }
        }
        if (t == MessageType.PREDICT_ANSWER) {
            long wait = user(s, now).predict.tryTake(now);
            if (wait > 0) {
                return Decision.deny(
                        ErrorCode.RATE_LIMITED, (int) wait, "Trả lời nhanh quá: chờ " + (wait + 999) / 1000 + " s");
            }
        }
        return Decision.ALLOW;
    }

    private PerUser user(Session s, long now) {
        return users.computeIfAbsent(
                s.userId(),
                id -> new PerUser(
                        new TokenBucket(run.burst(), run.every(), now),
                        new TokenBucket(predict.burst(), predict.every(), now)));
    }

    private static long perMinute(Limit l) {
        return 60_000 / l.every().toMillis();
    }
}
