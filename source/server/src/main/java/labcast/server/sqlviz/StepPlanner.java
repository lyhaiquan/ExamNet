package labcast.server.sqlviz;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import labcast.common.protocol.JsonPayload;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.Offset;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.update.Update;

/**
 * Tách một câu SQL thành các bước theo thứ tự thực thi logic (spec §7.6) và sinh trace cho bộ vẽ {@code
 * bang-sql}: FROM → JOIN → WHERE → GROUP BY → hàm gộp → HAVING → SELECT → DISTINCT → ORDER BY → LIMIT.
 *
 * <p>Mọi điều kiện (ON, WHERE, HAVING) do <b>SQLite</b> tính, nên NULL và kiểu dữ liệu đúng như khi chạy
 * thật; Java chỉ gom nhóm, bỏ trùng và khớp dòng. Mỗi dòng của bảng gốc được nhận diện bằng {@code
 * rowid}; dòng sau JOIN là bộ {@code rowid} của mọi bảng (NULL ở vế không có cặp).
 *
 * <p>Mọi sự kiện chỉ tới dòng bằng <b>chỉ số trong bảng đang làm việc</b> lúc đó, nên app áp lại từ đầu
 * được (tua). Cuối cùng so bảng đang làm việc với kết quả chạy thật; lệch (một trường hợp chưa lường
 * trước) thì bỏ các bước, chỉ hiện kết quả cuối kèm lời giải thích — animation không bao giờ dạy sai.
 */
public final class StepPlanner {
    private static final Logger LOG = Logger.getLogger(StepPlanner.class.getName());

    /** Bảng gốc hay bảng sau JOIN dài hơn thế thì không làm animation, chỉ hiện kết quả. */
    static final int MAX_ANIM_ROWS = 60;
    /** Bảng từ ngần này dòng trở xuống thì đánh dấu WHERE/HAVING, ghép JOIN từng dòng một. */
    static final int PER_ROW = 12;

