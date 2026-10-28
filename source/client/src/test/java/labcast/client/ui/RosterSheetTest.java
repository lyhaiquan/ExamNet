package labcast.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RosterSheetTest {

    @Test
    void ngaySinh_moiKieuGhiThuongGap() {
        LocalDate d = LocalDate.of(2004, 8, 5);
        assertEquals(d, RosterSheet.date("38204")); // ô định dạng ngày: số ngày kiểu Excel
        assertEquals(d, RosterSheet.date("38204.0"));
        assertEquals(d, RosterSheet.date("05/08/2004"));
        assertEquals(d, RosterSheet.date("5/8/2004"));
        assertEquals(d, RosterSheet.date("05-08-2004"));
        assertEquals(d, RosterSheet.date("05.08.2004"));
        assertEquals(d, RosterSheet.date("2004-08-05"));
        assertEquals(d, RosterSheet.date("05/08/2004 00:00:00"));
        assertEquals(d, RosterSheet.date("05082004"));
        assertEquals(d, RosterSheet.date("5082004")); // Excel coi là số, mất số 0 đầu
        assertEquals(d, RosterSheet.date("20040805"));
        assertEquals(d, RosterSheet.date("05/08/04"));
        assertEquals(LocalDate.of(1999, 12, 31), RosterSheet.date("31/12/99"));
        assertNull(RosterSheet.date("31/02/2004"));
        assertNull(RosterSheet.date("chưa có"));
        assertNull(RosterSheet.date(""));
        assertNull(RosterSheet.date("01/01/2999"));
    }

    @Test
    void maSv_soExcelDoiVeChuSo() {
        assertEquals("2251010001", RosterSheet.username("2251010001"));
        assertEquals("2251010001", RosterSheet.username("2251010001.0"));
        assertEquals("2251010001", RosterSheet.username("2.251010001E9"));
        assertEquals("B22DCCN001", RosterSheet.username(" B22 DCCN001 "));
    }

    @Test
    void doanTieuDe_boQuaDongTenTruong_ghepHoDemVaTen() {
        var rows = List.of(
                List.of("HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG"),
                List.<String>of(),
                List.of("STT", "Mã SV", "Họ đệm", "Tên", "Ngày sinh", "Lớp"),
                List.of("1", "B22DCCN001", "Nguyễn Văn", "An", "38204", "D22CQCN01"));
        var c = RosterSheet.guess(rows);
        assertEquals(new RosterSheet.Columns(2, 1, 2, 3, 4), c);
        var e = RosterSheet.entries(rows, c).get(0);
        assertEquals("B22DCCN001", e.username());
        assertEquals("Nguyễn Văn An", e.fullName());
        assertEquals("05/08/2004", e.birth());
        assertEquals("05082004", e.password());
        assertEquals(4, e.excelRow());
        assertTrue(e.ok());
    }

    @Test
    void doanTieuDe_hoVaTenMotCot() {
        var rows = List.of(List.of("Mã sinh viên", "Họ và tên", "Ngày sinh"), List.of("sv1", "Lê Hoa", "1/2/2005"));
        assertEquals(new RosterSheet.Columns(0, 0, -1, 1, 2), RosterSheet.guess(rows));
    }

    @Test
    void dongLoi_bao_lyDo_vaKhongVaoCsv() {
        var rows = List.of(
                List.of("MSV", "Họ tên", "Ngày sinh"),
                List.of("B01", "Một, Hai", "01/01/2004"),
                List.of("b01", "Trùng mã", "02/01/2004"),
                List.of("", "Không mã", "03/01/2004"),
                List.of("Mã có dấu", "Tên", "04/01/2004"),
                List.of("B05", "", "05/01/2004"),
                List.of("B06", "Thiếu ngày", ""),
                List.of("B07", "Sai ngày", "32/01/2004"),
                List.of("", "", ""),
                List.of("B08", "Đúng", "08/01/2004"));
        var es = RosterSheet.entries(rows, RosterSheet.guess(rows));
        assertEquals(8, es.size()); // dòng trống bị bỏ hẳn
        assertEquals("Một Hai", es.get(0).fullName()); // dấu phẩy làm hỏng CSV gửi server
        assertTrue(es.get(1).problem().contains("Trùng mã SV với dòng 2"));
        assertEquals("Thiếu mã SV", es.get(2).problem());
        assertTrue(es.get(3).problem().startsWith("Mã SV chỉ gồm"));
        assertEquals("Thiếu họ tên", es.get(4).problem());
        assertEquals("Thiếu ngày sinh", es.get(5).problem());
        assertTrue(es.get(6).problem().startsWith("Không đọc được ngày sinh"));
        assertEquals(2, RosterSheet.valid(es));
        assertEquals("username,full_name,password\nB01,Một Hai,01012004\nB08,Đúng,08012004\n", RosterSheet.csv(es));
    }

    @Test
    void docXlsx_chuDungChung_chuTrongO_soVaDongBoTrong(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("ds.xlsx");
        try (OutputStream out = Files.newOutputStream(f);
                ZipOutputStream z = new ZipOutputStream(out)) {
            put(
                    z,
                    "xl/workbook.xml",
                    "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\""
                            + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>"
                            + "<sheet name=\"DS lớp\" sheetId=\"1\" r:id=\"rId7\"/></sheets></workbook>");
            put(
                    z,
                    "xl/_rels/workbook.xml.rels",
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/"
                            + "2006/relationships\"><Relationship Id=\"rId7\" Type=\"x\" Target=\"worksheets/ds.xml\"/>"
                            + "</Relationships>");
            put(
                    z,
                    "xl/sharedStrings.xml",
                    "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                            + "<si><t>Mã SV</t></si><si><r><t>Họ </t></r><r><t>tên</t></r></si><si><t>Ngày sinh</t></si>"
                            + "<si><t>B22DCCN001</t></si><si><t xml:space=\"preserve\"> 05/08/2004 </t></si></sst>");
            put(
                    z,
                    "xl/worksheets/ds.xml",
                    "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/"
                            + "main\"><sheetData>"
                            + "<row r=\"2\"><c r=\"A2\" t=\"s\"><v>0</v></c><c r=\"B2\" t=\"s\"><v>1</v></c>"
                            + "<c r=\"C2\" t=\"s\"><v>2</v></c></row>"
                            + "<row r=\"3\"><c r=\"A3\" t=\"s\"><v>3</v></c><c r=\"B3\" t=\"inlineStr\"><is><t>Nguyễn An</t>"
                            + "</is></c><c r=\"C3\"><v>38204</v></c></row>"
                            + "<row r=\"5\"><c r=\"A5\"><v>2251010002</v></c><c r=\"C5\" t=\"s\"><v>4</v></c>"
                            + "<c r=\"B5\" t=\"str\"><v>Trần Bình</v></c></row>"
                            + "</sheetData></worksheet>");
        }
        var rows = XlsxReader.firstSheet(f);
        assertEquals(5, rows.size());
        assertEquals(List.of(), rows.get(0));
        assertEquals(List.of("Mã SV", "Họ tên", "Ngày sinh"), rows.get(1));
        assertEquals(List.of("B22DCCN001", "Nguyễn An", "38204"), rows.get(2));
        assertEquals(List.of("2251010002", "Trần Bình", "05/08/2004"), rows.get(4));
        var es = RosterSheet.entries(rows, RosterSheet.guess(rows));
        assertEquals(2, RosterSheet.valid(es));
        assertEquals(5, es.get(1).excelRow());
        assertEquals("05082004", es.get(1).password());
    }

    @Test
    void docXls_cuBaoLuuLaiXlsx(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("cu.xls");
        Files.write(f, new byte[] {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, 0, 0, 0, 0});
        var e = org.junit.jupiter.api.Assertions.assertThrows(
                XlsxReader.Unreadable.class, () -> XlsxReader.firstSheet(f));
        assertTrue(e.getMessage().contains(".xlsx"));
    }

    @Test
    void tenCot() {
        assertEquals("A", XlsxReader.columnName(0));
        assertEquals("Z", XlsxReader.columnName(25));
        assertEquals("AA", XlsxReader.columnName(26));
        assertEquals(27, XlsxReader.column("AB12"));
    }

    private static void put(ZipOutputStream z, String name, String xml) throws IOException {
        z.putNextEntry(new ZipEntry(name));
        z.write(xml.getBytes(StandardCharsets.UTF_8));
        z.closeEntry();
    }
}
