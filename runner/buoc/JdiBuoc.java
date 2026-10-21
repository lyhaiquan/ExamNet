import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.CharValue;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Chạy từng dòng một bài làm Java dưới JDI, ghi sự kiện {@code line}/{@code vars} (spec đa ngôn ngữ §5).
 *
 * <pre>java -cp runner/buoc JdiBuoc &lt;vào&gt; &lt;ra&gt; &lt;trace&gt; &lt;meta&gt; &lt;số dòng tối đa&gt; &lt;tuỳ chọn JVM&gt;</pre>
 *
 * Lớp {@code Main} (đã biên dịch với {@code javac -g}) tìm qua biến môi trường CLASSPATH: đường dẫn thư mục
 * tạm có thể chứa dấu cách, không nhét vào chuỗi tuỳ chọn được.
 *
 * <p>Không dùng bước dòng của JDI: vòng lặp nằm gọn trên một dòng ({@code while (true) { x++; }}) nhảy về chính
 * dòng đó, bước dòng không bao giờ dừng. Thay vào đó đặt breakpoint ở mọi mục của bảng dòng của các lớp trong
 * {@code Main.java} (một dòng {@code for} có nhiều mục: khởi tạo, kiểm điều kiện, tăng biến), nên không bao giờ
 * dừng trong code của JDK. Quá số dòng tối đa thì xoá hết breakpoint, chương trình chạy nốt để có output.
 */
public final class JdiBuoc {
    private static final int MAX_PHAN_TU = 20;
    private static final int MAX_GIA_TRI = 120;

    private final PrintWriter trace;
    private final int max;
    private int dem;
    private int dongCuoi;
    private String bienTruoc;
    private boolean biCat;
    /** Lần dừng trước: cùng dòng, cùng lời gọi (phương thức + độ sâu ngăn xếp), chỉ số lệnh tăng = cùng lần ghé. */
    private Method phuongThucTruoc;
    private int doSauTruoc = -1;
    private long chiSoTruoc = -1;

    private JdiBuoc(PrintWriter trace, int max) {
        this.trace = trace;
        this.max = max;
    }

    public static void main(String[] a) throws Exception {
        Path vao = Path.of(a[0]);
        Path ra = Path.of(a[1]);
        Path meta = Path.of(a[3]);
        int max = Integer.parseInt(a[4]);
        String tuyChon = a.length > 5 ? a[5] : "";
        try (PrintWriter trace = new PrintWriter(Files.newBufferedWriter(Path.of(a[2]), StandardCharsets.UTF_8), true)) {
            String ketQua;
            try {
                ketQua = new JdiBuoc(trace, max).chay(vao, ra, tuyChon);
            } catch (Exception e) {
                ketQua = "{\"loi_debugger\": " + chuoi(e.getClass().getSimpleName() + ": " + e.getMessage()) + "}";
            }
            Files.writeString(meta, ketQua, StandardCharsets.UTF_8);
        }
    }

    private String chay(Path vao, Path ra, String tuyChon) throws Exception {
        LaunchingConnector lc = Bootstrap.virtualMachineManager().defaultConnector();
        Map<String, Connector.Argument> args = lc.defaultArguments();
        args.get("main").setValue("Main");
        args.get("options").setValue(tuyChon);
        VirtualMachine vm = lc.launch(args);
        Process p = vm.process();

        Thread chepRa = chep(p.getInputStream(), new FileOutputStream(ra.toFile()), Long.MAX_VALUE);
        ByteArrayOutputStream loi = new ByteArrayOutputStream();
        Thread chepLoi = chep(p.getErrorStream(), loi, 64 * 1024);
        Thread ghiVao = new Thread(() -> {
            try (OutputStream o = p.getOutputStream()) {
                Files.copy(vao, o);
            } catch (IOException e) {
                // chương trình không đọc hết input rồi thoát: bình thường
            }
        });
        ghiVao.setDaemon(true);
        ghiVao.start();

        EventRequestManager erm = vm.eventRequestManager();
        ClassPrepareRequest cpr = erm.createClassPrepareRequest();
        cpr.addSourceNameFilter("Main.java");
        cpr.enable();
        vm.resume();

        boolean xong = false;
        while (!xong) {
            EventSet es;
            try {
                es = vm.eventQueue().remove();
            } catch (VMDisconnectedException e) {
                break;
            }
            for (Event e : es) {
                if (e instanceof ClassPrepareEvent cpe) {
                    if (!biCat) {
                        datBreakpoint(erm, cpe.referenceType());
                    }
                } else if (e instanceof BreakpointEvent be) {
                    if (!ghiDong(be.location(), be.thread())) {
                        // hết trần: xoá hết breakpoint, chương trình chạy nốt không ghi
                        erm.deleteEventRequests(new ArrayList<>(erm.breakpointRequests()));
                        cpr.disable();
                    }
                } else if (e instanceof VMDeathEvent || e instanceof VMDisconnectEvent) {
                    xong = true;
                }
            }
            if (!xong) {
                try {
                    es.resume();
                } catch (VMDisconnectedException e) {
                    break;
                }
            }
        }
        int ma = p.waitFor();
        chepRa.join(2_000);
        chepLoi.join(2_000);
        String stderr = loi.toString(StandardCharsets.UTF_8);
        return "{\"ma\": " + ma + ", \"truncated\": " + biCat + ", \"dong\": " + (dongCuoi == 0 ? "null" : dongCuoi)
                + ", \"stderr\": " + chuoi(stderr) + "}";
    }

