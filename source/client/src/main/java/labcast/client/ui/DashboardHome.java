package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
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
 * Trang chủ, mỗi vai trò một bố cục riêng theo ảnh mẫu (docs/plans/anh/giao-dien-mau.webp):
 *
 * <ul>
 *   <li>Quản trị: 6 thẻ số liệu toàn hệ thống có xu hướng, biểu đồ lượt làm bài 30 ngày, phân bố bài
 *       theo chủ đề, hoạt động hệ thống, bảng lớp học.
 *   <li>Giáo viên: thẻ số liệu các lớp mình dạy, tiến độ từng lớp, biểu đồ bài nộp, hoạt động của học
 *       viên, học viên đang online.
 *   <li>Học viên: chuỗi ngày học, tiếp tục luyện tập, XP và cấp, chủ đề, bài tập có bộ lọc, thành
 *       tích, xếp hạng lớp, hoạt động 7 ngày.
 * </ul>
 *
 * Số liệu theo thời gian lấy từ {@code HOME_DATA}; số liệu theo bài từ danh sách bài ({@link
 * DashboardStats}).
 */
final class DashboardHome extends ScrollPane {
    /** Việc trang chủ nhờ màn chính làm: mở bài, chuyển mục ở sidebar. */
    record Actions(
            Consumer<String> openLesson,
            Runnable showExercises,
            Consumer<String> showChapter,
            Runnable showPractice,
            Runnable showStudents,
            Runnable showAddExercise) {}

    private final LoginController.LoggedIn li;
    private final Actions actions;
    private final Role role;
    private List<JsonNode> lessons = List.of();
    private JsonNode classes = JsonNodeFactory.instance.arrayNode();
    private JsonNode students = JsonNodeFactory.instance.arrayNode();
    private JsonNode home = MissingNode.getInstance();
    private long classId;
    private boolean closed;
    private String lessonFilter = "Tất cả";

    DashboardHome(LoginController.LoggedIn li, Actions actions) {
        this.li = li;
        this.actions = actions;
        this.role = li.role();
        getStyleClass().addAll("dash-scroll", "dash-" + role.name().toLowerCase());
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
        refresh();
    }

    /** Tải lại số liệu theo thời gian (và học viên online với giáo viên, quản trị). */
    void refresh() {
        request(MessageType.HOME_REQ, MessageType.HOME_DATA, data -> home = data);
        if (role.isStaff() && classId > 0) {
            request(
                    MessageType.STUDENT_OVERVIEW_REQ,
                    MessageType.STUDENT_OVERVIEW_DATA,
                    data -> students = data.path("students"));
        }
    }

    void close() {
        closed = true;
    }

    private void request(MessageType req, MessageType resp, Consumer<JsonNode> apply) {
        var reply = li.client().expect(resp, MessageType.ERROR);
        li.client().send(req, new byte[0]);
        reply.orTimeout(10, TimeUnit.SECONDS).whenComplete((f, err) -> {
            // server cũ không biết HOME_REQ: trang chủ vẫn hiện phần tính từ danh sách bài
            if (err != null || f.type() != resp) return;
            try {
                JsonNode data = JsonPayload.tree(f);
                Platform.runLater(() -> {
                    if (closed) return;
                    apply.accept(data);
                    render();
                });
            } catch (Exception ignored) {
                // bỏ
            }
        });
    }

    private void render() {
        DashboardStats stats = DashboardStats.of(lessons, role == Role.STUDENT);
        Node body =
                switch (role) {
                    case ADMIN -> adminBody(stats);
                    case TEACHER -> teacherBody(stats);
                    case STUDENT -> studentBody(stats);
                };
        VBox page = new VBox(18, header(), body);
        page.getStyleClass().add("dash-page");
        setContent(page);
    }

