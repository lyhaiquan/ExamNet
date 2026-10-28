package labcast.client.ui;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Ghi một trang tính {@code .xlsx} đơn giản (chỉ chữ, vài kiểu ô) mà Excel, LibreOffice, Google Sheets mở được, không
 * cần thư viện ngoài. Mọi ô là chữ nên mật khẩu như {@code 05082004} giữ nguyên số 0 đầu.
 */
final class XlsxWriter {
    /** Kiểu ô: chỉ số trong {@code cellXfs} của styles.xml dưới đây. */
    static final int PLAIN = 0;

    static final int TITLE = 1;
    static final int HEADER = 2;
    static final int CELL = 3;
    static final int NOTE = 4;
    static final int STRONG_CELL = 5;

    /** Một dòng: kiểu áp cho mọi ô của dòng và chữ từng ô. Dòng không có ô là dòng trống. */
    record Line(int style, List<String> cells) {}

    private XlsxWriter() {}

    /** Ghi ra file tạm rồi đổi tên: lỗi giữa chừng không để lại file hỏng đè lên file cũ. */
    static void write(Path out, String sheetName, double[] widths, List<Line> lines) throws IOException {
        Path tmp = Files.createTempFile(out.toAbsolutePath().getParent(), ".labcast-", ".xlsx");
        try {
            try (OutputStream os = Files.newOutputStream(tmp);
                    ZipOutputStream z = new ZipOutputStream(os)) {
                put(z, "[Content_Types].xml", CONTENT_TYPES);
                put(z, "_rels/.rels", ROOT_RELS);
                put(z, "xl/workbook.xml", workbook(sheetName));
                put(z, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS);
                put(z, "xl/styles.xml", STYLES);
                put(z, "xl/worksheets/sheet1.xml", sheet(widths, lines));
            }
            Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static void put(ZipOutputStream z, String name, String xml) throws IOException {
        z.putNextEntry(new ZipEntry(name));
        z.write(xml.getBytes(StandardCharsets.UTF_8));
        z.closeEntry();
    }

    private static String workbook(String sheetName) {
        // tên trang Excel tối đa 31 ký tự, không có : \ / ? * [ ]
        String name = sheetName.replaceAll("[:\\\\/?*\\[\\]]", " ").strip();
        if (name.isEmpty()) name = "Trang 1";
        if (name.length() > 31) name = name.substring(0, 31);
        return XML_HEAD
                + "<workbook xmlns=\"" + MAIN + "\" xmlns:r=\"" + REL_NS + "\"><sheets><sheet name=\"" + esc(name)
                + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";
    }

    static String sheet(double[] widths, List<Line> lines) {
        StringBuilder sb = new StringBuilder(XML_HEAD)
                .append("<worksheet xmlns=\"")
                .append(MAIN)
                .append("\">");
        if (widths.length > 0) {
            sb.append("<cols>");
            for (int i = 0; i < widths.length; i++) {
                sb.append("<col min=\"")
                        .append(i + 1)
                        .append("\" max=\"")
                        .append(i + 1)
                        .append("\" width=\"")
                        .append(widths[i])
                        .append("\" customWidth=\"1\"/>");
            }
            sb.append("</cols>");
        }
        sb.append("<sheetData>");
        for (int r = 0; r < lines.size(); r++) {
            Line line = lines.get(r);
            sb.append("<row r=\"").append(r + 1).append("\">");
            for (int c = 0; c < line.cells().size(); c++) {
                String v = line.cells().get(c);
                sb.append("<c r=\"")
                        .append(XlsxReader.columnName(c))
                        .append(r + 1)
                        .append('"');
                if (line.style() != PLAIN)
                    sb.append(" s=\"").append(line.style()).append('"');
                if (v == null || v.isEmpty()) {
                    sb.append("/>");
                } else {
                    sb.append(" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                            .append(esc(v))
                            .append("</t></is></c>");
                }
            }
            sb.append("</row>");
        }
        return sb.append("</sheetData></worksheet>").toString();
    }

    /** Thoát ký tự XML; bỏ ký tự điều khiển (XML 1.0 không cho phép, Excel báo file hỏng). */
    static String esc(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                default -> {
                    if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') sb.append(ch);
                }
            }
        }
        return sb.toString();
    }

    private static final String XML_HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
    private static final String MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

    private static final String CONTENT_TYPES = XML_HEAD
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument"
            + ".spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-"
            + "officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument"
            + ".spreadsheetml.styles+xml\"/>"
            + "</Types>";

    private static final String ROOT_RELS = XML_HEAD
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"" + REL_NS + "/officeDocument\" Target=\"xl/workbook.xml\"/>"
            + "</Relationships>";

    private static final String WORKBOOK_RELS = XML_HEAD
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"" + REL_NS + "/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"" + REL_NS + "/styles\" Target=\"styles.xml\"/>"
            + "</Relationships>";

    // font: 0 thường, 1 đậm, 2 đậm 14, 3 nghiêng xám; fill: 2 nền tiêu đề cột; border: 1 viền mảnh
    private static final String STYLES = XML_HEAD
            + "<styleSheet xmlns=\"" + MAIN + "\">"
            + "<fonts count=\"4\">"
            + "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"14\"/><name val=\"Calibri\"/></font>"
            + "<font><i/><sz val=\"10\"/><color rgb=\"FF595959\"/><name val=\"Calibri\"/></font>"
            + "</fonts>"
            + "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill>"
            + "<fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFDDEBF7\"/><bgColor indexed=\"64\"/>"
            + "</patternFill></fill></fills>"
            + "<borders count=\"2\"><border><left/><right/><top/><bottom/><diagonal/></border>"
            + "<border><left style=\"thin\"><color rgb=\"FF999999\"/></left><right style=\"thin\"><color"
            + " rgb=\"FF999999\"/></right><top style=\"thin\"><color rgb=\"FF999999\"/></top><bottom"
            + " style=\"thin\"><color rgb=\"FF999999\"/></bottom><diagonal/></border></borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"6\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\""
            + " applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            + "<xf numFmtId=\"49\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\""
            + " applyBorder=\"1\"/>"
            + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
            + "<xf numFmtId=\"49\" fontId=\"1\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\""
            + " applyFont=\"1\" applyBorder=\"1\"/>"
            + "</cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";
}
