package labcast.client.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import labcast.client.views.cay.TreeModel;
import labcast.client.views.dothi.GraphModel;
import labcast.client.views.dslk.LinkedListModel;
import labcast.client.views.luoi.GridModel;
import labcast.client.views.mang.ArrayModel;
import labcast.client.views.nganxep.StackModel;
import labcast.common.trace.TraceEvent;
import labcast.server.lesson.Lesson;
import labcast.server.lesson.LessonRepository;
import labcast.server.lesson.ViewSpec;
import labcast.server.run.LocalPythonSandbox;
import org.junit.jupiter.api.Test;

/**
 * Hợp đồng giữa bộ ghi Python ({@code runner/viz}) và bộ vẽ Java: chạy thật lời giải mẫu của các bài qua
 * {@code runner/run.py}, đưa trace vào model của bộ vẽ. Kiểm hai điều mà test riêng từng bên không thấy:
 *
 * <ul>
 *   <li>tên trường và dạng ảnh chụp hai bên khớp nhau — trạng thái cuối của model bằng trạng thái thật mà
 *       Python chụp ở cuối;
 *   <li>tua: dựng lại từ một ảnh chụp giữa trace rồi áp tiếp phải ra đúng trạng thái như áp liền từ đầu.
 * </ul>
 */
class VizContractTest {
    static final ObjectMapper M = new ObjectMapper();

