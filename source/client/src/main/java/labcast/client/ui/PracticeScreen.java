package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import labcast.client.net.LcpClient;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;

/** Màn danh sách các đợt thực hành và luồng tạo đợt cho giáo viên. */
public final class PracticeScreen extends VBox {
    public record PracticeClass(long id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String SORT_ASC = "Số bài làm: tăng dần";
    private static final String SORT_DESC = "Số bài làm: giảm dần";
    private final LcpClient client;
    private final boolean teacher;
    private final List<JsonNode> lessons;
    private final Consumer<String> openLesson;
    private final Runnable onFinish;
    private final Runnable onEarlySubmit;
    private final Runnable onBack;
    private Runnable onDetailOpened = () -> {
    };
    private Runnable onDetailClosed = () -> {
    };
    private final List<PracticeClass> practiceClasses = new ArrayList<>();
    private long selectedPracticeClassId;
    private final Map<String, String> lessonStatuses = new HashMap<>();
    private final Label status = new Label();
    private final Label timer = new Label();
    private final VBox body = new VBox(8);
    private final ScrollPane bodyScroll = new ScrollPane(body);
    private final HBox teacherFilterBar = new HBox();
    private final Button deletePracticeButton = new Button("🗑");
    private final Button backButton = new Button("←");
    private final Button earlySubmitButton = new Button("Nộp bài sớm");
    private final Label headingTitle = new Label("Thực hành");
    private final Label headingState = new Label();
    private final Label headingMeta = new Label();
    private final Label headingQuestionCount = new Label();
    private final Label headingStartTime = new Label();
    private final Label headingEndTime = new Label();
    private final Label headingParticipation = new Label();
    private final ComboBox<String> studentFilter = new ComboBox<>();
    private final Timeline ticker = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
    private final Timeline refreshTicker = new Timeline(new KeyFrame(Duration.seconds(2), e -> refreshView()));
    private long closesAt;
    private long startsAt;
    private long practiceId;
    private long teacherPracticeId;
    private long listRequestVersion;
    private boolean sessionEnded;
    private boolean teacherDetail;
    private boolean teacherFilterInitialized;
    private JsonNode teacherStudents;
    private final List<String> teacherLessonIds = new ArrayList<>();

    public PracticeScreen(LcpClient client, Role role, List<JsonNode> lessons,
            Consumer<String> openLesson, Runnable onFinish) {
        this(client, role, lessons, openLesson, onFinish, onFinish, null);
    }

    public PracticeScreen(LcpClient client, Role role, List<JsonNode> lessons,
            Consumer<String> openLesson, Runnable onFinish, Runnable onEarlySubmit) {
        this(client, role, lessons, openLesson, onFinish, onEarlySubmit, null);
    }

    public PracticeScreen(LcpClient client, Role role, List<JsonNode> lessons,
            Consumer<String> openLesson, Runnable onFinish, Runnable onEarlySubmit, Runnable onBack) {
        super(12);
        this.client = client;
        this.teacher = role == Role.TEACHER;
        this.lessons = lessons;
        this.openLesson = openLesson;
        this.onFinish = onFinish;
        this.onEarlySubmit = onEarlySubmit;
        this.onBack = onBack;
        setPadding(new Insets(24));
        getStyleClass().add("practice-screen");
        backButton.getStyleClass().add("ghost");
        backButton.setAccessibleText("Về trang chủ");
        backButton.setTooltip(new javafx.scene.control.Tooltip("Về trang chủ"));
        backButton.setOnAction(e -> onBack.run());
        updateBackButton();
        headingTitle.getStyleClass().add("h1");
        headingState.getStyleClass().add("practice-state-badge");
        headingMeta.getStyleClass().add("practice-session-meta");
        headingQuestionCount.getStyleClass().add("practice-session-meta");
        headingStartTime.getStyleClass().add("practice-session-meta");
        headingEndTime.getStyleClass().add("practice-session-meta");
        headingParticipation.getStyleClass().add("practice-participation");
        timer.getStyleClass().add("practice-timer");
        earlySubmitButton.getStyleClass().add("primary");
        earlySubmitButton.setTooltip(new javafx.scene.control.Tooltip("Kết thúc đợt thực hành"));
        earlySubmitButton.setOnAction(e -> confirmEarlySubmit());
        HBox headingTopSpacer = new HBox();
        HBox.setHgrow(headingTopSpacer, javafx.scene.layout.Priority.ALWAYS);
        HBox headingTop = new HBox(10, backButton, headingTitle, headingState,
                headingTopSpacer, timer, earlySubmitButton);
        headingTop.setAlignment(Pos.CENTER_LEFT);
        HBox headingTimes = new HBox(12, headingStartTime, headingEndTime);
        headingTimes.setAlignment(Pos.CENTER_LEFT);
        VBox headingDetails = new VBox(4, headingQuestionCount, headingTimes, headingParticipation);
        headingDetails.setAlignment(Pos.CENTER_LEFT);
        VBox heading = new VBox(6, headingTop, headingDetails);
        getChildren().add(heading);
        bodyScroll.setFitToWidth(true);
        bodyScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        bodyScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        bodyScroll.setMaxWidth(Double.MAX_VALUE);
        bodyScroll.getStyleClass().add("practice-scroll");
        VBox.setVgrow(bodyScroll, javafx.scene.layout.Priority.ALWAYS);
        getChildren().addAll(status, bodyScroll);
        studentFilter.getItems().addAll("Tất cả", "Đang tham gia", "Chưa tham gia");
        studentFilter.setValue("Tất cả");
        studentFilter.setOnAction(e -> renderTeacherStudents());
        deletePracticeButton.getStyleClass().add("practice-delete-button");
        deletePracticeButton.setTooltip(new javafx.scene.control.Tooltip("Xóa bài thực hành"));
        deletePracticeButton.setOnAction(e -> confirmDeletePractice());
        updateEarlySubmitButton();
        ticker.setCycleCount(Timeline.INDEFINITE);
        refreshTicker.setCycleCount(Timeline.INDEFINITE);
        refreshTicker.play();
        load();
    }

    public void dispose() {
        ticker.stop();
        refreshTicker.stop();
    }

    public void setPracticeClasses(List<PracticeClass> classes) {
        practiceClasses.clear();
        if (classes != null)
            practiceClasses.addAll(classes);
    }

    public void setSelectedPracticeClass(long classId) {
        selectedPracticeClassId = classId;
    }

    public void setOnDetailOpened(Runnable callback) {
        onDetailOpened = callback == null ? () -> {
        } : callback;
    }

    public void setOnDetailClosed(Runnable callback) {
        onDetailClosed = callback == null ? () -> {
        } : callback;
    }

    /** Kết thúc đợt ở phía học sinh và quay lại danh sách các đợt thực hành. */
    public void finishForStudent() {
        if (sessionEnded)
            return;
        sessionEnded = true;
        ticker.stop();
        updateEarlySubmitButton();
        load();
    }

    public void submitForStudent() {
        if (teacher || sessionEnded || practiceId == 0)
            return;
        var reply = client.expect(MessageType.PRACTICE_STATE);
        client.send(MessageType.PRACTICE_SUBMIT, new PayloadWriter().i64(practiceId).toBytes());
        reply.orTimeout(10, TimeUnit.SECONDS).thenRun(() -> Platform.runLater(this::finishForStudent));
    }

    private void load() {
        long requestVersion = ++listRequestVersion;
        var reply = client.expect(MessageType.PRACTICE_STATE);
        client.send(MessageType.PRACTICE_LIST, new byte[0]);
        reply.orTimeout(10, TimeUnit.SECONDS).thenAccept(f -> javafx.application.Platform.runLater(() -> {
            try {
                if (requestVersion != listRequestVersion || teacherDetail)
                    return;
                renderList(JsonPayload.tree(f).path("sessions"));
            } catch (Exception e) {
                status.setText("Không tải được danh sách thực hành");
            }
        }));
    }

    private void renderList(JsonNode sessions) {
        if (teacherDetail && teacherPracticeId != 0)
            return;
        teacherFilterInitialized = false;
        teacherFilterBar.setVisible(false);
        teacherFilterBar.setManaged(false);
        headingTitle.setText("Thực hành");
        headingState.setText("");
        headingMeta.setText("");
        headingQuestionCount.setText("");
        headingStartTime.setText("");
        headingEndTime.setText("");
        headingParticipation.setText("");
        headingQuestionCount.setVisible(false);
        headingQuestionCount.setManaged(false);
        headingStartTime.setVisible(false);
        headingStartTime.setManaged(false);
        headingEndTime.setVisible(false);
        headingEndTime.setManaged(false);
        headingParticipation.setVisible(false);
        headingParticipation.setManaged(false);
        headingState.getStyleClass().removeAll("practice-state-ongoing", "practice-state-finished",
                "practice-state-upcoming");
        updateBackButton();
        updateEarlySubmitButton();
        body.getChildren().clear();
        if (teacher) {
            Button create = new Button("Tạo bài thực hành");
            create.getStyleClass().add("primary");
            create.setOnAction(e -> showCreateDialog());
            body.getChildren().add(create);
        }
        sessions.forEach(s -> {
            long startsAt = s.path("startsAt").asLong();
            long closesAt = s.path("closesAt").asLong();
            long now = System.currentTimeMillis();
            String state = state(startsAt, closesAt, now);
            if (s.path("ended").asBoolean(false)) {
                state = "finished";
            }
            String name = s.path("name").asText("Đợt thực hành");
            Label title = new Label(name);
            title.getStyleClass().add("practice-session-name");
            Label type = badge("THỰC HÀNH", "practice-type-badge");
            Label stateBadge = badge(stateText(state, startsAt), "practice-state-badge");
            stateBadge.getStyleClass().add("practice-state-" + state);
            HBox heading = new HBox(10, title, type, stateBadge);
            heading.setAlignment(Pos.CENTER_LEFT);
            Label questionCount = new Label(
                    s.path("questionCount").asInt(s.path("lessons").size()) + " câu hỏi · Bắt đầu "
                            + format(startsAt));
            questionCount.getStyleClass().add("practice-session-meta");
            VBox details = new VBox(7, heading, questionCount);
            HBox item = new HBox(12, details);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setMaxWidth(Double.MAX_VALUE);
            item.getStyleClass().add("practice-session-card");
            if (teacher) {
                item.setOnMouseClicked(e -> openTeacherSession(s.path("id").asLong()));
            } else if (state.equals("ongoing")) {
                Button arrow = new Button("→");
                arrow.getStyleClass().addAll("practice-join-arrow", "success");
                arrow.setTooltip(new javafx.scene.control.Tooltip("Tham gia thực hành"));
                arrow.setOnAction(e -> join(s.path("id").asLong()));
                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                item.getChildren().addAll(spacer, arrow);
            }
            body.getChildren().add(item);
        });
        if (sessions.isEmpty())
            status.setText("Chưa có đợt thực hành");
    }

    private static Label badge(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        return label;
    }

    private static String stateText(String state, long startsAt) {
        return switch (state) {
            case "ongoing" -> "● Đang diễn ra";
            case "finished" -> "● Kết thúc";
            default -> "● Sắp tới · " + format(startsAt);
        };
    }

    private void openTeacherSession(long id) {
        ++listRequestVersion;
        teacherPracticeId = id;
        teacherDetail = true;
        onDetailOpened.run();
        var reply = client.expect(MessageType.PRACTICE_STATE);
        client.send(MessageType.PRACTICE_START, new PayloadWriter().i64(id).toBytes());
        reply.orTimeout(10, TimeUnit.SECONDS).thenAccept(f -> Platform.runLater(() -> {
            try {
                JsonNode state = JsonPayload.tree(f);
                headingTitle.setText(state.path("name").asText("Thực hành"));
                startsAt = state.path("startsAt").asLong();
                closesAt = state.path("closesAt").asLong();
                String detailState = state(startsAt, closesAt, System.currentTimeMillis());
                headingState.setText(stateText(detailState, startsAt));
                headingQuestionCount.setText(state.path("lessons").size() + " câu hỏi");
                headingStartTime.setText("Bắt đầu " + format(startsAt));
                headingEndTime.setText("Kết thúc " + format(closesAt));
                headingQuestionCount.setVisible(true);
                headingQuestionCount.setManaged(true);
                headingStartTime.setVisible(true);
                headingStartTime.setManaged(true);
                headingEndTime.setVisible(true);
                headingEndTime.setManaged(true);
                updateTeacherTimer();
                renderTeacherDetails(state);
            } catch (Exception e) {
                status.setText("Không tải được danh sách học sinh");
            }
        }));
    }

    private void renderTeacherDetails(JsonNode state) {
        if (!teacherFilterInitialized) {
            body.getChildren().clear();
            teacherLessonIds.clear();
            state.path("lessons").forEach(lesson -> teacherLessonIds.add(lesson.asText()));
            String previousFilter = studentFilter.getValue();
            studentFilter.getItems().clear();
            studentFilter.getItems().addAll("Tất cả", "Đang tham gia", "Chưa tham gia");
            studentFilter.getItems().addAll(SORT_ASC, SORT_DESC);
            state.path("lessons").forEach(lessonId -> {
                JsonNode lesson = findLesson(lessonId.asText());
                String name = lesson == null ? lessonId.asText() : lesson.path("ten").asText(lessonId.asText());
                studentFilter.getItems().add("Bài: " + name);
            });
            studentFilter.setValue(previousFilter != null && studentFilter.getItems().contains(previousFilter)
                    ? previousFilter
                    : "Tất cả");
            teacherFilterBar.getChildren().setAll(studentFilter, deletePracticeButton);
            teacherFilterBar.setAlignment(Pos.CENTER_RIGHT);
            teacherFilterBar.setMaxWidth(Double.MAX_VALUE);
            teacherFilterBar.setVisible(true);
            teacherFilterBar.setManaged(true);
            body.getChildren().add(teacherFilterBar);
            teacherFilterInitialized = true;
        }
        updateDeletePracticeButton();
        teacherStudents = state.path("students");
        renderTeacherStudents();
    }

    private void confirmDeletePractice() {
        if (!teacher || !teacherDetail || teacherPracticeId == 0)
            return;
        if ("ongoing".equals(state(startsAt, closesAt, System.currentTimeMillis()))) {
            status.setText("Không thể xóa đợt thực hành đang diễn ra");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc chắn muốn xóa bài thực hành này không?\n"
                        + "Danh sách bài và dữ liệu theo dõi của đợt này sẽ bị xóa.",
                ButtonType.OK, ButtonType.CANCEL);
        confirmation.setTitle("Xác nhận xóa bài thực hành");
        confirmation.setHeaderText("Xóa bài thực hành");
        if (getScene() != null)
            confirmation.initOwner(getScene().getWindow());
        confirmation.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK)
                return;
            var reply = client.expect(MessageType.PRACTICE_STATE, MessageType.ERROR);
            client.send(MessageType.PRACTICE_DELETE, new PayloadWriter().i64(teacherPracticeId).toBytes());
            reply.orTimeout(10, TimeUnit.SECONDS).whenComplete((frame, error) -> Platform.runLater(() -> {
                if (error != null || frame.type() != MessageType.PRACTICE_STATE) {
                    status.setText("Không xóa được bài thực hành");
                    return;
                }
                teacherDetail = false;
                teacherPracticeId = 0;
                teacherStudents = null;
                teacherFilterInitialized = false;
                onDetailClosed.run();
                load();
            }));
        });
    }

    private void renderTeacherStudents() {
        if (teacherStudents == null)
            return;
        while (body.getChildren().size() > 1)
            body.getChildren().remove(1);
        String filter = studentFilter.getValue();
        List<JsonNode> visibleStudents = new ArrayList<>();
        teacherStudents.forEach(student -> {
            boolean active = student.path("active").asBoolean(false);
            if (("Đang tham gia".equals(filter) && !active) || ("Chưa tham gia".equals(filter) && active))
                return;
            if (filter != null && filter.startsWith("Bài: ")) {
                int lessonIndex = studentFilter.getItems().indexOf(filter) - 5;
                if (lessonIndex < 0 || lessonIndex >= teacherLessonIds.size()
                        || !student.path("completedLessons").has(teacherLessonIds.get(lessonIndex)))
                    return;
            }
            visibleStudents.add(student);
        });
        if (SORT_ASC.equals(filter) || SORT_DESC.equals(filter)) {
            visibleStudents.sort((left, right) -> {
                int comparison = Integer.compare(left.path("completed").asInt(), right.path("completed").asInt());
                return SORT_DESC.equals(filter) ? -comparison : comparison;
            });
        }
        visibleStudents.forEach(student -> {
            boolean active = student.path("active").asBoolean(false);
            Label name = new Label(student.path("fullName").asText());
            name.getStyleClass().add("practice-student-name");
            Label username = new Label(student.path("username").asText());
            username.getStyleClass().add("practice-student-username");
            VBox identity = new VBox(2, name, username);
            Label progress = new Label(student.path("completed").asInt() + " / "
                    + student.path("total").asInt());
            progress.getStyleClass().add("practice-student-progress");
            Label presence = new Label("●");
            presence.getStyleClass().add(active ? "practice-student-active" : "practice-student-inactive");
            HBox row = new HBox(10, presence, identity, progress);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(Double.MAX_VALUE);
            row.getStyleClass().add("practice-student-card");
            HBox.setHgrow(identity, javafx.scene.layout.Priority.ALWAYS);
            body.getChildren().add(row);
        });
        status.setText(teacherStudents.isEmpty() ? "Lớp chưa có học sinh" : "");
        if (teacherDetail) {
            long activeCount = 0;
            for (JsonNode student : teacherStudents) {
                if (student.path("active").asBoolean(false))
                    activeCount++;
            }
            boolean ongoing = "ongoing".equals(state(startsAt, closesAt, System.currentTimeMillis()));
            headingParticipation.setVisible(ongoing);
            headingParticipation.setManaged(ongoing);
            headingParticipation.setText("● Đang tham gia: " + activeCount + "/" + teacherStudents.size());
            headingParticipation.getStyleClass().removeAll("practice-participation-finished");
        }
    }

    private void refreshView() {
        if (teacherDetail && teacherPracticeId != 0) {
            openTeacherSession(teacherPracticeId);
        } else if (!teacher && practiceId != 0 && !sessionEnded) {
            touchPractice();
        } else if (practiceId == 0 || sessionEnded) {
            load();
        }
    }

    private void updateTeacherTimer() {
        if (!teacherDetail)
            return;
        long left = closesAt - System.currentTimeMillis();
        long untilStart = startsAt - System.currentTimeMillis();
        if (untilStart > 0) {
            headingState.setText("● Sắp tới");
            headingState.getStyleClass().removeAll("practice-state-finished", "practice-state-ongoing");
            headingState.getStyleClass().add("practice-state-upcoming");
            timer.setText("Bắt đầu sau " + formatDuration(untilStart));
            headingParticipation.setVisible(false);
            headingParticipation.setManaged(false);
        } else if (left <= 0) {
            headingState.setText("● Kết thúc");
            headingState.getStyleClass().removeAll("practice-state-ongoing", "practice-state-upcoming");
            headingState.getStyleClass().add("practice-state-finished");
            timer.setText("Đã hết giờ");
            headingParticipation.setVisible(false);
            headingParticipation.setManaged(false);
        } else {
            headingState.setText("● Đang diễn ra");
            headingState.getStyleClass().removeAll("practice-state-finished", "practice-state-upcoming");
            headingState.getStyleClass().add("practice-state-ongoing");
            timer.setText("Còn " + formatDuration(left));
        }
        updateDeletePracticeButton();
    }

    private void updateDeletePracticeButton() {
        boolean canDelete = teacher && teacherDetail
                && !"ongoing".equals(state(startsAt, closesAt, System.currentTimeMillis()));
        deletePracticeButton.setVisible(canDelete);
        deletePracticeButton.setManaged(canDelete);
    }

    private static String formatDuration(long millis) {
        long seconds = Math.max(0, millis / 1_000);
        return (seconds / 3600) + ":" + String.format("%02d:%02d", (seconds / 60) % 60, seconds % 60);
    }

    private void touchPractice() {
        var reply = client.expect(MessageType.PRACTICE_STATE);
        client.send(MessageType.PRACTICE_START, new PayloadWriter().i64(practiceId).toBytes());
        reply.orTimeout(5, TimeUnit.SECONDS).exceptionally(error -> null);
    }

    private void showCreateDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Tạo bài thực hành");
        dialog.setHeaderText("Thiết lập đợt thực hành");
        DialogPane pane = dialog.getDialogPane();
        ButtonType createType = new ButtonType("Tạo bài", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(createType, ButtonType.CANCEL);
        Theme.apply(pane);
        pane.getStyleClass().add("practice-dialog");
        pane.lookupButton(createType).getStyleClass().add("primary");

        VBox content = new VBox(16);
        content.getStyleClass().add("practice-dialog-content");
        content.setPadding(new Insets(20, 24, 16, 24));
        TextField name = new TextField();
        name.setPromptText("Ví dụ: Thực hành cấu trúc dữ liệu tuần 1");
        name.setMaxWidth(Double.MAX_VALUE);
        ComboBox<PracticeClass> classChoice = new ComboBox<>();
        classChoice.getItems().addAll(practiceClasses);
        classChoice.setMaxWidth(Double.MAX_VALUE);
        if (!classChoice.getItems().isEmpty()) {
            PracticeClass selected = classChoice.getItems().stream()
                    .filter(option -> option.id() == selectedPracticeClassId)
                    .findFirst()
                    .orElse(classChoice.getItems().get(0));
            classChoice.getSelectionModel().select(selected);
        }
        List<JsonNode> selectedLessons = new ArrayList<>();
        VBox selectedList = new VBox(4);
        Label selectedHint = new Label("Chưa chọn bài tập");
        selectedHint.getStyleClass().add("muted");
        selectedList.getChildren().add(selectedHint);
        Button choose = new Button("Chọn bài tập");
        choose.getStyleClass().add("primary");
        choose.setMaxWidth(Double.MAX_VALUE);
        choose.setOnAction(e -> showLessonPicker(selectedLessons, selectedList, selectedHint, choose));

        LocalDateTime defaultStart = LocalDateTime.now().plusMinutes(1);
        DatePicker date = new DatePicker(defaultStart.toLocalDate());
        TextField hour = numberField(String.valueOf(defaultStart.getHour()), "00 - 23");
        TextField minute = numberField(String.format("%02d", defaultStart.getMinute()), "00 - 59");
        TextField duration = numberField("60", "Số phút");
        hour.getStyleClass().add("practice-time-field");
        minute.getStyleClass().add("practice-time-field");
        duration.getStyleClass().add("practice-time-field");
        date.getStyleClass().add("practice-date-field");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.getStyleClass().add("practice-dialog-form");
        javafx.scene.layout.ColumnConstraints labelColumn = new javafx.scene.layout.ColumnConstraints();
        labelColumn.setMinWidth(118);
        javafx.scene.layout.ColumnConstraints fieldColumn = new javafx.scene.layout.ColumnConstraints();
        fieldColumn.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        form.getColumnConstraints().addAll(labelColumn, fieldColumn);

        addFormRow(form, 0, "Tên bài thực hành", name);
        addFormRow(form, 1, "Lớp thực hành", classChoice);
        VBox lessonSelection = new VBox(8, choose, selectedList);
        lessonSelection.setFillWidth(true);
        addFormRow(form, 2, "Bài tập", lessonSelection);

        HBox time = new HBox(8, hour, new Label(":"), minute);
        time.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(hour, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(minute, javafx.scene.layout.Priority.ALWAYS);
        addFormRow(form, 3, "Ngày bắt đầu", date);
        addFormRow(form, 4, "Giờ bắt đầu", time);
        addFormRow(form, 5, "Thời lượng", duration);
        content.getChildren().add(form);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(420);
        scroll.getStyleClass().add("practice-dialog-scroll");
        pane.setContent(scroll);
        pane.setPrefWidth(520);

        pane.lookupButton(createType).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            List<String> ids = new ArrayList<>();
            selectedLessons.forEach(lesson -> ids.add(lesson.path("id").asText()));
            if (name.getText().isBlank()) {
                status.setText("Hãy nhập tên bài thực hành");
                e.consume();
                return;
            }
            if (ids.isEmpty()) {
                status.setText("Hãy chọn ít nhất một bài");
                e.consume();
                return;
            }
            if (classChoice.getValue() == null) {
                status.setText("Hãy chọn lớp thực hành");
                e.consume();
                return;
            }
            create(name.getText(), classChoice.getValue().id(), ids, date, hour, minute, duration);
        });
        dialog.showAndWait();
    }

    private void showLessonPicker(List<JsonNode> selectedLessons, VBox selectedList, Label selectedHint,
            Button chooseButton) {
        Dialog<Void> picker = new Dialog<>();
        picker.setTitle("Chọn bài tập");
        picker.setHeaderText("Chọn các bài sẽ có trong đợt thực hành");
        DialogPane pane = picker.getDialogPane();
        ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(ok, ButtonType.CANCEL);

        VBox choicesBox = new VBox(6);
        choicesBox.setPadding(new Insets(12));
        List<CheckBox> choices = new ArrayList<>();
        for (JsonNode lesson : lessons) {
            if (!"bai-tap".equals(lesson.path("loai").asText())) {
                continue;
            }
            CheckBox choice = new CheckBox(lesson.path("id").asText() + " · " + lesson.path("ten").asText());
            choice.setSelected(selectedLessons.contains(lesson));
            choices.add(choice);
            choicesBox.getChildren().add(choice);
        }
        ScrollPane scroll = new ScrollPane(choicesBox);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(420);
        pane.setContent(scroll);
        pane.getStyleClass().add("practice-dialog");
        pane.setPrefWidth(520);
        Theme.apply(pane);
        pane.lookupButton(ok).getStyleClass().add("primary");

        pane.lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            selectedLessons.clear();
            for (int i = 0; i < choices.size(); i++) {
                if (choices.get(i).isSelected())
                    selectedLessons.add(lessons.get(i));
            }
            selectedList.getChildren().clear();
            if (selectedLessons.isEmpty()) {
                selectedHint.setText("Chưa chọn bài tập");
                selectedList.getChildren().add(selectedHint);
                chooseButton.setText("Chọn bài tập");
            } else {
                for (JsonNode lesson : selectedLessons) {
                    Label selected = new Label("• " + lesson.path("id").asText() + " · "
                            + lesson.path("ten").asText());
                    selected.getStyleClass().add("practice-selected-lesson");
                    selectedList.getChildren().add(selected);
                }
                chooseButton.setText("Chọn lại bài tập (" + selectedLessons.size() + ")");
            }
        });
        picker.showAndWait();
    }

    private static void addFormRow(GridPane form, int row, String labelText, javafx.scene.Node field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("practice-form-label");
        form.add(label, 0, row);
        form.add(field, 1, row);
        GridPane.setFillWidth(field, true);
        if (field instanceof javafx.scene.layout.Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
    }

    private static TextField numberField(String value, String prompt) {
        TextField field = new TextField(value);
        field.setPromptText(prompt);
        field.setMaxWidth(Double.MAX_VALUE);
        field.setTextFormatter(new javafx.scene.control.TextFormatter<>(
                change -> change.getControlNewText().matches("\\d{0,4}") ? change : null));
        return field;
    }

    private void create(String name, long classId, List<String> lessonIds, DatePicker date, TextField hour, TextField minute,
            TextField duration) {
        ObjectNode n = JsonPayload.MAPPER.createObjectNode();
        ArrayNode ids = n.putArray("lessons");
        try {
            lessonIds.forEach(ids::add);
            int hourValue = Integer.parseInt(hour.getText());
            int minuteValue = Integer.parseInt(minute.getText());
            int durationValue = Integer.parseInt(duration.getText());
            if (hourValue < 0 || hourValue > 23 || minuteValue < 0 || minuteValue > 59
                    || durationValue < 1 || durationValue > 24 * 60) {
                throw new IllegalArgumentException("Ngoài giới hạn");
            }
            long at = LocalDateTime.of(date.getValue(), java.time.LocalTime.of(hourValue, minuteValue))
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            n.put("startsAt", at);
            n.put("durationMs", durationValue * 60_000L);
            n.put("name", name.strip());
            n.put("classId", classId);
            var reply = client.expect(MessageType.PRACTICE_STATE);
            client.send(JsonPayload.frame(MessageType.PRACTICE_CREATE, n));
            reply.orTimeout(10, TimeUnit.SECONDS)
                    .thenRun(() -> javafx.application.Platform.runLater(() -> {
                        status.setText("Đã tạo đợt thực hành");
                        load();
                    }));
        } catch (Exception e) {
            status.setText("Thông tin tạo bài không hợp lệ");
        }
    }

    private static String state(long startsAt, long closesAt, long now) {
        if (now < startsAt)
            return "upcoming";
        if (now < closesAt)
            return "ongoing";
        return "finished";
    }

    private static String stateLabel(String state, long startsAt) {
        return switch (state) {
            case "upcoming" -> "Bắt đầu " + format(startsAt);
            case "ongoing" -> "Đang diễn ra";
            default -> "Kết thúc";
        };
    }

    private void join(long id) {
        if (teacher)
            return;
        var reply = client.expect(MessageType.PRACTICE_STATE);
        client.send(MessageType.PRACTICE_START, new PayloadWriter().i64(id).toBytes());
        reply.orTimeout(10, TimeUnit.SECONDS).thenAccept(f -> javafx.application.Platform.runLater(() -> {
            try {
                JsonNode n = JsonPayload.tree(f);
                practiceId = n.path("id").asLong();
                sessionEnded = false;
                onDetailOpened.run();
                updateBackButton();
                updateEarlySubmitButton();
                closesAt = n.path("closesAt").asLong();
                body.getChildren().clear();
                n.path("lessons").forEach(l -> {
                    String lessonId = l.asText();
                    JsonNode lesson = findLesson(lessonId);
                    String name = lesson == null ? lessonId : lesson.path("ten").asText(lessonId);
                    String subject = lesson == null ? "" : lesson.path("mon").asText("").toUpperCase();
                    Label idLabel = new Label(lessonId);
                    idLabel.getStyleClass().add("practice-lesson-id");
                    Label typeLabel = new Label(subject.isBlank() ? "BÀI TẬP" : subject);
                    typeLabel.getStyleClass().add("practice-lesson-type");
                    Label nameLabel = new Label(name);
                    nameLabel.getStyleClass().add("practice-lesson-name");
                    String lessonStatus = lessonStatuses.get(lessonId);
                    Label check = new Label("correct".equals(lessonStatus) ? "✓"
                            : "wrong".equals(lessonStatus) ? "✕" : "");
                    check.getStyleClass().add("practice-lesson-check");
                    HBox card = new HBox(10, idLabel, typeLabel, nameLabel, check);
                    card.setAlignment(Pos.CENTER_LEFT);
                    card.setMaxWidth(Double.MAX_VALUE);
                    card.setUserData(lessonId);
                    card.getStyleClass().add("practice-lesson-card");
                    if ("correct".equals(lessonStatus)) {
                        card.getStyleClass().add("practice-lesson-correct");
                    } else if ("wrong".equals(lessonStatus)) {
                        card.getStyleClass().add("practice-lesson-wrong");
                    }
                    card.setOnMouseClicked(e -> openLesson.accept(lessonId));
                    body.getChildren().add(card);
                });
                ticker.playFromStart();
                tick();
            } catch (Exception e) {
                status.setText("Không vào được đợt thực hành");
            }
        }));
    }

    private void updateBackButton() {
        boolean canGoBack = onBack != null && (teacher || practiceId == 0 || sessionEnded);
        backButton.setVisible(canGoBack);
        backButton.setManaged(canGoBack);
    }

    private void updateEarlySubmitButton() {
        boolean visible = !teacher && practiceId != 0 && !sessionEnded;
        earlySubmitButton.setVisible(visible);
        earlySubmitButton.setManaged(visible);
    }

    private void confirmEarlySubmit() {
        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn nộp bài thực hành sớm không?",
                ButtonType.OK,
                ButtonType.CANCEL);
        confirmation.setTitle("Xác nhận nộp bài");
        confirmation.setHeaderText("Nộp bài thực hành sớm");
        if (getScene() != null) {
            confirmation.initOwner(getScene().getWindow());
        }
        confirmation.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                onEarlySubmit.run();
            }
        });
    }

    private JsonNode findLesson(String id) {
        for (JsonNode lesson : lessons) {
            if (id.equals(lesson.path("id").asText()))
                return lesson;
        }
        return null;
    }

    public void updateLessonStatus(String lessonId, boolean correct) {
        lessonStatuses.put(lessonId, correct ? "correct" : "wrong");
        for (javafx.scene.Node node : body.getChildren()) {
            if (node instanceof HBox card && lessonId.equals(card.getUserData())) {
                card.getStyleClass().removeAll("practice-lesson-correct", "practice-lesson-wrong");
                card.getStyleClass().add(correct ? "practice-lesson-correct" : "practice-lesson-wrong");
                if (card.getChildren().size() > 3 && card.getChildren().get(3) instanceof Label check) {
                    check.setText(correct ? "✓" : "✕");
                }
            }
        }
    }

    private void tick() {
        long left = closesAt - System.currentTimeMillis();
        if (left <= 0) {
            timer.setText("Đã hết giờ");
            ticker.stop();
            onFinish.run();
            return;
        }
        timer.setText("Còn " + (left / 60_000) + ":" + String.format("%02d", (left / 1_000) % 60));
    }

    private static String format(long value) {
        return TIME.format(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(value), ZoneId.systemDefault()));
    }
}
