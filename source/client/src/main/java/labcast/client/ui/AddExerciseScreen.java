package labcast.client.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
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
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

/**
 * Tạo gói nội dung bài tập theo đúng định dạng mà LessonRepository đọc từ thư mục content.
 * Hiện server chưa có giao thức ADMIN_LESSON_CREATE, do đó không giả vờ cập nhật server qua mạng:
 * giáo viên phải lưu trong thư mục content của server và khởi động lại server để nạp bài.
 */
public final class AddExerciseScreen extends BorderPane {
    private final TextField root = new TextField();
    private final TextField chapter = new TextField("co-ban");
    private final TextField slug = new TextField();
    private final TextField title = new TextField();
    private final ComboBox<String> subject = new ComboBox<>();
    private final ComboBox<String> type = new ComboBox<>();
    private final TextArea description = new TextArea();
    private final TextArea starter = new TextArea();
    private final TextArea solution = new TextArea();
    private final TextArea testInput = new TextArea();
    private final TextArea testOutput = new TextArea();
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
        type.getItems().addAll("Bài tập", "Thực hành");
        type.getSelectionModel().selectFirst();
        type.setMaxWidth(Double.MAX_VALUE);
        subject.valueProperty().addListener((obs, old, value) -> updateSubject());
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
        chapter.setPromptText("VD: bam, mang, truy-van");
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
        addGrid(meta, 0, 1, field("Mã chương", chapter));
        addGrid(meta, 1, 1, field("Mã bài", slug));
        VBox metadataCard = card(
                "02   Thông tin bài tập",
                "Nhập thông tin nhận diện và tên hiển thị của bài.",
                meta,
                field("Tên bài tập", title));

        VBox statementCard =
                card("03   Nội dung đề bài", "Hỗ trợ văn bản Markdown (de.md).", field("Đề bài", description));

        HBox codeEditors = new HBox(14);
        VBox starterBox = field("Code khởi tạo", starter);
        VBox solutionBox = field("Lời giải tham khảo", solution);
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
        VBox testCard =
                card("05   Dữ liệu kiểm thử", "Input và Output cần khớp với nhau; bài SQL yêu cầu đủ dữ liệu.", tests);

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

        form.getChildren().addAll(locationCard, metadataCard, statementCard, editorCard, testCard, saveCard);
        // Cùng chiều cao cho các ô nhập một dòng và cùng chiều cao cho textarea ngang hàng.
        for (TextField input : new TextField[] {root, chapter, slug, title}) {
            input.getStyleClass().add("exercise-author-input");
            input.setMinHeight(44);
            input.setPrefHeight(44);
            input.setMaxHeight(44);
        }
        for (TextArea input : new TextArea[] {description, starter, solution, testInput, testOutput})
            input.getStyleClass().add("exercise-author-input");
        description.setPrefRowCount(6);
        starter.setPrefRowCount(8);
        solution.setPrefRowCount(8);
        testInput.setPrefRowCount(4);
        testOutput.setPrefRowCount(4);
        subject.setMinHeight(44);
        subject.setPrefHeight(44);
        subject.setMaxHeight(44);
        type.setMinHeight(44);
        type.setPrefHeight(44);
        type.setMaxHeight(44);
        choose.setMinHeight(44);
        choose.setPrefHeight(44);
        choose.setMaxHeight(44);
        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("exercise-author-scroll");
        setCenter(scroll);
        updateSubject();
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

    private static String defaultContentPath() {
        Path here =
                Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        for (Path candidate :
                new Path[] {here.resolve("content"), here.resolve("../content").normalize()}) {
            if (Files.isDirectory(candidate)) return candidate.toString();
        }
        return here.resolve("content").toString();
    }

    private void updateSubject() {
        boolean sql = "SQL".equals(subject.getValue());
        testInput.setPromptText(sql ? "SQL tạo bảng (schema.sql)" : "Input kiểm thử mẫu");
        testOutput.setPromptText(sql ? "SQL thêm dữ liệu mẫu (du_lieu_mau.sql)" : "Output mong đợi");
    }

    private void saveLesson() {
        feedback.setText("");
        String mon = "SQL".equals(subject.getValue()) ? "sql" : "dsa";
        String loai = "Thực hành".equals(type.getValue()) ? "luyen-tap" : "bai-tap";
        String chuong = chapter.getText().trim().toLowerCase(Locale.ROOT);
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
        if (sql
                && (solution.getText().isBlank()
                        || testInput.getText().isBlank()
                        || testOutput.getText().isBlank())) {
            error("Bài SQL cần có đáp án, schema.sql và du_lieu_mau.sql.");
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
            // Chuỗi YAML được bao trong nháy kép và thoát ký tự đặc biệt.
            String yaml = "id: " + id + "\n"
                    + "mon: " + mon + "\n"
                    + "chuong: " + chuong + "\n"
                    + "ten: " + yamlString(ten) + "\n"
                    + "loai: " + loai + "\n"
                    + "bac: [xem, code]\n"
                    + "khung_nhin: []\n"
                    + "cam_dung: []\n"
                    + "cam_import: []\n"
                    + "cam_cu_phap: []\n"
                    + "gioi_han: { thoi_gian_ms: 2000, bo_nho_mb: 256 }\n"
                    + "test_xem: " + (!sql && !testInput.getText().isEmpty() ? "[mau-1]" : "[]") + "\n"
                    + "so_test_an: 0\n"
                    + "mo_khoa: []\n";
            Files.createDirectories(folder);
            write(folder.resolve("lesson.yaml"), yaml);
            write(folder.resolve("de.md"), description.getText());
            if (sql) {
                write(folder.resolve("khung.sql"), starter.getText());
                write(folder.resolve("dap_an.sql"), solution.getText());
                write(folder.resolve("schema.sql"), testInput.getText());
                write(folder.resolve("du_lieu_mau.sql"), testOutput.getText());
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

    private static void write(Path path, String data) throws IOException {
        Files.writeString(path, data, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    private static String yamlString(String v) {
        return "\""
                + v.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\r", " ")
                        .replace("\n", " ") + "\"";
    }

    private void error(String text) {
        if (!feedback.getStyleClass().contains("error"))
            feedback.getStyleClass().add("error");
        feedback.setText(text);
    }
}