    private Node header() {
        Label hello = new Label(role == Role.ADMIN ? "Chào mừng trở lại," : "Xin chào,");
        hello.getStyleClass().add("dash-hello");
        Label name = new Label(li.fullName() + " 👋");
        name.getStyleClass().add("dash-name");
        Label sub = new Label(
                switch (role) {
                    case STUDENT -> "Hôm nay bạn muốn chinh phục bài tập nào?";
                    case TEACHER -> "Đây là tổng quan các lớp của bạn.";
                    case ADMIN -> "Dưới đây là tổng quan hệ thống hôm nay.";
                });
        sub.getStyleClass().add("dash-sub");
        VBox text = new VBox(2, hello, name, sub);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox box = new HBox(12, text, gap);
        box.setAlignment(Pos.CENTER_LEFT);
        switch (role) {
            case STUDENT -> box.getChildren().add(streakCard());
            case TEACHER -> box.getChildren()
                    .addAll(
                            primary("+  Thêm bài tập", actions.showAddExercise()),
                            ghost("Lớp thực hành", actions.showPractice()));
            case ADMIN -> {
                Label range = new Label("📅  30 ngày qua");
                range.getStyleClass().add("dash-range");
                box.getChildren().addAll(range, primary("+  Thêm bài tập", actions.showAddExercise()));
            }
        }
        return box;
    }

    // ================= Quản trị =================

    private Node adminBody(DashboardStats s) {
        JsonNode u = home.path("users");
        HBox cards = statRow(
                stat(
                        "Người dùng",
                        u.path("total").asText("—"),
                        DashboardStats.trend(
                                u.path("new30").asLong(), u.path("newPrev30").asLong()),
                        "users"),
                stat(
                        "Giáo viên",
                        u.path("teachers").asText("—"),
                        DashboardStats.trend(
                                u.path("teachersNew30").asLong(),
                                u.path("teachersNewPrev30").asLong()),
                        "teacher"),
                stat(
                        "Học viên",
                        u.path("students").asText("—"),
                        DashboardStats.trend(
                                u.path("studentsNew30").asLong(),
                                u.path("studentsNewPrev30").asLong()),
                        "students"),
                stat(
                        "Lớp học",
                        home.path("classesTotal").asText(Integer.toString(classes.size())),
                        DashboardStats.trend(
                                home.path("classesNew30").asLong(),
                                home.path("classesNewPrev30").asLong()),
                        "class"),
                stat("Bài tập", Integer.toString(s.total()), s.hidden() + " bài đang ẩn", "lessons"),
                stat(
                        "Lượt nộp bài",
                        home.path("submitsTotal").asText("—"),
                        DashboardStats.trend(
                                home.path("submits30").asLong(),
                                home.path("submitsPrev30").asLong()),
                        "rate"));

        VBox chart = card("Lượt làm bài trong 30 ngày qua", null, null);
        Node area = areaChart(home.path("days"));
        VBox.setVgrow(area, Priority.ALWAYS);
        chart.getChildren().add(area);
        GridPane row =
                columns(new double[] {46, 24, 30}, chart, distributionCard(s), activityCard(home.path("recent")));
        return new VBox(18, cards, row, classTable());
    }

    private Node areaChart(JsonNode days) {
        if (days.isMissingNode() || days.size() == 0) return muted("Chưa có số liệu (server cần bản có HOME_REQ).");
        // Trục x là số thứ tự ngày để chỉ hiện nhãn mỗi 5 ngày, nhãn đổi lại thành dd/MM
        NumberAxis x = new NumberAxis(0, days.size() - 1, 5);
        x.setMinorTickVisible(false);
        x.setTickLabelFormatter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Number n) {
                int i = n.intValue();
                return i >= 0 && i < days.size() ? days.get(i).path("date").asText() : "";
            }