    /** Breakpoint ở mọi mục bảng dòng của mọi phương thức trong lớp (kể cả constructor, lớp lồng nạp sau). */
    private static void datBreakpoint(EventRequestManager erm, ReferenceType t) {
        for (Method m : t.methods()) {
            if (m.isAbstract() || m.isNative()) {
                continue;
            }
            try {
                for (Location dong : m.allLineLocations()) {
                    for (Location loc : m.locationsOfLine(dong.lineNumber())) {
                        erm.createBreakpointRequest(loc).enable();
                    }
                }
            } catch (AbsentInformationException e) {
                // lớp do trình biên dịch sinh, không có bảng dòng
            }
        }
    }

    /** Ghi một bước; trả false khi đã đủ số dòng tối đa. */
    private boolean ghiDong(Location loc, ThreadReference luong) {
        try {
            if (!"Main.java".equals(loc.sourceName())) {
                return true;
            }
        } catch (AbsentInformationException e) {
            return true;
        }
        int doSau;
        try {
            doSau = luong.frameCount();
        } catch (IncompatibleThreadStateException e) {
            doSau = -1;
        }
        // cùng dòng, cùng lời gọi, chỉ số lệnh đi tiếp về sau: vẫn là lần ghé dòng này (phần tăng biến của dòng
        // for…) — chỉ cập nhật biến. Nhảy ngược về là vòng lặp mới: thêm một bước
        boolean cungLan = loc.lineNumber() == dongCuoi && loc.method().equals(phuongThucTruoc) && doSau == doSauTruoc
                && loc.codeIndex() > chiSoTruoc;
        if (!cungLan) {
            if (dem >= max) {
                biCat = true;
                return false;
            }
            dem++;
            dongCuoi = loc.lineNumber();
            phuongThucTruoc = loc.method();
            doSauTruoc = doSau;
            trace.println("{\"t\": \"line\", \"n\": " + dongCuoi + "}");
        }
        chiSoTruoc = loc.codeIndex();
        String bien = docBien(luong);
        if (bien != null && !bien.equals(bienTruoc)) {
            trace.println("{\"t\": \"vars\", \"f\": " + chuoi(loc.method().name()) + ", \"d\": " + bien + "}");
            bienTruoc = bien;
        }
        return true;
    }

    private static String docBien(ThreadReference luong) {
        try {
            StackFrame khung = luong.frame(0);
            List<LocalVariable> ds = khung.visibleVariables();
            Map<LocalVariable, Value> giaTri = khung.getValues(ds);
            List<String> cap = new ArrayList<>();
            boolean laMain = khung.location().method().name().equals("main");
            for (LocalVariable v : ds) {
                if (laMain && v.name().equals("args")) {
                    continue; // luôn rỗng ở bài chấm bằng stdin: chỉ chiếm chỗ
                }
                cap.add("[" + chuoi(v.name()) + ", " + chuoi(cat(hien(giaTri.get(v), 0))) + "]");
            }
            return "[" + String.join(", ", cap) + "]";
        } catch (AbsentInformationException | IncompatibleThreadStateException e) {
            return null;
        }
    }

