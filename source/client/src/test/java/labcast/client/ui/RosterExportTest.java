package labcast.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RosterExportTest {
    private static final List<List<String>> DS = List.of(
            List.of("Mã SV", "Họ và tên", "Ngày sinh"),
            List.of("B01", "Nguyễn An", "05/08/2004"),
            List.of("B02", "Trần Bình", "03/01/2004"),
            List.of("B02", "Trùng mã", "04/01/2004"),
            List.of("B03", "Lê <Cường> & Co", "2004-12-21"));

    @Test
    void fileTaiKhoan_moiCoMatKhau_cuGhiChu_loiKhongCo() {
        var es = RosterSheet.entries(DS, RosterSheet.guess(DS));
        var lines = RosterSheet.accountSheet(
                "DSA K23", es, Set.of("B01", "B03"), Set.of("B02"), LocalDateTime.of(2026, 10, 28, 9, 5));
        assertTrue(lines.get(0).cells().get(0).contains("DSA K23"));
        assertTrue(lines.get(1).cells().get(0).startsWith("Xuất lúc 09:05 28/10/2026"));
        int header = 5;
        assertEquals("Mật khẩu tạm", lines.get(header).cells().get(4));
        var rows = lines.subList(header + 1, lines.size());
        assertEquals(3, rows.size()); // dòng trùng mã không có
        assertEquals(
                List.of("1", "B01", "Nguyễn An", "05/08/2004", "05082004", "Đổi mật khẩu ở lần đăng nhập đầu"),
                rows.get(0).cells());
        assertEquals("", rows.get(1).cells().get(4), "tài khoản có sẵn giữ mật khẩu cũ: không ghi ngày sinh");
        assertTrue(rows.get(1).cells().get(5).contains("mật khẩu cũ"));
        assertEquals("3", rows.get(2).cells().get(0));
    }

    @Test
    void ghiXlsx_docLaiDuoc_giuSo0DauVaKyTuDacBiet(@TempDir Path dir) throws Exception {
        var es = RosterSheet.entries(DS, RosterSheet.guess(DS));
        Path f = dir.resolve("tk.xlsx");
        RosterImportDialog.export(f, "DSA K23", es, Set.of("B01", "B03"), Set.of("B02"));
        var back = XlsxReader.firstSheet(f);
        assertEquals("Tài khoản LabCast - lớp DSA K23", back.get(0).get(0));
        assertEquals(List.of(), back.get(4));
        assertEquals("05082004", back.get(6).get(4));
        assertEquals("Lê <Cường> & Co", back.get(8).get(2));
        assertFalse(
                Files.list(dir).anyMatch(p -> p.getFileName().toString().startsWith(".labcast-")),
                "không sót file tạm");
    }

    @Test
    void tenFile_khongDau() {
        assertEquals("tai-khoan-DSA-Nang-cao-K23.xlsx", RosterImportDialog.fileName("DSA Nâng cao - K23"));
        assertEquals("tai-khoan-Lop-Dien-tu.xlsx", RosterImportDialog.fileName("Lớp Điện tử"));
        assertEquals("tai-khoan-lop.xlsx", RosterImportDialog.fileName("***"));
    }

    @Test
    void thoatXml_boKyTuDieuKhien() {
        assertEquals("a&amp;b&lt;c&gt;&quot;d", XlsxWriter.esc("a&b<c>\"d\u0001"));
    }
}
