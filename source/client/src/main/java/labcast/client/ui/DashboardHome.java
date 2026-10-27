package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.SVGPath;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.Role;

/**
 * Trang chủ theo vai trò: thẻ số liệu, tiến độ, hoạt động gần đây. Học viên thấy bài nên làm tiếp,
 * tiến độ theo chương, huy hiệu; giáo viên và quản trị thấy lớp, tỉ lệ hoàn thành, phân bố bài và
 * hoạt động của học viên lớp đang chọn. Chỉ dùng dữ liệu đã có ({@link DashboardStats}).
 */
final class DashboardHome extends ScrollPane {
    /** Việc trang chủ nhờ màn chính làm: mở bài, chuyển mục ở sidebar. */
    record Actions(
            Consumer<String> openLesson,
            Runnable showExercises,
            Runnable showPractice,
            Runnable showStudents,
            Runnable showAddExercise) {}

    private final LoginController.LoggedIn li;
    private final Actions actions;
    private final boolean student;
    private List<JsonNode> lessons = List.of();
    private JsonNode classes = JsonNodeFactory.instance.arrayNode();
    private JsonNode students = JsonNodeFactory.instance.arrayNode();
    private long classId;
    private boolean closed;

    DashboardHome(LoginController.LoggedIn li, Actions actions) {
        this.li = li;
        this.actions = actions;
        this.student = li.role() == Role.STUDENT;
        getStyleClass().add("dash-scroll");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        render();
    }

    void setLessons(List<JsonNode> all) {
        lessons = List.copyOf(all);
        render();
    }

    void setClasses(JsonNode list, long selected) {
        classes = list;
        classId = selected;
        render();
        if (!student && classId > 0) loadStudents();
    }

    void close() {
        closed = true;
    }

    private void loadStudents() {
        var reply = li.client().expect(MessageType.STUDENT_OVERVIEW_DATA, MessageType.ERROR);
        li.client().send(MessageType.STUDENT_OVERVIEW_REQ, new byte[0]);
        reply.orTimeout(10, TimeUnit.SECONDS).whenComplete((f, err) -> {
            if (err != null || f.type() != MessageType.STUDENT_OVERVIEW_DATA) return;
            try {
                JsonNode list = JsonPayload.tree(f).path("students");
                Platform.runLater(() -> {
                    if (closed) return;
                    students = list;
                    render();
                });
            } catch (Exception ignored) {
                // trang chủ vẫn hiện phần còn lại
            }
        });
    }

    private void render() {
        DashboardStats stats = DashboardStats.of(lessons, student);
        VBox page = new VBox(18, header(), student ? studentBody(stats) : staffBody(stats));
        page.getStyleClass().add("dash-page");
        setContent(page);
    }

    private Node header() {
        Label hello = new Label("Xin chào,");
        hello.getStyleClass().add("dash-hello");
        Label name = new Label(li.fullName() + " 👋");
        name.getStyleClass().add("dash-name");
        Label sub = new Label(
                switch (li.role()) {
                    case STUDENT -> "Hôm nay bạn muốn chinh phục bài tập nào?";
                    case TEACHER -> "Đây là tổng quan các lớp của bạn.";
                    case ADMIN -> "Dưới đây là tổng quan hệ thống hôm nay.";
                });
        sub.getStyleClass().add("dash-sub");
        VBox text = new VBox(2, hello, name, sub);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox box = new HBox(10, text, gap);
        box.setAlignment(Pos.CENTER_LEFT);
        if (student) {
            box.getChildren().add(primary("Vào luyện tập", actions.showExercises()));
        } else {
            box.getChildren()
                    .addAll(
                            primary("+  Thêm bài tập", actions.showAddExercise()),
                            ghost("Lớp thực hành", actions.showPractice()));
        }
        return box;
    }

    // ---------- Học viên ----------

    private Node studentBody(DashboardStats s) {
        HBox cards = statRow(
                stat("Hoàn thành", s.done() + "/" + s.total(), s.percentDone() + "% bài của lớp", "done"),
                stat("Đang làm", Integer.toString(s.inProgress()), "bài chưa đạt", "progress"),
                stat("Tổng XP", Integer.toString(s.xp()), "tổng điểm các bài đạt", "xp"),
                stat("Cấp độ", "Lv. " + s.level(), (500 - s.xp() % 500) + " XP để lên cấp", "level"));

        GridPane grid = twoColumns(continueCard(s), badgesCard(s), chaptersCard(s), recentLessonsCard(s));
        return new VBox(18, cards, grid);
    }

