package labcast.client.ui;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Danh sách lớp lấy từ Excel của trường (mã SV, họ tên, ngày sinh) thành các dòng tạo tài khoản: tên đăng nhập là
 * mã SV, mật khẩu tạm là ngày sinh {@code ddMMyyyy}. Tự đoán dòng tiêu đề và cột; người dùng chỉnh lại trên màn xem
 * trước. Thuần Java để test.
 */
final class RosterSheet {
    static final DateTimeFormatter PASSWORD = DateTimeFormatter.ofPattern("ddMMyyyy");
    static final DateTimeFormatter SHOWN = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 30);
    private static final int HEADER_SCAN = 20;

    private static final Set<String> ID = Set.of(
            "masv",
            "masinhvien",
            "msv",
            "mssv",
            "mahv",
            "mahocvien",
            "mahs",
            "mahocsinh",
            "ma",
            "masosinhvien",
            "username",
            "tendangnhap",
            "studentid",
            "id");
    private static final Set<String> FULL_NAME =
            Set.of("hoten", "hovaten", "hotensinhvien", "hotenhocvien", "hovatensinhvien", "fullname", "name");
    private static final Set<String> SURNAME = Set.of("ho", "hodem", "holot", "hovatendem", "hovadem");
    private static final Set<String> GIVEN = Set.of("ten");
    private static final Set<String> BIRTH = Set.of("ngaysinh", "ns", "sinhngay", "dob", "birthday", "birthdate");

    private RosterSheet() {}

    /**
     * Dòng tiêu đề (chỉ số 0, {@code -1} nếu không có) và cột của từng thông tin ({@code -1}: không có). Họ tên
     * tách hai cột "Họ đệm" + "Tên" thì {@code surname} là cột họ đệm, {@code name} là cột tên.
     */
    record Columns(int header, int id, int surname, int name, int birth) {
        Columns withHeader(int h) {
            return new Columns(h, id, surname, name, birth);
        }

        Columns withId(int c) {
            return new Columns(header, c, surname, name, birth);
        }

        Columns withSurname(int c) {
            return new Columns(header, id, c, name, birth);
        }

        Columns withName(int c) {
            return new Columns(header, id, surname, c, birth);
        }

        Columns withBirth(int c) {
            return new Columns(header, id, surname, name, c);
        }
    }

    /** Một học viên trong file; {@code problem} khác {@code null} thì dòng này bị bỏ khi tạo. */
    record Entry(int excelRow, String username, String fullName, String birth, String password, String problem) {
        boolean ok() {
            return problem == null;
        }
    }

    static Columns guess(List<List<String>> rows) {
        for (int r = 0; r < Math.min(rows.size(), HEADER_SCAN); r++) {
            Map<String, Integer> found = new HashMap<>();
            List<String> row = rows.get(r);
            for (int c = 0; c < row.size(); c++) {
                String k = key(row.get(c));
                if (ID.contains(k)) found.putIfAbsent("id", c);
                else if (FULL_NAME.contains(k)) found.putIfAbsent("full", c);
                else if (SURNAME.contains(k)) found.putIfAbsent("sur", c);
                else if (GIVEN.contains(k)) found.putIfAbsent("given", c);
                else if (BIRTH.contains(k)) found.putIfAbsent("birth", c);
            }
            boolean hasName = found.containsKey("full") || found.containsKey("given");
            if (found.containsKey("id") && (hasName || found.containsKey("birth"))) {
                int name = found.getOrDefault("full", found.getOrDefault("given", -1));
                int surname = found.containsKey("full") ? -1 : found.getOrDefault("sur", -1);
                return new Columns(r, found.get("id"), surname, name, found.getOrDefault("birth", -1));
            }
        }
        // không thấy tiêu đề: coi ba cột đầu là mã SV, họ tên, ngày sinh
        return new Columns(-1, 0, -1, 1, 2);
    }

    static List<Entry> entries(List<List<String>> rows, Columns c) {
        List<Entry> out = new ArrayList<>();
        Map<String, Integer> seen = new HashMap<>();
        for (int r = c.header() + 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            String id = username(cell(row, c.id()));
            String name = fullName(cell(row, c.surname()), cell(row, c.name()));
            String rawBirth = cell(row, c.birth());
            if (id.isEmpty() && name.isEmpty() && rawBirth.isEmpty()) continue; // dòng trống, dòng kẻ
            LocalDate born = date(rawBirth);
            String problem;
            Integer dup = id.isEmpty() ? null : seen.putIfAbsent(id.toLowerCase(Locale.ROOT), r + 1);
            if (id.isEmpty()) problem = "Thiếu mã SV";
            else if (!AccountForm.USERNAME.matcher(id).matches())
                problem = "Mã SV chỉ gồm chữ không dấu, số và . _ - (2–32 ký tự)";
            else if (dup != null) problem = "Trùng mã SV với dòng " + dup;
            else if (name.isEmpty()) problem = "Thiếu họ tên";
            else if (name.length() > AccountForm.MAX_NAME)
                problem = "Họ tên dài quá " + AccountForm.MAX_NAME + " ký tự";
            else if (rawBirth.isEmpty()) problem = "Thiếu ngày sinh";
            else if (born == null) problem = "Không đọc được ngày sinh \"" + rawBirth + "\"";
            else problem = null;
            out.add(new Entry(
                    r + 1,
                    id,
                    name,
                    born == null ? rawBirth : born.format(SHOWN),
                    born == null ? "" : born.format(PASSWORD),
                    problem));
        }
        return out;
    }

    /** Nội dung gửi server ({@code ADMIN_ROSTER_IMPORT}): chỉ các dòng hợp lệ, dạng {@code username,họ tên,mk}. */
    static String csv(List<Entry> entries) {
        StringBuilder sb = new StringBuilder("username,full_name,password\n");
        for (Entry e : entries) {
            if (e.ok())
                sb.append(e.username())
                        .append(',')
                        .append(e.fullName())
                        .append(',')
                        .append(e.password())
                        .append('\n');
        }
        return sb.toString();
    }

    static long valid(List<Entry> entries) {
        return entries.stream().filter(Entry::ok).count();
    }

    /**
     * Ngày sinh từ ô Excel: số ngày kiểu Excel (ô định dạng ngày), {@code dd/MM/yyyy} (cả {@code -} {@code .}, năm
     * hai chữ số), {@code yyyy-MM-dd}, hay {@code ddMMyyyy} liền (thiếu số 0 đầu do Excel coi là số cũng nhận).
     */
    static LocalDate date(String raw) {
        String s = raw == null ? "" : raw.strip();
        if (s.isEmpty()) return null;
        int space = s.indexOf(' ');
        if (space > 0 && s.indexOf(':') > space) s = s.substring(0, space); // "05/08/2004 00:00:00"
        if (s.endsWith("T00:00:00")) s = s.substring(0, s.length() - 9);
        LocalDate d;
        try {
            if (s.matches("\\d+(\\.\\d+)?([eE][+-]?\\d+)?")) {
                String digits = s.matches("\\d+") ? s : null;
                if (digits != null && (digits.length() == 7 || digits.length() == 8)) {
                    if (digits.length() == 7) digits = "0" + digits;
                    int year = Integer.parseInt(digits.substring(4));
                    if (year >= 1900) {
                        d = LocalDate.of(
                                year,
                                Integer.parseInt(digits.substring(2, 4)),
                                Integer.parseInt(digits.substring(0, 2)));
                    } else { // "20040805": yyyyMMdd
                        d = LocalDate.of(
                                Integer.parseInt(digits.substring(0, 4)),
                                Integer.parseInt(digits.substring(4, 6)),
                                Integer.parseInt(digits.substring(6)));
                    }
                } else if (digits != null && digits.length() == 6) {
                    d = LocalDate.of(
                            year(Integer.parseInt(digits.substring(4))),
                            Integer.parseInt(digits.substring(2, 4)),
                            Integer.parseInt(digits.substring(0, 2)));
                } else {
                    double serial = new BigDecimal(s).doubleValue();
                    if (serial < 1 || serial > 100_000) return null;
                    d = EXCEL_EPOCH.plusDays((long) Math.floor(serial));
                }
            } else {
                String[] p = s.split("\\s*[/.\\-]\\s*");
                if (p.length != 3) return null;
                for (String x : p) if (!x.matches("\\d{1,4}")) return null;
                if (p[0].length() == 4) {
                    d = LocalDate.of(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                } else {
                    d = LocalDate.of(year(Integer.parseInt(p[2])), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
                }
            }
        } catch (DateTimeException | NumberFormatException e) {
            return null;
        }
        LocalDate today = LocalDate.now();
        return d.getYear() < 1900 || d.isAfter(today) ? null : d;
    }

    /** Năm hai chữ số: không quá năm nay thì là 20xx, còn lại 19xx. */
    private static int year(int y) {
        if (y >= 100) return y;
        int now = LocalDate.now().getYear() % 100;
        return y <= now ? 2000 + y : 1900 + y;
    }

    /** Mã SV Excel lưu dạng số ("2251010001.0", "2.251010001E9") thì đổi về chữ số; bỏ khoảng trắng. */
    static String username(String raw) {
        String s = raw.replaceAll("\\s+", "");
        if (s.matches("\\d+\\.\\d*([eE][+-]?\\d+)?|\\d+(\\.\\d+)?[eE][+-]?\\d+")) {
            try {
                BigDecimal b = new BigDecimal(s);
                if (b.stripTrailingZeros().scale() <= 0)
                    return b.toBigIntegerExact().toString();
            } catch (ArithmeticException | NumberFormatException ignored) {
                // giữ nguyên, để báo mã không hợp lệ
            }
        }
        return s;
    }

    /** Ghép "Họ đệm" + "Tên" nếu tách cột; dấu phẩy thành khoảng trắng (server tách cột bằng dấu phẩy). */
    static String fullName(String surname, String name) {
        String s = (surname + " " + name).replace(',', ' ');
        return s.replaceAll("\\s+", " ").strip();
    }

    private static String cell(List<String> row, int c) {
        return c >= 0 && c < row.size() ? row.get(c).strip() : "";
    }

    /** "Mã SV" → "masv", "Ngày sinh" → "ngaysinh", "Họ và tên" → "hovaten". */
    static String key(String header) {
        String s = Normalizer.normalize(header == null ? "" : header, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
        return s.replaceAll("[^a-z0-9]", "");
    }
}
