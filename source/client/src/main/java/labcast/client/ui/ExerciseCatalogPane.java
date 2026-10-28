package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

/**
 * Kho bài tập: thẻ chương ở trên; bên trái danh sách chương và loại bài; bên phải bảng bài có tìm
 * kiếm, lọc trạng thái và chia trang. Chỉ hiện môn có trong danh sách bài của lớp. Giáo viên tạm
 * ẩn/mở và xoá bài ngay trong bảng.
 */
public final class ExerciseCatalogPane extends ScrollPane {
    private static final int PAGE = 10;
    private static final List<String> KINDS = List.of("giang-giai", "bai-tap", "luyen-tap");

    private final List<JsonNode> lessons = new ArrayList<>();
    private final Consumer<String> onOpen;
    private final boolean student;
    private BiConsumer<String, Boolean> onToggle = (id, on) -> {};
    private BiConsumer<String, String> onDelete = (id, name) -> {};

    private final ComboBox<String> subjectBox = new ComboBox<>();
    private final TextField search = new TextField();
    private final ComboBox<String> statusBox = new ComboBox<>();
    private final HBox chapterCards = new HBox(14);
    private final VBox chapterList = new VBox(2);
    private final VBox kindList = new VBox(8);
    private final VBox table = new VBox();
    private final HBox pager = new HBox(6);
    private final Label subtitle = new Label();
    private final HBox summary = new HBox(8);

    private String chapter;
    private final Set<String> kinds = new HashSet<>();
    private int page;

    public ExerciseCatalogPane(boolean student, Consumer<String> onOpen) {
        this.student = student;
        this.onOpen = onOpen;
        getStyleClass().addAll("dash-scroll", "catalog");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);

        Label heading = new Label("Kho bài tập");
        heading.getStyleClass().add("dash-name");
        subtitle.getStyleClass().add("dash-sub");
        VBox title = new VBox(6, heading, subtitle, summary);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        subjectBox.getStyleClass().add("catalog-select");
        subjectBox.setOnAction(e -> {
            chapter = null;
            page = 0;
            render();
        });
        HBox header = new HBox(12, title, gap, subjectBox);
        header.setAlignment(Pos.CENTER_LEFT);