    private static final Pattern AGG = Pattern.compile(
            "\\b(count|sum|avg|min|max|total|group_concat|string_agg)\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern WINDOW = Pattern.compile("\\bover\\s*\\(", Pattern.CASE_INSENSITIVE);

    private final SqliteEngine engine;
    private final Path db;
    private final Duration timeout;
    private final List<ObjectNode> out = new ArrayList<>();

    /** Không làm animation được cho câu này; lời nhắn giải thích vì sao. */
    static final class Unsupported extends Exception {
        Unsupported(String message) {
            super(message);
        }
    }

    /** Tổng thời gian cho mọi câu phụ khi tách bước một câu: một câu chậm không giữ luồng chạy quá lâu. */
    static final Duration BUDGET = Duration.ofSeconds(3);
    /** Câu chính chạy lâu hơn thế thì không tách bước (mỗi bước lại chạy một câu tương tự). */
    static final Duration SLOW_QUERY = Duration.ofSeconds(1);

    private final Duration budget;
    private long deadline;

    public StepPlanner(SqliteEngine engine, Path db, Duration timeout) {
        this(engine, db, timeout, BUDGET);
    }

    StepPlanner(SqliteEngine engine, Path db, Duration timeout, Duration budget) {
        this.engine = engine;
        this.db = db;
        this.timeout = timeout;
        this.budget = budget;
    }

    /** Câu phụ khi tách bước: chung một hạn cho cả lượt; hết hạn thì thôi tách, chỉ hiện kết quả. */
    private QueryResult q(String sql) throws SqlRejected, Unsupported {
        long left = deadline - System.nanoTime();
        if (left <= 0) {
            throw new Unsupported("Câu lệnh chạy khá lâu nên không tách từng bước.");
        }
        return engine.query(db, sql, Duration.ofNanos(Math.min(timeout.toNanos(), left)));
    }

    /**
     * Trace của một câu SELECT (dòng JSON, có {@code hdr} và {@code end}). Câu lỗi thì ném {@link
     * SqlRejected} như khi chạy thật.
     */
    public List<String> explainSelect(String lessonId, SqlGuard.Checked c) throws SqlRejected {
        long t0 = System.nanoTime();
        QueryResult real = engine.query(db, c.sql(), timeout);
        long took = System.nanoTime() - t0;
        deadline = System.nanoTime() + budget.toNanos();
        out.clear();
        try {
            if (real.truncated()) {
                throw new Unsupported("Kết quả có hơn " + SqliteEngine.MAX_ROWS + " dòng.");
            }
            if (took > SLOW_QUERY.toNanos()) {
                throw new Unsupported("Câu lệnh chạy khá lâu nên không tách từng bước.");
            }
            Statement st = c.statement();
            if (st instanceof PlainSelect ps && ps.getWithItemsList() == null) {
                plainSelect(ps, real);
            } else if (st instanceof SetOperationList so && so.getWithItemsList() == null) {
                setOperation(so, real);
            } else {
                throw new Unsupported("Câu dùng WITH hoặc truy vấn con ở FROM: phần này chưa làm animation từng bước.");
            }
        } catch (Unsupported | SqlRejected e) {
            finalOnly(real, e.getMessage());
        } catch (RuntimeException e) {
            // Lỗi lập trình ở bộ tách bước: học viên vẫn thấy kết quả đúng, không bị báo lỗi oan
            LOG.log(Level.WARNING, "Tách bước câu SQL lỗi: " + c.sql(), e);
            finalOnly(real, "Chưa tách được từng bước cho câu này.");
        }
        return lines(lessonId, out);
    }

    private void finalOnly(QueryResult real, String why) {
        out.clear();
        say("Chỉ hiện kết quả cuối. " + why);
        ObjectNode r = ev("sql.result");
        putTable(r, real.columns(), real.rows());
    }

    // ------------------------------------------------------------------ SELECT đơn

    /** Bảng đang làm việc ở phía server — đi song song với model của app để biết chỉ số dòng. */
    private static final class Work {
        List<String> cols = new ArrayList<>();
        List<List<Object>> rows = new ArrayList<>();
        /** Bộ rowid của mỗi dòng (theo thứ tự các bảng trong FROM/JOIN); null khi đã gộp nhóm. */
        List<List<Long>> keys = new ArrayList<>();

        int size() {
            return rows.size();
        }

        void keep(List<Integer> idx) {
            List<List<Object>> r = new ArrayList<>();
            List<List<Long>> k = new ArrayList<>();
            for (int i : idx) {
                r.add(rows.get(i));
                k.add(keys.isEmpty() ? null : keys.get(i));
            }
            rows = r;
            keys = keys.isEmpty() ? keys : k;
        }
    }

    private record Base(String name, String qual, List<String> cols, List<List<Object>> rows, List<Long> rowids) {}

    private void plainSelect(PlainSelect ps, QueryResult real) throws Unsupported, SqlRejected {
        if (ps.getFromItem() == null) {
            throw new Unsupported("Câu không có FROM.");
        }
        String all = ps.toString();
        if (WINDOW.matcher(all).find()) {
            throw new Unsupported("Câu dùng hàm cửa sổ (OVER): phần này chưa làm animation từng bước.");
        }
        List<Join> joins = ps.getJoins() == null ? List.of() : ps.getJoins();
        List<Base> bases = new ArrayList<>();
        bases.add(base(ps.getFromItem()));
        for (Join j : joins) {
            if (j.isNatural()
                    || (j.getUsingColumns() != null && !j.getUsingColumns().isEmpty())) {
                throw new Unsupported("JOIN … USING / NATURAL JOIN chưa làm animation; viết lại bằng ON.");
            }
            bases.add(base(j.getRightItem()));
        }
        Set<String> quals = new HashSet<>();
        for (Base b : bases) {
            if (!quals.add(b.qual.toLowerCase(Locale.ROOT))) {
                throw new Unsupported("Bảng " + b.name + " xuất hiện hai lần mà không có tên gọi khác (alias).");
            }
        }
        Set<String> realColumns = new HashSet<>();
        for (Base b : bases) {
            b.cols.forEach(cn -> realColumns.add(cn.toLowerCase(Locale.ROOT)));
        }
        Aliases aliases = new Aliases(ps.getSelectItems(), realColumns);

        // Chỉ hiện các cột câu lệnh dùng tới (và cột đầu, thường là khoá): bảng ghép đủ mọi cột của mọi bảng
        // thì quá rộng, phải thu nhỏ tới không đọc được. Chỉ để hiển thị — mọi bước vẫn tính bằng câu SQL thật.
        boolean star = ps.getSelectItems().stream().anyMatch(it -> it.getExpression() instanceof AllColumns);
        if (!star) {
            Set<String> words = identifiers(all);
            for (int i = 0; i < bases.size(); i++) {
                bases.set(i, prune(bases.get(i), words));
            }
        }

        // FROM
        Base first = bases.get(0);
        key("FROM " + first.name);
        say("FROM " + label(first) + ": lấy cả bảng " + first.name + " (" + first.rows.size() + " dòng).");
        ObjectNode t = ev("sql.table").put("name", first.name).put("alias", first.qual);
        putTable(t, first.cols, first.rows);
        Work w = new Work();
        boolean qualified = !joins.isEmpty();
        w.cols = qualified ? qualify(first) : new ArrayList<>(first.cols);
        w.rows = new ArrayList<>(first.rows);
        for (Long id : first.rowids) {
            w.keys.add(new ArrayList<>(List.of(id)));
        }
        if (qualified) {
            ev("sql.rename").set("cols", strings(w.cols));
        }

        // JOIN, từng phép một
        StringBuilder from = new StringBuilder(ps.getFromItem().toString());
        for (int k = 0; k < joins.size(); k++) {
            Join j = joins.get(k);
            Base right = bases.get(k + 1);
            from.append(j.isSimple() ? ", " + j.getRightItem() : " " + j);
            join(w, j, right, bases.subList(0, k + 2), from.toString());
        }
        String fromText = from.toString();
        List<Base> used = bases;

        // WHERE
        if (ps.getWhere() != null) {
            String cond = aliases.expand(ps.getWhere().toString());
            List<Boolean> ok = flags(
                    rowidList(used) + ", CASE WHEN (" + cond + ") THEN 1 ELSE 0 END", fromText, "", w, used.size());
            key("WHERE " + ps.getWhere());
            say("WHERE " + ps.getWhere() + ": xét từng dòng, dòng không thoả (kể cả khi so với NULL) bị loại.");
            markAndKeep(w, ok, "WHERE");
        }

        String where = ps.getWhere() == null
                ? ""
                : " WHERE " + aliases.expand(ps.getWhere().toString());
        boolean grouped = ps.getGroupBy() != null || ps.getHaving() != null || hasAggregate(ps.getSelectItems());
        if (grouped) {
            grouped(ps, w, used, fromText, where, aliases);
        } else {
            String items = selectList(ps.getSelectItems());
            QueryResult p = q("SELECT " + rowidList(used) + ", " + items + " FROM " + fromText + where);
            Map<String, List<Object>> byKey = new HashMap<>();
            for (List<Object> r : p.rows()) {
                byKey.put(keyOf(r.subList(0, used.size())), r.subList(used.size(), r.size()));
            }
            List<List<Object>> rows = new ArrayList<>();
            for (List<Long> k : w.keys) {
                List<Object> r = byKey.get(keyOf(new ArrayList<>(k)));
                if (r == null) {
                    throw new Unsupported("Không khớp được dòng khi chọn cột.");
                }
                rows.add(r);
            }
            key("SELECT " + items);
            say("SELECT " + items + ": chỉ giữ, hoặc tính ra, các cột cần lấy.");
            ObjectNode e = ev("sql.project");
            List<String> cols = p.columns().subList(used.size(), p.columns().size());
            putTable(e, cols, rows);
            w.cols = new ArrayList<>(cols);
            w.rows = rows;
        }

        // DISTINCT
        if (ps.getDistinct() != null) {
            List<Integer> keep = new ArrayList<>();
            List<Integer> dup = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < w.size(); i++) {
                (seen.add(keyOf(w.rows.get(i))) ? keep : dup).add(i);
            }
            key("DISTINCT");
            say(
                    dup.isEmpty()
                            ? "DISTINCT: không có dòng nào trùng."
                            : "DISTINCT: " + dup.size()
                                    + " dòng trùng với dòng đứng trước bị bỏ, mỗi giá trị chỉ còn một dòng.");
            ev("sql.distinct").set("dup", ints(dup));
            w.keep(keep);
        }

        // ORDER BY
        List<OrderByElement> order = ps.getOrderByElements();
        Limit limit = ps.getLimit();
        Offset offset = ps.getOffset();
        if (order != null && !order.isEmpty()) {
            ps.setLimit(null);
            ps.setOffset(null);
            String full;
            try {
                full = ps.toString();
            } finally {
                ps.setLimit(limit);
                ps.setOffset(offset);
            }
            QueryResult sorted = q(full);
            List<Integer> perm = match(w.rows, sorted.rows());
            key("ORDER BY " + joinText(order));
            say("ORDER BY " + joinText(order) + ": sắp lại các dòng.");
            ev("sql.order").set("perm", ints(perm));
            w.keep(perm);
        }

        // LIMIT / OFFSET
        if (limit != null || offset != null) {
            int start = locate(w.rows, real.rows());
            String text = (limit == null ? "" : limit.toString().trim())
                    + (offset == null ? "" : " " + offset.toString().trim());
            key(text.trim());
            say(text.trim() + ": chỉ lấy " + real.rows().size() + " dòng"
                    + (start > 0 ? " sau khi bỏ qua " + start + " dòng đầu." : " đầu tiên."));
            ev("sql.limit").put("offset", start).put("count", real.rows().size());
            List<Integer> keep = new ArrayList<>();
            for (int i = start; i < start + real.rows().size(); i++) {
                keep.add(i);
            }
            w.keep(keep);
        }

        // Kiểm cuối: các bước phải ra đúng kết quả chạy thật
        QueryResult mine = new QueryResult(w.cols, w.rows, false);
        boolean ordered = order != null && !order.isEmpty();
        if (!ResultComparer.same(mine, real, ordered)) {
            throw new Unsupported("Các bước tách ra không khớp kết quả chạy thật.");
        }
        key("Kết quả");
        say("Kết quả: " + real.rows().size() + " dòng, " + real.columns().size() + " cột.");
        ObjectNode r = ev("sql.result");
        putTable(r, real.columns(), real.rows());
    }

    private Base base(FromItem item) throws Unsupported, SqlRejected {
        if (!(item instanceof Table t)) {
            throw new Unsupported("Truy vấn con ở FROM chưa làm animation từng bước.");
        }
        String name = t.getFullyQualifiedName();
        String qual = t.getAlias() != null ? t.getAlias().getName() : name;
        QueryResult r = q("SELECT rowid AS __rid, * FROM " + name);
        if (r.rows().size() > MAX_ANIM_ROWS) {
            throw new Unsupported("Bảng " + name + " có " + r.rows().size() + " dòng, quá nhiều để làm animation.");
        }
        List<Long> ids = new ArrayList<>();
        List<List<Object>> rows = new ArrayList<>();
        for (List<Object> row : r.rows()) {
            ids.add((Long) row.get(0));
            rows.add(row.subList(1, row.size()));
        }
        return new Base(name, qual, r.columns().subList(1, r.columns().size()), rows, ids);
    }

    /** Mọi tên (chữ, số, gạch dưới) xuất hiện trong câu, viết thường; bỏ qua nội dung chuỗi '…'. */
    static Set<String> identifiers(String sql) {
        Set<String> out = new HashSet<>();
        Matcher m = Pattern.compile("'(?:[^']|'')*'|\"([^\"]*)\"|([A-Za-z_][A-Za-z0-9_]*)")
                .matcher(sql);
        while (m.find()) {
            String w = m.group(2) != null ? m.group(2) : m.group(1);
            if (w != null) {
                out.add(w.toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    /** Bảng gốc chỉ giữ cột đầu và các cột có tên xuất hiện trong câu lệnh. */
    private static Base prune(Base b, Set<String> words) {
        List<Integer> keep = new ArrayList<>();
        for (int k = 0; k < b.cols.size(); k++) {
            if (k == 0 || words.contains(b.cols.get(k).toLowerCase(Locale.ROOT))) {
                keep.add(k);
            }
        }
        if (keep.size() == b.cols.size()) {
            return b;
        }
        List<String> cols = new ArrayList<>();
        keep.forEach(k -> cols.add(b.cols.get(k)));
        List<List<Object>> rows = new ArrayList<>();
        for (List<Object> r : b.rows) {
            List<Object> pr = new ArrayList<>();
            keep.forEach(k -> pr.add(r.get(k)));
            rows.add(pr);
        }
        return new Base(b.name, b.qual, cols, rows, b.rowids);
    }

    private void join(Work w, Join j, Base right, List<Base> used, String fromText) throws Unsupported, SqlRejected {
        String kind = j.isSimple() || j.isCross()
                ? "CROSS"
                : j.isLeft() ? "LEFT" : j.isRight() ? "RIGHT" : j.isFull() ? "FULL" : "INNER";
        key(j.isSimple() ? ", " + right.name : j.toString());
        String on = j.getOnExpressions() == null || j.getOnExpressions().isEmpty()
                ? ""
                : " ON " + joinText(new ArrayList<>(j.getOnExpressions()));
        say(
                switch (kind) {
                    case "CROSS" -> "Ghép mọi dòng với mọi dòng của " + right.name + ".";
                    case "LEFT" -> "LEFT JOIN " + label(right) + on
                            + ": ghép từng dòng bên trái với các dòng khớp; dòng không có cặp vẫn giữ, phần thiếu là NULL.";
                    case "RIGHT" -> "RIGHT JOIN " + label(right) + on
                            + ": dòng bên phải không có cặp vẫn giữ, phần bên trái là NULL.";
                    case "FULL" -> "FULL JOIN " + label(right) + on + ": giữ dòng không có cặp của cả hai bên.";
                    default -> "JOIN " + label(right) + on + ": ghép mỗi dòng với các dòng của " + right.name
                            + " thoả điều kiện; dòng không có cặp bị loại.";
                });
        ObjectNode t =
                ev("sql.table").put("name", right.name).put("alias", right.qual).put("side", true);
        putTable(t, right.cols, right.rows);

        QueryResult r = q("SELECT " + rowidList(used) + " FROM " + fromText);
        if (r.rows().size() > MAX_ANIM_ROWS * 3) {
            throw new Unsupported("Phép ghép ra " + r.rows().size() + " dòng, quá nhiều để làm animation.");
        }
        Map<String, Integer> leftIdx = new HashMap<>();
        for (int i = 0; i < w.keys.size(); i++) {
            leftIdx.put(keyOf(new ArrayList<>(w.keys.get(i))), i);
        }
        Map<Long, Integer> rightIdx = new HashMap<>();
        for (int i = 0; i < right.rowids.size(); i++) {
            rightIdx.put(right.rowids.get(i), i);
        }
        int n = used.size();
        List<int[]> pairs = new ArrayList<>();
        for (List<Object> row : r.rows()) {
            List<Object> prefix = row.subList(0, n - 1);
            boolean leftNull = prefix.stream().allMatch(x -> x == null);
            Integer li = leftNull ? Integer.valueOf(-1) : leftIdx.get(keyOf(prefix));
            Object rid = row.get(n - 1);
            Integer ri = rid == null ? Integer.valueOf(-1) : rightIdx.get((Long) rid);
            if (li == null || ri == null) {
                throw new Unsupported("Không khớp được dòng khi ghép bảng.");
            }
            pairs.add(new int[] {li, ri});
        }
        Comparator<Integer> lastNeg = (a, b) -> a < 0 == b < 0 ? Integer.compare(a, b) : a < 0 ? 1 : -1;
        pairs.sort((a, b) -> a[0] != b[0] ? lastNeg.compare(a[0], b[0]) : lastNeg.compare(a[1], b[1]));

        if (pairs.size() <= PER_ROW * 2) {
            for (int[] p : pairs) {
                ev("sql.pair").put("l", p[0]).put("r", p[1]);
            }
        }
        List<String> cols = new ArrayList<>(w.cols);
        cols.addAll(qualify(right));
        int leftWidth = w.cols.size();
        List<List<Object>> rows = new ArrayList<>();
        List<List<Long>> keys = new ArrayList<>();
        for (int[] p : pairs) {
            List<Object> row = new ArrayList<>();
            List<Long> k = new ArrayList<>();
            if (p[0] >= 0) {
                row.addAll(w.rows.get(p[0]));
                k.addAll(w.keys.get(p[0]));
            } else {
                for (int i = 0; i < leftWidth; i++) {
                    row.add(null);
                }
                for (int i = 0; i < n - 1; i++) {
                    k.add(null);
                }
            }
            if (p[1] >= 0) {
                row.addAll(right.rows.get(p[1]));
                k.add(right.rowids.get(p[1]));
            } else {
                for (int i = 0; i < right.cols.size(); i++) {
                    row.add(null);
                }
                k.add(null);
            }
            rows.add(row);
            keys.add(k);
        }
        ObjectNode e = ev("sql.join").put("kind", kind);
        ArrayNode ps = e.putArray("pairs");
        for (int[] p : pairs) {
            ps.addArray().add(p[0]).add(p[1]);
        }
        putTable(e, cols, rows);
        w.cols = cols;
        w.rows = rows;
        w.keys = keys;
        say("Sau khi ghép: " + rows.size() + " dòng.");
    }

    private void grouped(PlainSelect ps, Work w, List<Base> used, String fromText, String where, Aliases aliases)
            throws Unsupported, SqlRejected {
        List<String> gs = new ArrayList<>();
        if (ps.getGroupBy() != null && ps.getGroupBy().getGroupByExpressionList() != null) {
            for (Object o : ps.getGroupBy().getGroupByExpressionList()) {
                gs.add(aliases.resolve((Expression) o));
            }
        }
        String gList = String.join(", ", gs);
        String groupBy = gs.isEmpty() ? "" : " GROUP BY " + gList;

        // GROUP BY: SQLite tính khoá nhóm của từng dòng, Java gom (NULL cùng một nhóm như SQLite)
        List<List<Integer>> groups = new ArrayList<>();
        List<List<Object>> groupKeys = new ArrayList<>();
        if (gs.isEmpty()) {
            List<Integer> allRows = new ArrayList<>();
            for (int i = 0; i < w.size(); i++) {
                allRows.add(i);
            }
            groups.add(allRows);
            groupKeys.add(List.of());
            key("Hàm gộp trên cả bảng");
            say("Không có GROUP BY nhưng có hàm gộp: cả bảng (" + w.size() + " dòng) là một nhóm.");
        } else {
            QueryResult k = q("SELECT " + rowidList(used) + ", " + gList + " FROM " + fromText + where);
            Map<String, List<Object>> keyOfRow = new HashMap<>();
            for (List<Object> r : k.rows()) {
                keyOfRow.put(keyOf(r.subList(0, used.size())), r.subList(used.size(), r.size()));
            }
            // Thứ tự nhóm theo thứ tự SQLite trả kết quả GROUP BY
            QueryResult order = q("SELECT " + gList + " FROM " + fromText + where + groupBy);
            Map<String, Integer> pos = new LinkedHashMap<>();
            for (List<Object> r : order.rows()) {
                pos.put(keyOf(r), groups.size());
                groups.add(new ArrayList<>());
                groupKeys.add(r);
            }
            for (int i = 0; i < w.size(); i++) {
                List<Object> gk = keyOfRow.get(keyOf(new ArrayList<>(w.keys.get(i))));
                Integer g = gk == null ? null : pos.get(keyOf(gk));
                if (g == null) {
                    throw new Unsupported("Không khớp được dòng khi gom nhóm.");
                }
                groups.get(g).add(i);
            }
            key("GROUP BY " + gList);
            say("GROUP BY " + gList + ": gom các dòng cùng giá trị thành " + groups.size() + " nhóm.");
        }
        ObjectNode ge = ev("sql.group");
        ArrayNode ga = ge.putArray("groups");
        for (List<Integer> g : groups) {
            ga.add(ints(g));
        }
        ArrayNode ka = ge.putArray("keys");
        for (List<Object> gk : groupKeys) {
            ka.add(values(gk));
        }
        ge.put("by", gList);

        // Hàm gộp + SELECT: mỗi nhóm thành một dòng
        String items = selectList(ps.getSelectItems());
        QueryResult agg =
                q("SELECT " + (gs.isEmpty() ? "" : gList + ", ") + items + " FROM " + fromText + where + groupBy);
        Map<String, List<Object>> aggByKey = new HashMap<>();
        for (List<Object> r : agg.rows()) {
            aggByKey.put(keyOf(r.subList(0, gs.size())), r.subList(gs.size(), r.size()));
        }
        List<List<Object>> rows = new ArrayList<>();
        for (List<Object> gk : groupKeys) {
            List<Object> r = aggByKey.get(keyOf(gk));
            if (r == null) {
                throw new Unsupported("Không khớp được nhóm khi tính hàm gộp.");
            }
            rows.add(r);
        }
        List<String> cols = agg.columns().subList(gs.size(), agg.columns().size());
        key("SELECT " + items);
        say("SELECT " + items + ": tính cho từng nhóm, mỗi nhóm còn một dòng.");
        ObjectNode ae = ev("sql.agg");
        putTable(ae, cols, rows);
        w.cols = new ArrayList<>(cols);
        w.rows = rows;
        w.keys = new ArrayList<>();
        List<List<Object>> keysNow = new ArrayList<>(groupKeys);

        // HAVING: SQLite tính điều kiện cho từng nhóm
        if (ps.getHaving() != null) {
            String cond = aliases.expand(ps.getHaving().toString());
            QueryResult h = q("SELECT " + (gs.isEmpty() ? "" : gList + ", ") + "CASE WHEN (" + cond
                    + ") THEN 1 ELSE 0 END FROM " + fromText + where + groupBy);
            Map<String, Boolean> pass = new HashMap<>();
            for (List<Object> r : h.rows()) {
                pass.put(keyOf(r.subList(0, gs.size())), Long.valueOf(1).equals(r.get(gs.size())));
            }
            List<Boolean> ok = new ArrayList<>();
            for (List<Object> gk : keysNow) {
                Boolean b = pass.get(keyOf(gk));
                if (b == null) {
                    throw new Unsupported("Không khớp được nhóm khi xét HAVING.");
                }
                ok.add(b);
            }
            key("HAVING " + ps.getHaving());
            say("HAVING " + ps.getHaving() + ": xét từng nhóm, nhóm không thoả bị loại.");
            markAndKeep(w, ok, "HAVING");
        }
    }

    // ------------------------------------------------------------------ UNION, INTERSECT, EXCEPT

    /**
     * Các phép tập hợp trong SQLite cùng độ ưu tiên, tính từ trái sang phải: {@code A EXCEPT B UNION C} là
     * {@code (A EXCEPT B) UNION C}. Mỗi phép một bước: kết quả tạm của các vế bên trái (SQLite tính lại phần đầu
     * của câu) gặp vế tiếp theo.
     */
    private void setOperation(SetOperationList so, QueryResult real) throws Unsupported, SqlRejected {
        List<Select> sides = so.getSelects();
        List<String> ops = new ArrayList<>();
        for (Object o : so.getOperations()) {
            ops.add(o.toString().trim().toUpperCase(Locale.ROOT));
        }
        for (int i = 0; i < 2; i++) {
            side(sides.get(i), i);
        }
        StringBuilder prefix = new StringBuilder(sides.get(0).toString());
        for (int k = 0; k < ops.size(); k++) {
            if (k > 0) {
                side(sides.get(k + 1), k + 1);
            }
            String op = ops.get(k);
            prefix.append(' ').append(op).append(' ').append(sides.get(k + 1));
            boolean last = k == ops.size() - 1;
            QueryResult r = last ? real : q(prefix.toString());
            if (r.rows().size() > MAX_ANIM_ROWS) {
                throw new Unsupported("Kết quả tạm có quá nhiều dòng để làm animation.");
            }
            String left = k == 0 ? "vế 1" : "kết quả tạm";
            String right = "vế " + (k + 2);
            key(op);
            say(switch (op) {
                        case "UNION" -> "UNION: gộp " + left + " với " + right + ", bỏ dòng trùng.";
                        case "UNION ALL" -> "UNION ALL: gộp " + left + " với " + right + ", giữ cả dòng trùng.";
                        case "INTERSECT" -> "INTERSECT: chỉ giữ dòng có ở cả " + left + " và " + right + ".";
                        case "EXCEPT" -> "EXCEPT: dòng của " + left + " mà " + right + " không có.";
                        default -> op + ".";
                    } + (last ? " Kết quả " : " Kết quả tạm ") + r.rows().size() + " dòng.");
            ObjectNode e = ev("sql.setop").put("op", op);
            putTable(e, r.columns(), r.rows());
        }
    }

    /** Vế thứ {@code i} (đếm từ 0) của phép tập hợp: vế đầu là bảng chính, các vế sau đứng cạnh. */
    private void side(Select s, int i) throws Unsupported, SqlRejected {
        QueryResult r = q(s.toString());
        if (r.rows().size() > MAX_ANIM_ROWS) {
            throw new Unsupported("Vế " + (i + 1) + " có quá nhiều dòng để làm animation.");
        }
        key("Vế " + (i + 1));
        say("Vế " + (i + 1) + ": " + s + " → " + r.rows().size() + " dòng.");
        ObjectNode t =
                ev("sql.table").put("name", "Vế " + (i + 1)).put("alias", "").put("side", i > 0);
        putTable(t, r.columns(), r.rows());
    }

    // ------------------------------------------------------------------ DML

    /**
     * Trace của câu INSERT/UPDATE/DELETE: bảng bị đụng trước khi chạy, rồi các dòng thêm/sửa/xoá. Chạy trên
     * {@code copy} (bản sao); trả trace, câu chạy thật xong {@code copy} là trạng thái sau.
     */
    public List<String> explainDml(String lessonId, SqlGuard.Checked c, Path copy) throws SqlRejected {
        out.clear();
        Statement st = c.statement();
        Table target;
        if (st instanceof Update u) {
            target = u.getTable();
        } else if (st instanceof Delete d) {
            target = d.getTable();
        } else if (st instanceof Insert i) {
            target = i.getTable();
        } else {
            throw new SqlRejected("Không phải câu thay đổi dữ liệu.");
        }
        String name = target.getFullyQualifiedName();
        String select = "SELECT rowid AS __rid, * FROM " + name;
        QueryResult before = engine.query(copy, select, timeout);
        int changed = engine.execute(copy, c.sql(), timeout);
        QueryResult after = engine.query(copy, select, timeout);
        List<String> cols = before.columns().subList(1, before.columns().size());
        if (before.rows().size() > MAX_ANIM_ROWS || after.rows().size() > MAX_ANIM_ROWS) {
            say("Bảng " + name + " quá lớn để làm animation; câu lệnh đổi " + changed + " dòng.");
            ObjectNode r = ev("sql.result");
            putTable(r, cols, strip(after.rows()));
            return lines(lessonId, out);
        }
        try {
            diff(name, cols, before, after, c);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Tách bước câu DML lỗi: " + c.sql(), e);
            out.clear();
            say("Chỉ hiện bảng sau khi chạy. Câu lệnh đổi " + changed + " dòng.");
            putTable(ev("sql.result"), cols, strip(after.rows()));
        }
        return lines(lessonId, out);
    }

    /** Bảng trước khi chạy, rồi các dòng thêm/sửa (đúng ô)/xoá theo rowid. */
    private void diff(String name, List<String> cols, QueryResult before, QueryResult after, SqlGuard.Checked c) {
        key("Bảng " + name + " trước khi chạy");
        say("Bảng " + name + " trước khi chạy câu lệnh (" + before.rows().size() + " dòng).");
        ObjectNode t = ev("sql.table").put("name", name).put("alias", name);
        putTable(t, cols, strip(before.rows()));

        Map<Object, List<Object>> afterById = new LinkedHashMap<>();
        for (List<Object> r : after.rows()) {
            afterById.put(r.get(0), r);
        }
        ArrayNode changes = JsonPayload.MAPPER.createArrayNode();
        Set<Object> seen = new HashSet<>();
        int sua = 0;
        int xoa = 0;
        int them = 0;
        for (int i = 0; i < before.rows().size(); i++) {
            List<Object> b = before.rows().get(i);
            List<Object> a = afterById.get(b.get(0));
            seen.add(b.get(0));
            if (a == null) {
                changes.addObject().put("kind", "xoa").put("i", i);
                xoa++;
            } else if (!ResultComparer.sameRow(b, a)) {
                ArrayNode cells = JsonPayload.MAPPER.createArrayNode();
                for (int k = 1; k < b.size(); k++) {
                    if (!ResultComparer.sameValue(b.get(k), a.get(k))) {
                        cells.add(k - 1);
                    }
                }
                ObjectNode ch = changes.addObject().put("kind", "sua").put("i", i);
                ch.set("cells", cells);
                ch.set("row", values(a.subList(1, a.size())));
                sua++;
            }
        }
        for (List<Object> a : after.rows()) {
            if (!seen.contains(a.get(0))) {
                changes.addObject().put("kind", "them").set("row", values(a.subList(1, a.size())));
                them++;
            }
        }
        key(statementKind(c.statement()));
        say(c.sql().strip() + " → " + (them > 0 ? them + " dòng thêm" : sua > 0 ? sua + " dòng sửa" : xoa + " dòng xoá")
                + (them + sua + xoa == 0 ? " (không dòng nào thoả điều kiện)." : "."));
        ev("sql.diff").set("changes", changes);
        say("Bảng " + name + " sau khi chạy: " + after.rows().size() + " dòng.");
    }

    private static String statementKind(Statement s) {
        return s instanceof Update ? "UPDATE" : s instanceof Delete ? "DELETE" : "INSERT";
    }

    // ------------------------------------------------------------------ tiện ích

    /** Tên gọi khác của cột SELECT, để GROUP BY, ORDER BY, WHERE, HAVING dùng tên gọi khác vẫn tách được bước. */
    private static final class Aliases {
        final List<SelectItem<?>> items;
        final Map<String, String> byName = new HashMap<>();
        final Set<String> realColumns;

        Aliases(List<SelectItem<?>> items, Set<String> realColumns) {
            this.items = items;
            this.realColumns = realColumns;
            for (SelectItem<?> it : items) {
                if (it.getAlias() != null && !(it.getExpression() instanceof AllColumns)) {
                    byName.put(it.getAlias().getName().toLowerCase(Locale.ROOT), "(" + it.getExpression() + ")");
                }
            }
        }

        /** GROUP BY 1, GROUP BY tên_gọi_khác → biểu thức thật. */
        String resolve(Expression e) {
            if (e instanceof LongValue lv) {
                int i = (int) lv.getValue();
                if (i >= 1 && i <= items.size()) {
                    return "(" + items.get(i - 1).getExpression() + ")";
                }
            }
            if (e instanceof Column c && c.getTable() == null) {
                String n = c.getColumnName().toLowerCase(Locale.ROOT);
                if (byName.containsKey(n) && !realColumns.contains(n)) {
                    return byName.get(n);
                }
            }
            return e.toString();
        }

        /** Thay tên gọi khác đứng riêng (không nằm trong chuỗi) bằng biểu thức thật. */
        String expand(String sql) {
            if (byName.isEmpty()) {
                return sql;
            }
            StringBuilder sb = new StringBuilder();
            Matcher m = Pattern.compile(
                            "'(?:[^']|'')*'|\"[^\"]*\"|[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?")
                    .matcher(sql);
            int last = 0;
            while (m.find()) {
                String tok = m.group();
                String low = tok.toLowerCase(Locale.ROOT);
                sb.append(sql, last, m.start());
                boolean replace = !tok.startsWith("'")
                        && !tok.startsWith("\"")
                        && !tok.contains(".")
                        && byName.containsKey(low)
                        && !realColumns.contains(low);
                sb.append(replace ? byName.get(low) : tok);
                last = m.end();
            }
            sb.append(sql.substring(last));
            return sb.toString();
        }
    }

    private void markAndKeep(Work w, List<Boolean> ok, String clause) {
        if (w.size() <= PER_ROW) {
            for (int i = 0; i < ok.size(); i++) {
                ObjectNode m = ev("sql.mark");
                m.putArray("rows").add(i);
                m.putArray("ok").add(ok.get(i));
            }
        } else {
            ObjectNode m = ev("sql.mark");
            ArrayNode rows = m.putArray("rows");
            ArrayNode oks = m.putArray("ok");
            for (int i = 0; i < ok.size(); i++) {
                rows.add(i);
                oks.add(ok.get(i));
            }
        }
        List<Integer> keep = new ArrayList<>();
        for (int i = 0; i < ok.size(); i++) {
            if (ok.get(i)) {
                keep.add(i);
            }
        }
        ev("sql.keep").set("rows", ints(keep));
        say(clause + ": còn " + keep.size() + "/" + ok.size() + (clause.equals("HAVING") ? " nhóm." : " dòng."));
        w.keep(keep);
    }

    /** Cờ đúng/sai cho từng dòng đang làm việc, theo cột cuối của câu {@code SELECT <rowids>, <cờ> FROM …}. */
    private List<Boolean> flags(String select, String fromText, String where, Work w, int n)
            throws Unsupported, SqlRejected {
        QueryResult r = q("SELECT " + select + " FROM " + fromText + where);
        Map<String, Boolean> byKey = new HashMap<>();
        for (List<Object> row : r.rows()) {
            byKey.put(keyOf(row.subList(0, n)), Long.valueOf(1).equals(row.get(n)));
        }
        List<Boolean> ok = new ArrayList<>();
        for (List<Long> k : w.keys) {
            Boolean b = byKey.get(keyOf(new ArrayList<>(k)));
            if (b == null) {
                throw new Unsupported("Không khớp được dòng khi xét điều kiện.");
            }
            ok.add(b);
        }
        return ok;
    }

    /** Hoán vị đưa {@code rows} về đúng thứ tự {@code sorted} (hai bên cùng là một đa tập dòng). */
    private static List<Integer> match(List<List<Object>> rows, List<List<Object>> sorted) throws Unsupported {
        if (rows.size() != sorted.size()) {
            throw new Unsupported("Số dòng trước và sau khi sắp không khớp.");
        }
        boolean[] used = new boolean[rows.size()];
        List<Integer> perm = new ArrayList<>();
        for (List<Object> s : sorted) {
            int found = -1;
            for (int i = 0; i < rows.size() && found < 0; i++) {
                if (!used[i] && ResultComparer.sameRow(rows.get(i), s)) {
                    found = i;
                }
            }
            if (found < 0) {
                throw new Unsupported("Không khớp được dòng khi sắp xếp.");
            }
            used[found] = true;
            perm.add(found);
        }
        return perm;
    }

    /** Vị trí bắt đầu của {@code part} (kết quả có LIMIT) trong {@code rows} (đã sắp). */
    private static int locate(List<List<Object>> rows, List<List<Object>> part) throws Unsupported {
        for (int s = 0; s + part.size() <= rows.size(); s++) {
            boolean ok = true;
            for (int i = 0; i < part.size() && ok; i++) {
                ok = ResultComparer.sameRow(rows.get(s + i), part.get(i));
            }
            if (ok) {
                return s;
            }
        }
        throw new Unsupported("Không tìm được đoạn LIMIT trong bảng đã sắp.");
    }

    /** Có hàm gộp của chính câu này (hàm gộp nằm trong câu con như {@code (SELECT AVG(…) …)} không tính). */
    private static boolean hasAggregate(List<SelectItem<?>> items) {
        for (SelectItem<?> it : items) {
            if (AGG.matcher(withoutSubqueries(String.valueOf(it.getExpression())))
                    .find()) {
                return true;
            }
        }
        return false;
    }

    /** Bỏ các câu con {@code (SELECT …)} (cặp ngoặc cân bằng, không xét ngoặc trong chuỗi). */
    static String withoutSubqueries(String sql) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (c == '\'') {
                int j = sql.indexOf('\'', i + 1);
                j = j < 0 ? sql.length() : j + 1;
                sb.append(sql, i, j);
                i = j;
            } else if (c == '(' && sql.substring(i + 1).stripLeading().regionMatches(true, 0, "select", 0, 6)) {
                int depth = 0;
                int j = i;
                for (; j < sql.length(); j++) {
                    char d = sql.charAt(j);
                    if (d == '(') {
                        depth++;
                    } else if (d == ')' && --depth == 0) {
                        break;
                    }
                }
                sb.append("(?)");
                i = j + 1;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static String selectList(List<SelectItem<?>> items) {
        List<String> parts = new ArrayList<>();
        for (SelectItem<?> it : items) {
            parts.add(it.toString());
        }
        return String.join(", ", parts);
    }

    private static String rowidList(List<Base> bases) {
        List<String> parts = new ArrayList<>();
        for (Base b : bases) {
            parts.add(b.qual + ".rowid");
        }
        return String.join(", ", parts);
    }

    private static List<String> qualify(Base b) {
        List<String> out = new ArrayList<>();
        for (String c : b.cols) {
            out.add(b.qual + "." + c);
        }
        return out;
    }

    private static String label(Base b) {
        return b.qual.equals(b.name) ? b.name : b.name + " " + b.qual;
    }

    private static String joinText(List<?> parts) {
        List<String> s = new ArrayList<>();
        for (Object p : parts) {
            s.add(String.valueOf(p));
        }
        return String.join(", ", s);
    }

    /** Khoá so sánh của một dòng/bộ giá trị: số nguyên và số thực cùng giá trị cho cùng khoá, NULL riêng. */
    static String keyOf(List<?> values) {
        StringBuilder sb = new StringBuilder();
        for (Object v : values) {
            if (v == null) {
                sb.append("N|");
            } else if (v instanceof Number n) {
                double d = n.doubleValue();
                sb.append(d == Math.rint(d) && Math.abs(d) < 1e15 ? Long.toString((long) d) : Double.toString(d))
                        .append("#|");
            } else {
                sb.append('s').append(v.toString().replace("|", "||")).append("|");
            }
        }
        return sb.toString();
    }

    private static List<List<Object>> strip(List<List<Object>> rowsWithId) {
        List<List<Object>> out = new ArrayList<>();
        for (List<Object> r : rowsWithId) {
            out.add(r.subList(1, r.size()));
        }
        return out;
    }

    private ObjectNode ev(String t) {
        ObjectNode n = JsonPayload.MAPPER.createObjectNode().put("t", t).put("v", "sql");
        out.add(n);
        return n;
    }

    private void say(String s) {
        out.add(JsonPayload.MAPPER.createObjectNode().put("t", "say").put("s", s));
    }

    private void key(String s) {
        out.add(JsonPayload.MAPPER.createObjectNode().put("t", "key").put("s", s));
    }

    private static void putTable(ObjectNode n, List<String> cols, List<List<Object>> rows) {
        n.set("cols", strings(cols));
        ArrayNode rs = n.putArray("rows");
        for (List<Object> r : rows) {
            rs.add(values(r));
        }
    }

    private static ArrayNode strings(List<String> s) {
        ArrayNode a = JsonPayload.MAPPER.createArrayNode();
        s.forEach(a::add);
        return a;
    }

    private static ArrayNode ints(List<Integer> s) {
        ArrayNode a = JsonPayload.MAPPER.createArrayNode();
        s.forEach(a::add);
        return a;
    }

    static ArrayNode values(List<Object> r) {
        ArrayNode a = JsonPayload.MAPPER.createArrayNode();
        for (Object v : r) {
            if (v == null) {
                a.addNull();
            } else if (v instanceof Long l) {
                a.add(l);
            } else if (v instanceof Double d) {
                a.add(d);
            } else {
                a.add(v.toString());
            }
        }
        return a;
    }

    private static List<String> lines(String lessonId, List<ObjectNode> events) {
        List<String> lines = new ArrayList<>();
        ObjectNode hdr = JsonPayload.MAPPER.createObjectNode().put("t", "hdr").put("lesson", lessonId);
        hdr.putArray("views").addObject().put("v", "sql").put("kieu", "bang-sql");
        lines.add(hdr.toString());
        for (ObjectNode e : events) {
            lines.add(e.toString());
        }
        lines.add("{\"t\":\"end\",\"truncated\":false}");
        return lines;
    }
}