    static Path repo() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.isDirectory(p.resolve("runner"))) {
            p = p.getParent();
        }
        return p;
    }

    /** Chạy {@code code} với {@code input}, có ghi trace; trả các dòng sự kiện (bỏ hdr/end/result). */
    static List<TraceEvent> chay(String code, String input) throws Exception {
        String py = LocalPythonSandbox.detectPython();
        assumeTrue(py != null, "máy không có Python 3");
        ObjectNode job = M.createObjectNode();
        job.put("code", code);
        job.put("input", input);
        job.put("trace", true);
        job.put("time_limit_ms", 5000);
        Process p = new ProcessBuilder(
                        py, repo().resolve("runner").resolve("run.py").toString())
                .redirectErrorStream(true)
                .start();
        p.getOutputStream().write(M.writeValueAsBytes(job));
        p.getOutputStream().close();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(p.waitFor(30, TimeUnit.SECONDS));
        List<TraceEvent> evs = new ArrayList<>();
        JsonNode result = null;
        for (String line : out.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            ObjectNode o = (ObjectNode) M.readTree(line);
            String t = o.path("t").asText();
            if (t.equals("result")) {
                result = o;
            } else if (!t.equals("hdr") && !t.equals("end")) {
                evs.add(new TraceEvent(t, o));
            }
        }
        assertTrue(result != null && result.path("ok").asBoolean(), out);
        assertFalse(result.path("truncated").asBoolean(), "trace bị cắt");
        return evs;
    }

    static String bai(String path, String file) throws IOException {
        return Files.readString(repo().resolve("content").resolve(path).resolve(file), StandardCharsets.UTF_8);
    }

    /** Chỉ số các sự kiện snap. */
    static List<Integer> snaps(List<TraceEvent> evs) {
        List<Integer> out = new ArrayList<>();
        for (int k = 0; k < evs.size(); k++) {
            if (evs.get(k).t().equals("snap")) {
                out.add(k);
            }
        }
        return out;
    }

    /**
     * Áp mọi sự kiện của biến {@code var} kể từ ảnh chụp thứ {@code from}, trả trạng thái sau cùng dạng chữ.
     * {@code state} đọc trạng thái hiện tại của model.
     */
    static <T> String phatTu(
            List<TraceEvent> evs,
            int from,
            String var,
            Function<JsonNode, T> resetFrom,
            java.util.function.BiConsumer<T, TraceEvent> apply,
            Function<T, String> state) {
        T model = resetFrom.apply(evs.get(from).get("state").get(var));
        for (int k = from + 1; k < evs.size(); k++) {
            TraceEvent e = evs.get(k);
            if (var.equals(e.var())) {
                apply.accept(model, e);
            }
        }
        return state.apply(model);
    }

    // ---------- ngăn xếp / hàng đợi ----------

    static StackModel stackFrom(JsonNode snap) {
        StackModel m = new StackModel();
        m.reset(snap, false);
        return m;
    }

    static String stackState(StackModel m) {
        return (m.queue() ? "q" : "s") + m.items();
    }

    /** Python chụp lần cuối bằng trang_thai() ở cuối code thử, để so với model. */
    static final String CHUP_CUOI = "\nimport viz.recorder as _r\n"
            + "_r.current.register('_xong', type('X', (), {'trang_thai': lambda self: 0})())\n";

    static void kiemNganXep(String code, String input, String var) throws Exception {
        List<TraceEvent> evs = chay(code + CHUP_CUOI, input);
        List<Integer> s = snaps(evs);
        assertTrue(s.size() >= 2, "cần ảnh chụp đầu và cuối");
        JsonNode that = evs.get(s.get(s.size() - 1)).get("state").get(var);
        String expected = stackState(stackFrom(that));
        for (int from : s) {
            assertEquals(
                    expected,
                    phatTu(evs, from, var, VizContractTest::stackFrom, StackModel::apply, VizContractTest::stackState),
                    "dựng lại từ ảnh chụp ở sự kiện " + from);
        }
    }

    @Test
    void baiKiemTraNgoac_traceVaoDungBoVeNganXep() throws Exception {
        String p = "dsa/ngan-xep/kiem-tra-ngoac";
        kiemNganXep(bai(p, "loi_giai.py"), bai(p, "demo.in"), "st");
        // trace dài hơn 200 sự kiện để có ảnh chụp giữa chừng
        kiemNganXep(bai(p, "loi_giai.py"), "(".repeat(150) + "[]".repeat(40) + ")".repeat(150) + "\n", "st");
    }

    @Test
    void baiSoNhiPhan_hangDoiVeNamNgang() throws Exception {
        String p = "dsa/hang-doi/so-nhi-phan";
        kiemNganXep(bai(p, "loi_giai.py"), "5\n", "q");
        kiemNganXep(bai(p, "loi_giai.py"), "120\n", "q");
        List<TraceEvent> evs = chay(bai(p, "loi_giai.py"), "3\n");
        StackModel m = stackFrom(evs.get(0).get("state").get("q"));
        assertTrue(m.queue(), "ảnh chụp đầu đã cho biết là hàng đợi, kể cả khi còn rỗng");
    }

    // ---------- bảng 2 chiều ----------

    static GridModel gridFrom(JsonNode snap) {
        GridModel m = new GridModel();
        m.reset(snap);
        return m;
    }

    static String gridState(GridModel m) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < m.rows(); i++) {
            for (int j = 0; j < m.cols(); j++) {
                sb.append(m.text(i, j))
                        .append(m.written(i, j) ? "*" : "")
                        .append(m.marked(i, j) ? "!" : "")
                        .append(' ');
            }
            sb.append('|').append(m.rowLabel(i)).append('\n');
        }
        return sb.toString();
    }

    static void kiemBang(String code, String input) throws Exception {
        List<TraceEvent> evs = chay(code + CHUP_CUOI, input);
        List<Integer> s = snaps(evs);
        JsonNode that = evs.get(s.get(s.size() - 1)).get("state").get("dp");
        String expected = gridState(gridFrom(that));
        for (int from : s) {
            assertEquals(
                    expected,
                    phatTu(evs, from, "dp", VizContractTest::gridFrom, GridModel::apply, VizContractTest::gridState),
                    "dựng lại từ ảnh chụp ở sự kiện " + from);
        }
    }

    @Test
    void baiLcs_traceVaoDungBoVeBang_keCaTruyVet() throws Exception {
        String p = "dsa/quy-hoach-dong/lcs";
        kiemBang(bai(p, "loi_giai.py"), bai(p, "demo.in"));
        kiemBang(bai(p, "loi_giai.py"), "ABCDABCDABCD\nBDCABADCBA\n"); // > 200 sự kiện
        List<TraceEvent> evs = chay(bai(p, "loi_giai.py"), bai(p, "demo.in"));
        GridModel m = gridFrom(evs.get(0).get("state").get("dp"));
        assertEquals("A", m.rowLabel(1));
        assertEquals("B", m.colLabel(1));
        assertEquals(8, m.rows());
        assertEquals(7, m.cols());
    }

    /** Trace của {@code code} (có chụp lần cuối), dựng lại từ mọi ảnh chụp phải ra đúng trạng thái cuối. */
    static <T> void kiem(
            String code,
            String input,
            String var,
            Function<JsonNode, T> resetFrom,
            java.util.function.BiConsumer<T, TraceEvent> apply,
            Function<T, String> state)
            throws Exception {
        List<TraceEvent> evs = chay(code + CHUP_CUOI, input);
        List<Integer> s = snaps(evs);
        assertTrue(s.size() >= 2, "cần ảnh chụp đầu và cuối");
        String expected = state.apply(
                resetFrom.apply(evs.get(s.get(s.size() - 1)).get("state").get(var)));
        for (int from : s) {
            assertEquals(
                    expected,
                    phatTu(evs, from, var, resetFrom, apply, state),
                    "dựng lại từ ảnh chụp ở sự kiện " + from);
        }
    }

    // ---------- cây, heap ----------

    static TreeModel treeFrom(JsonNode snap) {
        TreeModel m = new TreeModel();
        m.reset(snap);
        return m;
    }

    static String treeState(TreeModel m) {
        StringBuilder sb = new StringBuilder(m.shape()).append(" |");
        m.layout().forEach((id, p) -> sb.append(' ')
                .append(m.node(id).text)
                .append('@')
                .append(p.col())
                .append(',')
                .append(p.depth())
                .append(m.node(id).mark == null ? "" : ":" + m.node(id).mark));
        return sb.toString();
    }

    @Test
    void baiChenCay_traceVaoDungBoVeCay() throws Exception {
        String p = "dsa/cay/bst-chen";
        kiem(
                bai(p, "loi_giai.py"),
                bai(p, "demo.in"),
                "T",
                VizContractTest::treeFrom,
                TreeModel::apply,
                VizContractTest::treeState);
        // 40 khoá: hơn 200 sự kiện, có ảnh chụp giữa chừng
        StringBuilder in = new StringBuilder("40\n");
        for (int k = 0; k < 40; k++) {
            in.append((k * 37) % 101).append(' ');
        }
        kiem(
                bai(p, "loi_giai.py"),
                in + "\n",
                "T",
                VizContractTest::treeFrom,
                TreeModel::apply,
                VizContractTest::treeState);
        List<TraceEvent> evs = chay(bai(p, "loi_giai.py"), bai(p, "demo.in"));
        TreeModel m = treeFrom(evs.get(0).get("state").get("T"));
        for (TraceEvent e : evs) {
            if ("T".equals(e.var())) {
                m.apply(e);
            }
        }
        assertEquals("((20 30 (35 40 .)) 50 (60 70 80))", m.shape());
    }

    static ArrayModel arrayFrom(JsonNode snap) {
        ArrayModel m = new ArrayModel();
        List<Integer> init = new ArrayList<>();
        snap.forEach(x -> init.add(x.asInt()));
        m.reset(init);
        return m;
    }

    static String arrayState(ArrayModel m) {
        m.flush();
        return m.values().toString();
    }

    @Test
    void baiThemHeap_traceVaoDungBoVeHeap() throws Exception {
        String p = "dsa/cay/heap-them";
        kiem(
                bai(p, "loi_giai.py"),
                bai(p, "demo.in"),
                "h",
                VizContractTest::arrayFrom,
                ArrayModel::apply,
                VizContractTest::arrayState);
        kiem(
                bai(p, "loi_giai.py"),
                "30\n" + "9 8 7 6 5 4 3 2 1 0 ".repeat(3) + "\n",
                "h",
                VizContractTest::arrayFrom,
                ArrayModel::apply,
                VizContractTest::arrayState);
    }

    /** Bài khai báo một {@code kieu} chưa có bộ vẽ thì biến đó lặng lẽ không được vẽ: chặn ngay ở test. */
    @Test
    void moiKieuTrongBaiDeuCoBoVe() {
        LessonRepository repo = LessonRepository.load(repo().resolve("content"));
        assertTrue(repo.errors().isEmpty(), repo.errors().toString());
        for (Lesson l : repo.all()) {
            for (ViewSpec v : l.khungNhin()) {
                assertTrue(Views.KINDS.contains(v.kieu()), l.id() + " khai báo kieu chưa có bộ vẽ: " + v.kieu());
            }
        }
    }

    // ---------- đồ thị ----------

    static GraphModel graphFrom(JsonNode snap) {
        GraphModel m = new GraphModel();
        m.reset(snap);
        return m;
    }

    @Test
    void baiBfs_traceVaoDungBoVeDoThi() throws Exception {
        String p = "dsa/do-thi/bfs";
        kiem(
                bai(p, "loi_giai.py"),
                bai(p, "demo.in"),
                "g",
                VizContractTest::graphFrom,
                GraphModel::apply,
                GraphModel::describe);
        kiem(
                bai(p, "loi_giai.py"),
                bai(p, "demo.in"),
                "q",
                VizContractTest::stackFrom,
                StackModel::apply,
                VizContractTest::stackState);
        // lưới 5×5: hơn 200 sự kiện
        StringBuilder in = new StringBuilder("25 40\n");
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                int u = i * 5 + j + 1;
                if (j < 4) {
                    in.append(u).append(' ').append(u + 1).append('\n');
                }
                if (i < 4) {
                    in.append(u).append(' ').append(u + 5).append('\n');
                }
            }
        }
        kiem(
                bai(p, "loi_giai.py"),
                in.toString(),
                "g",
                VizContractTest::graphFrom,
                GraphModel::apply,
                GraphModel::describe);
    }

    // ---------- danh sách liên kết ----------

    static LinkedListModel listFrom(JsonNode snap) {
        LinkedListModel m = new LinkedListModel();
        m.reset(snap);
        return m;
    }

    @Test
    void baiDaoNguoc_traceVaoDungBoVeDanhSach_keCaBienTro() throws Exception {
        String p = "dsa/dslk/dao-nguoc";
        kiem(
                bai(p, "loi_giai.py"),
                bai(p, "demo.in"),
                "ds",
                VizContractTest::listFrom,
                LinkedListModel::apply,
                LinkedListModel::describe);
        kiem(
                bai(p, "loi_giai.py"),
                "40\n" + "7 ".repeat(40) + "\n",
                "ds",
                VizContractTest::listFrom,
                LinkedListModel::apply,
                LinkedListModel::describe);
    }
}