            @Override
            public Number fromString(String s) {
                return 0;
            }
        });
        NumberAxis y = new NumberAxis();
        y.setMinorTickVisible(false);
        AreaChart<Number, Number> chart = new AreaChart<>(x, y);
        chart.getStyleClass().add("dash-chart");
        chart.setLegendSide(javafx.geometry.Side.TOP);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setPrefHeight(260);
        XYChart.Series<Number, Number> runs = new XYChart.Series<>();
        runs.setName("Chạy thử");
        XYChart.Series<Number, Number> submits = new XYChart.Series<>();
        submits.setName("Nộp bài");
        for (int i = 0; i < days.size(); i++) {
            JsonNode d = days.get(i);
            runs.getData().add(new XYChart.Data<>(i, d.path("runs").asInt()));
            submits.getData().add(new XYChart.Data<>(i, d.path("submits").asInt()));
        }
        chart.getData().add(runs);
        chart.getData().add(submits);
        return chart;
    }

    private Node classTable() {
        VBox card = card("Lớp học trong hệ thống", "Học viên lớp đang chọn", actions.showStudents());
        JsonNode list = home.path("classes");
        if (list.size() == 0) card.getChildren().add(muted("Chưa có lớp nào."));
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(6);
        String[] head = {"Lớp", "Giáo viên", "Học viên", "Hoàn thành"};
        for (int i = 0; i < head.length; i++) {
            Label h = new Label(head[i]);
            h.getStyleClass().add("dash-th");
            grid.add(h, i, 0);
        }
        int r = 1;
        for (JsonNode c : list) {
            Label icon = new Label(Character.toString(c.path("name").asText("?").charAt(0)));
            icon.getStyleClass().addAll("dash-class-icon", "dash-tone-" + (r % 4));
            Label name = new Label(c.path("name").asText());
            name.getStyleClass().add("dash-row-title");
            grid.add(new HBox(10, icon, name), 0, r);
            grid.add(meta(c.path("teacher").asText()), 1, r);
            grid.add(meta(c.path("students").asText()), 2, r);
            grid.add(progressCell(c.path("completion").asInt()), 3, r);
            r++;
        }
        ColumnConstraints grow = new ColumnConstraints();
        grow.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints()
                .addAll(grow, new ColumnConstraints(), new ColumnConstraints(), new ColumnConstraints(220));
        card.getChildren().add(grid);
        return card;
    }

    // ================= Giáo viên =================

    private Node teacherBody(DashboardStats s) {
        JsonNode mine = home.path("classes");
        int studentsTotal = 0;
        long completionSum = 0;
        for (JsonNode c : mine) {
            studentsTotal += c.path("students").asInt();
            completionSum += c.path("completion").asInt();
        }
        int avg = mine.size() == 0 ? 0 : Math.round((float) completionSum / mine.size());
        HBox cards = statRow(
                stat("Lớp học", Integer.toString(Math.max(mine.size(), classes.size())), "lớp bạn dạy", "class"),
                stat(
                        "Học viên",
                        Integer.toString(studentsTotal),
                        DashboardStats.online(students) + " đang online",
                        "students"),
                stat(
                        "Bài nộp 30 ngày",
                        home.path("submits30").asText("—"),
                        DashboardStats.trend(
                                home.path("submits30").asLong(),
                                home.path("submitsPrev30").asLong()),
                        "rate"),
                stat("Tỷ lệ hoàn thành", avg + "%", "trung bình các lớp", "done"));

        VBox classesCard = card("Lớp học của bạn", "Xem học viên", actions.showStudents());
        if (mine.size() == 0) classesCard.getChildren().add(muted("Chưa có lớp nào."));
        int i = 0;
        for (JsonNode c : mine) {
            Label icon = new Label(Character.toString(c.path("name").asText("?").charAt(0)));
            icon.getStyleClass().addAll("dash-class-icon", "dash-tone-" + (i++ % 4));
            Label name = new Label(c.path("name").asText());
            name.getStyleClass().add("dash-row-title");
            Label sub = meta(c.path("students").asInt() + " học viên"
                    + (c.path("id").asLong() == classId ? " · đang chọn" : ""));
            VBox text = new VBox(1, name, sub);
            HBox.setHgrow(text, Priority.ALWAYS);
            HBox row =
                    new HBox(12, icon, text, progressCell(c.path("completion").asInt()));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("dash-row");
            classesCard.getChildren().add(row);
        }

        VBox chart = card("Bài nộp 14 ngày qua", null, null);
        chart.getChildren().add(barChart(home.path("days"), 14, "submits", 220));

        GridPane top = columns(new double[] {62, 38}, classesCard, activityCard(home.path("recent")));
        GridPane bottom = columns(new double[] {62, 38}, chart, onlineCard());
        return new VBox(18, cards, top, bottom);
    }

    private Node onlineCard() {
        VBox card = card("Học viên lớp đang chọn", "Quản lý", actions.showStudents());
        if (students.size() == 0) card.getChildren().add(muted("Lớp đang chọn chưa có học viên."));
        List<JsonNode> list = DashboardStats.recent(students, 6);
        for (JsonNode st : list) {
            String full = st.path("fullName").asText();
            Label avatar = avatar(full, st.path("online").asBoolean());
            Label who = new Label(full);
            who.getStyleClass().add("dash-row-title");
            VBox text = new VBox(
                    1,
                    who,
                    meta(st.path("completed").asInt() + " bài đạt · "
                            + st.path("activity").asText("—")));
            HBox.setHgrow(text, Priority.ALWAYS);
            Label state = new Label(st.path("online").asBoolean() ? "online" : "offline");
            state.getStyleClass()
                    .addAll("dash-pill", st.path("online").asBoolean() ? "dash-pill-ok" : "dash-pill-muted");
            HBox row = new HBox(10, avatar, text, state);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("dash-row");
            card.getChildren().add(row);
        }
        return card;
    }

    // ================= Học viên =================

    private Node studentBody(DashboardStats s) {
        GridPane top = columns(new double[] {40, 30, 30}, continueCard(s), dailyCard(s), levelCard(s));
        Node topics = topicsCard(s);
        VBox left = new VBox(18, topics, lessonsCard(s));
        VBox right = new VBox(18, badgesCard(s), leaderboardCard(), weekCard());
        GridPane bottom = columns(new double[] {64, 36}, left, right);
        return new VBox(18, top, bottom);
    }

    private Node streakCard() {
        SVGPath flame = new SVGPath();
        flame.setContent("M12 2 C13 6 17 8 17 13 A5 5 0 0 1 7 13 C7 10 9 9 9 6 C10 8 11 8 12 2 Z");
        flame.getStyleClass().add("dash-streak-flame");
        StackPane fire = new StackPane(flame);
        fire.getStyleClass().add("dash-streak-icon");
        Label t = new Label("Chuỗi ngày học");
        t.getStyleClass().add("dash-stat-title");
        int streak = home.path("streak").asInt(0);
        Label v = new Label(streak + " ngày");
        v.getStyleClass().add("dash-streak-value");
        HBox box = new HBox(10, fire, new VBox(0, t, v));
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().addAll("dash-card", "dash-streak");
        return box;
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
        Region gap = new Region();
        VBox.setVgrow(gap, Priority.ALWAYS);
        card.getChildren().addAll(title, chapter, gap, bottom);
        return card;
    }

    /** "Thử thách hôm nay": bài chưa làm kế tiếp sau bài đang làm, thưởng bằng điểm bài. */
    private Node dailyCard(DashboardStats s) {
        VBox card = card("Thử thách hôm nay", null, null);
        JsonNode pick = null;
        String current = s.continueLesson().map(l -> l.path("id").asText()).orElse("");
        for (JsonNode l : lessons) {
            if (!"NOT_STARTED".equals(l.path("status").asText("NOT_STARTED"))) continue;
            if (!l.path("active").asBoolean(true) || l.path("id").asText().equals(current)) continue;
            if (!"giang-giai".equals(l.path("loai").asText())) {
                pick = l;
                break;
            }
        }
        if (pick == null) {
            card.getChildren().add(muted("Không còn bài mới, ôn lại bài cũ nhé!"));
            return card;
        }
        Label name = new Label(pick.path("ten").asText());
        name.getStyleClass().add("dash-row-title");
        name.setWrapText(true);
        Label level = new Label(LessonScreen.chapterLabel(pick.path("chuong").asText()));
        level.getStyleClass().addAll("dash-pill", "dash-pill-ok");
        Button start = primary("Bắt đầu", () -> {});
        String id = pick.path("id").asText();
        start.setOnAction(e -> actions.openLesson().accept(id));
        Label reward = new Label("+100 XP");
        reward.getStyleClass().add("dash-reward");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox bottom = new HBox(8, start, gap, reward);
        bottom.setAlignment(Pos.CENTER_LEFT);
        Region fill = new Region();
        VBox.setVgrow(fill, Priority.ALWAYS);
        card.getChildren().addAll(name, level, fill, bottom);
        return card;
    }

    private Node levelCard(DashboardStats s) {
        VBox card = card("Tiến độ của bạn", null, null);
        Label xpTitle = meta("Tổng XP");
        Label xp = new Label(String.format("%,d", s.xp()));
        xp.getStyleClass().add("dash-stat-value");
        Label lvTitle = meta("Cấp độ");
        Label lv = new Label("Lv. " + s.level());
        lv.getStyleClass().addAll("dash-stat-value", "dash-level");
        VBox a = new VBox(2, xpTitle, xp);
        VBox b = new VBox(2, lvTitle, lv);
        HBox.setHgrow(a, Priority.ALWAYS);
        HBox.setHgrow(b, Priority.ALWAYS);
        ProgressBar bar = new ProgressBar(s.levelProgress());
        bar.getStyleClass().add("dash-bar");
        bar.setMaxWidth(Double.MAX_VALUE);
        int myRank = home.path("myRank").asInt(0);
        Label hint = meta((500 - s.xp() % 500) + " XP để lên cấp"
                + (myRank > 0
                        ? " · hạng " + myRank + "/" + home.path("classSize").asInt() + " lớp"
                        : ""));
        card.getChildren().addAll(new HBox(12, a, b), bar, hint);
        return card;
    }

    private Node topicsCard(DashboardStats s) {
        VBox card = card("Chủ đề", "Xem tất cả", actions.showExercises());
        FlowPane tiles = new FlowPane(10, 10);
        int i = 0;
        List<DashboardStats.ChapterProgress> shown = new ArrayList<>(s.chapters());
        shown.sort((a, b) -> Boolean.compare(b.done() > 0, a.done() > 0));
        for (DashboardStats.ChapterProgress c : shown.subList(0, Math.min(8, shown.size()))) {
            Label icon = new Label(c.mon().equals("sql") ? "SQL" : "{ }");
            icon.getStyleClass().addAll("dash-topic-icon", "dash-tone-" + (i++ % 4));
            Label name = new Label(c.label());
            name.getStyleClass().add("dash-row-title");
            Label count = meta(c.done() + "/" + c.total() + " bài");
            VBox tile = new VBox(5, icon, name, count);
            tile.getStyleClass().add("dash-topic");
            if (c.total() > 0 && c.done() == c.total()) tile.getStyleClass().add("dash-topic-done");
            String key = c.chuong();
            tile.setOnMouseClicked(e -> actions.showChapter().accept(key));
            tiles.getChildren().add(tile);
        }
        if (tiles.getChildren().isEmpty()) card.getChildren().add(muted("Chưa có bài nào."));
        card.getChildren().add(tiles);
        return card;
    }

    private Node lessonsCard(DashboardStats s) {
        VBox card = card("Bài tập", null, null);
        HBox head = (HBox) card.getChildren().get(0);
        ToggleGroup group = new ToggleGroup();
        for (String name : List.of("Tất cả", "Đã đạt", "Đang làm", "Chưa làm")) {
            ToggleButton t = new ToggleButton(name);
            t.getStyleClass().add("dash-tab");
            t.setToggleGroup(group);
            t.setSelected(name.equals(lessonFilter));
            t.setOnAction(e -> {
                lessonFilter = name;
                render();
            });
            head.getChildren().add(t);
        }
        Predicate<JsonNode> keep =
                switch (lessonFilter) {
                    case "Đã đạt" -> l -> "MASTERED".equals(l.path("status").asText());
                    case "Đang làm" -> l -> "OPEN".equals(l.path("status").asText());
                    case "Chưa làm" -> l ->
                            "NOT_STARTED".equals(l.path("status").asText("NOT_STARTED"));
                    default -> l -> true;
                };
        int shown = 0;
        for (JsonNode l : lessons) {
            if (!l.path("active").asBoolean(true) || !keep.test(l)) continue;
            if (shown++ >= 7) break;
            String status = l.path("status").asText("NOT_STARTED");
            Label name = new Label(l.path("ten").asText());
            name.getStyleClass().add("dash-row-title");
            name.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(name, Priority.ALWAYS);
            Label type = new Label(LessonScreen.loaiLabel(l.path("loai").asText()));
            type.getStyleClass().addAll("dash-pill", "dash-pill-info");
            Label chapter = meta(LessonScreen.chapterLabel(l.path("chuong").asText()));
            chapter.setMinWidth(110);
            Label state = new Label(
                    switch (status) {
                        case "MASTERED" -> "Đạt " + l.path("score").asText("");
                        case "OPEN" -> "Đang làm";
                        default -> "Chưa làm";
                    });
            state.getStyleClass()
                    .addAll(
                            "dash-pill",
                            switch (status) {
                                case "MASTERED" -> "dash-pill-ok";
                                case "OPEN" -> "dash-pill-warn";
                                default -> "dash-pill-muted";
                            });
            state.setMinWidth(70);
            HBox row = new HBox(10, name, type, chapter, state);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("dash-row");
            String id = l.path("id").asText();
            row.setOnMouseClicked(e -> actions.openLesson().accept(id));
            card.getChildren().add(row);
        }
        if (shown == 0) card.getChildren().add(muted("Không có bài nào ở mục này."));
        return card;
    }

    private Node badgesCard(DashboardStats s) {
        VBox card = card("Thành tích", null, null);
        List<String> badges = new ArrayList<>(s.badges());
        int streak = home.path("streak").asInt(0);
        if (streak >= 3) badges.add(0, "Chuỗi " + streak + " ngày");
        if (s.done() >= 10) badges.add(0, "10 bài đạt");
        if (s.done() >= 1) badges.add(0, "Bài đạt đầu tiên");
        if (badges.isEmpty()) {
            card.getChildren().add(muted("Nộp đạt bài đầu tiên để nhận huy hiệu."));
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
            text.setMaxWidth(80);
            VBox one = new VBox(4, medal, text);
            one.setAlignment(Pos.TOP_CENTER);
            row.getChildren().add(one);
        }
        card.getChildren().add(row);
        return card;
    }

    private Node leaderboardCard() {
        VBox card = card("Bảng xếp hạng lớp", null, null);
        JsonNode board = home.path("leaderboard");
        if (board.size() == 0) card.getChildren().add(muted("Chưa có xếp hạng."));
        for (JsonNode r : board) {
            int rank = r.path("rank").asInt();
            Label pos = new Label(Integer.toString(rank));
            pos.getStyleClass().addAll("dash-rank", "dash-rank-" + Math.min(rank, 4));
            Label who = new Label(r.path("name").asText() + (r.path("me").asBoolean() ? " (bạn)" : ""));
            who.getStyleClass().add("dash-row-title");
            HBox.setHgrow(who, Priority.ALWAYS);
            who.setMaxWidth(Double.MAX_VALUE);
            Label xp =
                    meta(r.path("mastered").asInt() + " bài · " + r.path("xp").asInt() + " XP");
            HBox row = new HBox(10, pos, avatar(r.path("name").asText(), false), who, xp);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("dash-row");
            if (r.path("me").asBoolean()) row.getStyleClass().add("dash-row-me");
            card.getChildren().add(row);
        }
        return card;
    }

    private Node weekCard() {
        VBox card = card("Hoạt động 7 ngày", null, null);
        card.getChildren().add(barChart(home.path("days"), 7, "both", 160));
        card.getChildren().add(meta(home.path("activeDays30").asInt(0) + " ngày có làm bài trong 30 ngày qua"));
        return card;
    }

    // ================= Dùng chung =================

    private Node barChart(JsonNode days, int last, String field, double height) {
        if (days.isMissingNode() || days.size() == 0) return muted("Chưa có số liệu.");
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        y.setMinorTickVisible(false);
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.getStyleClass().add("dash-chart");
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPrefHeight(height);
        chart.setCategoryGap(6);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = Math.max(0, days.size() - last); i < days.size(); i++) {
            JsonNode d = days.get(i);
            int v = "both".equals(field)
                    ? d.path("runs").asInt() + d.path("submits").asInt()
                    : d.path(field).asInt();
            series.getData().add(new XYChart.Data<>(d.path("date").asText(), v));
        }
        chart.getData().add(series);
        return chart;
    }

    private Node distributionCard(DashboardStats s) {
        VBox card = card("Phân bố theo chủ đề", null, null);
        List<DashboardStats.Slice> slices = new ArrayList<>();
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
        VBox legend = new VBox(6);
        for (int i = 0; i < slices.size(); i++) {
            var sl = slices.get(i);
            Label dot = new Label();
            dot.getStyleClass().addAll("dash-legend-dot", "dash-fill-" + i);
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            Label pct = meta(Math.round(sl.count() * 100f / Math.max(1, s.total())) + "%");
            HBox row = new HBox(8, dot, meta(sl.label()), gap, pct);
            row.setAlignment(Pos.CENTER_LEFT);
            legend.getChildren().add(row);
        }
        VBox body = new VBox(14, donut, legend);
        body.setAlignment(Pos.TOP_CENTER);
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
        Label big = new Label(Integer.toString(total));
        big.getStyleClass().add("dash-donut-total");
        VBox center = new VBox(0, meta("Tổng"), big, meta("bài tập"));
        center.setAlignment(Pos.CENTER);
        StackPane pane = new StackPane(g, center);
        pane.setMinSize(150, 150);
        return pane;
    }

    private Node activityCard(JsonNode recent) {
        VBox card = card("Hoạt động gần đây", null, null);
        if (recent.size() == 0) card.getChildren().add(muted("Chưa có hoạt động nào."));
        for (JsonNode e : recent) {
            String kind = e.path("kind").asText();
            Label icon = new Label(
                    switch (kind) {
                        case "submit-ok" -> "✓";
                        case "submit-wrong" -> "✗";
                        case "user" -> "+";
                        default -> "▣";
                    });
            icon.getStyleClass().addAll("dash-event-icon", "dash-event-" + kind);
            Label who = new Label(e.path("who").asText());
            who.getStyleClass().add("dash-row-title");
            Label what = meta(e.path("text").asText());
            what.setWrapText(true);
            VBox text = new VBox(1, who, what);
            HBox.setHgrow(text, Priority.ALWAYS);
            Label when = meta(ago(e.path("at").asLong()));
            when.setMinWidth(Region.USE_PREF_SIZE);
            HBox row = new HBox(10, icon, text, when);
            row.setAlignment(Pos.TOP_LEFT);
            row.getStyleClass().add("dash-row");
            card.getChildren().add(row);
        }
        return card;
    }

    private static String ago(long ts) {
        if (ts <= 0) return "";
        long min = (System.currentTimeMillis() - ts) / 60_000;
        if (min < 1) return "vừa xong";
        if (min < 60) return min + " phút trước";
        if (min < 24 * 60) return (min / 60) + " giờ trước";
        return (min / (24 * 60)) + " ngày trước";
    }

    private static Label avatar(String fullName, boolean online) {
        Label a = new Label(HomeScreen.initial(fullName.isBlank() ? "?" : fullName));
        a.getStyleClass().add("dash-avatar");
        if (online) a.getStyleClass().add("dash-avatar-online");
        return a;
    }

    private static Node progressCell(int pct) {
        ProgressBar bar = new ProgressBar(pct / 100.0);
        bar.getStyleClass().add("dash-bar");
        bar.setPrefWidth(150);
        Label l = meta(pct + "%");
        l.setMinWidth(36);
        HBox box = new HBox(8, bar, l);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
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
        if (hint.startsWith("↓")) h.getStyleClass().add("dash-stat-hint-down");
        SVGPath icon = new SVGPath();
        icon.setContent(
                switch (kind) {
                    case "done", "rate" -> "M5 12 L10 17 L19 7";
                    case "users" -> "M3 19 V17 C3 14.5 5.5 13.5 8 13.5 C10.5 13.5 13 14.5 13 17 V19 M11 8 A3 3 0 1 0 5 8 A3 3 0 0 0 11 8 M15 13.5 C18 13.5 21 14.5 21 17 V19 M15 5 A3 3 0 0 1 15 11";
                    case "teacher" -> "M4 19 V17 C4 14 7 13 10 13 C13 13 16 14 16 17 V19 M13 8 A3 3 0 1 0 7 8 A3 3 0 0 0 13 8 M18 7 V12 M15.5 9.5 H20.5";
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
        HBox head = new HBox(6, t, gap);
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

    private static GridPane columns(double[] widths, Node... cells) {
        GridPane grid = new GridPane();
        grid.setHgap(18);
        for (double w : widths) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(w);
            grid.getColumnConstraints().add(c);
        }
        for (int i = 0; i < cells.length; i++) {
            grid.add(cells[i], i, 0);
            GridPane.setFillHeight(cells[i], true);
            if (cells[i] instanceof Region r) r.setMaxHeight(Double.MAX_VALUE);
        }
        return grid;
    }

    private static Label meta(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("dash-row-meta");
        return l;
    }

    private static Label muted(String text) {
        Label l = meta(text);
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
