package labcast.server.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import labcast.common.protocol.CodeDelta;
import labcast.common.protocol.Role;
import labcast.server.account.UserRepository;
import labcast.server.store.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CodeStoreTest {
    private static final String BAI = "dsa.sap-xep.noi-bot";
    private Database db;
    private long sv;

    @BeforeEach
    void mo() throws Exception {
        db = Database.open(":memory:");
        sv = new UserRepository(db).create("sv", "SV", Role.STUDENT, "x");
        db.read(c -> c.createStatement()
                .executeUpdate("INSERT INTO lessons (id, mon, chuong, ten, loai, path, content_hash) VALUES ('" + BAI
                        + "', 'dsa', 'sap-xep', 'Nổi bọt', 'giang-giai', 'x', 'h')"));
    }

    @AfterEach
    void dong() throws SQLException {
        db.close();
    }

    private CodeStore store() {
        return new CodeStore(db, id -> "# khung\n", () -> 0L);
    }

    private static CodeDelta chen(long seq, int pos, String s) {
        return new CodeDelta(BAI, seq, pos, 0, s, 0);
    }

    @Test
    void apTheoThuTu_codeDung() throws Exception {
        var s = store();
        assertEquals(1, s.accept(sv, chen(1, 8, "a")));
        assertEquals(2, s.accept(sv, chen(2, 9, "b")));
        assertEquals(3, s.accept(sv, chen(3, 10, "c")));
        assertEquals(new CodeStore.Snapshot("# khung\nabc", 3), s.current(sv, BAI));
        assertEquals(3, s.lastSeq(sv, BAI));
    }

    @Test
    void guiTrung_boQuaAckVanGiu() throws Exception {
        var s = store();
        s.accept(sv, chen(1, 8, "a"));
        s.accept(sv, chen(2, 9, "b"));
        s.accept(sv, chen(3, 10, "c"));
        assertEquals(3, s.accept(sv, chen(2, 9, "b")));
        assertEquals("# khung\nabc", s.current(sv, BAI).code());
    }

    @Test
    void denSom_choDuRoiApCaChuoi() throws Exception {
        var s = store();
        s.accept(sv, chen(1, 8, "1"));
        s.accept(sv, chen(2, 9, "2"));
        s.accept(sv, chen(3, 10, "3"));
        assertEquals(3, s.accept(sv, chen(5, 12, "5")));
        assertEquals("# khung\n123", s.current(sv, BAI).code());
        assertEquals(5, s.accept(sv, chen(4, 11, "4")));
        assertEquals("# khung\n12345", s.current(sv, BAI).code());
    }

    @Test
    void storeMoiCungDatabase_dungLaiDung() throws Exception {
        var s = store();
        s.accept(sv, chen(1, 8, "x = 1"));
        s.accept(sv, new CodeDelta(BAI, 2, 12, 1, "2", 0)); // thay "1" bằng "2"
        var moi = store();
        assertEquals(new CodeStore.Snapshot("# khung\nx = 2", 2), moi.current(sv, BAI));
    }

    @Test
    void chuaCoDelta_traKhung() throws Exception {
        assertEquals(new CodeStore.Snapshot("# khung\n", 0), store().current(sv, BAI));
    }

    @Test
    void deltaLech_boQuaKhongKetDong() throws Exception {
        var s = store();
        s.accept(sv, new CodeDelta(BAI, 1, 999, 0, "x", 0)); // ngoài phạm vi
        assertEquals(2, s.accept(sv, chen(2, 0, "y")));
        assertEquals("y# khung\n", s.current(sv, BAI).code());
    }
}