    private Node continueCard(DashboardStats s) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("dash-card", "dash-hero");
        Label kicker = new Label("Tiếp tục luyện tập");
        kicker.getStyleClass().add("dash-hero-kicker");
        card.getChildren().add(kicker);
        var next = s.continueLesson();
        if (next.isEmpty()) {
            Label done = new Label(s.total() == 0 ? "Lớp chưa mở bài nào" : "Bạn đã làm xong tất cả bài 🎉");
            done.getStyleClass().add("dash-hero-title");
            card.getChildren().add(done);
            return card;
        }
        JsonNode l = next.get();
        Label title = new Label(l.path("ten").asText());
        title.getStyleClass().add("dash-hero-title");
        title.setWrapText(true);
        Label chapter = new Label(l.path("mon").asText().toUpperCase() + " · "
                + LessonScreen.chapterLabel(l.path("chuong").asText()) + " · "
                + LessonScreen.loaiLabel(l.path("loai").asText()));
        chapter.getStyleClass().add("dash-hero-meta");
        ProgressBar bar = new ProgressBar(s.total() == 0 ? 0 : (double) s.done() / s.total());
        bar.getStyleClass().add("dash-hero-bar");
        bar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(bar, Priority.ALWAYS);
        Label pct = new Label(s.percentDone() + "%");
        pct.getStyleClass().add("dash-hero-meta");
        Button go = new Button("Tiếp tục  →");
        go.getStyleClass().add("dash-hero-button");
        String id = l.path("id").asText();
        go.setOnAction(e -> actions.openLesson().accept(id));
        HBox bottom = new HBox(10, bar, pct, go);
        bottom.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().addAll(title, chapter, bottom);
        return card;
    }

    private Node badgesCard(DashboardStats s) {
        VBox card = card("Thành tích", null, null);
        List<String> badges = s.badges();
        if (badges.isEmpty()) {
            card.getChildren().add(muted("Làm xong hết bài của một chương để nhận huy hiệu đầu tiên."));
            return card;
        }
        FlowPane row = new FlowPane(10, 10);
        int i = 0;
        for (String b : badges) {
            Label medal = new Label("★");
            medal.getStyleClass().addAll("dash-badge", "dash-tone-" + (i++ % 4));
            Label text = new Label(b);
            text.getStyleClass().add("dash-badge-text");
            text.setWrapText(true);
            text.setMaxWidth(90);
            VBox one = new VBox(4, medal, text);
            one.setAlignment(Pos.TOP_CENTER);
            row.getChildren().add(one);
        }
        card.getChildren().add(row);
        return card;
    }

    private Node chaptersCard(DashboardStats s) {
        VBox card = card("Tiến độ theo chương", "Xem tất cả", actions.showExercises());
        if (s.chapters().isEmpty()) card.getChildren().add(muted("Chưa có bài nào."));
        // Chương đang học trước (đã có bài đạt), tối đa 8 dòng; xem đủ ở mục Bài tập.
        List<DashboardStats.ChapterProgress> shown = new ArrayList<>(s.chapters());
        shown.sort(
                (a, b) -> Boolean.compare(b.done() > 0 && b.done() < b.total(), a.done() > 0 && a.done() < a.total()));
        for (DashboardStats.ChapterProgress c : shown.subList(0, Math.min(8, shown.size()))) {
            card.getChildren().add(progressRow(c.mon().toUpperCase() + " · " + c.label(), c.done(), c.total()));
        }
        return card;
    }

    private Node recentLessonsCard(DashboardStats s) {
        VBox card = card("Bài làm gần đây", null, null);
        List<JsonNode> touched = s.touched(6);
        if (touched.isEmpty()) card.getChildren().add(muted("Bạn chưa nộp bài nào."));
        for (JsonNode l : touched) {
            boolean ok = "MASTERED".equals(l.path("status").asText());
            Label name = new Label(l.path("ten").asText());
            name.getStyleClass().add("dash-row-title");
            name.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(name, Priority.ALWAYS);
            Label chapter = new Label(LessonScreen.chapterLabel(l.path("chuong").asText()));
            chapter.getStyleClass().add("dash-row-meta");
            Label state = new Label(ok ? "Đạt " + l.path("score").asText("") : "Đang làm");
            state.getStyleClass().addAll("dash-pill", ok ? "dash-pill-ok" : "dash-pill-warn");
            HBox row = new HBox(10, name, chapter, state);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("dash-row");
            String id = l.path("id").asText();
            row.setOnMouseClicked(e -> actions.openLesson().accept(id));
            card.getChildren().add(row);
        }
        return card;
    }

    // ---------- Giáo viên, quản trị ----------

    private Node staffBody(DashboardStats s) {
        int completion = DashboardStats.classCompletion(students, s.total());
        HBox cards = statRow(
                stat(
                        "Lớp học",
                        Integer.toString(classes.size()),
                        li.role() == Role.ADMIN ? "toàn hệ thống" : "lớp bạn dạy",
                        "class"),
                stat(
                        "Học viên",
                        Integer.toString(students.size()),
                        DashboardStats.online(students) + " đang online",
                        "students"),
                stat("Bài tập", Integer.toString(s.total()), s.hidden() + " bài đang ẩn", "lessons"),
                stat("Tỉ lệ hoàn thành", completion + "%", "lớp đang chọn", "rate"));

        GridPane grid = twoColumns(classesCard(completion), distributionCard(s), activityCard(), chaptersOverview(s));
        return new VBox(18, cards, grid);
    }

    private Node classesCard(int completion) {
        VBox card = card("Lớp học", "Xem học viên", actions.showStudents());
        if (classes.size() == 0) card.getChildren().add(muted("Chưa có lớp nào."));
        int i = 0;
        for (JsonNode c : classes) {
            boolean current = c.path("id").asLong() == classId;
            Label icon = new Label(Character.toString(c.path("name").asText("?").charAt(0)));
            icon.getStyleClass().addAll("dash-class-icon", "dash-tone-" + (i++ % 4));
            Label name = new Label(c.path("name").asText());
            name.getStyleClass().add("dash-row-title");
            String code = c.path("joinCode").asText("");
            Label meta = new Label((current ? students.size() + " học viên" : "Chọn lớp ở thanh trên để xem")
                    + (code.isEmpty() ? "" : " · mã " + code));
            meta.getStyleClass().add("dash-row-meta");
            VBox text = new VBox(1, name, meta);
            HBox.setHgrow(text, Priority.ALWAYS);
            HBox row = new HBox(10, icon, text);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("dash-row");
            if (current) {
                ProgressBar bar = new ProgressBar(completion / 100.0);
                bar.getStyleClass().add("dash-bar");
                bar.setPrefWidth(110);
                Label pct = new Label(completion + "%");
                pct.getStyleClass().add("dash-row-meta");
                row.getChildren().addAll(bar, pct);
            }
            card.getChildren().add(row);
        }
        return card;
    }

    private Node distributionCard(DashboardStats s) {
        VBox card = card("Phân bố bài tập", null, null);
        List<DashboardStats.Slice> slices = new ArrayList<>();
        // Theo chương: 4 chương nhiều bài nhất, còn lại gộp "Khác".
        List<DashboardStats.ChapterProgress> chapters = new ArrayList<>(s.chapters());
        chapters.sort((a, b) -> Integer.compare(b.total(), a.total()));
        int other = 0;
        for (int i = 0; i < chapters.size(); i++) {
            var c = chapters.get(i);
            if (i < 4) slices.add(new DashboardStats.Slice(c.label(), c.total()));
            else other += c.total();
        }
        if (other > 0) slices.add(new DashboardStats.Slice("Khác", other));

        StackPane donut = donut(slices, s.total());
        VBox legend = new VBox(8);
        for (int i = 0; i < slices.size(); i++) {
            var sl = slices.get(i);
            Label dot = new Label();
            dot.getStyleClass().addAll("dash-legend-dot", "dash-fill-" + i);
            Label name = new Label(sl.label());
            name.getStyleClass().add("dash-row-meta");
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            Label pct = new Label(Math.round(sl.count() * 100f / Math.max(1, s.total())) + "% (" + sl.count() + ")");
            pct.getStyleClass().add("dash-row-meta");
            HBox row = new HBox(8, dot, name, gap, pct);
            row.setAlignment(Pos.CENTER_LEFT);
            legend.getChildren().add(row);
        }
        HBox.setHgrow(legend, Priority.ALWAYS);
        HBox body = new HBox(22, donut, legend);
        body.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(body);
        return card;
    }

    private static StackPane donut(List<DashboardStats.Slice> slices, int total) {
        double r = 62;
        Group g = new Group();
        Arc track = new Arc(0, 0, r, r, 0, 360);
        track.setType(ArcType.OPEN);
        track.getStyleClass().add("dash-donut-track");
        g.getChildren().add(track);
        double start = 90;
        for (int i = 0; i < slices.size() && total > 0; i++) {
            double sweep = -360.0 * slices.get(i).count() / total;
            Arc a = new Arc(0, 0, r, r, start, sweep + (slices.size() > 1 ? 2 : 0));
            a.setType(ArcType.OPEN);
            a.getStyleClass().addAll("dash-donut-slice", "dash-stroke-" + i);
            g.getChildren().add(a);
            start += sweep;
        }
        Label small = new Label("Tổng");
        small.getStyleClass().add("dash-row-meta");
        Label big = new Label(Integer.toString(total));
        big.getStyleClass().add("dash-donut-total");
        Label unit = new Label("bài tập");
        unit.getStyleClass().add("dash-row-meta");
        VBox center = new VBox(0, small, big, unit);
        center.setAlignment(Pos.CENTER);
        StackPane pane = new StackPane(g, center);
        pane.setMinSize(150, 150);
        return pane;
    }

    private Node activityCard() {
        VBox card = card("Hoạt động gần đây", null, null);
        List<JsonNode> recent = DashboardStats.recent(students, 6);
        if (recent.isEmpty()) card.getChildren().add(muted("Chưa có học viên nào trong lớp đang chọn."));
        for (JsonNode st : recent) {
            String full = st.path("fullName").asText();
            Label avatar = new Label(HomeScreen.initial(full.isBlank() ? "?" : full));
            avatar.getStyleClass().add("dash-avatar");
            if (st.path("online").asBoolean()) avatar.getStyleClass().add("dash-avatar-online");
            Label who = new Label(full);
            who.getStyleClass().add("dash-row-title");
            String lesson = st.path("lessonId").asText("");
            Label what = new Label(st.path("activity").asText("—") + (lesson.isEmpty() ? "" : " · " + lesson));
            what.getStyleClass().add("dash-row-meta");
            VBox text = new VBox(1, who, what);
            HBox.setHgrow(text, Priority.ALWAYS);
            Label when = new Label(ago(st.path("updatedAt").asLong()));
            when.getStyleClass().add("dash-row-meta");
            HBox row = new HBox(10, avatar, text, when);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("dash-row");
            card.getChildren().add(row);
        }
        return card;
    }

    private Node chaptersOverview(DashboardStats s) {
        VBox card = card("Bài theo chương", "Quản lý bài", actions.showExercises());
        if (s.chapters().isEmpty()) card.getChildren().add(muted("Chưa có bài nào."));
        for (DashboardStats.ChapterProgress c :
                s.chapters().subList(0, Math.min(8, s.chapters().size()))) {
            Label name = new Label(c.mon().toUpperCase() + " · " + c.label());
            name.getStyleClass().add("dash-row-title");
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            Label count = new Label(c.total() + " bài");
            count.getStyleClass().addAll("dash-pill", "dash-pill-info");
            HBox row = new HBox(8, name, gap, count);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("dash-row");
            card.getChildren().add(row);
        }
        return card;
    }

    // ---------- Khối dựng dùng chung ----------

    private static String ago(long ts) {
        if (ts <= 0) return "";
        long min = (System.currentTimeMillis() - ts) / 60_000;
        if (min < 1) return "vừa xong";
        if (min < 60) return min + " phút trước";
        if (min < 24 * 60) return (min / 60) + " giờ trước";
        return (min / (24 * 60)) + " ngày trước";
    }

    private static HBox statRow(Node... cards) {
        HBox row = new HBox(14, cards);
        for (Node c : cards) {
            HBox.setHgrow(c, Priority.ALWAYS);
            ((Region) c).setMaxWidth(Double.MAX_VALUE);
        }
        row.getStyleClass().add("dash-stats");
        return row;
    }

    private static VBox stat(String title, String value, String hint, String kind) {
        Label t = new Label(title);
        t.getStyleClass().add("dash-stat-title");
        Label v = new Label(value);
        v.getStyleClass().add("dash-stat-value");
        Label h = new Label(hint);
        h.getStyleClass().add("dash-stat-hint");
        SVGPath icon = new SVGPath();
        icon.setContent(
                switch (kind) {
                    case "done", "rate" -> "M5 12 L10 17 L19 7";
                    case "progress" -> "M12 4 A8 8 0 1 0 20 12 M12 8 V12 L15 14";
                    case "xp" -> "M13 3 L5 14 H12 L11 21 L19 10 H12 Z";
                    case "level" -> "M12 3 L15 9 L21 10 L16.5 14.5 L18 21 L12 17.5 L6 21 L7.5 14.5 L3 10 L9 9 Z";
                    case "class" -> "M4 7 H20 V18 H4 Z M8 7 V5 H16 V7";
                    case "students" -> "M4 19 V17 C4 14 7 13 10 13 C13 13 16 14 16 17 V19 M13 8 A3 3 0 1 0 7 8 A3 3 0 0 0 13 8";
                    default -> "M6 5 H18 V19 H6 Z M9 9 H15 M9 13 H15";
                });
        icon.getStyleClass().add("dash-stat-icon-shape");
        StackPane badge = new StackPane(icon);
        badge.getStyleClass().addAll("dash-stat-icon", "dash-icon-" + kind);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox top = new HBox(t, gap, badge);
        top.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(4, top, v, h);
        card.getStyleClass().addAll("dash-card", "dash-stat");
        return card;
    }

    private static VBox card(String title, String link, Runnable onLink) {
        Label t = new Label(title);
        t.getStyleClass().add("dash-card-title");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox head = new HBox(t, gap);
        head.setAlignment(Pos.CENTER_LEFT);
        if (link != null) {
            Button b = new Button(link);
            b.getStyleClass().add("dash-link");
            b.setOnAction(e -> onLink.run());
            head.getChildren().add(b);
        }
        VBox card = new VBox(10, head);
        card.getStyleClass().add("dash-card");
        return card;
    }

    private static Node progressRow(String label, int done, int total) {
        Label name = new Label(label);
        name.getStyleClass().add("dash-row-title");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Label count = new Label(done + "/" + total);
        count.getStyleClass().add("dash-row-meta");
        ProgressBar bar = new ProgressBar(total == 0 ? 0 : (double) done / total);
        bar.getStyleClass().add("dash-bar");
        bar.setMaxWidth(Double.MAX_VALUE);
        return new VBox(4, new HBox(name, gap, count), bar);
    }

    private static GridPane twoColumns(Node a, Node b, Node c, Node d) {
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(18);
        var left = new javafx.scene.layout.ColumnConstraints();
        left.setPercentWidth(58);
        var right = new javafx.scene.layout.ColumnConstraints();
        right.setPercentWidth(42);
        grid.getColumnConstraints().addAll(left, right);
        grid.add(a, 0, 0);
        grid.add(b, 1, 0);
        grid.add(c, 0, 1);
        grid.add(d, 1, 1);
        for (Node n : List.of(a, b, c, d)) {
            GridPane.setFillHeight(n, true);
            if (n instanceof Region r) r.setMaxHeight(Double.MAX_VALUE);
        }
        return grid;
    }

    private static Label muted(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("dash-row-meta");
        l.setWrapText(true);
        return l;
    }

    private static Button primary(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("dash-primary");
        b.setOnAction(e -> action.run());
        return b;
    }

    private static Button ghost(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("dash-ghost");
        b.setOnAction(e -> action.run());
        return b;
    }
}
