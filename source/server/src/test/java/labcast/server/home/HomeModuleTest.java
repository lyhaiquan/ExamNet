package labcast.server.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.Role;
import labcast.server.LabCastServer;
import labcast.server.ServerConfig;
import labcast.server.TestClient;
import labcast.server.account.AccountModule;
import labcast.server.account.UserRepository;
import labcast.server.classroom.ClassRepository;
import labcast.server.store.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HomeModuleTest {
    private static final long DAY = 86_400_000L;
    private Database db;
    private LabCastServer server;

    @BeforeEach
    void batServer() throws Exception {
        db = Database.open(":memory:");
        var cfg = ServerConfig.forTest().withLop("Lớp A").withTaoAdmin("ad:1:Quản trị");
        server = new LabCastServer(cfg, db, List.of(new AccountModule(), new HomeModule()));
        server.start();
        var users = new UserRepository(db);
        long gv = users.create("gv", "Cô A", Role.TEACHER, "1");
        long gv2 = users.create("gv2", "Thầy B", Role.TEACHER, "1");
        var classes = new ClassRepository(db);
        long lopA = classes.ensure("Lớp A", gv);
        long lopB = classes.ensure("Lớp B", gv2);
        long an = users.create("an", "An", Role.STUDENT, "1");
        long binh = users.create("binh", "Bình", Role.STUDENT, "1");
        long cuong = users.create("cuong", "Cường", Role.STUDENT, "1");
        classes.enroll(lopA, an);
        classes.enroll(lopA, binh);
        classes.enroll(lopB, cuong);
        long now = System.currentTimeMillis();
        db.tx(c -> {
            var st = c.createStatement();
            st.executeUpdate("INSERT INTO lessons VALUES ('dsa.sap-xep.a','dsa','sap-xep','Bài A','bai-tap','p','h')");
            st.executeUpdate("INSERT INTO lessons VALUES ('dsa.sap-xep.b','dsa','sap-xep','Bài B','bai-tap','p','h')");
            // An: nộp đạt hôm nay và hôm qua, chạy thử hôm kia; Bình: một bài đạt; Cường (lớp B): một lần nộp
            sub(c, an, "dsa.sap-xep.a", "SUBMIT", "OK", now);
            sub(c, an, "dsa.sap-xep.b", "SUBMIT", "OK", now - DAY);
            sub(c, an, "dsa.sap-xep.a", "RUN", "OK", now - 2 * DAY);
            sub(c, binh, "dsa.sap-xep.a", "SUBMIT", "OK", now - 3 * DAY);
            sub(c, cuong, "dsa.sap-xep.a", "SUBMIT", "WRONG", now - 40 * DAY);
            prog(c, an, "dsa.sap-xep.a", 100, now);
            prog(c, an, "dsa.sap-xep.b", 80, now);
            prog(c, binh, "dsa.sap-xep.a", 100, now);
            return null;
        });
    }

    private static void sub(java.sql.Connection c, long u, String l, String kind, String verdict, long at)
            throws java.sql.SQLException {
        try (var ps = c.prepareStatement("INSERT INTO submissions (user_id, lesson_id, code_seq, kind, verdict,"
                + " created_at) VALUES (?, ?, 1, ?, ?, ?)")) {
            ps.setLong(1, u);
            ps.setString(2, l);
            ps.setString(3, kind);
            ps.setString(4, verdict);
            ps.setLong(5, at);
            ps.executeUpdate();
        }
    }

    private static void prog(java.sql.Connection c, long u, String l, int score, long at) throws java.sql.SQLException {
        try (var ps = c.prepareStatement("INSERT INTO progress (user_id, lesson_id, tier, status, score, updated_at)"
                + " VALUES (?, ?, 'code', 'MASTERED', ?, ?)")) {
            ps.setLong(1, u);
            ps.setString(2, l);
            ps.setInt(3, score);
            ps.setLong(4, at);
            ps.executeUpdate();
        }
    }

    @AfterEach
    void tatServer() throws Exception {
        server.close();
        db.close();
    }

    private JsonNode home(String user) throws Exception {
        try (var c = TestClient.connect(server.port())) {
            assertEquals(MessageType.AUTH_OK, c.login(user, "1").type());
            c.send(MessageType.HOME_REQ, new byte[0]);
            return JsonPayload.tree(c.readUntil(MessageType.HOME_DATA));
        }
    }

    @Test
    void quanTri_thayCaHeThong() throws Exception {
        JsonNode h = home("ad");
        assertEquals(6, h.path("users").path("total").asInt());
        assertEquals(3, h.path("users").path("students").asInt());
        assertEquals(2, h.path("users").path("teachers").asInt());
        assertEquals(2, h.path("classesTotal").asInt());
        assertEquals(4, h.path("submitsTotal").asInt());
        assertEquals(3, h.path("submits30").asInt());
        assertEquals(1, h.path("submitsPrev30").asInt());
        assertEquals(30, h.path("days").size());
        assertEquals(1, h.path("days").get(29).path("submits").asInt(), "hôm nay An nộp một lần");
        assertEquals(2, h.path("classes").size());
        assertTrue(h.path("recent").size() > 0);
    }

    @Test
    void giaoVien_chiThayLopMinh() throws Exception {
        JsonNode h = home("gv");
        assertEquals(1, h.path("classes").size());
        JsonNode lop = h.path("classes").get(0);
        assertEquals("Lớp A", lop.path("name").asText());
        assertEquals(2, lop.path("students").asInt());
        assertEquals(75, lop.path("completion").asInt(), "3 bài đạt / (2 học viên × 2 bài)");
        assertEquals(3, h.path("submits30").asInt(), "không tính bài nộp của lớp B");
        assertFalse(h.has("users"));
        for (JsonNode e : h.path("recent")) assertFalse(e.path("who").asText().equals("Cường"));
    }

    @Test
    void hocVien_chuoiNgay_vaXepHangLop() throws Exception {
        JsonNode h = home("an");
        assertEquals(3, h.path("streak").asInt());
        assertEquals(1, h.path("myRank").asInt());
        assertEquals(2, h.path("classSize").asInt());
        assertEquals("An", h.path("leaderboard").get(0).path("name").asText());
        assertTrue(h.path("leaderboard").get(0).path("me").asBoolean());
        assertEquals(180, h.path("leaderboard").get(0).path("xp").asInt());
        assertFalse(h.has("classes"));
    }

    @Test
    void chuoiNgay_homNayChuaLamThiTinhTuHomQua() {
        LocalDate d = LocalDate.of(2026, 10, 27);
        assertEquals(2, HomeModule.streak(Set.of(d.minusDays(1), d.minusDays(2)), d));
        assertEquals(0, HomeModule.streak(Set.of(d.minusDays(2)), d));
        assertEquals(1, HomeModule.streak(Set.of(d), d));
    }

    @Test
    void ngay_chiaTheoMuiGio() {
        long now = LocalDate.of(2026, 10, 27)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                + 3_600_000;
        var days = HomeModule.days(List.of(new long[] {now, 1}, new long[] {now - DAY, 0}), now, ZoneOffset.UTC);
        assertEquals(30, days.size());
        assertEquals("27/10", days.get(29).get("date"));
        assertEquals(1, days.get(29).get("submits"));
        assertEquals(1, days.get(28).get("runs"));
    }
}
