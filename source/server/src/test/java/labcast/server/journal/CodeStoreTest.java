package labcast.server.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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

    // ---- Dán khối lớn (task 1.14) ----

    /** Chữ "mới" dài n ký tự: chữ cái ngẫu nhiên theo hạt giống, không trùng với code đang có. */
    private static String moi(int n, long seed) {
        var r = new java.util.Random(seed);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append((char) ('a' + r.nextInt(26)));
        }
        return sb.toString();
    }

    private static CodeDelta luc(long seq, int pos, int del, String s, long clientTs) {
        return new CodeDelta(BAI, seq, pos, del, s, clientTs);
    }

    private List<CodeStore.LargeInsert> nghe(CodeStore s) {
        List<CodeStore.LargeInsert> co = new ArrayList<>();
        s.onLargeInsert(co::add);
        return co;
    }

    @Test
    void chen201KyTu_coCo_chen200_khong() throws Exception {
        var s = store();
        var co = nghe(s);
        s.accept(sv, chen(1, 8, moi(200, 1)));
        assertEquals(0, co.size(), "200 ký tự chưa tính");
        s.accept(sv, luc(2, 8, 0, moi(201, 2), 10_000));
        assertEquals(1, co.size());
        assertEquals(201, co.get(0).chars());
        assertFalse(co.get(0).burst());
        assertEquals(BAI, co.get(0).lessonId());
    }

    @Test
    void demTheoKyTuUnicode_khongTheoDoDaiChuoi() throws Exception {
        var s = store();
        var co = nghe(s);
        // 150 ký tự có dấu và emoji: chuỗi Java dài hơn 200 nhưng chỉ 150 ký tự
        s.accept(sv, chen(1, 8, "😀".repeat(75) + "ệ".repeat(75)));
        assertEquals(0, co.size());
    }

    @Test
    void appGuiLaiDeltaCu_khongCoThem() throws Exception {
        var s = store();
        var co = nghe(s);
        String khoi = moi(300, 3);
        s.accept(sv, chen(1, 8, khoi));
        s.accept(sv, chen(1, 8, khoi)); // gửi lại sau khi nối lại
        assertEquals(1, co.size());
    }

    @Test
    void chepLaiDoanCuaChinhMinh_khongCo() throws Exception {
        var s = store();
        var co = nghe(s);
        // gõ dần 300 ký tự (mỗi delta 10 ký tự, cách nhau 1 s theo giờ app)
        StringBuilder all = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            String phan = moi(10, 100 + i);
            s.accept(sv, luc(i + 1, 8 + all.length(), 0, phan, i * 1_000L));
            all.append(phan);
        }
        assertEquals(0, co.size(), "gõ tay không bị đánh dấu");
        // Ctrl+C rồi Ctrl+V cả khối 300 ký tự vào cuối
        s.accept(sv, luc(31, 8 + all.length(), 0, all.toString(), 40_000));
        assertEquals(0, co.size(), "dán lại đoạn đã có trong code");
    }

    @Test
    void catRoiDanChoKhac_vanKhongCo_keCaCoBackspaceOGiua() throws Exception {
        // code đang có một khối 250 ký tự của chính học viên (ở đây cho sẵn trong khung cho gọn)
        String khoi = moi(250, 4);
        var s = new CodeStore(db, id -> "# khung\n" + khoi, () -> 0L);
        var co = nghe(s);
        s.accept(sv, luc(1, 8, khoi.length(), "", 50_000)); // Ctrl+X cả khối
        s.accept(sv, luc(2, 7, 1, "", 50_100)); // Backspace một ký tự
        s.accept(sv, luc(3, 0, 0, khoi, 51_000)); // Ctrl+V lên đầu
        assertEquals(khoi + "# khung", s.current(sv, BAI).code());
        assertEquals(0, co.size());
    }

    @Test
    void phanMemGoTuDong_nhieuDeltaNho_trong2Giay_coCo() throws Exception {
        var s = store();
        var co = nghe(s);
        int pos = 8;
        // 3 delta × 80 ký tự trong 200 ms theo giờ app: nhanh hơn người gõ nhiều lần
        for (int i = 0; i < 3; i++) {
            String phan = moi(80, 10 + i);
            s.accept(sv, luc(i + 1, pos, 0, phan, 1_000 + i * 100L));
            pos += phan.length();
        }
        assertEquals(1, co.size());
        assertTrue(co.get(0).burst());
        assertEquals(240, co.get(0).chars());
    }

    @Test
    void cung240KyTu_nhungRaiTrongNhieuGiay_khongCo() throws Exception {
        var s = store();
        var co = nghe(s);
        int pos = 8;
        for (int i = 0; i < 3; i++) {
            String phan = moi(80, 20 + i);
            s.accept(sv, luc(i + 1, pos, 0, phan, i * 2_500L));
            pos += phan.length();
        }
        assertEquals(0, co.size());
    }

    @Test
    void appGuiLaiNhieuDeltaSauKhiMatMang_tinhTheoGioGo_khongTheoGioToi() throws Exception {
        var s = store();
        var co = nghe(s);
        int pos = 8;
        // 40 delta × 10 ký tự gõ trong 40 s lúc mất mạng, tới server cùng một lúc khi nối lại
        for (int i = 0; i < 40; i++) {
            String phan = moi(10, 200 + i);
            s.accept(sv, luc(i + 1, pos, 0, phan, i * 1_000L));
            pos += phan.length();
        }
        assertEquals(0, co.size());
    }
}
