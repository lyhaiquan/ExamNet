package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import labcast.client.net.LcpClient;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;

/** Thực hành: danh sách lớp -> tạo lớp (giáo viên) / chọn lớp -> quản lý sinh viên và buổi học. */
public final class PracticeRoomsScreen extends BorderPane {
    private enum View {
        DIRECTORY,
        CREATE,
        DETAIL
    }

    private final LcpClient client;
    private final boolean teacher;
    private final VBox content = new VBox(17);
    private final VBox classList = new VBox(10);
    private final VBox roomList = new VBox(12);
    private final VBox studentList = new VBox(7);
    private final Label status = new Label();
    private final Label heading = new Label();
    private final Label subtitle = new Label();
    private final Label rosterCount = new Label("0 sinh viên");
    private final TextField className = new TextField();
    private final TextField roomName = new TextField();
    private final TextField inviteCode = new TextField();
    private final List<JsonNode> availableLessons = new ArrayList<>();
    private View view = View.DIRECTORY;
    private long currentClassId;
    private String currentClassName = "";
    private String currentInvite = "";
    private LongConsumer onClassChanged = id -> {};
    private Consumer<JsonNode> onClassesLoaded = json -> {};
    private Consumer<String> onJoin = name -> {};
    private Consumer<String> onOpenLesson = id -> {};
    private Runnable onImportRoster = () -> {};
    private volatile boolean disposed;

    public PracticeRoomsScreen(LcpClient client, boolean teacher) {
        this(client, teacher, List.of());
    }

    public PracticeRoomsScreen(LcpClient client, boolean teacher, List<JsonNode> lessons) {
        this.client = client;
        this.teacher = teacher;
        setAvailableLessons(lessons);
        getStyleClass().add("practice-screen-v2");
        setPadding(new Insets(22, 28, 20, 28));
        heading.getStyleClass().add("workspace-heading");
        subtitle.getStyleClass().add("muted");
        subtitle.setWrapText(true);
        status.getStyleClass().add("workspace-status");
        VBox top = new VBox(6, heading, subtitle, status);
        top.setPadding(new Insets(0, 0, 17, 0));
        setTop(top);
        ScrollPane scroller = new ScrollPane(content);
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.getStyleClass().add("workspace-scroll");
        setCenter(scroller);
        showDirectory();
    }

    public void close() {
        disposed = true;
    }

    public void setAvailableLessons(List<JsonNode> lessons) {
        availableLessons.clear();
        if (lessons != null)
            for (JsonNode n : lessons) if ("bai-tap".equals(n.path("loai").asText())) availableLessons.add(n);
    }

    public void onJoined(Consumer<String> cb) {
        onJoin = cb;
    }

    public void onClassChanged(LongConsumer cb) {
        onClassChanged = cb;
    }

    public void onClassesLoaded(Consumer<JsonNode> cb) {
        onClassesLoaded = cb;
    }

    public void onOpenLesson(Consumer<String> cb) {
        onOpenLesson = cb;
    }

    public void onImportRoster(Runnable cb) {
        onImportRoster = cb;
    }

    private Label section(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("workspace-section-title");
        return l;
    }

    private Button primary(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("workspace-primary-button");
        b.setOnAction(e -> action.run());
        return b;
    }

    private Button secondary(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("workspace-secondary-button");
        b.setOnAction(e -> action.run());
        return b;
    }

    private VBox panel(javafx.scene.Node... children) {
        VBox box = new VBox(13, children);
        box.getStyleClass().add("workspace-room-card");
        return box;
    }

