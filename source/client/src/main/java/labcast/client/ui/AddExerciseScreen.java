package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

/**
 * Tạo gói nội dung bài tập theo đúng định dạng mà LessonRepository đọc từ thư mục content.
 * Hiện server chưa có giao thức ADMIN_LESSON_CREATE, do đó không giả vờ cập nhật server qua mạng:
 * giáo viên phải lưu trong thư mục content của server và khởi động lại server để nạp bài.
 */
public final class AddExerciseScreen extends BorderPane {
    private final TextField root = new TextField();
    /** Chủ đề có sẵn (tên hiện → mã chương) theo môn, lấy từ danh sách bài của server. */
    private final ComboBox<String> chapterBox = new ComboBox<>();

    private final Map<String, String> chapterCodes = new LinkedHashMap<>();
    private List<JsonNode> knownLessons = List.of();
    private final TextField newChapterName = new TextField();
    private final TextField newChapterCode = new TextField();
    private final VBox newChapterBox = new VBox(10);
    private final VBox viewRows = new VBox(8);
    private final VBox viewCard;
    private final TextField slug = new TextField();
    private final TextField title = new TextField();
    private final ComboBox<String> subject = new ComboBox<>();
    private final ComboBox<String> type = new ComboBox<>();
    private final ComboBox<String> difficulty = new ComboBox<>();
    private final TextArea description = new TextArea();
    private final TextArea starter = new TextArea();
    private final TextArea solution = new TextArea();
    private final TextArea testInput = new TextArea();
    private final TextArea testOutput = new TextArea();
    // Bài SQL: CSDL dùng chung trong content/sql/_csdl hoặc tự nhập bảng
    private final ComboBox<String> database = new ComboBox<>();
    private final ComboBox<String> queryKind = new ComboBox<>();
    private final javafx.scene.control.CheckBox ordered =
            new javafx.scene.control.CheckBox("So kết quả theo đúng thứ tự dòng (đề yêu cầu ORDER BY)");
    private final TextArea schema = new TextArea();
    private final TextArea sampleData = new TextArea();
    private VBox sqlCard;
    private VBox testCard;
    private Label starterCaption;
    private Label solutionCaption;
    /** Ô soạn dùng chung cho mọi lần phóng to (tô màu, thụt dòng, tự đóng ngoặc). */
    private final labcast.client.editor.CodeEditor zoomEditor = new labcast.client.editor.CodeEditor();

    private FocusLayer zoom;
    private final Label feedback = new Label();

