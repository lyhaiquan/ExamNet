package labcast.server.practice;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Predicate;
import labcast.common.protocol.JsonPayload;
import labcast.server.grade.Grader;
import labcast.server.grade.Verdict;
import labcast.server.lesson.Lesson;
import labcast.server.store.Database;

/**
 * Luật nộp bài của spec §8.4 (phần dùng được khi chưa có bậc dự đoán; đủ luật ở task 3.10). Chủ: Người 3.
 *
 * <ul>
 *   <li>Nộp sai lần thứ 1, 2, 3, 4+ (tính từ lúc chưa qua) → phải chờ 15, 30, 60, 120 s mới nộp lại.
 *   <li>Điểm = 100 − 20 × số lần nộp sai trước lần đúng, thấp nhất 40.
 *   <li>Đã thành thạo (nộp đúng) thì nộp lại vẫn được chấm, nhưng không đổi điểm và không phải chờ.
 *   <li>CHƯA làm "khoá nộp sau 3 lần sai tới khi làm đúng một đề dự đoán": bậc dự đoán chưa có (task 3.10),
 *       khoá vào thì học viên không có cách nào mở.
 * </ul>
 *
 * <p>Thời gian chờ, số lần sai và dòng {@code submissions} ghi trong CÙNG một giao dịch với kết quả chấm.
 */
public final class ProgressService {
    static final long[] COOLDOWN_MS = {15_000, 30_000, 60_000, 120_000};

    public record SubmitOutcome(int score, long cooldownMs, boolean mastered, int wrongSubmits) {}

    private final Database db;

    public ProgressService(Database db) {
        this.db = db;
    }

    /** Còn phải chờ bao nhiêu ms mới được nộp lại (0 = nộp được). */
    public long cooldownLeft(long userId, String lessonId, long now) throws SQLException {
        return db.read(c -> {
            try (PreparedStatement ps =
                    c.prepareStatement("SELECT cooldown_until FROM progress WHERE user_id = ? AND lesson_id = ?")) {
                ps.setLong(1, userId);
                ps.setString(2, lessonId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return 0L;
                    }
                    long until = rs.getLong(1);
                    return rs.wasNull() ? 0L : Math.max(0L, until - now);
                }
            }
        });
    }

    /** Ghi kết quả một lượt nộp đã chấm xong; trả điểm, thời gian chờ mới, đã thành thạo chưa. */
    public SubmitOutcome record(long userId, Lesson l, long codeSeq, Grader.Result res, long now) throws SQLException {
        return record(userId, l, codeSeq, res, now, false);
    }

    public SubmitOutcome record(long userId, Lesson l, long codeSeq, Grader.Result res, long now,
            boolean isolatedSubmission) throws SQLException {
        return db.tx(c -> {
            String status = null;
            int wrong = 0;
            Integer score = null;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT status, wrong_submits, score FROM progress WHERE user_id = ? AND lesson_id = ?")) {
                ps.setLong(1, userId);
                ps.setString(2, l.id());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        status = rs.getString(1);
                        wrong = rs.getInt(2);
                        score = rs.getInt(3);
                        if (rs.wasNull()) {
                            score = null;
                        }
                    }
                }
            }
            SubmitOutcome out;
            if (isolatedSubmission) {
                out = new SubmitOutcome(res.verdict() == Verdict.OK ? 100 : 0, 0,
                        res.verdict() == Verdict.OK, 0);
            } else if ("MASTERED".equals(status)) {
                out = new SubmitOutcome(score == null ? 0 : score, 0, true, wrong);
            } else if (res.verdict() == Verdict.OK) {
                int s = Math.max(40, 100 - 20 * wrong);
                if (!isolatedSubmission) {
                    upsert(c, userId, l.id(), "MASTERED", wrong, s, null, now);
                }
                out = new SubmitOutcome(s, 0, true, wrong);
            } else {
                int w = wrong + 1;
                long cd = COOLDOWN_MS[Math.min(w, COOLDOWN_MS.length) - 1];
                if (!isolatedSubmission) {
                    upsert(c, userId, l.id(), "OPEN", w, null, now + cd, now);
                }
                out = new SubmitOutcome(0, cd, false, w);
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO submissions (user_id, lesson_id, code_seq,"
                    + " kind, verdict, detail_json, score, created_at) VALUES (?, ?, ?, 'SUBMIT', ?, ?, ?, ?)")) {
                ps.setLong(1, userId);
                ps.setString(2, l.id());
                ps.setLong(3, codeSeq);
                ps.setString(4, res.verdict().name());
                ps.setString(5, detail(res));
                if (res.verdict() == Verdict.OK) {
                    ps.setInt(6, out.score());
                } else {
                    ps.setNull(6, java.sql.Types.INTEGER);
                }
                ps.setLong(7, now);
                ps.executeUpdate();
            }
            return out;
        });
    }

    /** Bài trong {@code mo_khoa} có thật trong kho bài (bài kế chưa soạn thì bỏ). */
    public static List<String> unlocked(Lesson l, Predicate<String> exists) {
        return l.moKhoa().stream().filter(exists).toList();
    }

    private static void upsert(
            java.sql.Connection c,
            long userId,
            String lessonId,
            String status,
            int wrong,
            Integer score,
            Long cooldownUntil,
            long now)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO progress (user_id, lesson_id, tier, status,"
                + " wrong_submits, score, cooldown_until, updated_at) VALUES (?, ?, 'code', ?, ?, ?, ?, ?)"
                + " ON CONFLICT (user_id, lesson_id) DO UPDATE SET status = excluded.status,"
                + " wrong_submits = excluded.wrong_submits, score = excluded.score,"
                + " cooldown_until = excluded.cooldown_until, updated_at = excluded.updated_at")) {
            ps.setLong(1, userId);
            ps.setString(2, lessonId);
            ps.setString(3, status);
            ps.setInt(4, wrong);
            if (score == null) {
                ps.setNull(5, java.sql.Types.INTEGER);
            } else {
                ps.setInt(5, score);
            }
            if (cooldownUntil == null) {
                ps.setNull(6, java.sql.Types.INTEGER);
            } else {
                ps.setLong(6, cooldownUntil);
            }
            ps.setLong(7, now);
            ps.executeUpdate();
        }
    }

    private static String detail(Grader.Result res) {
        ObjectNode n = JsonPayload.MAPPER.createObjectNode();
        n.put("message", res.message());
        n.put("failedTest", res.failedTest());
        ArrayNode tests = n.putArray("tests");
        for (Grader.TestResult t : res.tests()) {
            tests.addObject()
                    .put("id", t.id())
                    .put("verdict", t.verdict().name())
                    .put("timeMs", t.timeMs());
        }
        return n.toString();
    }
}
