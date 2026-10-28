package labcast.client.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Đọc trang tính đầu tiên của file Excel {@code .xlsx} thành bảng chữ, không cần thư viện ngoài: {@code .xlsx} là
 * file zip chứa XML. Ô số giữ nguyên chữ Excel lưu (ngày là số ngày kể từ 1899-12-30, {@link RosterSheet} tự đổi).
 * Dòng trống vẫn giữ chỗ để chỉ số dòng khớp số dòng Excel hiện.
 */
final class XlsxReader {
    static final int MAX_ROWS = 5000;
    static final int MAX_COLS = 60;

    private XlsxReader() {}

    /** Lỗi đọc file mà người dùng sửa được (file .xls cũ, file hỏng…); thông báo viết sẵn để hiện lên. */
    static final class Unreadable extends IOException {
        Unreadable(String message) {
            super(message);
        }
    }

    static List<List<String>> firstSheet(Path file) throws IOException {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            List<String> shared = sharedStrings(zip);
            ZipEntry sheet = zip.getEntry(firstSheetPath(zip));
            if (sheet == null) sheet = zip.getEntry("xl/worksheets/sheet1.xml");
            if (sheet == null) throw new Unreadable("File Excel không có trang tính nào.");
            try (InputStream in = zip.getInputStream(sheet)) {
                return rows(in, shared);
            }
        } catch (ZipException e) {
            throw new Unreadable("Không mở được file. Nếu là file .xls cũ, mở bằng Excel rồi \"Lưu thành\" .xlsx.");
        } catch (XMLStreamException e) {
            throw new Unreadable("File Excel bị hỏng: " + e.getMessage());
        }
    }

    private static XMLStreamReader xml(InputStream in) throws XMLStreamException {
        XMLInputFactory f = XMLInputFactory.newFactory();
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return f.createXMLStreamReader(in, "UTF-8");
    }

    /** Trang đầu theo thứ tự trong workbook.xml (không nhất thiết là sheet1.xml). */
    private static String firstSheetPath(ZipFile zip) throws IOException, XMLStreamException {
        ZipEntry wb = zip.getEntry("xl/workbook.xml");
        ZipEntry rels = zip.getEntry("xl/_rels/workbook.xml.rels");
        if (wb == null || rels == null) return "xl/worksheets/sheet1.xml";
        String rid = null;
        try (InputStream in = zip.getInputStream(wb)) {
            XMLStreamReader r = xml(in);
            while (r.hasNext() && rid == null) {
                if (r.next() == XMLStreamConstants.START_ELEMENT
                        && r.getLocalName().equals("sheet")) {
                    for (int i = 0; i < r.getAttributeCount(); i++) {
                        if (r.getAttributeLocalName(i).equals("id")) rid = r.getAttributeValue(i);
                    }
                }
            }
        }
        if (rid == null) return "xl/worksheets/sheet1.xml";
        try (InputStream in = zip.getInputStream(rels)) {
            XMLStreamReader r = xml(in);
            while (r.hasNext()) {
                if (r.next() == XMLStreamConstants.START_ELEMENT
                        && r.getLocalName().equals("Relationship")
                        && rid.equals(r.getAttributeValue(null, "Id"))) {
                    String target = r.getAttributeValue(null, "Target");
                    if (target == null) break;
                    return target.startsWith("/") ? target.substring(1) : "xl/" + target;
                }
            }
        }
        return "xl/worksheets/sheet1.xml";
    }

    private static List<String> sharedStrings(ZipFile zip) throws IOException, XMLStreamException {
        List<String> out = new ArrayList<>();
        ZipEntry e = zip.getEntry("xl/sharedStrings.xml");
        if (e == null) return out;
        try (InputStream in = zip.getInputStream(e)) {
            XMLStreamReader r = xml(in);
            StringBuilder cur = null;
            int phonetic = 0; // <rPh>: phiên âm kèm chữ Nhật, không thuộc nội dung ô
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    switch (r.getLocalName()) {
                        case "si" -> cur = new StringBuilder();
                        case "rPh" -> phonetic++;
                        case "t" -> {
                            String t = r.getElementText();
                            if (cur != null && phonetic == 0) cur.append(t);
                        }
                        default -> {}
                    }
                } else if (ev == XMLStreamConstants.END_ELEMENT) {
                    if (r.getLocalName().equals("rPh")) phonetic--;
                    else if (r.getLocalName().equals("si") && cur != null) {
                        out.add(cur.toString());
                        cur = null;
                    }
                }
            }
        }
        return out;
    }

    private static List<List<String>> rows(InputStream in, List<String> shared) throws XMLStreamException {
        List<List<String>> rows = new ArrayList<>();
        XMLStreamReader r = xml(in);
        Map<Integer, String> row = null;
        int rowNo = 0;
        int col = -1;
        String type = null;
        String value = null;
        StringBuilder inline = null;
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                switch (r.getLocalName()) {
                    case "row" -> {
                        String n = r.getAttributeValue(null, "r");
                        rowNo = n == null ? rowNo + 1 : Integer.parseInt(n);
                        row = new HashMap<>();
                        col = -1;
                    }
                    case "c" -> {
                        String ref = r.getAttributeValue(null, "r");
                        col = ref == null ? col + 1 : column(ref);
                        type = r.getAttributeValue(null, "t");
                        value = null;
                        inline = null;
                    }
                    case "v" -> value = r.getElementText();
                    case "is" -> inline = new StringBuilder();
                    case "t" -> {
                        String t = r.getElementText();
                        if (inline != null) inline.append(t);
                    }
                    default -> {}
                }
            } else if (ev == XMLStreamConstants.END_ELEMENT) {
                switch (r.getLocalName()) {
                    case "c" -> {
                        if (row != null && col >= 0 && col < MAX_COLS) {
                            String text = cell(type, value, inline, shared);
                            if (!text.isEmpty()) row.put(col, text);
                        }
                    }
                    case "row" -> {
                        if (rowNo > MAX_ROWS) return rows;
                        while (rows.size() < rowNo - 1) rows.add(List.of());
                        int width =
                                row.keySet().stream().mapToInt(i -> i + 1).max().orElse(0);
                        List<String> cells = new ArrayList<>(width);
                        for (int i = 0; i < width; i++) cells.add(row.getOrDefault(i, ""));
                        rows.add(cells);
                        row = null;
                    }
                    default -> {}
                }
            }
        }
        return rows;
    }

    private static String cell(String type, String value, StringBuilder inline, List<String> shared) {
        if ("inlineStr".equals(type))
            return inline == null ? "" : inline.toString().strip();
        if (value == null) return "";
        if ("s".equals(type)) {
            try {
                int i = Integer.parseInt(value.strip());
                return i >= 0 && i < shared.size() ? shared.get(i).strip() : "";
            } catch (NumberFormatException e) {
                return "";
            }
        }
        if ("b".equals(type)) return "1".equals(value) ? "TRUE" : "FALSE";
        return value.strip();
    }

    /** "B12" → 1, "AA3" → 26. */
    static int column(String ref) {
        int c = 0;
        for (int i = 0; i < ref.length() && Character.isLetter(ref.charAt(i)); i++) {
            c = c * 26 + (Character.toUpperCase(ref.charAt(i)) - 'A' + 1);
        }
        return c - 1;
    }

    /** 0 → "A", 26 → "AA". */
    static String columnName(int index) {
        StringBuilder sb = new StringBuilder();
        for (int n = index + 1; n > 0; n = (n - 1) / 26) sb.insert(0, (char) ('A' + (n - 1) % 26));
        return sb.toString();
    }
}
