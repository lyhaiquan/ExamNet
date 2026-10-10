package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import labcast.client.journal.JournalClient;
import labcast.client.net.LcpClient;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.Role;

/**
 * Màn chính sau đăng nhập: danh sách bài bên trái, bài đang mở ở giữa, thanh trạng thái kết nối ở
 * trên. Giáo viên có thêm nút nhập danh sách lớp. Giai đoạn 2 tách thành màn học viên và màn giáo
 * viên đầy đủ (sơ đồ lớp, chiếu, câu hỏi nhanh).
 */
public final class HomeScreen extends BorderPane {
    private final LoginController.LoggedIn li;
    private final JournalClient journal;
    private final ListView<JsonNode> lessons = new ListView<>();
    private final Label status = new Label();
    private LessonScreen current;

    public HomeScreen(LoginController.LoggedIn li, JournalClient journal, Runnable onSessionEnded) {
        this.li = li;
        this.journal = journal;
        LcpClient client = li.client();

        Label hello = new Label("Xin chào, " + li.fullName() + " — " + li.className());
        hello.setStyle("-fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(10, hello, spacer, status);
        if (li.role() == Role.TEACHER) {
            Button importBtn = new Button("Nhập danh sách lớp (CSV)…");
            importBtn.setOnAction(e -> importRoster());
            bar.getChildren().add(1, importBtn);
        }
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8));
        bar.setStyle("-fx-background-color: #eef2f5;");
        setTop(bar);

        lessons.setPrefWidth(240);
        lessons.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(JsonNode n, boolean empty) {
                super.updateItem(n, empty);
                setText(
                        empty || n == null
                                ? null
                                : n.path("ten").asText() + "\n" + n.path("id").asText());
            }
        });
        lessons.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                open(n.path("id").asText());
            }
        });
        setLeft(lessons);
        setCenter(new Label("Chọn một bài ở danh sách bên trái."));

        client.onStateChange(s -> Platform.runLater(() -> {
            switch (s) {
                case RECONNECTING -> {
                    status.setText("Mất kết nối, đang nối lại… (code đang gõ vẫn được giữ)");
                    status.setStyle("-fx-text-fill: #d35400;");
                }
                case CONNECTED -> {
                    status.setText("Đã nối lại");
                    status.setStyle("-fx-text-fill: #27ae60;");
                }
                case CLOSED -> onSessionEnded.run();
            }
        }));
        client.on(MessageType.ERROR, f -> {
            try {
                String msg = ErrorMessage.from(f).message();
                Platform.runLater(() -> {
                    status.setText(msg);
                    status.setStyle("-fx-text-fill: #c0392b;");
                });
            } catch (Exception ignored) {
                // bỏ
            }
        });
        loadLessons();
    }

    private void loadLessons() {
        LcpClient c = li.client();
        var reply = c.expect(MessageType.LESSON_DATA);
        c.send(MessageType.LESSON_LIST, new byte[0]);
        reply.orTimeout(10, TimeUnit.SECONDS).thenAccept(f -> {
            try {
                JsonNode list = JsonPayload.tree(f).path("lessons");
                Platform.runLater(() -> {
                    lessons.getItems().clear();
                    list.forEach(lessons.getItems()::add);
                });
            } catch (Exception e) {
                Platform.runLater(() -> status.setText("Không đọc được danh sách bài"));
            }
        });
    }

    private void open(String lessonId) {
        LcpClient c = li.client();
        var reply = c.expect(MessageType.LESSON_DATA, MessageType.ERROR);
        c.send(MessageType.LESSON_FETCH, new PayloadWriter().str(lessonId).toBytes());
        status.setText("Đang mở bài…");
        reply.orTimeout(30, TimeUnit.SECONDS)
                .whenComplete((f, err) -> Platform.runLater(() -> {
                    if (err != null || f.type() != MessageType.LESSON_DATA) {
                        status.setText("Không mở được bài " + lessonId);
                        return;
                    }
                    try {
                        JsonNode lesson = JsonPayload.tree(f);
                        if (current != null) {
                            current.dispose();
                        }
                        current = new LessonScreen(c, journal, lesson);
                        setCenter(current);
                        status.setText("");
                    } catch (Exception e) {
                        status.setText("Bài hỏng: " + e.getMessage());
                    }
                }));
    }

    private void importRoster() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Chọn file CSV: username,họ tên,mật khẩu");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv", "*.txt"));
        File file = fc.showOpenDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        String csv;
        try {
            csv = Files.readString(file.toPath(), StandardCharsets.UTF_8).replace("﻿", "");
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Không đọc được file: " + e.getMessage()).show();
            return;
        }
        LcpClient c = li.client();
        var reply = c.expect(MessageType.ADMIN_OK, MessageType.ERROR);
        c.send(MessageType.ADMIN_ROSTER_IMPORT, new PayloadWriter().str(csv).toBytes());
        reply.orTimeout(60, TimeUnit.SECONDS)
                .whenComplete((Frame f, Throwable err) -> Platform.runLater(() -> {
                    try {
                        String msg = err != null
                                ? "Server không trả lời"
                                : f.type() == MessageType.ADMIN_OK
                                        ? f.reader().str()
                                        : ErrorMessage.from(f).message();
                        new Alert(Alert.AlertType.INFORMATION, msg).show();
                    } catch (Exception e) {
                        new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
                    }
                }));
    }
}