    private static String hien(Value v, int sau) {
        if (v == null) {
            return "null";
        }
        if (v instanceof StringReference s) {
            return chuoi(s.value());
        }
        if (v instanceof CharValue c) {
            return "'" + c.value() + "'";
        }
        if (v instanceof PrimitiveValue) {
            return v.toString();
        }
        if (v instanceof ArrayReference ar) {
            return mang(ar.getValues(0, Math.min(ar.length(), MAX_PHAN_TU)), ar.length(), sau);
        }
        ObjectReference o = (ObjectReference) v;
        ReferenceType t = o.referenceType();
        String ten = t.name();
        switch (ten) {
            case "java.lang.Integer", "java.lang.Long", "java.lang.Double", "java.lang.Float", "java.lang.Short",
                    "java.lang.Byte", "java.lang.Boolean", "java.lang.Character" -> {
                return hien(o.getValue(t.fieldByName("value")), sau);
            }
            case "java.util.ArrayList", "java.util.PriorityQueue", "java.util.Vector", "java.util.Stack" -> {
                String truongMang = ten.equals("java.util.PriorityQueue") ? "queue" : "elementData";
                String truongCo = ten.endsWith("ArrayList") || ten.endsWith("PriorityQueue") ? "size" : "elementCount";
                Value mangV = o.getValue(timTruong(t, truongMang));
                Value coV = o.getValue(timTruong(t, truongCo));
                if (mangV instanceof ArrayReference ar && coV instanceof IntegerValue co) {
                    int n = co.value();
                    return mang(ar.getValues(0, Math.min(n, MAX_PHAN_TU)), n, sau);
                }
            }
            case "java.util.ArrayDeque" -> {
                Value mangV = o.getValue(timTruong(t, "elements"));
                Value dau = o.getValue(timTruong(t, "head"));
                Value cuoi = o.getValue(timTruong(t, "tail"));
                if (mangV instanceof ArrayReference ar && dau instanceof IntegerValue h && cuoi instanceof IntegerValue tl) {
                    List<Value> ds = new ArrayList<>();
                    int cap = ar.length();
                    int n = Math.floorMod(tl.value() - h.value(), Math.max(cap, 1));
                    for (int i = 0; i < Math.min(n, MAX_PHAN_TU); i++) {
                        ds.add(ar.getValue((h.value() + i) % cap));
                    }
                    return mang(ds, n, sau);
                }
            }
            default -> {
                // bỏ qua: xử lý chung ở dưới
            }
        }
        String gon = ten.substring(Math.max(ten.lastIndexOf('.'), ten.lastIndexOf('$')) + 1);
        Field co = timTruong(t, "size");
        if (ten.startsWith("java.") && co != null && o.getValue(co) instanceof IntegerValue n) {
            return gon + "(size=" + n.value() + ")";
        }
        if (ten.startsWith("java.") || sau >= 1) {
            return gon;
        }
        // lớp học viên tự viết (Node, Edge…): các trường, tối đa 5
        List<String> truong = new ArrayList<>();
        for (Field f : t.allFields()) {
            if (f.isStatic()) {
                continue;
            }
            if (truong.size() == 5) {
                truong.add("…");
                break;
            }
            truong.add(f.name() + "=" + hien(o.getValue(f), sau + 1));
        }
        return gon + "{" + String.join(", ", truong) + "}";
    }

    private static Field timTruong(ReferenceType t, String ten) {
        Field f = t.fieldByName(ten);
        if (f != null) {
            return f;
        }
        for (Field g : t.allFields()) {
            if (g.name().equals(ten)) {
                return g;
            }
        }
        return null;
    }

    private static String mang(List<Value> ds, int n, int sau) {
        if (sau >= 2) {
            return "[…]";
        }
        List<String> phan = new ArrayList<>();
        for (Value x : ds) {
            phan.add(hien(x, sau + 1));
        }
        if (n > ds.size()) {
            phan.add("…");
        }
        return "[" + String.join(", ", phan) + "]";
    }

    private static String cat(String s) {
        return s.length() <= MAX_GIA_TRI ? s : s.substring(0, MAX_GIA_TRI - 1) + "…";
    }

    /** Chuỗi JSON có dấu ngoặc kép. */
    private static String chuoi(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        return b.append('"').toString();
    }

    private static Thread chep(InputStream vao, OutputStream ra, long toiDa) {
        Thread t = new Thread(() -> {
            byte[] buf = new byte[8192];
            long da = 0;
            try (InputStream in = vao; OutputStream out = ra) {
                int n;
                while ((n = in.read(buf)) > 0) {
                    if (da < toiDa) {
                        out.write(buf, 0, (int) Math.min(n, toiDa - da));
                        da += n;
                    }
                }
            } catch (IOException e) {
                // tiến trình đã tắt
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }
}