    public AddExerciseScreen() {
        getStyleClass().add("exercise-author-screen");
        Label heading = new Label("Thêm bài tập");
        heading.getStyleClass().addAll("h1", "lesson-main-title");
        Label desc = new Label("Tạo bài mới cho thư mục content của LabCast Server. Chỉ dành cho giáo viên.");
        desc.getStyleClass().add("muted");
        desc.setWrapText(true);
        VBox headingBox = new VBox(4, heading, desc);
        headingBox.getStyleClass().add("exercise-author-heading");
        setTop(headingBox);

        subject.getItems().addAll("DSA", "SQL");
        subject.getSelectionModel().selectFirst();
        subject.setMaxWidth(Double.MAX_VALUE);
        type.getItems().addAll(TYPE_SAMPLE, TYPE_EXERCISE, TYPE_PRACTICE);
        difficulty.getItems().addAll("Chưa phân loại", "Dễ", "Trung bình", "Khó");
        difficulty.getSelectionModel().selectFirst();
        type.setValue(TYPE_EXERCISE);
        type.setMaxWidth(Double.MAX_VALUE);
        subject.valueProperty().addListener((obs, old, value) -> {
            updateSubject();
            fillChapters();
        });
        chapterBox.setMaxWidth(Double.MAX_VALUE);
        chapterBox.setOnAction(e -> {
            boolean adding = NEW_CHAPTER.equals(chapterBox.getValue());
            newChapterBox.setVisible(adding);
            newChapterBox.setManaged(adding);
        });
        newChapterName.setPromptText("VD: Hàng đợi ưu tiên");
        newChapterCode.setPromptText("tự sinh từ tên, VD: hang-doi-uu-tien");
        newChapterName.textProperty().addListener((o, a, b) -> newChapterCode.setText(ExerciseCatalog.slug(b)));
        title.textProperty().addListener((o, a, b) -> {
            if (slug.getText().isBlank() || slug.getText().equals(ExerciseCatalog.slug(a)))
                slug.setText(ExerciseCatalog.slug(b));
        });
        root.setPromptText("Chọn thư mục content trên máy giáo viên/server...");
        root.setText(defaultContentPath());
        Button choose = new Button("Chọn thư mục...");
        choose.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Chọn thư mục content của LabCast Server");
            try {
                Path p = Path.of(root.getText());
                if (Files.isDirectory(p)) chooser.setInitialDirectory(p.toFile());
            } catch (RuntimeException ignored) {
                // Đường dẫn chưa hợp lệ thì để JavaFX chọn thư mục mặc định.
            }
            var file = chooser.showDialog(getScene() == null ? null : getScene().getWindow());
            if (file != null) root.setText(file.getAbsolutePath());
        });
        HBox folder = new HBox(8, root, choose);
        HBox.setHgrow(root, Priority.ALWAYS);
        slug.setPromptText("VD: tim-kiem-nhi-phan");
        title.setPromptText("Tên bài hiển thị cho học viên");
        description.setPromptText("Nội dung đề bài (Markdown)");
        description.setPrefRowCount(6);
        starter.setPromptText("Mã khung để học viên bắt đầu...");
        starter.setPrefRowCount(5);
        solution.setPromptText("Không bắt buộc; dùng khi muốn bổ sung lời giải mẫu / đáp án SQL");
        solution.setPrefRowCount(5);
        testInput.setPrefRowCount(2);
        testOutput.setPrefRowCount(2);
        testInput.setPromptText("Input kiểm thử mẫu");
        testOutput.setPromptText("Output tương ứng (để trống cả hai nếu chưa có test)");

        // Bố cục thành từng nhóm rõ ràng, các ô trên cùng hàng rộng bằng nhau.
        // Giữ ScrollPane để biểu mẫu hoạt động tốt ở cửa sổ nhỏ.
        VBox form = new VBox(16);
        form.getStyleClass().add("exercise-author-form");

        VBox locationCard = card(
                "01   Vị trí lưu bài",
                "Các file sẽ được xuất vào thư mục content trên máy giáo viên hoặc Server.",
                field("Thư mục content của Server", folder));

        GridPane meta = twoColumnGrid();
        addGrid(meta, 0, 0, field("Môn học", subject));
        addGrid(meta, 1, 0, field("Loại bài", type));
        addGrid(meta, 0, 1, field("Chủ đề (chương)", chapterBox));
        addGrid(meta, 1, 1, field("Mã bài", slug));
        addGrid(meta, 0, 2, field("Độ khó", difficulty));
        GridPane newChapter = twoColumnGrid();
        addGrid(newChapter, 0, 0, field("Tên chủ đề mới", newChapterName));
        addGrid(newChapter, 1, 0, field("Mã chủ đề", newChapterCode));
        Label newHint = new Label("Chủ đề mới được tạo cùng bài này; các bài sau chọn lại được trong danh sách.");
        newHint.getStyleClass().add("author-hint");
        newChapterBox.getChildren().addAll(newChapter, newHint);
        newChapterBox.setVisible(false);
        newChapterBox.setManaged(false);
        VBox metadataCard = card(
                "02   Thông tin bài tập",
                "Chọn môn, loại bài và chủ đề. Bài mẫu có animation, xếp đầu chương; bài tập, luyện tập xếp sau.",
                meta,
                newChapterBox,
                field("Tên bài tập", title));

        Button addView = new Button("+ Thêm biến");
        addView.setOnAction(e -> addViewRow("", "mang"));
        Label viewHint = new Label("Mỗi biến trong lời giải được vẽ bằng một bộ vẽ, VD biến a vẽ dạng Mảng. "
                + "Bài mẫu cần ít nhất một biến để sinh animation từ lời giải tham khảo.");
        viewHint.setWrapText(true);
        viewHint.getStyleClass().add("author-hint");
        viewCard = card("03   Animation (khung nhìn)", "Chọn bộ vẽ cho từng biến.", viewRows, addView, viewHint);
        addViewRow("a", "mang");

        VBox statementCard =
                card("03   Nội dung đề bài", "Hỗ trợ văn bản Markdown (de.md).", field("Đề bài", description));

        HBox codeEditors = new HBox(14);
        VBox starterBox = field("Code khởi tạo", starter);
        VBox solutionBox = field("Lời giải tham khảo", solution);
        starterCaption = (Label) starterBox.getChildren().get(0);
        solutionCaption = (Label) solutionBox.getChildren().get(0);
        HBox.setHgrow(starterBox, Priority.ALWAYS);
        HBox.setHgrow(solutionBox, Priority.ALWAYS);
        starterBox.setMaxWidth(Double.MAX_VALUE);
        solutionBox.setMaxWidth(Double.MAX_VALUE);
        starterBox
                .prefWidthProperty()
                .bind(codeEditors.widthProperty().subtract(14).divide(2));
        solutionBox
                .prefWidthProperty()
                .bind(codeEditors.widthProperty().subtract(14).divide(2));
        codeEditors.getChildren().addAll(starterBox, solutionBox);
        VBox editorCard = card(
                "04   Code mẫu và đáp án", "Điền code khởi tạo; lời giải có thể để trống với bài DSA.", codeEditors);

        GridPane tests = twoColumnGrid();
        addGrid(tests, 0, 0, field("Input mẫu", testInput));
        addGrid(tests, 1, 0, field("Output mẫu", testOutput));
        testCard = card("05   Dữ liệu kiểm thử", "Input và Output cần khớp với nhau.", tests);

        ordered.getStyleClass().add("catalog-kind");
        queryKind.getItems().addAll(QUERY_SELECT, QUERY_DML);
        queryKind.setValue(QUERY_SELECT);
        database.setOnAction(e -> loadDatabase());
        root.textProperty().addListener((o, a, b) -> fillDatabases());
        schema.setPromptText("CREATE TABLE ... (schema.sql)");
        sampleData.setPromptText("INSERT INTO ... (du_lieu_mau.sql)");
        schema.setPrefRowCount(8);
        sampleData.setPrefRowCount(8);
        GridPane sqlMeta = twoColumnGrid();
        addGrid(sqlMeta, 0, 0, field("Cơ sở dữ liệu", database));
        addGrid(sqlMeta, 1, 0, field("Loại câu", queryKind));
        GridPane sqlTables = twoColumnGrid();
        addGrid(sqlTables, 0, 0, field("Bảng (schema.sql)", schema));
        addGrid(sqlTables, 1, 0, field("Dữ liệu mẫu (du_lieu_mau.sql)", sampleData));
        Label sqlHint =
                new Label("Chọn CSDL dùng chung thì app chép schema.sql, du_lieu_mau.sql và sinh_du_lieu.py vào "
                        + "thư mục bài (server sinh thêm CSDL ẩn để chấm). Chọn \"Tự nhập bảng\" để viết bảng riêng.");
        sqlHint.setWrapText(true);
        sqlHint.getStyleClass().add("author-hint");
        sqlCard = card(
                "03   Cơ sở dữ liệu và kiểu câu",
                "Bài SQL chạy trên bảng; học viên thấy bảng kết quả.",
                sqlMeta,
                ordered,
                sqlTables,
                sqlHint);

        Label tip = new Label("Bài mới được lưu thành file trong thư mục content. "
                + "Bạn cần đặt các file trên Server và khởi động lại Server để học viên nhìn thấy bài.");
        tip.setWrapText(true);
        tip.getStyleClass().add("exercise-author-note");
        Button save = new Button("+ Tạo bộ file bài tập");
        save.getStyleClass().addAll("primary", "exercise-author-save");
        save.setOnAction(e -> saveLesson());
        HBox actions = new HBox(16, tip, save);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(tip, Priority.ALWAYS);
        tip.setMaxWidth(Double.MAX_VALUE);
        feedback.setWrapText(true);
        feedback.getStyleClass().add("exercise-author-feedback");
        VBox saveCard = card("Hoàn tất", "Kiểm tra thông tin rồi xuất bài tập.", actions, feedback);

        form.getChildren()
                .addAll(locationCard, metadataCard, viewCard, sqlCard, statementCard, editorCard, testCard, saveCard);
        // Cùng chiều cao cho các ô nhập một dòng và cùng chiều cao cho textarea ngang hàng.
        for (TextField input : new TextField[] {root, slug, title, newChapterName, newChapterCode}) {
            input.getStyleClass().add("exercise-author-input");
            input.setMinHeight(44);
            input.setPrefHeight(44);
            input.setMaxHeight(44);
        }
        for (TextArea input :
                new TextArea[] {description, starter, solution, testInput, testOutput, schema, sampleData})
            input.getStyleClass().add("exercise-author-input");
        description.setPrefRowCount(6);
        starter.setPrefRowCount(8);
        solution.setPrefRowCount(8);
        testInput.setPrefRowCount(4);
        testOutput.setPrefRowCount(4);
        subject.setMinHeight(44);
        subject.setPrefHeight(44);
        subject.setMaxHeight(44);
        difficulty.setMaxWidth(Double.MAX_VALUE);
        difficulty.setMinHeight(44);
        difficulty.setPrefHeight(44);
        difficulty.setMaxHeight(44);
        type.setMinHeight(44);
        type.setPrefHeight(44);
        type.setMaxHeight(44);
        chapterBox.setMinHeight(44);
        chapterBox.setPrefHeight(44);
        chapterBox.setMaxHeight(44);
        for (ComboBox<String> box : List.of(database, queryKind)) {
            box.setMinHeight(44);
            box.setPrefHeight(44);
            box.setMaxHeight(44);
        }
        choose.setMinHeight(44);
        choose.setPrefHeight(44);
        choose.setMaxHeight(44);
        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("exercise-author-scroll");
        setCenter(scroll);
        zoomable(schema, "Bảng (schema.sql)", () -> "sql", false);
        zoomable(sampleData, "Dữ liệu mẫu (du_lieu_mau.sql)", () -> "sql", false);
        zoomable(starter, "Code khởi tạo", this::codeLanguage, true);
        zoomable(solution, "Lời giải / đáp án", this::codeLanguage, true);
        zoomable(testInput, "Input mẫu", () -> "plain", false);
        zoomable(testOutput, "Output mẫu", () -> "plain", false);
        zoomable(description, "Đề bài (Markdown)", () -> "plain", false);
        updateSubject();
        fillChapters();
    }

    private String codeLanguage() {
        return "SQL".equals(subject.getValue()) ? "sql" : "python";
    }

    /**
     * Bấm vào ô thì ô nổi lên giữa màn chiếm 70%, nền mờ; soạn bằng ô code có tô màu và thụt dòng. Esc, bấm ra
     * ngoài hoặc "Xong" thì chữ ghi lại vào ô cũ. {@code formattable}: có nút định dạng câu SQL.
     */
    private void zoomable(
            TextArea area, String caption, java.util.function.Supplier<String> lang, boolean formattable) {
        area.setEditable(false);
        area.setCursor(javafx.scene.Cursor.HAND);
        area.setTooltip(new javafx.scene.control.Tooltip("Bấm để mở rộng ô soạn"));
        area.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            e.consume();
            if (zoom != null && zoom.isOpen()) return;
            boolean readOnly = area.getProperties().containsKey("readonly");
            zoomEditor.setEditable(!readOnly);
            String language = lang.get();
            zoomEditor.setSyntaxLanguage(language);
            zoomEditor.setText(area.getText());
            Label title = new Label(caption);
            title.getStyleClass().add("focus-title");
            Label hint = new Label(
                    readOnly
                            ? "CSDL dùng chung: chỉ xem. Muốn sửa thì chọn \"Tự nhập bảng\" · Esc để đóng"
                            : "Esc hoặc bấm ra ngoài để đóng · Tab = 4 dấu cách · tự đóng ngoặc, tự thụt dòng");
            hint.getStyleClass().add("author-hint");
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            Button done = new Button("Xong");
            done.getStyleClass().add("dash-primary");
            HBox head = new HBox(10, new VBox(2, title, hint), gap);
            head.setAlignment(Pos.CENTER_LEFT);
            if (formattable && "sql".equals(language)) {
                Button format = new Button("✨ Định dạng SQL");
                format.getStyleClass().add("dash-ghost");
                format.setOnAction(
                        ev -> zoomEditor.setText(labcast.client.editor.CodeAssist.formatSql(zoomEditor.text())));
                head.getChildren().add(format);
            }
            head.getChildren().add(done);
            VBox box = new VBox(12, head, zoomEditor.node());
            VBox.setVgrow(zoomEditor.node(), Priority.ALWAYS);
            box.getStyleClass().add("focus-panel");
            zoom = FocusLayer.open(this, box, 0.7, () -> {
                if (!readOnly) area.setText(zoomEditor.text());
            });
            done.setOnAction(ev -> zoom.close());
            javafx.application.Platform.runLater(zoomEditor::focus);
        });
    }

    static final String QUERY_SELECT = "SELECT: truy vấn, so bảng kết quả";
    static final String QUERY_DML = "DML: INSERT / UPDATE / DELETE, so bảng sau khi chạy";
    private static final String OWN_TABLES = "Tự nhập bảng";

    /** CSDL dùng chung: thư mục con của content/sql/_csdl. */
    private Path databaseRoot() {
        try {
            return Path.of(root.getText().trim()).resolve("sql").resolve("_csdl");
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void fillDatabases() {
        String keep = database.getValue();
        database.getItems().setAll(OWN_TABLES);
        Path dir = databaseRoot();
        if (dir != null && Files.isDirectory(dir)) {
            try (var list = Files.list(dir)) {
                list.filter(d -> Files.isRegularFile(d.resolve("schema.sql")))
                        .map(d -> d.getFileName().toString())
                        .sorted()
                        .forEach(n -> database.getItems().add(n));
            } catch (IOException ignored) {
                // không đọc được thì chỉ còn tự nhập bảng
            }
        }
        database.setValue(
                keep != null && database.getItems().contains(keep)
                        ? keep
                        : database.getItems().get(database.getItems().size() > 1 ? 1 : 0));
        loadDatabase();
    }

    /** Chọn CSDL dùng chung: hiện bảng và dữ liệu để xem, không cho sửa (sửa thì chọn tự nhập). */
    private void loadDatabase() {
        boolean own = OWN_TABLES.equals(database.getValue()) || database.getValue() == null;
        // ô luôn mở bằng lớp phóng to; CSDL dùng chung thì phóng to chỉ để xem (đánh dấu readonly)
        for (TextArea t : List.of(schema, sampleData)) {
            if (own) t.getProperties().remove("readonly");
            else t.getProperties().put("readonly", true);
        }
        if (own) return;
        Path dir = databaseRoot().resolve(database.getValue());
        try {
            schema.setText(Files.readString(dir.resolve("schema.sql")));
            sampleData.setText(Files.readString(dir.resolve("du_lieu_mau.sql")));
        } catch (IOException e) {
            error("Không đọc được CSDL " + database.getValue() + ": " + e.getMessage());
        }
    }

    static final String TYPE_SAMPLE = "Bài mẫu (có animation)";
    static final String TYPE_EXERCISE = "Bài tập";
    static final String TYPE_PRACTICE = "Luyện tập";
    private static final String NEW_CHAPTER = "+ Thêm chủ đề mới…";
    /** Bộ vẽ hỗ trợ: mã trong lesson.yaml → tên hiện. */
    private static final Map<String, String> VIEWS = new LinkedHashMap<>();

    static {
        VIEWS.put("mang", "Mảng");
        VIEWS.put("ngan-xep", "Ngăn xếp");
        VIEWS.put("hang-doi", "Hàng đợi");
        VIEWS.put("dslk", "Danh sách liên kết");
        VIEWS.put("cay", "Cây");
        VIEWS.put("heap", "Heap");
        VIEWS.put("cay-goi", "Cây gọi đệ quy");
        VIEWS.put("do-thi", "Đồ thị");
        VIEWS.put("luoi", "Lưới 2 chiều");
        VIEWS.put("bam", "Bảng băm");
        VIEWS.put("code-bien", "Code và biến (chỉ bài mẫu)");
    }

    /** Danh sách bài của lớp để gợi ý chủ đề có sẵn. */
    public void setLessons(List<JsonNode> lessons) {
        knownLessons = List.copyOf(lessons);
        // lớp chỉ một môn thì chọn sẵn môn đó
        var subjects = ExerciseCatalog.subjects(knownLessons);
        if (subjects.size() == 1) subject.setValue(subjects.get(0));
        fillChapters();
    }

    private void fillChapters() {
        String mon = "SQL".equals(subject.getValue()) ? "sql" : "dsa";
        String keep = chapterBox.getValue();
        chapterCodes.clear();
        List<JsonNode> sorted = new ArrayList<>(knownLessons);
        sorted.sort(ExerciseCatalog.ORDER);
        for (JsonNode l : sorted) {
            if (!mon.equals(l.path("mon").asText())) continue;
            chapterCodes.putIfAbsent(
                    ExerciseCatalog.chapterName(l), l.path("chuong").asText());
        }
        // chưa tải được bài thì gợi ý theo bảng chương chuẩn của môn
        if (chapterCodes.isEmpty()) {
            List<String> std = LessonScreen.CHAPTERS;
            int split = std.indexOf("co-ban");
            for (String c : mon.equals("sql") ? std.subList(split, std.size()) : std.subList(0, split))
                chapterCodes.put(LessonScreen.chapterLabel(c), c);
        }
        chapterBox.getItems().setAll(chapterCodes.keySet());
        chapterBox.getItems().add(NEW_CHAPTER);
        if (keep != null && chapterBox.getItems().contains(keep)) chapterBox.setValue(keep);
        else chapterBox.getSelectionModel().selectFirst();
    }

    private void addViewRow(String bien, String kieu) {
        TextField name = new TextField(bien);
        name.setPromptText("tên biến, VD: a");
        name.setPrefWidth(160);
        ComboBox<String> kind = new ComboBox<>();
        kind.getItems().addAll(VIEWS.values());
        kind.setValue(VIEWS.getOrDefault(kieu, "Mảng"));
        Button remove = new Button("✕");
        HBox row = new HBox(8, name, kind, remove);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("author-view-row");
        row.setUserData(new Object[] {name, kind});
        remove.setOnAction(e -> viewRows.getChildren().remove(row));
        viewRows.getChildren().add(row);
    }

    /** Các dòng khung nhìn đã nhập: biến → mã bộ vẽ. */
    @SuppressWarnings("unchecked")
    private Map<String, String> views() {
        Map<String, String> out = new LinkedHashMap<>();
        for (Node n : viewRows.getChildren()) {
            Object[] parts = (Object[]) n.getUserData();
            String bien = ((TextField) parts[0]).getText().trim();
            String shown = ((ComboBox<String>) parts[1]).getValue();
            if (bien.isEmpty() || shown == null) continue;
            VIEWS.forEach((code, label) -> {
                if (label.equals(shown)) out.put(bien, code);
            });
        }
        return out;
    }

    private static GridPane twoColumnGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        ColumnConstraints left = new ColumnConstraints();
        ColumnConstraints right = new ColumnConstraints();
        left.setPercentWidth(50);
        right.setPercentWidth(50);
        left.setHgrow(Priority.ALWAYS);
        right.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(left, right);
        return grid;
    }

    private static void addGrid(GridPane grid, int column, int row, VBox field) {
        GridPane.setHgrow(field, Priority.ALWAYS);
        field.setMaxWidth(Double.MAX_VALUE);
        grid.add(field, column, row);
    }

    private static VBox field(String title, Node input) {
        Label caption = label(title);
        caption.getStyleClass().add("exercise-author-field-label");
        if (input instanceof javafx.scene.control.Control control) control.setMaxWidth(Double.MAX_VALUE);
        VBox wrapper = new VBox(7, caption, input);
        wrapper.getStyleClass().add("exercise-author-field");
        wrapper.setFillWidth(true);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        return wrapper;
    }

    private static VBox card(String title, String description, Node... fields) {
        Label name = new Label(title);
        name.getStyleClass().add("exercise-author-card-title");
        Label explanation = new Label(description);
        explanation.getStyleClass().add("exercise-author-card-description");
        explanation.setWrapText(true);
        VBox group = new VBox(12, name, explanation);
        group.getChildren().addAll(fields);
        group.getStyleClass().add("exercise-author-card");
        group.setMaxWidth(Double.MAX_VALUE);
        return group;
    }

    private static Label label(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("card-title");
        return label;
    }

    /** Thư mục content gần nhất tính từ thư mục chạy app, đi ngược lên các thư mục cha. */
    private static String defaultContentPath() {
        Path here =
                Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        for (Path p = here; p != null; p = p.getParent()) {
            Path candidate = p.resolve("content");
            if (Files.isDirectory(candidate.resolve("dsa")) || Files.isDirectory(candidate.resolve("sql")))
                return candidate.toString();
        }
        return here.resolve("content").toString();
    }

    private void updateSubject() {
        boolean sql = "SQL".equals(subject.getValue());
        if (viewCard != null) {
            // SQL luôn vẽ bằng bảng, không cần chọn bộ vẽ
            viewCard.setVisible(!sql);
            viewCard.setManaged(!sql);
        }
        if (sqlCard != null) {
            sqlCard.setVisible(sql);
            sqlCard.setManaged(sql);
            testCard.setVisible(!sql);
            testCard.setManaged(!sql);
            starterCaption.setText(sql ? "Câu SQL khởi đầu (khung.sql)" : "Code khởi tạo");
            solutionCaption.setText(sql ? "Câu SQL đáp án (dap_an.sql)" : "Lời giải tham khảo");
            if (sql) fillDatabases();
        }
        starter.setPromptText(sql ? "-- học viên viết câu SQL ở đây" : "Mã khung để học viên bắt đầu...");
        solution.setPromptText(
                sql ? "SELECT ... ; câu đúng để chấm" : "Không bắt buộc với bài tập; bắt buộc với bài mẫu");
    }

    private void saveLesson() {
        feedback.setText("");
        String mon = "SQL".equals(subject.getValue()) ? "sql" : "dsa";
        String loai =
                switch (type.getValue()) {
                    case TYPE_SAMPLE -> "giang-giai";
                    case TYPE_PRACTICE -> "luyen-tap";
                    default -> "bai-tap";
                };
        boolean newChapter = NEW_CHAPTER.equals(chapterBox.getValue());
        String chuong = (newChapter ? newChapterCode.getText() : chapterCodes.getOrDefault(chapterBox.getValue(), ""))
                .trim()
                .toLowerCase(Locale.ROOT);
        if (newChapter && newChapterName.getText().isBlank()) {
            error("Nhập tên chủ đề mới.");
            return;
        }
        String code = slug.getText().trim().toLowerCase(Locale.ROOT);
        String ten = title.getText().trim();
        if (!chuong.matches("[a-z0-9]+(?:-[a-z0-9]+)*") || !code.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            error("Mã chương và mã bài chỉ được chứa a-z, 0-9 và dấu '-'.");
            return;
        }
        if (ten.isBlank() || description.getText().isBlank()) {
            error("Bạn cần nhập tên bài và nội dung đề bài.");
            return;
        }
        boolean sql = "sql".equals(mon);
        Map<String, String> views = sql ? Map.of() : views();
        if ("giang-giai".equals(loai)
                && !sql
                && (views.isEmpty() || solution.getText().isBlank())) {
            error("Bài mẫu cần lời giải tham khảo và ít nhất một biến có bộ vẽ để sinh animation.");
            return;
        }
        if (!"giang-giai".equals(loai) && views.containsValue("code-bien")) {
            error("Bộ vẽ Code và biến hiện nguyên lời giải nên chỉ dùng cho bài mẫu.");
            return;
        }
        if (sql
                && (solution.getText().isBlank()
                        || schema.getText().isBlank()
                        || sampleData.getText().isBlank())) {
            error("Bài SQL cần câu đáp án, bảng (schema.sql) và dữ liệu mẫu (du_lieu_mau.sql).");
            return;
        }
        if (!sql && testInput.getText().isBlank() != testOutput.getText().isBlank()) {
            error("Input và Output mẫu phải được nhập đồng thời hoặc để trống cả hai.");
            return;
        }
        Path folder;
        try {
            folder = Path.of(root.getText().trim())
                    .toAbsolutePath()
                    .normalize()
                    .resolve(mon)
                    .resolve(chuong)
                    .resolve(code);
            if (Files.exists(folder)) {
                error("Bài đã tồn tại, không ghi đè: " + folder);
                return;
            }
            String id = mon + "." + chuong + "." + code;
            String yaml = sql
                    ? LessonYaml.sql(
                            id,
                            chuong,
                            ten,
                            loai,
                            QUERY_DML.equals(queryKind.getValue()) ? "dml" : "select",
                            ordered.isSelected(),
                            difficultyCode())
                    : LessonYaml.dsa(
                            id, chuong, ten, loai, views, !testInput.getText().isEmpty(), difficultyCode());
            Files.createDirectories(folder);
            Path chapterName = folder.getParent().resolve("ten-chuong.txt");
            if (newChapter && !Files.exists(chapterName))
                write(chapterName, newChapterName.getText().trim());
            write(folder.resolve("lesson.yaml"), yaml);
            write(folder.resolve("de.md"), description.getText());
            if (sql) {
                write(folder.resolve("khung.sql"), starter.getText());
                write(folder.resolve("dap_an.sql"), solution.getText());
                if (OWN_TABLES.equals(database.getValue()) || database.getValue() == null) {
                    write(folder.resolve("schema.sql"), schema.getText());
                    write(folder.resolve("du_lieu_mau.sql"), sampleData.getText());
                } else {
                    // CSDL dùng chung: chép nguyên bộ để server sinh CSDL ẩn giống các bài có sẵn
                    Path db = databaseRoot().resolve(database.getValue());
                    for (String f : List.of("schema.sql", "du_lieu_mau.sql", "sinh_du_lieu.py")) {
                        if (Files.isRegularFile(db.resolve(f))) Files.copy(db.resolve(f), folder.resolve(f));
                    }
                }
            } else {
                write(folder.resolve("khung.py"), starter.getText());
                if (!solution.getText().isBlank()) write(folder.resolve("loi_giai.py"), solution.getText());
                if (!testInput.getText().isEmpty()) {
                    Path tests = folder.resolve("tests");
                    Files.createDirectories(tests);
                    write(tests.resolve("mau-1.in"), testInput.getText());
                    write(tests.resolve("mau-1.out"), testOutput.getText());
                }
            }
            feedback.getStyleClass().remove("error");
            feedback.setText("Đã tạo: " + folder + "\nKhởi động lại Server để nạp bài mới.");
            Alert alert = new Alert(
                    Alert.AlertType.INFORMATION,
                    "Đã tạo bộ file cho bài " + id + ".\n\n" + folder
                            + "\n\nHãy đặt trong content của Server và khởi động lại Server.");
            if (getScene() != null) alert.initOwner(getScene().getWindow());
            alert.show();
        } catch (IOException | RuntimeException e) {
            error("Không lưu được file: " + e.getMessage());
        }
    }

    /** Độ khó đã chọn ở dạng ghi vào lesson.yaml; rỗng nếu chưa phân loại. */
    private String difficultyCode() {
        return switch (String.valueOf(difficulty.getValue())) {
            case "Dễ" -> "de";
            case "Trung bình" -> "vua";
            case "Khó" -> "kho";
            default -> "";
        };
    }

    private static void write(Path path, String data) throws IOException {
        Files.writeString(path, data, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    private void error(String text) {
        if (!feedback.getStyleClass().contains("error"))
            feedback.getStyleClass().add("error");
        feedback.setText(text);
    }
}
