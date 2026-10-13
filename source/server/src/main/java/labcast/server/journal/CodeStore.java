package labcast.server.journal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.logging.Logger;
import labcast.common.protocol.CodeDelta;
import labcast.server.store.Database;

/**
 * Dòng code của từng (học viên, bài) trên server (ĐG2, spec §17.3).
 *
 * <ul>
 *   <li>Mỗi delta ghi vào {@code code_deltas} bằng {@code INSERT OR IGNORE} với khoá chính {@code
 *       (user_id, lesson_id, seq)}: app gửi lại bao nhiêu lần cũng chỉ một dòng.
 *   <li>Chỉ <b>áp</b> delta khi {@code seq == lastSeq + 1}. Delta đến sớm thì lưu bảng, chờ delta
 *       còn thiếu tới rồi áp tiếp cả chuỗi.
 *   <li>Chữ mới hiện ra quá nhiều cùng lúc thì báo {@link #onLargeInsert} (task 1.14, xem {@link LargeInsert}).
 * </ul>
 */
public final class CodeStore {
    private static final Logger LOG = Logger.getLogger(CodeStore.class.getName());

    public record Snapshot(String code, long seq) {}

    /**
     * Chữ mới hiện ra trong code nhiều hơn người gõ được (task 1.14, spec §8.4): một delta chèn hơn {@link
     * #PASTE_CHARS} ký tự, hoặc cộng dồn hơn ngần ấy ký tự trong {@link #BURST_MS} ms theo giờ app ghi trên
     * delta ({@code burst = true}) — kiểu phần mềm gõ phím tự động gõ thẳng code chép từ ngoài vào, việc mà
     * chặn dán ở app không thấy. Không tính chữ đã có sẵn trong code hoặc vừa bị xoá (chép lại đoạn của chính
     * mình, cắt rồi dán chỗ khác, Ctrl+Z). Chỉ là gợi ý cho giáo viên, không phạt.
     */
    public record LargeInsert(long userId, String lessonId, int chars, boolean burst) {}

    public static final int PASTE_CHARS = 200;
    public static final long BURST_MS = 2_000;
    /** Đoạn ngắn hơn thế thì "đã có trong code" chỉ là trùng ngẫu nhiên, vẫn tính là chữ mới. */
    static final int OWN_TEXT_MIN = 20;

    private record Key(long userId, String lessonId) {}

    private static final class Buffer {
        String code;
        long seq;
        // theo dõi chữ mới hiện ra (task 1.14)
        long burstFrom = Long.MIN_VALUE / 2;
        int burstChars;
        String lastRemoved = "";
    }

    private final Database db;
    private final StarterCode starter;
    private final LongSupplier clock;
    private final Map<Key, Buffer> buffers = new ConcurrentHashMap<>();
    private final List<Consumer<LargeInsert>> largeInsertListeners = new CopyOnWriteArrayList<>();

    public CodeStore(Database db, StarterCode starter, LongSupplier clock) {
        this.db = db;
        this.starter = starter;
        this.clock = clock;
    }

    /** Lưu delta, áp nếu liền mạch; trả {@code seq} liền mạch cuối cùng (dùng cho CODE_ACK). */
    public long accept(long userId, CodeDelta d) throws SQLException {
        Buffer b = buffer(userId, d.lessonId());
        LargeInsert flag = null;
        long last;
        synchronized (b) {
            // delta gửi lại (đã có trong bảng) thì không xét lần nữa
            if (insertIgnore(userId, d) > 0) {
                flag = watch(userId, d, b);
            }
            if (d.seq() == b.seq + 1) {
                applyOne(b, d);
                applyFollowing(userId, d.lessonId(), b);
            }
            last = b.seq;
        }
        if (flag != null) {
            for (Consumer<LargeInsert> l : largeInsertListeners) {
                l.accept(flag);
            }
        }
        return last;
    }

    /** Nghe chữ mới hiện ra quá nhiều cùng lúc. Gọi trên luồng nghiệp vụ đang nhận delta, ngoài khoá. */
    public void onLargeInsert(Consumer<LargeInsert> listener) {
        largeInsertListeners.add(listener);
    }