    private HBox row(javafx.scene.Node... children) {
        HBox box = new HBox(10, children);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /** Trang đầu tiên luôn là danh sách lớp, thay vì form tạo lớp chiếm đầu màn hình. */
    private void showDirectory() {
        view = View.DIRECTORY;
        heading.setText(teacher ? "Lớp thực hành" : "Thực hành của tôi");
        subtitle.setText(
                teacher
                        ? "Danh sách lớp bạn đang phụ trách. Chọn lớp để quản lý bài, buổi học và sinh viên."
                        : "Các lớp giáo viên đã thêm bạn vào. Chọn lớp để xem và làm bài được giao.");
        status.setText("");
        content.getChildren().clear();
        HBox toolbar = row(section(teacher ? "DANH SÁCH LỚP" : "LỚP ĐÃ THAM GIA"));
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        toolbar.getChildren().add(gap);
        if (teacher) toolbar.getChildren().add(primary("+ Tạo lớp thực hành", this::showCreate));
        else toolbar.getChildren().add(secondary("↻ Làm mới", this::reload));
        content.getChildren().addAll(toolbar, classList);
        if (!teacher) {
            inviteCode.setPromptText("Nhập mã lớp gồm 6 chữ số (nếu chưa được thêm)");
            inviteCode.setPrefWidth(330);
            VBox join = panel(
                    section("THAM GIA BẰNG MÃ LỚP"),
                    row(
                            inviteCode,
                            primary(
                                    "Tham gia",
                                    () -> createOrJoinClass(MessageType.CLASS_JOIN_REQ, inviteCode.getText()))));
            content.getChildren().add(join);
        }
        loadClasses();
    }

    /** Trang tạo lớp chỉ yêu cầu tên lớp; không còn thao tác tạo bài mới trong form. */
    private void showCreate() {
        if (!teacher) return;
        view = View.CREATE;
        heading.setText("Tạo lớp thực hành");
        subtitle.setText(
                "Đặt tên lớp. Sau khi tạo, bạn sẽ chuyển sang trang chi tiết để tải CSV sinh viên và tổ chức buổi học.");
        status.setText("");
        className.setPromptText("Ví dụ: Lập trình mạng - Nhóm 01");
        className.setMaxWidth(Double.MAX_VALUE);
        VBox form = panel(
                section("THÔNG TIN LỚP"),
                new Label("Tên lớp thực hành"),
                className,
                row(
                        secondary("← Quay lại", this::showDirectory),
                        primary(
                                "Tạo lớp thực hành",
                                () -> createOrJoinClass(MessageType.CLASS_CREATE_REQ, className.getText()))));
        form.setMaxWidth(700);
        content.getChildren().setAll(form);
    }

    private void showDetail(long id, String name, String code) {
        currentClassId = id;
        currentClassName = name;
        currentInvite = code;
        view = View.DETAIL;
        heading.setText(name);
        subtitle.setText(
                teacher
                        ? "Quản lý sinh viên và các buổi thực hành của lớp."
                        : "Chọn buổi thực hành, sau đó mở bài tập được giáo viên giao.");
        status.setText("");
        HBox actions = row(secondary("← Tất cả lớp", this::showDirectory));
        if (teacher) {
            Label invite = new Label(code.isEmpty() ? "Mã mời: chưa có" : "Mã mời lớp: " + code);
            invite.getStyleClass().add("workspace-invite-badge");
            actions.getChildren().add(invite);
        }
        VBox roomsPanel = panel(section("CÁC BUỔI THỰC HÀNH"));
        if (teacher) {
            roomName.setPromptText("Tên buổi thực hành mới (ví dụ: Buổi 1 - TCP/UDP)");
            HBox createRoomRow = row(roomName, primary("+ Tạo buổi", this::createRoom));
            HBox.setHgrow(roomName, Priority.ALWAYS);
            roomsPanel.getChildren().add(createRoomRow);
        }
        roomsPanel.getChildren().add(roomList);
        content.getChildren().setAll(actions);
        if (teacher) {
            HBox rosterHeader = row(section("DANH SÁCH SINH VIÊN"));
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            rosterCount.getStyleClass().add("workspace-count-badge");
            rosterHeader
                    .getChildren()
                    .addAll(
                            spacer,
                            rosterCount,
                            secondary("↓ Nhập CSV sinh viên", () -> onImportRoster.run()),
                            secondary("↻", this::loadStudents));
            Label rosterInfo = new Label(
                    "Định dạng CSV: username,full_name,password. Sinh viên đã tồn tại được ghi danh vào lớp này.");
            rosterInfo.getStyleClass().add("muted");
            VBox rosterPanel = panel(rosterHeader, rosterInfo, studentList);
            content.getChildren().add(rosterPanel);
        }
        content.getChildren().add(roomsPanel);
        reload();
    }

    private void createOrJoinClass(MessageType type, String input) {
        String value = input == null ? "" : input.strip();
        if (value.isEmpty()) {
            status.setText("Vui lòng nhập tên hoặc mã lớp.");
            return;
        }
        MessageType responseType =
                type == MessageType.CLASS_CREATE_REQ ? MessageType.CLASS_CREATE_RESULT : MessageType.CLASS_JOIN_RESULT;
        var pending = client.expect(responseType, MessageType.ERROR);
        client.send(type, new PayloadWriter().str(value).toBytes());
        pending.orTimeout(12, TimeUnit.SECONDS)
                .whenComplete((frame, error) -> Platform.runLater(() -> {
                    if (disposed) return;
                    if (error != null || frame.type() == MessageType.ERROR) {
                        try {
                            status.setText(
                                    error != null
                                            ? error.getMessage()
                                            : ErrorMessage.from(frame).message());
                        } catch (Exception ignored) {
                            status.setText("Không thực hiện được yêu cầu.");
                        }
                        return;
                    }
                    try {
                        JsonNode data = JsonPayload.tree(frame);
                        long id = data.path("id").asLong();
                        onClassChanged.accept(id);
                        showDetail(
                                id,
                                data.path("name").asText(value),
                                data.path("joinCode").asText(""));
                        if (teacher) status.setText("Đã tạo lớp. Bạn có thể tải danh sách sinh viên bằng CSV.");
                        else status.setText("Đã tham gia lớp thành công.");
                    } catch (Exception ex) {
                        status.setText(ex.getMessage());
                    }
                }));
    }

    private void loadClasses() {
        if (disposed) return;
        var pending = client.expect(MessageType.CLASS_LIST_DATA, MessageType.ERROR);
        client.send(MessageType.CLASS_LIST_REQ, new byte[0]);
        pending.orTimeout(12, TimeUnit.SECONDS)
                .whenComplete((frame, error) -> Platform.runLater(() -> {
                    if (disposed) return;
                    if (error != null || frame.type() == MessageType.ERROR) {
                        if (view == View.DIRECTORY) status.setText("Không tải được danh sách lớp từ Server.");
                        return;
                    }
                    try {
                        JsonNode json = JsonPayload.tree(frame);
                        onClassesLoaded.accept(json);
                        if (view != View.DIRECTORY) return;
                        classList.getChildren().clear();
                        JsonNode classes = json.path("classes");
                        if (!classes.isArray() || classes.isEmpty()) {
                            Label empty = new Label(
                                    teacher
                                            ? "Bạn chưa tạo lớp nào. Chọn “Tạo lớp thực hành” để bắt đầu."
                                            : "Chưa có lớp. Giáo viên cần thêm bạn hoặc cung cấp mã tham gia.");
                            empty.getStyleClass().add("workspace-empty");
                            classList.getChildren().add(empty);
                            return;
                        }
                        for (JsonNode klass : classes) {
                            long id = klass.path("id").asLong();
                            String name = klass.path("name").asText();
                            String joinCode = klass.path("joinCode").asText("");
                            Label title = new Label(name);
                            title.getStyleClass().add("workspace-room-title");
                            Label meta = new Label(
                                    teacher
                                            ? "Mã lớp: " + (joinCode.isEmpty() ? "—" : joinCode)
                                            : "Đã được ghi danh · Chọn để vào lớp");
                            meta.getStyleClass().add("muted");
                            VBox texts = new VBox(5, title, meta);
                            HBox.setHgrow(texts, Priority.ALWAYS);
                            Label arrow = new Label("›");
                            arrow.getStyleClass().add("workspace-card-arrow");
                            Button card = new Button();
                            card.setGraphic(row(texts, arrow));
                            card.setMaxWidth(Double.MAX_VALUE);
                            card.setMinHeight(80);
                            card.setPrefHeight(80);
                            card.setMaxHeight(80);
                            card.getStyleClass().add("workspace-directory-card");
                            card.setOnAction(e -> switchClass(id, name, joinCode));
                            classList.getChildren().add(card);
                        }
                    } catch (Exception ex) {
                        status.setText("Danh sách lớp không hợp lệ: " + ex.getMessage());
                    }
                }));
    }

    private void switchClass(long id, String name, String code) {
        var pending = client.expect(MessageType.CLASS_SELECT_RESULT, MessageType.ERROR);
        client.send(MessageType.CLASS_SELECT_REQ, new PayloadWriter().i64(id).toBytes());
        pending.orTimeout(12, TimeUnit.SECONDS)
                .whenComplete((frame, error) -> Platform.runLater(() -> {
                    if (disposed) return;
                    if (error != null || frame.type() == MessageType.ERROR) {
                        status.setText("Không thể chọn lớp. Kiểm tra quyền truy cập.");
                        return;
                    }
                    onClassChanged.accept(id);
                    showDetail(id, name, code);
                }));
    }

    /** Gọi từ HomeScreen sau khi nhập CSV thành công: tải lại danh sách sinh viên ngay trong lớp. */
    public void reload() {
        if (disposed) return;
        if (view == View.DIRECTORY) {
            loadClasses();
            return;
        }
        if (view != View.DETAIL) return;
        if (teacher) loadStudents();
        var pending = client.expect(MessageType.PRACTICE_LIST_DATA, MessageType.ERROR);
        client.send(MessageType.PRACTICE_LIST_REQ, new byte[0]);
        long requestedClass = currentClassId;
        pending.orTimeout(12, TimeUnit.SECONDS)
                .whenComplete((frame, error) -> Platform.runLater(() -> {
                    if (disposed || view != View.DETAIL || currentClassId != requestedClass) return;
                    roomList.getChildren().clear();
                    if (error != null || frame.type() == MessageType.ERROR) {
                        roomList.getChildren().add(new Label("Không tải được các buổi thực hành."));
                        return;
                    }
                    try {
                        JsonNode rooms = JsonPayload.tree(frame).path("rooms");
                        if (!rooms.isArray() || rooms.isEmpty()) {
                            Label empty = new Label(
                                    teacher
                                            ? "Chưa có buổi nào. Tạo buổi đầu tiên ở phía trên."
                                            : "Giáo viên chưa tạo buổi thực hành cho lớp này.");
                            empty.getStyleClass().add("workspace-empty");
                            roomList.getChildren().add(empty);
                            return;
                        }
                        for (JsonNode room : rooms) addRoomCard(room);
                    } catch (Exception ex) {
                        status.setText("Lỗi danh sách buổi: " + ex.getMessage());
                    }
                }));
    }

    private void loadStudents() {
        if (!teacher || disposed || view != View.DETAIL) return;
        var pending = client.expect(MessageType.STUDENT_OVERVIEW_DATA, MessageType.ERROR);
        client.send(MessageType.STUDENT_OVERVIEW_REQ, new byte[0]);
        long requestedClass = currentClassId;
        pending.orTimeout(12, TimeUnit.SECONDS)
                .whenComplete((frame, error) -> Platform.runLater(() -> {
                    if (disposed || view != View.DETAIL || requestedClass != currentClassId) return;
                    studentList.getChildren().clear();
                    if (error != null || frame.type() == MessageType.ERROR) {
                        Label warning = new Label("Không tải được danh sách sinh viên. Hãy cập nhật Server.");
                        warning.getStyleClass().add("muted");
                        studentList.getChildren().add(warning);
                        return;
                    }
                    try {
                        JsonNode students = JsonPayload.tree(frame).path("students");
                        int total = students.isArray() ? students.size() : 0;
                        rosterCount.setText(total + " sinh viên");
                        if (total == 0) {
                            Label empty = new Label("Lớp chưa có sinh viên. Bấm “Nhập CSV sinh viên” để thêm.");
                            empty.getStyleClass().add("workspace-empty");
                            studentList.getChildren().add(empty);
                            return;
                        }
                        int i = 0;
                        for (JsonNode student : students) {
                            Label ordinal = new Label(Integer.toString(++i));
                            ordinal.getStyleClass().add("workspace-roster-number");
                            Label name = new Label(student.path("fullName").asText());
                            name.getStyleClass().add("workspace-roster-name");
                            Label username = new Label(student.path("username").asText());
                            username.getStyleClass().add("workspace-roster-user");
                            VBox person = new VBox(2, name, username);
                            HBox.setHgrow(person, Priority.ALWAYS);
                            Label connection = new Label(student.path("online").asBoolean() ? "● Online" : "○ Offline");
                            connection
                                    .getStyleClass()
                                    .add(student.path("online").asBoolean() ? "workspace-online" : "workspace-offline");
                            HBox studentRow = row(ordinal, person, connection);
                            studentRow.getStyleClass().add("workspace-roster-row");
                            studentList.getChildren().add(studentRow);
                        }
                    } catch (Exception ex) {
                        status.setText("Lỗi danh sách sinh viên: " + ex.getMessage());
                    }
                }));
    }

    private record LessonOption(JsonNode data) {
        String id() {
            return data.path("id").asText();
        }

        @Override
        public String toString() {
            return data.path("ten").asText() + "  ·  " + id();
        }
    }

    private void addRoomCard(JsonNode room) {
        long roomId = room.path("id").asLong();
        Label name = new Label(room.path("title").asText());
        name.getStyleClass().add("workspace-room-title");
        Label info = new Label(
                teacher
                        ? "Mã buổi: " + room.path("joinCode").asText()
                        : (room.path("joined").asBoolean() ? "Đã tham gia" : "Nhấn để tham gia buổi"));
        info.getStyleClass().add("muted");
        VBox titleBox = new VBox(4, name, info);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        HBox roomHeader = row(titleBox);
        VBox assignments = new VBox(7);
        assignments.getStyleClass().add("workspace-assignment-list");
        VBox container = panel(roomHeader);
        if (teacher) {
            ComboBox<LessonOption> choice = new ComboBox<>();
            choice.setPromptText("Chọn bài tập có sẵn trong kho...");
            choice.setMaxWidth(Double.MAX_VALUE);
            availableLessons.stream().map(LessonOption::new).forEach(choice.getItems()::add);
            HBox form = row(choice, primary("+ Gán bài", () -> {
                LessonOption l = choice.getValue();
                if (l == null) {
                    status.setText("Chọn bài có sẵn trước khi gán.");
                    return;
                }
                changeAssignment(roomId, l.id(), true, assignments);
            }));
            HBox.setHgrow(choice, Priority.ALWAYS);
            container.getChildren().add(form);
            loadAssignments(roomId, assignments);
        } else {
            roomHeader
                    .getChildren()
                    .add(primary(
                            "Xem bài được giao →",
                            () -> joinAndLoadRoom(roomId, room.path("title").asText(), assignments)));
            if (room.path("joined").asBoolean()) loadAssignments(roomId, assignments);
        }
        container.getChildren().add(assignments);
        roomList.getChildren().add(container);
    }

    private void changeAssignment(long roomId, String lesson, boolean add, VBox target) {
        var pending = client.expect(MessageType.PRACTICE_ACTION_RESULT, MessageType.ERROR);
        client.send(
                add ? MessageType.PRACTICE_ASSIGN_REQ : MessageType.PRACTICE_UNASSIGN_REQ,
                new PayloadWriter().i64(roomId).str(lesson).toBytes());
        pending.orTimeout(12, TimeUnit.SECONDS)
                .whenComplete((frame, error) -> Platform.runLater(() -> {
                    if (disposed) return;
                    if (error != null || frame.type() == MessageType.ERROR) {
                        status.setText("Không cập nhật được bài tập được giao.");
                        return;
                    }
                    status.setText(add ? "Đã gán bài vào buổi thực hành." : "Đã bỏ gán bài.");
                    loadAssignments(roomId, target);
                }));
    }

    private void joinAndLoadRoom(long id, String roomTitle, VBox target) {
        var pending = client.expect(MessageType.PRACTICE_ACTION_RESULT, MessageType.ERROR);
        client.send(MessageType.PRACTICE_JOIN_REQ, new PayloadWriter().i64(id).toBytes());
        pending.orTimeout(12, TimeUnit.SECONDS)
                .whenComplete((frame, error) -> Platform.runLater(() -> {
                    if (disposed) return;
                    if (error != null || frame.type() == MessageType.ERROR) {
                        status.setText("Không thể vào buổi thực hành.");
                        return;
                    }
                    onJoin.accept(roomTitle);
                    loadAssignments(id, target);
                }));
    }

    private void loadAssignments(long id, VBox target) {
        var pending = client.expect(MessageType.PRACTICE_LESSONS_DATA, MessageType.ERROR);
        client.send(
                MessageType.PRACTICE_LESSONS_REQ, new PayloadWriter().i64(id).toBytes());
        pending.orTimeout(12, TimeUnit.SECONDS)
                .whenComplete((frame, error) -> Platform.runLater(() -> {
                    if (disposed) return;
                    target.getChildren().clear();
                    if (error != null || frame.type() == MessageType.ERROR) {
                        target.getChildren().add(new Label("Không tải được bài được giao."));
                        return;
                    }
                    try {
                        JsonNode assignments = JsonPayload.tree(frame).path("lessons");
                        if (!assignments.isArray() || assignments.isEmpty()) {
                            target.getChildren().add(new Label("Buổi này chưa có bài được giao."));
                            return;
                        }
                        for (JsonNode item : assignments) {
                            String lessonId = item.path("id").asText();
                            String name = availableLessons.stream()
                                    .filter(l -> lessonId.equals(l.path("id").asText()))
                                    .map(l -> l.path("ten").asText())
                                    .findFirst()
                                    .orElse(lessonId);
                            Label text = new Label(name);
                            text.getStyleClass().add("workspace-assignment-title");
                            text.setMaxWidth(Double.MAX_VALUE);
                            text.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
                            HBox.setHgrow(text, Priority.ALWAYS);
                            Button action;
                            if (teacher) {
                                action = new Button("×");
                                action.getStyleClass().add("workspace-remove-assignment");
                                action.setTooltip(new javafx.scene.control.Tooltip("Bỏ gán bài: " + name));
                                action.setMinSize(36, 36);
                                action.setPrefSize(36, 36);
                                action.setMaxSize(36, 36);
                                action.setOnAction(e -> changeAssignment(id, lessonId, false, target));
                            } else {
                                action = secondary("Làm bài  →", () -> onOpenLesson.accept(lessonId));
                            }
                            HBox line = row(text, action);
                            line.setMinHeight(52);
                            line.getStyleClass().add("workspace-assignment-row");
                            target.getChildren().add(line);
                        }
                    } catch (Exception ex) {
                        status.setText("Dữ liệu bài được giao không hợp lệ.");
                    }
                }));
    }

    private void createRoom() {
        String title = roomName.getText().strip();
        if (title.isBlank()) {
            status.setText("Hãy nhập tên buổi thực hành.");
            return;
        }
        var pending = client.expect(MessageType.PRACTICE_ACTION_RESULT, MessageType.ERROR);
        client.send(
                MessageType.PRACTICE_CREATE_REQ, new PayloadWriter().str(title).toBytes());
        pending.orTimeout(12, TimeUnit.SECONDS)
                .whenComplete((frame, error) -> Platform.runLater(() -> {
                    if (disposed) return;
                    if (error != null || frame.type() == MessageType.ERROR) {
                        status.setText("Không tạo được buổi thực hành. Kiểm tra lớp đang chọn.");
                        return;
                    }
                    roomName.clear();
                    status.setText("Đã tạo buổi. Bạn có thể gán bài ngay bên dưới.");
                    reload();
                }));
    }
}
