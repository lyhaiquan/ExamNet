package labcast.server.store;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bảng {@code audit_log}: đăng nhập đúng/sai, frame hỏng, bị đá do đăng nhập nơi khác, lệnh quản trị.
 * Ghi lỗi thì chỉ log, không làm hỏng việc đang xử lý.
 */
public final class AuditLog {
    private static final Logger LOG = Logger.getLogger(AuditLog.class.getName());

    private final Database db;

    public AuditLog(Database db) {
        this.db = db;
    }

    public void record(String actor, String action, String detail) {
        try {
            db.read(c -> {
                try (PreparedStatement ps =
                        c.prepareStatement("INSERT INTO audit_log (ts, actor, action, detail) VALUES (?, ?, ?, ?)")) {
                    ps.setLong(1, System.currentTimeMillis());
                    ps.setString(2, actor);
                    ps.setString(3, action);
                    ps.setString(4, detail);
                    return ps.executeUpdate();
                }
            });
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Không ghi được audit_log", e);
        }
    }
}
