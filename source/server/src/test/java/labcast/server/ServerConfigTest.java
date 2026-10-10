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

    @Test
    void thuMucRunner_canhThuMucContent() {
        var c = ServerConfig.defaults().withContent(Path.of("/repo/content"));
        assertEquals(Path.of("/repo/runner").toAbsolutePath(), c.runnerDir());
    }
}
