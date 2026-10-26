package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Trang thư viện bài tập dạng card toàn chiều rộng. Dữ liệu tiến độ là kết quả thật của LESSON_LIST. */
public final class ExerciseCatalogPane extends VBox {
    private final List<JsonNode> lessons = new ArrayList<>();
    private final Consumer<String> onOpen;
    private final boolean student;
    private final VBox rows = new VBox(8);
    private final TextField search = new TextField();
    private final ComboBox<String> filter = new ComboBox<>();
    private final Label stats = new Label();
    private final Label secondaryStats = new Label();
    private final Label thirdStats = new Label();
    private final Label fourthStats = new Label();
    private final ProgressBar progress = new ProgressBar(0);

    public ExerciseCatalogPane(boolean student, Consumer<String> onOpen) {
        this.student = student;
        this.onOpen = onOpen;
        getStyleClass().add("exercise-library");
        setSpacing(13);
        setPadding(new Insets(22, 28, 20, 28));

        Label heading = new Label("Bài tập");
        heading.getStyleClass().add("exercise-library-heading");
        Label intro = new Label(
                student
                        ? "Chọn bài để bắt đầu lập trình. Bài đã hoàn thành được đánh dấu màu xanh."
                        : "Duyệt toàn bộ bài tập hiện có và mở trình soạn thảo cho từng bài.");
        intro.getStyleClass().add("muted");
        intro.setWrapText(true);

        stats.getStyleClass().add("exercise-library-stat-value");
        secondaryStats.getStyleClass().add("exercise-library-stat-value");
        Label statTitle = new Label(student ? "ĐÃ HOÀN THÀNH" : "TỔNG BÀI TẬP");
        statTitle.getStyleClass().add("exercise-library-stat-label");
        Label stat2Title = new Label(student ? "TIẾN ĐỘ CỦA BẠN" : "CÁC MÔN HỌC");
        stat2Title.getStyleClass().add("exercise-library-stat-label");
        Label thirdTitle = new Label(student ? "CHƯA HOÀN THÀNH" : "SỐ BÀI DSA");
        Label fourthTitle = new Label(student ? "TỔNG BÀI" : "SỐ BÀI SQL");
        for (Label caption : new Label[] {thirdTitle, fourthTitle})
            caption.getStyleClass().add("exercise-library-stat-label");
        for (Label num : new Label[] {thirdStats, fourthStats})
            num.getStyleClass().add("exercise-library-stat-value");
        VBox first = new VBox(6, statTitle, stats);
        VBox second = new VBox(6, stat2Title, secondaryStats);
        VBox third = new VBox(6, thirdTitle, thirdStats);
        VBox fourth = new VBox(6, fourthTitle, fourthStats);
        HBox summary = new HBox(24, first, second, third, fourth);
        for (VBox card : new VBox[] {first, second, third, fourth}) HBox.setHgrow(card, Priority.ALWAYS);
        summary.getStyleClass().add("exercise-library-summary");
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.getStyleClass().add("exercise-library-progress");
        VBox summaryCard = new VBox(13, summary, progress);
        summaryCard.getStyleClass().add("exercise-library-summary-card");

        search.setPromptText("Tìm theo tên, mã bài hoặc chương...");
        search.getStyleClass().add("exercise-library-search");
        search.setMinHeight(48);
        search.setPrefHeight(48);
        search.setMaxHeight(48);
        search.textProperty().addListener((o, before, after) -> render());
        filter.getItems().addAll("Tất cả", "DSA", "SQL", "Bài giảng", "Bài tập");
        if (student) filter.getItems().addAll("Đã hoàn thành", "Chưa hoàn thành");
        filter.getSelectionModel().selectFirst();
        filter.setMinWidth(190);
        filter.setPrefHeight(48);
        filter.setMinHeight(48);
        filter.setMaxHeight(48);
        filter.getStyleClass().add("exercise-library-filter");
        filter.setOnAction(e -> render());
        Button reset = new Button("↻");
        reset.getStyleClass().add("exercise-library-refresh");
        reset.setMinSize(48, 48);
        reset.setPrefSize(48, 48);
        reset.setMaxSize(48, 48);
        reset.setOnAction(e -> {
            search.clear();
            filter.getSelectionModel().selectFirst();
        });
        HBox toolbar = new HBox(10, search, filter, reset);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(search, Priority.ALWAYS);

        rows.setFillWidth(true);
        rows.setPadding(new Insets(0, 3, 18, 0));
        ScrollPane scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPannable(true);
        scroll.getStyleClass().add("exercise-library-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().addAll(heading, intro, summaryCard, toolbar, scroll);
        update(List.of());
    }

    public void update(List<JsonNode> items) {
        lessons.clear();
        // Đủ mọi loại bài: bài giảng giải (nơi giáo viên chiếu cho lớp), luyện tập và bài tập
        lessons.addAll(items);
        lessons.sort(Comparator.comparing((JsonNode l) -> l.path("mon").asText())
                .thenComparingInt(l -> {
                    int i = LessonScreen.CHAPTERS.indexOf(l.path("chuong").asText());
                    return i < 0 ? Integer.MAX_VALUE : i;
                })
                .thenComparingInt(l -> LessonScreen.loaiRank(l.path("loai").asText()))
                .thenComparing(l -> l.path("ten").asText()));
        long completed = lessons.stream()
                .filter(l -> "MASTERED".equals(l.path("status").asText()))
                .count();
        int total = lessons.size();
        stats.setText(student ? completed + " / " + total : Integer.toString(total));
        secondaryStats.setText(
                student
                        ? (total == 0 ? "0%" : Math.round(completed * 100.0 / total) + "%")
                        : lessons.stream()
                                        .map(l -> l.path("mon").asText())
                                        .distinct()
                                        .count() + " môn");
        thirdStats.setText(
                student
                        ? Long.toString(total - completed)
                        : Long.toString(lessons.stream()
                                .filter(l ->
                                        "dsa".equalsIgnoreCase(l.path("mon").asText()))
                                .count()));
        fourthStats.setText(
                student
                        ? Integer.toString(total)
                        : Long.toString(lessons.stream()
                                .filter(l ->
                                        "sql".equalsIgnoreCase(l.path("mon").asText()))
                                .count()));
        progress.setProgress(total == 0 ? 0 : (double) completed / total);
        if (!student) progress.setVisible(false);
        progress.setManaged(student);
        render();
    }

    /** Giữ API của màn cũ để các nơi gọi không bị lỗi, trang thư viện không tô chọn. */
    public void select(String id) {}

    private void render() {
        rows.getChildren().clear();
        String query = search.getText() == null ? "" : search.getText().strip().toLowerCase(Locale.ROOT);
        String option = filter.getValue() == null ? "Tất cả" : filter.getValue();
        int count = 0;
        for (JsonNode lesson : lessons) {
            String id = lesson.path("id").asText();
            String title = lesson.path("ten").asText();
            String subject = lesson.path("mon").asText("dsa").toUpperCase(Locale.ROOT);
            String chapter = LessonScreen.chapterLabel(lesson.path("chuong").asText());
            boolean done = student && "MASTERED".equals(lesson.path("status").asText());
            if (!query.isEmpty()
                    && !(id + " " + title + " " + subject + " " + chapter)
                            .toLowerCase(Locale.ROOT)
                            .contains(query)) continue;
            if (("DSA".equals(option) || "SQL".equals(option)) && !option.equals(subject)) continue;
            String kind = lesson.path("loai").asText();
            if ("Bài giảng".equals(option) && !"giang-giai".equals(kind)) continue;
            if ("Bài tập".equals(option) && "giang-giai".equals(kind)) continue;
            if ("Đã hoàn thành".equals(option) && !done) continue;
            if ("Chưa hoàn thành".equals(option) && done) continue;

            // Danh sách gọn: không hiện mã dsa.xxx/sql.xxx trước tên bài.
            // Nhãn môn/chương có cùng chiều rộng nên tên các bài thẳng cột.
            Label subjectLabel = new Label(subject + "  ·  " + chapter);
            subjectLabel.getStyleClass().add("exercise-library-tag");
            subjectLabel.setMinWidth(154);
            subjectLabel.setPrefWidth(154);
            subjectLabel.setMaxWidth(154);
            subjectLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("exercise-library-row-title");
            titleLabel.setWrapText(false);
            titleLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
            titleLabel.setMinWidth(90);
            titleLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(titleLabel, Priority.ALWAYS);
            Label indicator = new Label(done ? "✓" : "›");
            indicator.getStyleClass().add("exercise-library-indicator");
            indicator.setMinWidth(28);
            indicator.setAlignment(Pos.CENTER_RIGHT);
            HBox line = new HBox(14, subjectLabel, titleLabel, indicator);
            line.setAlignment(Pos.CENTER_LEFT);
            line.setMinHeight(40);
            line.setPrefHeight(40);
            Button item = new Button();
            item.setGraphic(line);
            line.prefWidthProperty().bind(item.widthProperty().subtract(40));
            item.setMaxWidth(Double.MAX_VALUE);
            item.setMinHeight(66);
            item.setPrefHeight(66);
            item.setMaxHeight(66);
            item.getStyleClass().add("exercise-library-item");
            if (done) item.getStyleClass().add("exercise-library-completed");
            item.setTooltip(new javafx.scene.control.Tooltip("Mở bài: " + title + " (" + id + ")"));
            item.setOnAction(e -> onOpen.accept(id));
            rows.getChildren().add(item);
            count++;
        }
        if (count == 0) {
            Label empty = new Label("Chưa có bài tập phù hợp với bộ lọc.");
            empty.getStyleClass().add("exercise-library-empty");
            rows.getChildren().add(empty);
        }
    }
}
