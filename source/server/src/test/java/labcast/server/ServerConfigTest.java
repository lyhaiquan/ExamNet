package labcast.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ServerConfigTest {

    @Test
    void macDinh() {
        var c = ServerConfig.parse(new String[0], Map.of());
        assertEquals(7000, c.port());
        assertEquals("labcast.db", c.db());
        assertEquals(Path.of("content"), c.content());
        assertFalse(c.khongSandbox());
    }

    @Test
    void docDungTungThamSo() {
        var c = ServerConfig.parse(
                new String[] {
                    "--port", "7100",
                    "--db", "x.db",
                    "--content", "../content",
                    "--lop", "D22CQCN01",
                    "--lan", "Ethernet",
                    "--tao-giao-vien", "gv:mk:Cô Lan",
                    "--khong-sandbox"
                },
                Map.of());
        assertEquals(7100, c.port());
        assertEquals("x.db", c.db());
        assertEquals(Path.of("../content"), c.content());
        assertEquals("D22CQCN01", c.lop());
        assertEquals("Ethernet", c.lan());
        assertEquals("gv:mk:Cô Lan", c.taoGiaoVien());
        assertTrue(c.khongSandbox());
    }

    @Test
    void bienMoiTruong_thayMacDinh_nhungThuaThamSo() {
        assertEquals(
                "/data/labcast.db",
                ServerConfig.parse(new String[0], Map.of("LABCAST_DB", "/data/labcast.db"))
                        .db());
        assertEquals(
                "x.db",
                ServerConfig.parse(new String[] {"--db", "x.db"}, Map.of("LABCAST_DB", "/data/labcast.db"))
                        .db());
    }

    @Test
    void thamSoLa_thongBaoDeHieu() {
        var e = assertThrows(
                IllegalArgumentException.class, () -> ServerConfig.parse(new String[] {"--cong", "1"}, Map.of()));
        assertTrue(e.getMessage().contains("--cong"));
        assertTrue(e.getMessage().contains("Cách dùng"));
    }

    @Test
    void thieuGiaTri_hoacCongSai() {
        assertThrows(IllegalArgumentException.class, () -> ServerConfig.parse(new String[] {"--port"}, Map.of()));
        assertThrows(
                IllegalArgumentException.class, () -> ServerConfig.parse(new String[] {"--port", "abc"}, Map.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> ServerConfig.parse(new String[] {"--lop", "--khong-sandbox"}, Map.of()));
    }

    /** File cấu hình đọc giả: tên file → nội dung. */
    static ServerConfig parse(String[] args, Map<String, String> env, boolean lossy, Map<String, String> files) {
        return ServerConfig.parse(args, env, lossy, p -> {
            String f = files.get(p.toString());
            if (f == null) {
                throw new IllegalArgumentException("Không đọc được file cấu hình " + p);
            }
            return f;
        });
    }

    @Test
    void fileCauHinh_giuNguyenTiengViet() {
        var c = parse(
                new String[] {"--cau-hinh", "lop.conf"},
                Map.of(),
                true,
                Map.of(
                        "lop.conf",
                        "\uFEFF# cấu hình lớp thứ Ba\n"
                                + "lop = Lớp D22CQCN01 — Cô Nguyễn Thị Lan\n"
                                + "\n"
                                + "tao-giao-vien = gv:mật khẩu ư:Nguyễn Thị Lan\n"
                                + "content = \"../nội dung\"\n"
                                + "khong-sandbox = true\n"
                                + "port = 7100\n"));
        assertEquals("Lớp D22CQCN01 — Cô Nguyễn Thị Lan", c.lop());
        assertEquals("gv:mật khẩu ư:Nguyễn Thị Lan", c.taoGiaoVien());
        assertEquals(Path.of("../nội dung"), c.content());
        assertTrue(c.khongSandbox());
        assertEquals(7100, c.port());
    }

    @Test
    void thuTuThang_dongLenh_file_bienMoiTruong_macDinh() {
        Map<String, String> env = Map.of("LABCAST_LOP", "Lớp từ biến", "LABCAST_LAN", "Wi-Fi", "LABCAST_DB", "env.db");
        Map<String, String> files = Map.of("a.conf", "lop = Lớp từ file\ndb = file.db\n");
        assertEquals("Lớp từ biến", parse(new String[0], env, true, files).lop());
        var c = parse(new String[] {"--cau-hinh", "a.conf"}, env, true, files);
        assertEquals("Lớp từ file", c.lop());
        assertEquals("file.db", c.db());
        assertEquals("Wi-Fi", c.lan(), "file không ghi thì vẫn lấy biến môi trường");
        assertEquals(
                "D22",
                parse(new String[] {"--cau-hinh", "a.conf", "--lop", "D22"}, env, true, files)
                        .lop());
    }

    @Test
    void bienMoiTruong_taoGiaoVienVaThuMucBai() {
        var c = ServerConfig.parse(
                new String[0],
                Map.of("LABCAST_TAO_GIAO_VIEN", "gv:mk:Lê Thị Ưng", "LABCAST_CONTENT", "D:/Bài học"),
                true,
                p -> "");
        assertEquals("gv:mk:Lê Thị Ưng", c.taoGiaoVien());
        assertEquals(Path.of("D:/Bài học"), c.content());
    }

    /** Windows đọc tham số theo Cp1252: "Lớp" tới Java thành "L?p". Phải dừng, chỉ cách sửa. */
    @Test
    void thamSoDaMatChuTiengViet_dungLaiVaChiCachSua() {
        var e = assertThrows(
                IllegalArgumentException.class,
                () -> ServerConfig.parse(new String[] {"--lop", "L?p D22CQCN01"}, Map.of(), true, p -> ""));
        assertTrue(e.getMessage().contains("--lop"), e.getMessage());
        assertTrue(e.getMessage().contains("--cau-hinh"), e.getMessage());
        assertTrue(e.getMessage().contains("LABCAST_LOP"), e.getMessage());
        assertThrows(
                IllegalArgumentException.class,
                () -> ServerConfig.parse(
                        new String[] {"--tao-giao-vien", "gv:mk:Nguy?n Th? Lan"}, Map.of(), true, p -> ""));
        // máy đọc tham số bằng UTF-8 (Linux, Windows bật UTF-8 toàn hệ thống) thì '?' là chữ thật
        assertEquals(
                "Ai?",
                ServerConfig.parse(new String[] {"--lop", "Ai?"}, Map.of(), false, p -> "")
                        .lop());
    }

    @Test
    void fileCauHinhCoKhoaLa_baoDong() {
        var e = assertThrows(
                IllegalArgumentException.class,
                () -> parse(
                        new String[] {"--cau-hinh", "x.conf"},
                        Map.of(),
                        true,
                        Map.of("x.conf", "lop = A\ncong = 1\n")));
        assertTrue(e.getMessage().contains("dòng 2"), e.getMessage());
        assertTrue(e.getMessage().contains("cong"), e.getMessage());
    }

    /** Mỗi lớp một môn: server lớp DSA chỉ nạp bài DSA, server lớp SQL chỉ nạp bài SQL. */
    @Test
    void mon_chiNhanDsaHoacSql_khongGhiThiNapCaHai() {
        assertEquals(null, ServerConfig.parse(new String[0], Map.of()).mon());
        assertEquals(
                "sql",
                ServerConfig.parse(new String[] {"--mon", "sql"}, Map.of()).mon());
        assertEquals(
                "dsa",
                ServerConfig.parse(new String[] {"--mon", "DSA"}, Map.of()).mon());
        assertEquals(
                "dsa",
                ServerConfig.parse(new String[0], Map.of("LABCAST_MON", "dsa")).mon());
        assertEquals(
                "sql",
                parse(new String[] {"--cau-hinh", "a.conf"}, Map.of(), true, Map.of("a.conf", "mon = sql\n"))
                        .mon());
        var e = assertThrows(
                IllegalArgumentException.class, () -> ServerConfig.parse(new String[] {"--mon", "java"}, Map.of()));
        assertTrue(e.getMessage().contains("dsa") && e.getMessage().contains("sql"), e.getMessage());
    }

    /** Hai lớp trên cùng một máy: mỗi server một cổng app, một cổng dashboard, một database. */
    @Test
    void portDashboard_doiDuoc() {
        assertEquals(9000, ServerConfig.parse(new String[0], Map.of()).statusPort());
        assertEquals(
                8081,
                ServerConfig.parse(new String[] {"--port-dashboard", "8081"}, Map.of())
                        .statusPort());
        assertEquals(
                8082,
                parse(
                                new String[] {"--cau-hinh", "a.conf"},
                                Map.of(),
                                true,
                                Map.of("a.conf", "port-dashboard = 8082\n"))
                        .statusPort());
        assertThrows(
                IllegalArgumentException.class,
                () -> ServerConfig.parse(new String[] {"--port-dashboard", "x"}, Map.of()));
    }

    @Test
    void thuMucRunner_canhThuMucContent() {
        var c = ServerConfig.defaults().withContent(Path.of("/repo/content"));
        assertEquals(Path.of("/repo/runner").toAbsolutePath(), c.runnerDir());
    }
}
