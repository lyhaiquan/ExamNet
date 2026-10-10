package labcast.server.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import labcast.common.protocol.Role;
import labcast.server.classroom.ClassRepository;
import labcast.server.store.Database;
import org.junit.jupiter.api.Test;

class RosterImporterTest {

    @Test
    void baDong_taoBaHocVien_boDongTrungVaThieuCot() throws Exception {
        try (Database db = Database.open(":memory:")) {
            var users = new UserRepository(db);
            var classes = new ClassRepository(db);
            long gv = users.create("gv", "Giáo viên", Role.TEACHER, "x");
            long lop = classes.ensure("Lớp thử", gv);
            String csv =
                    """
                    username,họ tên,mật khẩu
                    sv1,Nguyễn Văn A,123
                    sv2,Trần Thị B,456
                    sv1,Trùng tên,789
                    sv3,Lê Văn C,000
                    thieu,cot
                    """;
            var r = new RosterImporter(users, classes).importCsv(csv, lop);
            assertEquals(3, r.created());
            assertEquals(2, r.skipped().size());
            assertTrue(r.skipped().get(0).contains("sv1"));
            assertTrue(r.message().contains("Đã tạo 3"));
            var sv2 = users.byUsername("sv2").orElseThrow();
            assertEquals("Trần Thị B", sv2.fullName());
            assertEquals(Role.STUDENT, sv2.role());
            assertTrue(PasswordHasher.verify("456", sv2.password()));
            assertEquals(lop, classes.classOf(sv2.id()));
        }
    }

    @Test
    void tenDaCoTrongDatabase_biBo() throws Exception {
        try (Database db = Database.open(":memory:")) {
            var users = new UserRepository(db);
            users.create("sv1", "Có rồi", Role.STUDENT, "x");
            var r = new RosterImporter(users, new ClassRepository(db)).importCsv("sv1,Mới,y\n", 0);
            assertEquals(0, r.created());
            assertEquals(1, r.skipped().size());
        }
    }
}
