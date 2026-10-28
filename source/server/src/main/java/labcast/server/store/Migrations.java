package labcast.server.store;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Đưa DB của bản trước lên lược đồ hiện tại. Số phiên bản lưu trong {@code PRAGMA user_version}.
 *
 * <p>DB mới tạo không chạy bước nào: {@code schema.sql} đã là lược đồ mới nhất, chỉ đặt {@code user_version =
 * LATEST}. DB đã có thì chạy các bước còn thiếu <b>trước</b> {@code schema.sql}, rồi {@code schema.sql} thêm
 * bảng mới (mọi câu trong đó đều {@code IF NOT EXISTS}).
 *
 * <p>Thêm bước: tăng {@link #LATEST}, thêm một {@link Step} cuối danh sách, sửa {@code schema.sql} cho DB mới,
 * thêm trường hợp vào {@code MigrationsTest}. Không sửa bước đã phát hành.
 */
public final class Migrations {

    /** Phiên bản lược đồ của bản này. 0: DB tạo trước khi có migration (trước giai đoạn 6). */
    public static final int LATEST = 4;

    /** Một bước nâng từ {@code version - 1} lên {@code version}. */
    private record Step(int version, String name, boolean foreignKeysOff, Database.SqlWork<Void> work) {}

    private static final List<Step> STEPS = List.of(
            new Step(1, "users nhận vai trò ADMIN", true, Migrations::usersWithAdminRole),
            new Step(2, "users có cờ phải đổi mật khẩu", false, Migrations::usersMustChangePassword),
            new Step(3, "users có cờ bị khoá", false, Migrations::usersDisabled),
            new Step(4, "users giữ mật khẩu tạm tới khi tự đổi", false, Migrations::usersTempPassword));

    private Migrations() {}

    /**
     * Chạy trước {@code schema.sql}. {@code fresh}: DB vừa tạo, chưa có bảng nào.
     *
     * @throws SQLException DB do bản LabCast mới hơn tạo ({@code user_version > LATEST}), hoặc một bước lỗi
     *     (bước đó được ROLLBACK, DB giữ nguyên phiên bản trước nó)
     */
    static void upgrade(Connection c, boolean fresh) throws SQLException {
        int version = userVersion(c);
        if (version > LATEST) {
            throw new SQLException("DB này do bản LabCast mới hơn tạo (phiên bản lược đồ " + version + ", bản này chỉ"
                    + " hiểu tới " + LATEST + "). Dùng bản LabCast mới hơn để mở.");
        }
        if (fresh) {
            return;
        }
        for (Step s : STEPS) {
            if (s.version() > version) {
                apply(c, s);
            }
        }
    }

    /** Chạy sau {@code schema.sql}: DB mới cũng mang số phiên bản mới nhất. */
    static void markLatest(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA user_version = " + LATEST);
        }
    }

    private static void apply(Connection c, Step s) throws SQLException {
        // Dựng lại bảng có khoá ngoại trỏ tới phải tắt khoá ngoại, mà chỉ tắt được ngoài giao dịch
        // (quy trình 12 bước ở sqlite.org/lang_altertable.html)
        try (Statement st = c.createStatement()) {
            if (s.foreignKeysOff()) {
                st.execute("PRAGMA foreign_keys = OFF");
            }
            c.setAutoCommit(false);
            try {
                s.work().run(c);
                try (ResultSet rs = st.executeQuery("PRAGMA foreign_key_check")) {
                    if (rs.next()) {
                        throw new SQLException(
                                "Migration \"" + s.name() + "\" làm hỏng khoá ngoại ở bảng " + rs.getString(1));
                    }
                }
                st.execute("PRAGMA user_version = " + s.version());
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
                if (s.foreignKeysOff()) {
                    st.execute("PRAGMA foreign_keys = ON");
                }
            }
        }
    }

    private static int userVersion(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Mật khẩu tạm (giáo viên đặt lại, nhập từ CSV): lần đăng nhập sau phải tự đặt mật khẩu của mình. */
    private static Void usersMustChangePassword(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("ALTER TABLE users ADD COLUMN must_change_pwd INTEGER NOT NULL DEFAULT 0");
        }
        return null;
    }

    /** Quản trị khoá tài khoản: đăng nhập bị từ chối, dữ liệu bài làm vẫn giữ. */
    private static Void usersDisabled(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("ALTER TABLE users ADD COLUMN disabled INTEGER NOT NULL DEFAULT 0");
        }
        return null;
    }

    /**
     * Mật khẩu tạm (nhập lớp, quản trị tạo, đặt lại) giữ dạng chữ tới khi người dùng tự đặt mật khẩu riêng, để quản
     * trị xuất lại file tài khoản gửi giáo viên bất cứ lúc nào. Tự đặt xong thì xoá.
     */
    private static Void usersTempPassword(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("ALTER TABLE users ADD COLUMN temp_password TEXT");
        }
        return null;
    }

    /** SQLite không sửa được {@code CHECK} của cột: tạo bảng mới, chép dữ liệu, xoá bảng cũ, đổi tên. */
    private static Void usersWithAdminRole(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE users_moi ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " username TEXT NOT NULL UNIQUE,"
                    + " full_name TEXT NOT NULL,"
                    + " role TEXT NOT NULL CHECK (role IN ('ADMIN', 'TEACHER', 'STUDENT')),"
                    + " pwd_hash BLOB NOT NULL,"
                    + " salt BLOB NOT NULL,"
                    + " iterations INTEGER NOT NULL,"
                    + " created_at INTEGER NOT NULL)");
            st.execute("INSERT INTO users_moi (id, username, full_name, role, pwd_hash, salt, iterations, created_at)"
                    + " SELECT id, username, full_name, role, pwd_hash, salt, iterations, created_at FROM users");
            st.execute("DROP TABLE users");
            st.execute("ALTER TABLE users_moi RENAME TO users");
        }
        return null;
    }
}