    /** Gọi trong khoá của {@code b}, TRƯỚC khi áp delta. */
    private LargeInsert watch(long userId, CodeDelta d, Buffer b) {
        String ins = d.insText() == null ? "" : d.insText();
        boolean inOrder =
                d.seq() == b.seq + 1 && d.pos() >= 0 && d.delLen() >= 0 && d.pos() + d.delLen() <= b.code.length();
        String removed = inOrder ? b.code.substring(d.pos(), d.pos() + d.delLen()) : "";
        LargeInsert out = null;
        int n = ins.codePointCount(0, ins.length());
        if (n > 0 && !ownText(b, ins)) {
            if (d.clientTs() < b.burstFrom || d.clientTs() - b.burstFrom > BURST_MS) {
                b.burstFrom = d.clientTs();
                b.burstChars = 0;
            }
            b.burstChars += n;
            if (n > PASTE_CHARS) {
                out = new LargeInsert(userId, d.lessonId(), n, false);
                b.burstChars = 0;
            } else if (b.burstChars > PASTE_CHARS) {
                out = new LargeInsert(userId, d.lessonId(), b.burstChars, true);
                b.burstChars = 0;
            }
        }
        // chỉ nhớ đoạn xoá đủ dài: một lần Backspace không được xoá mất khối vừa cắt
        if (removed.length() >= OWN_TEXT_MIN) {
            b.lastRemoved = removed;
        }
        return out;
    }

    private static boolean ownText(Buffer b, String ins) {
        return ins.length() >= OWN_TEXT_MIN && (b.code.contains(ins) || b.lastRemoved.contains(ins));
    }

    /** Code đầy đủ tại {@code seq} liền mạch cuối. */
    public Snapshot current(long userId, String lessonId) throws SQLException {
        Buffer b = buffer(userId, lessonId);
        synchronized (b) {
            return new Snapshot(b.code, b.seq);
        }
    }

    /** {@code seq} liền mạch cuối; 0 nếu chưa có delta nào. Nối vào {@code SessionManager.setCodeSeqLookup}. */
    public long lastSeq(long userId, String lessonId) {
        try {
            return current(userId, lessonId).seq();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private Buffer buffer(long userId, String lessonId) throws SQLException {
        Key k = new Key(userId, lessonId);
        Buffer b = buffers.get(k);
        if (b != null) {
            return b;
        }
        Buffer fresh = new Buffer();
        fresh.code = starter.of(lessonId);
        fresh.seq = 0;
        // Lần đầu (hoặc sau khi server khởi động lại): dựng lại từ các delta đã lưu.
        // Giai đoạn 2 thêm mốc lưu để không phải áp từ đầu.
        synchronized (fresh) {
            applyFollowing(userId, lessonId, fresh);
        }
        Buffer prev = buffers.putIfAbsent(k, fresh);
        return prev != null ? prev : fresh;
    }

    /** Áp lần lượt các delta đã lưu có seq kế tiếp, dừng ở chỗ còn thiếu. */
    private void applyFollowing(long userId, String lessonId, Buffer b) throws SQLException {
        while (true) {
            List<CodeDelta> next = loadFrom(userId, lessonId, b.seq + 1);
            if (next.isEmpty()) {
                return;
            }
            for (CodeDelta d : next) {
                if (d.seq() != b.seq + 1) {
                    return; // còn lỗ hổng
                }
                applyOne(b, d);
            }
        }
    }

    private void applyOne(Buffer b, CodeDelta d) {
        try {
            b.code = TextOps.apply(b.code, d.pos(), d.delLen(), d.insText());
        } catch (IllegalArgumentException e) {
            // App và server lệch nhau: bỏ delta này để không kẹt cả dòng code.
            // Giai đoạn 2 app tải lại bản chụp (CHECKPOINT_FETCH) khi phát hiện lệch.
            LOG.warning("Bỏ delta không áp được " + d + ": " + e.getMessage());
        }
        b.seq = d.seq();
    }

    /** Trả 1 nếu delta mới, 0 nếu đã có (app gửi lại). */
    private int insertIgnore(long userId, CodeDelta d) throws SQLException {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT OR IGNORE INTO code_deltas"
                    + " (user_id, lesson_id, seq, pos, del_len, ins_text, client_ts, server_ts)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setLong(1, userId);
                ps.setString(2, d.lessonId());
                ps.setLong(3, d.seq());
                ps.setInt(4, d.pos());
                ps.setInt(5, d.delLen());
                ps.setString(6, d.insText());
                ps.setLong(7, d.clientTs());
                ps.setLong(8, clock.getAsLong());
                return ps.executeUpdate();
            }
        });
    }

    private List<CodeDelta> loadFrom(long userId, String lessonId, long fromSeq) throws SQLException {
        return db.read(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT seq, pos, del_len, ins_text, client_ts"
                    + " FROM code_deltas WHERE user_id = ? AND lesson_id = ? AND seq >= ?"
                    + " ORDER BY seq LIMIT 500")) {
                ps.setLong(1, userId);
                ps.setString(2, lessonId);
                ps.setLong(3, fromSeq);
                List<CodeDelta> out = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(new CodeDelta(
                                lessonId, rs.getLong(1), rs.getInt(2), rs.getInt(3), rs.getString(4), rs.getLong(5)));
                    }
                }
                return out;
            }
        });
    }
}
