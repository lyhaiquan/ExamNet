package labcast.server.account;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import labcast.common.protocol.Role;
import labcast.server.classroom.ClassRepository;

/** Nhập danh sách lớp từ CSV, mỗi dòng {@code username,họ tên,mật khẩu}. */
public final class RosterImporter {

    /** {@code createdNames}: tài khoản mới (mật khẩu là mật khẩu trong CSV); {@code enrolledNames}: đã có từ trước. */
    public record Result(List<String> createdNames, List<String> enrolledNames, List<String> skipped) {
        public int created() {
            return createdNames.size();
        }

        public int enrolledExisting() {
            return enrolledNames.size();
        }

        public String message() {
            String s = "Đã tạo " + created() + " học viên, ghi danh " + enrolledExisting() + " tài khoản đã có";
            return skipped.isEmpty() ? s : s + "; bỏ qua " + skipped.size() + " dòng: " + String.join("; ", skipped);
        }
    }

    private final UserRepository users;
    private final ClassRepository classes;

    public RosterImporter(UserRepository users, ClassRepository classes) {
        this.users = users;
        this.classes = classes;
    }

    /** Dòng thiếu cột, trùng tên đăng nhập (trong file hoặc đã có) thì bỏ và ghi lý do. */
    public Result importCsv(String csv, long classId) throws SQLException {
        List<String> created = new ArrayList<>();
        List<String> enrolled = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String[] lines = csv.split("\r?\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (line.isEmpty() || (i == 0 && line.toLowerCase().startsWith("username"))) {
                continue;
            }
            String[] cols = line.split(",", -1);
            int lineNo = i + 1;
            if (cols.length != 3 || cols[0].isBlank() || cols[1].isBlank() || cols[2].isBlank()) {
                skipped.add("dòng " + lineNo + " phải có đủ 3 cột");
                continue;
            }
            String username = cols[0].strip();
            if (!seen.add(username)) {
                skipped.add("dòng " + lineNo + " trùng trong CSV: " + username);
                continue;
            }
            var existing = users.byUsername(username);

            if (existing.isPresent()) {
                if (existing.get().role() != Role.STUDENT) {
                    skipped.add("dòng " + lineNo + " tài khoản không phải sinh viên: " + username);
                    continue;
                }

                if (classId <= 0) {
                    skipped.add("dòng " + lineNo + " tài khoản đã tồn tại: " + username);
                    continue;
                }

                classes.enroll(classId, existing.get().id());
                enrolled.add(existing.get().username());
                continue;
            }
            long id = users.create(username, cols[1].strip(), Role.STUDENT, cols[2].strip());
            users.setMustChangePassword(id, true); // mật khẩu trong CSV là mật khẩu tạm
            if (classId > 0) {
                classes.enroll(classId, id);
            }
            created.add(username);
        }
        return new Result(created, enrolled, skipped);
    }
}