        ScrollPane cardsScroll = new ScrollPane(chapterCards);
        cardsScroll.getStyleClass().add("catalog-cards-scroll");
        cardsScroll.setFitToHeight(true);
        cardsScroll.setVbarPolicy(ScrollBarPolicy.NEVER);
        cardsScroll.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
        // không để hàng thẻ đòi rộng bằng tổng các thẻ, nếu không cả trang nở theo và thanh trượt không bao giờ hiện
        cardsScroll.setMinWidth(0);
        cardsScroll.setPrefWidth(0);
        chapterCards.getStyleClass().add("catalog-cards");
        // lăn chuột dọc trên hàng thẻ thì trượt ngang; hai nút ‹ › trượt một thẻ
        cardsScroll.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
            double w = chapterCards.getWidth() - cardsScroll.getViewportBounds().getWidth();
            if (w <= 0 || e.getDeltaY() == 0) return;
            cardsScroll.setHvalue(Math.max(0, Math.min(1, cardsScroll.getHvalue() - e.getDeltaY() / w)));
            e.consume();
        });
        Button prev = slideButton("‹", cardsScroll, -1);
        Button next = slideButton("›", cardsScroll, 1);
        javafx.scene.layout.StackPane cardsBox = new javafx.scene.layout.StackPane(cardsScroll, prev, next);
        cardsBox.setMinWidth(0);
        javafx.scene.layout.StackPane.setAlignment(prev, Pos.CENTER_LEFT);
        javafx.scene.layout.StackPane.setAlignment(next, Pos.CENTER_RIGHT);
        // nút chỉ hiện khi còn thẻ khuất ở phía đó
        Runnable arrows = () -> {
            boolean overflow =
                    chapterCards.getWidth() > cardsScroll.getViewportBounds().getWidth() + 1;
            prev.setVisible(overflow && cardsScroll.getHvalue() > 0.001);
            next.setVisible(overflow && cardsScroll.getHvalue() < 0.999);
        };
        cardsScroll.hvalueProperty().addListener((o, a, b) -> arrows.run());
        chapterCards.widthProperty().addListener((o, a, b) -> arrows.run());
        cardsScroll.viewportBoundsProperty().addListener((o, a, b) -> arrows.run());

        Label topicTitle = new Label("CHỦ ĐỀ");
        topicTitle.getStyleClass().add("catalog-side-title");
        Label kindTitle = new Label("LOẠI BÀI");
        kindTitle.getStyleClass().add("catalog-side-title");
        VBox side = new VBox(8, topicTitle, chapterList, new Region(), kindTitle, kindList);
        side.getStyleClass().addAll("dash-card", "catalog-side");
        side.setMinWidth(230);
        side.setPrefWidth(250);
        side.setMaxWidth(260);

        search.setPromptText("Tìm kiếm bài tập...");
        search.getStyleClass().add("catalog-search");
        search.textProperty().addListener((o, a, b) -> {
            page = 0;
            render();
        });
        HBox.setHgrow(search, Priority.ALWAYS);
        statusBox
                .getItems()
                .addAll(
                        student
                                ? List.of("Tất cả", "Đã giải", "Bài sai", "Chưa làm")
                                : List.of("Tất cả", "Đang mở", "Đang ẩn", "Có thảo luận mới"));
        statusBox.getSelectionModel().selectFirst();
        statusBox.getStyleClass().add("catalog-select");
        statusBox.setOnAction(e -> {
            page = 0;
            render();
        });
        Label statusLabel = new Label("Trạng thái");
        statusLabel.getStyleClass().add("dash-row-meta");
        HBox toolbar = new HBox(10, search, statusLabel, statusBox);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        pager.setAlignment(Pos.CENTER_LEFT);
        VBox main = new VBox(12, toolbar, table, pager);
        main.getStyleClass().addAll("dash-card", "catalog-main");
        HBox.setHgrow(main, Priority.ALWAYS);

        HBox body = new HBox(18, side, main);
        main.setMinWidth(0);
        // cửa sổ hẹp: cất cột chủ đề bên trái (hàng thẻ chương phía trên vẫn chọn được chương)
        viewportBoundsProperty().addListener((o, a, b) -> {
            boolean wide = b.getWidth() >= 1000;
            side.setVisible(wide);
            side.setManaged(wide);
        });
        VBox content = new VBox(18, header, cardsBox, body);
        content.getStyleClass().add("dash-page");
        setContent(content);
        update(List.of());
    }

    /** Nút tròn trượt hàng thẻ chương sang trái ({@code dir} -1) hoặc phải (1) một thẻ. */
    private Button slideButton(String text, ScrollPane scroll, int dir) {
        Button b = new Button(text);
        b.getStyleClass().add("catalog-slide");
        b.setVisible(false);
        b.setOnAction(e -> {
            double w = chapterCards.getWidth() - scroll.getViewportBounds().getWidth();
            if (w <= 0) return;
            double step = 254 / w; // một thẻ (240) và khoảng cách
            scroll.setHvalue(Math.max(0, Math.min(1, scroll.getHvalue() + dir * step)));
        });
        return b;
    }

    /** Nút tạm ẩn/mở và xoá bài cho giáo viên, quản trị. */
    public void onTeacherActions(BiConsumer<String, Boolean> toggle, BiConsumer<String, String> delete) {
        onToggle = toggle;
        onDelete = delete;
    }

    public void update(List<JsonNode> items) {
        lessons.clear();
        lessons.addAll(items);
        lessons.sort(ExerciseCatalog.ORDER);
        List<String> subjects = ExerciseCatalog.subjects(lessons);
        String keep = subjectBox.getValue();
        subjectBox.getItems().setAll(subjects);
        if (subjects.size() > 1) subjectBox.getItems().add(0, "Tất cả môn");
        if (keep != null && subjectBox.getItems().contains(keep)) subjectBox.setValue(keep);
        else if (!subjectBox.getItems().isEmpty())
            // mặc định môn đầu (DSA), không trộn hai môn trong một bảng
            subjectBox.setValue(subjects.get(0));
        // một môn thì không cần ô chọn môn để lọc nhầm ra bảng trống
        subjectBox.setDisable(subjects.size() <= 1);
        render();
    }

    /** Giữ API của màn cũ để các nơi gọi không bị lỗi, trang thư viện không tô chọn. */
    public void select(String id) {}

    /** Mở thẳng một chương (bấm thẻ chủ đề ở trang chủ). */
    public void showChapter(String chuong) {
        chapter = chuong;
        page = 0;
        render();
    }

    private String subject() {
        String v = subjectBox.getValue();
        return v == null || v.startsWith("Tất cả") ? "" : v.toLowerCase(Locale.ROOT);
    }

    private void render() {
        String subj = subject();
        List<JsonNode> scope =
                ExerciseCatalog.select(lessons, new ExerciseCatalog.Query(subj, null, Set.of(), "Tất cả", ""), student);
        subtitle.setText("Luyện tập theo chủ đề để nâng cao kỹ năng "
                + (subj.isEmpty()
                        ? String.join(", ", ExerciseCatalog.subjects(lessons))
                        : subj.toUpperCase(Locale.ROOT))
                + " · " + scope.size() + " bài");
        summary.getChildren().clear();
        if (student) {
            long done = scope.stream().filter(ExerciseCatalog::isDone).count();
            long wrong = scope.stream().filter(ExerciseCatalog::isWrong).count();
            summary.getChildren()
                    .addAll(
                            chip("✓ " + done + " bài đúng", "catalog-chip-done"),
                            chip("✗ " + wrong + " bài sai", "catalog-chip-wrong"),
                            chip("○ " + (scope.size() - done - wrong) + " chưa làm", "catalog-chip-new"));
        }
        renderChapters(scope);
        renderKinds(scope);
        renderTable();
    }

    private static Label chip(String text, String style) {
        Label l = new Label(text);
        l.getStyleClass().addAll("catalog-chip", style);
        return l;
    }

    /** "3 bài mẫu · 4 luyện tập", kèm số đúng/sai với học viên. */
    private String chapterCounts(List<JsonNode> list) {
        long samples = list.stream().filter(ExerciseCatalog::isSample).count();
        String text = samples + " bài mẫu · " + (list.size() - samples) + " luyện tập";
        if (student) {
            text += " · ✓ " + list.stream().filter(ExerciseCatalog::isDone).count() + " đúng · ✗ "
                    + list.stream().filter(ExerciseCatalog::isWrong).count() + " sai";
        }
        return text;
    }

    private Map<String, List<JsonNode>> byChapter(List<JsonNode> scope) {
        Map<String, List<JsonNode>> map = new LinkedHashMap<>();
        for (JsonNode l : scope)
            map.computeIfAbsent(l.path("chuong").asText(), k -> new ArrayList<>())
                    .add(l);
        return map;
    }

    private void renderChapters(List<JsonNode> scope) {
        Map<String, List<JsonNode>> groups = byChapter(scope);
        chapterCards.getChildren().clear();
        chapterList.getChildren().clear();
        chapterList.getChildren().add(chapterButton(null, "Tất cả chủ đề", scope.size()));
        int i = 0;
        for (var e : groups.entrySet()) {
            String key = e.getKey();
            List<JsonNode> list = e.getValue();
            long done = list.stream()
                    .filter(l -> "MASTERED".equals(l.path("status").asText()))
                    .count();
            String chapterTitle = ExerciseCatalog.chapterName(list.get(0));
            chapterList.getChildren().add(chapterButton(key, chapterTitle, list.size()));

            Label icon = new Label(list.get(0).path("mon").asText().equals("sql") ? "SQL" : "{ }");
            icon.getStyleClass().addAll("catalog-card-icon", "dash-tone-" + (i++ % 4));
            Label name = new Label(chapterTitle);
            name.getStyleClass().add("dash-row-title");
            long samples = list.stream().filter(ExerciseCatalog::isSample).count();
            Label count = new Label(samples + " bài mẫu · " + (list.size() - samples) + " luyện tập");
            count.getStyleClass().add("dash-row-meta");
            HBox top = new HBox(12, icon, new VBox(2, name, count));
            top.setAlignment(Pos.CENTER_LEFT);
            int pct = Math.round(done * 100f / list.size());
            long wrongs = list.stream().filter(ExerciseCatalog::isWrong).count();
            Label pctLabel = new Label(
                    student
                            ? "✓ " + done + " đúng · ✗ " + wrongs + " sai · " + pct + "%"
                            : "● " + pct + "% đã hoàn thành");
            pctLabel.getStyleClass().add("dash-row-meta");
            ProgressBar bar = new ProgressBar(pct / 100.0);
            bar.getStyleClass().add("dash-bar");
            bar.setMaxWidth(Double.MAX_VALUE);
            VBox card = new VBox(10, top, pctLabel, bar);
            card.getStyleClass().addAll("dash-card", "catalog-card");
            if (key.equals(chapter)) card.getStyleClass().add("catalog-card-active");
            card.setOnMouseClicked(ev -> showChapter(key.equals(chapter) ? null : key));
            chapterCards.getChildren().add(card);
        }
    }

    private Node chapterButton(String key, String label, int count) {
        Label name = new Label(label);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Label n = new Label(Integer.toString(count));
        n.getStyleClass().add("catalog-count");
        HBox row = new HBox(8, name, gap, n);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("catalog-topic");
        boolean on = key == null ? chapter == null : key.equals(chapter);
        if (on) row.getStyleClass().add("catalog-topic-active");
        row.setOnMouseClicked(e -> showChapter(key));
        return row;
    }

    private void renderKinds(List<JsonNode> scope) {
        kindList.getChildren().clear();
        for (String k : KINDS) {
            long n = scope.stream()
                    .filter(l -> k.equals(l.path("loai").asText()))
                    .count();
            if (n == 0) continue;
            CheckBox box = new CheckBox(LessonScreen.loaiLabel(k));
            box.getStyleClass().addAll("catalog-kind", "catalog-kind-" + k);
            box.setSelected(kinds.contains(k));
            box.setOnAction(e -> {
                if (box.isSelected()) kinds.add(k);
                else kinds.remove(k);
                page = 0;
                renderTable();
            });
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            Label count = new Label(Long.toString(n));
            count.getStyleClass().add("catalog-count");
            HBox row = new HBox(8, box, gap, count);
            row.setAlignment(Pos.CENTER_LEFT);
            kindList.getChildren().add(row);
        }
    }

    private static final double[] COLS = {44, -1, 150, 100, 130, 120};

    private static HBox row(Node... cells) {
        HBox r = new HBox(12);
        for (int i = 0; i < cells.length; i++) {
            Node c = cells[i];
            if (c instanceof Region reg) {
                if (i < COLS.length && COLS[i] > 0) {
                    // cột co lại được khi khung hẹp, chữ trong ô tự cắt thay vì đẩy bảng tràn ra ngoài
                    reg.setMinWidth(i == 0 ? COLS[i] : COLS[i] * 0.6);
                    reg.setPrefWidth(COLS[i]);
                    reg.setMaxWidth(COLS[i]);
                } else if (i < COLS.length) {
                    // rộng ưa thích cố định để mọi dòng co giãn như nhau, cột thẳng với dòng tiêu đề
                    reg.setMinWidth(110);
                    reg.setPrefWidth(110);
                    reg.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(reg, Priority.ALWAYS);
                }
            }
            r.getChildren().add(c);
        }
        r.setAlignment(Pos.CENTER_LEFT);
        return r;
    }

    private void renderTable() {
        List<JsonNode> rows = ExerciseCatalog.select(
                lessons,
                new ExerciseCatalog.Query(
                        subject(), chapter, Set.copyOf(kinds), statusBox.getValue(), search.getText()),
                student);
        table.getChildren().clear();
        HBox head = row(
                new javafx.scene.layout.StackPane(th("#")),
                th("Bài tập"),
                th("Chủ đề"),
                th("Loại"),
                th(student ? "Điểm" : "Hiển thị"),
                th(student ? "Trạng thái" : ""));
        if (!student) head.getChildren().add(th(""));
        head.getStyleClass().add("catalog-head");
        table.getChildren().add(head);

        int pages = Math.max(1, (rows.size() + PAGE - 1) / PAGE);
        page = Math.min(page, pages - 1);
        int from = page * PAGE;
        int to = Math.min(rows.size(), from + PAGE);
        // Nhóm theo chương: mỗi lần đổi chương thì chèn một dòng tiêu đề chương
        Map<String, List<JsonNode>> groups = byChapter(rows);
        String last = null;
        for (int i = from; i < to; i++) {
            JsonNode l = rows.get(i);
            String key = l.path("mon").asText() + "/" + l.path("chuong").asText();
            if (!key.equals(last)) {
                table.getChildren()
                        .add(groupHeader(l, groups.get(l.path("chuong").asText())));
                last = key;
            }
            table.getChildren().add(lessonRow(i + 1, l));
        }
        if (rows.isEmpty()) {
            Label empty = new Label("Không có bài nào khớp bộ lọc.");
            empty.getStyleClass().add("dash-row-meta");
            empty.setPadding(new javafx.geometry.Insets(18, 8, 18, 8));
            table.getChildren().add(empty);
        }
        renderPager(rows.size(), pages, from, to);
    }

    private Node groupHeader(JsonNode first, List<JsonNode> list) {
        Label name = new Label(ExerciseCatalog.chapterName(first));
        name.getStyleClass().add("catalog-group-title");
        Label counts = new Label(chapterCounts(list));
        counts.getStyleClass().add("dash-row-meta");
        HBox h = new HBox(12, name, counts);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add("catalog-group");
        return h;
    }

    private static Label th(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("dash-th");
        return l;
    }

    private Node lessonRow(int index, JsonNode l) {
        String id = l.path("id").asText();
        String ten = l.path("ten").asText();
        String status = l.path("status").asText("NOT_STARTED");
        boolean active = l.path("active").asBoolean(true);

        Label number = new Label(Integer.toString(index));
        number.getStyleClass().add("dash-row-meta");
        // vạch ngắn bo tròn: xanh dương bài mẫu, xanh lá luyện tập; các dòng nối nhau như một sợi dây
        Region stripe = new Region();
        stripe.getStyleClass()
                .addAll(
                        "catalog-stripe",
                        ExerciseCatalog.isSample(l) ? "catalog-stripe-sample" : "catalog-stripe-practice");
        stripe.setMinSize(4, 30);
        stripe.setMaxSize(4, 30);
        javafx.scene.layout.StackPane numberCell = new javafx.scene.layout.StackPane(number);
        HBox.setHgrow(numberCell, Priority.ALWAYS);
        HBox num = new HBox(0, stripe, numberCell);
        num.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(ten);
        title.getStyleClass().add("dash-row-title");
        title.setTextOverrun(OverrunStyle.ELLIPSIS);
        Marquee.installOnHover(title); // tên dài bị cắt thì rê chuột vào để chữ chạy
        int wrongSubmits = l.path("wrongSubmits").asInt(0);
        Label sub = new Label(l.path("mon").asText().toUpperCase(Locale.ROOT) + " · " + id
                + (student && wrongSubmits > 0 ? " · nộp sai " + wrongSubmits + " lần" : ""));
        sub.getStyleClass().add("dash-row-meta");
        sub.setTextOverrun(OverrunStyle.ELLIPSIS);
        HBox subLine = new HBox(8, sub);
        subLine.setAlignment(Pos.CENTER_LEFT);
        int talk = l.path("discuss").asInt(0);
        if (talk > 0) {
            int fresh = l.path("discussNew").asInt(0);
            Label badge = new Label(talk + " thảo luận" + (fresh > 0 ? " · " + fresh + " mới" : ""));
            badge.getStyleClass().addAll("catalog-talk", fresh > 0 ? "catalog-talk-new" : "catalog-talk-old");
            badge.setMinWidth(Region.USE_PREF_SIZE);
            subLine.getChildren().add(badge);
        }
        VBox name = new VBox(2, title, subLine);
        Tooltip.install(name, new Tooltip(ten));

        Label topic = new Label(ExerciseCatalog.chapterName(l));
        topic.getStyleClass().add("catalog-tag");
        StackPane topicCell = left(topic);

        String loai = l.path("loai").asText();
        Label kind = new Label(LessonScreen.loaiLabel(loai));
        kind.getStyleClass().addAll("catalog-kind-pill", "catalog-kind-pill-" + loai);
        StackPane kindCell = left(kind);

        Node scoreCell;
        if (student) {
            int score = l.path("score").asInt(0);
            Label v = new Label("MASTERED".equals(status) || l.has("score") ? score + "%" : "—");
            v.getStyleClass().add("dash-row-meta");
            ProgressBar bar = new ProgressBar(score / 100.0);
            bar.getStyleClass()
                    .addAll(
                            "dash-bar",
                            score >= 80 ? "catalog-bar-good" : score >= 50 ? "catalog-bar-mid" : "catalog-bar-low");
            bar.setMaxWidth(Double.MAX_VALUE);
            scoreCell = new VBox(4, v, bar);
        } else {
            Label t = new Label(active ? "● Đang mở" : "○ Tạm ẩn");
            t.getStyleClass().addAll("catalog-toggle", active ? "catalog-toggle-on" : "catalog-toggle-off");
            t.setTooltip(new Tooltip(active ? "Bấm để tạm ẩn bài với học viên" : "Bấm để mở bài cho học viên"));
            t.setOnMouseClicked(e -> {
                e.consume();
                onToggle.accept(id, !active);
            });
            scoreCell = left(t);
        }

        Label state;
        if (!student) {
            state = new Label("Xem bài  →");
            state.getStyleClass().addAll("catalog-state", "catalog-state-new");
        } else if (!active) {
            state = new Label("Đang ẩn");
            state.getStyleClass().addAll("catalog-state", "catalog-state-hidden");
        } else if ("MASTERED".equals(status)) {
            state = new Label("✓ Đã giải");
            state.getStyleClass().addAll("catalog-state", "catalog-state-done");
        } else if (ExerciseCatalog.isWrong(l)) {
            state = new Label("✗ Sai · làm lại");
            state.getStyleClass().addAll("catalog-state", "catalog-state-wrong");
        } else {
            state = new Label("○ Chưa làm");
            state.getStyleClass().addAll("catalog-state", "catalog-state-new");
        }
        HBox r = row(num, name, topicCell, kindCell, (Region) scoreCell, left(state));
        if (!student) {
            SVGPath bin = new SVGPath();
            bin.setContent("M5 7 H19 M10 7 V5 H14 V7 M7 7 L8 19 H16 L17 7");
            bin.getStyleClass().add("catalog-delete-icon");
            Button del = new Button();
            del.setGraphic(bin);
            del.getStyleClass().add("catalog-delete");
            del.setTooltip(new Tooltip("Xoá bài khỏi hệ thống"));
            del.setOnAction(e -> onDelete.accept(id, ten));
            r.getChildren().add(del);
        }
        r.getStyleClass()
                .addAll("catalog-row", ExerciseCatalog.isSample(l) ? "catalog-row-sample" : "catalog-row-practice");
        // tô cả dòng: đạt màu xanh, nộp sai màu đỏ; chữ vẫn đọc được trên nền nhạt
        if (student && active && ExerciseCatalog.isDone(l)) r.getStyleClass().add("catalog-row-done");
        else if (student && active && ExerciseCatalog.isWrong(l))
            r.getStyleClass().add("catalog-row-wrong");
        r.setOnMouseClicked(e -> onOpen.accept(id));
        return r;
    }

    private static StackPane left(Node n) {
        StackPane p = new StackPane(n);
        p.setAlignment(Pos.CENTER_LEFT);
        return p;
    }

    private void renderPager(int total, int pages, int from, int to) {
        pager.getChildren().clear();
        Button prev = pageButton("←", page > 0, page - 1);
        pager.getChildren().add(prev);
        for (int p = 0; p < pages; p++) {
            // trang đầu, cuối và quanh trang đang xem; chỗ khác gộp thành "…"
            if (p == 0 || p == pages - 1 || Math.abs(p - page) <= 1) {
                Button b = pageButton(Integer.toString(p + 1), true, p);
                if (p == page) b.getStyleClass().add("catalog-page-active");
                pager.getChildren().add(b);
            } else if (p == page - 2 || p == page + 2) {
                Label dots = new Label("…");
                dots.getStyleClass().add("dash-row-meta");
                pager.getChildren().add(dots);
            }
        }
        pager.getChildren().add(pageButton("→", page < pages - 1, page + 1));
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Label info = new Label(total == 0 ? "" : "Hiển thị " + (from + 1) + "–" + to + " trong " + total + " bài tập");
        info.getStyleClass().add("dash-row-meta");
        pager.getChildren().addAll(gap, info);
    }

    private Button pageButton(String text, boolean enabled, int target) {
        Button b = new Button(text);
        b.getStyleClass().add("catalog-page");
        b.setDisable(!enabled);
        b.setOnAction(e -> {
            page = target;
            renderTable();
        });
        return b;
    }
}
