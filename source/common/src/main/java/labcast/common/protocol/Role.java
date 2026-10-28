package labcast.common.protocol;

import java.util.Set;

/** Vai trò tài khoản. Trên dây gửi bằng {@link #name()}: thêm vai trò mới thì thêm cuối, không đổi tên cũ. */
public enum Role {
    TEACHER,
    STUDENT,
    /** Quản trị: tạo lớp, tài khoản giáo viên; làm được mọi việc quản lý của giáo viên. */
    ADMIN;

    /** Vai trò được làm việc quản lý (lớp, bài, danh sách học viên): giáo viên và quản trị. */
    public static final Set<Role> STAFF = Set.of(TEACHER, ADMIN);

    public boolean isStaff() {
        return this != STUDENT;
    }
}
