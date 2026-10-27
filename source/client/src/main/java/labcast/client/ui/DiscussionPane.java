package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import labcast.client.net.LcpClient;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;

/**
 * Thẻ "Thảo luận" của một bài: bình luận của lớp đang chọn, trả lời lồng một cấp, nhận bình luận mới ngay khi
 * bạn cùng lớp gửi. Học viên không dán được chữ vào ô gõ (như ô code) để thảo luận không thành chỗ dán lời
 * giải; giáo viên ẩn/hiện được từng bình luận.
 */
final class DiscussionPane extends VBox {
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm dd/MM").withZone(ZoneId.systemDefault());

    private final LcpClient client;
    private final String lessonId;
    private final boolean staff;
    private final DiscussionModel model;
    private final VBox list = new VBox(10);
    private final TextArea input = new TextArea();
    private final Label replyTo = new Label();
    private final Label status = new Label();
    private final Consumer<Frame> onNew = this::onNew;
    private final IntConsumer onCount;
    private long parentId;

    DiscussionPane(LcpClient client, String lessonId, boolean staff, IntConsumer onCount) {
        this.client = client;
        this.lessonId = lessonId;
        this.staff = staff;
        this.onCount = onCount;
        this.model = new DiscussionModel(staff);
        getStyleClass().add("discuss-pane");
        setSpacing(10);

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("discuss-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        list.getStyleClass().add("discuss-list");

        input.setPromptText(
                staff
                        ? "Trả lời hoặc nhắc cả lớp…"
                        : "Hỏi về đề bài, cách nghĩ… (không dán được chữ, đừng gửi lời giải)");
        input.setWrapText(true);
        input.setPrefRowCount(3);
        input.getStyleClass().add("discuss-input");
        if (!staff) blockPaste(input);
        input.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && e.isControlDown()) {
                e.consume();
                send();
            }
        });
        replyTo.getStyleClass().add("discuss-reply-to");
        replyTo.managedProperty().bind(replyTo.visibleProperty());
        replyTo.setVisible(false);
        replyTo.setOnMouseClicked(e -> setReply(0, null));
        status.getStyleClass().add("dash-row-meta");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Button send = new Button("Gửi  (Ctrl+Enter)");
        send.getStyleClass().add("dash-primary");
        send.setOnAction(e -> send());
        HBox actions = new HBox(8, status, gap, send);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox composer = new VBox(6, replyTo, input, actions);
        composer.getStyleClass().add("discuss-composer");
        getChildren().addAll(scroll, composer);

        client.on(MessageType.DISCUSS_NEW, onNew);
        load();
    }

    /** Chặn mọi đường dán: Ctrl+V, Shift+Insert, menu chuột phải, kéo thả. */
    private static void blockPaste(TextArea area) {
        area.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if ((e.isShortcutDown() && e.getCode() == KeyCode.V)
                    || (e.isShiftDown() && e.getCode() == KeyCode.INSERT)) {
                e.consume();
            }
        });
        area.setContextMenu(new ContextMenu());
        area.addEventFilter(javafx.scene.input.DragEvent.ANY, javafx.event.Event::consume);
    }

    void dispose() {
        client.off(MessageType.DISCUSS_NEW, onNew);
    }

    private void load() {
        var reply = client.expect(MessageType.DISCUSS_DATA, MessageType.ERROR);
        client.send(MessageType.DISCUSS_REQ, new PayloadWriter().str(lessonId).toBytes());
        reply.orTimeout(10, TimeUnit.SECONDS).whenComplete((f, err) -> {
            JsonNode items = null;
            String error = null;
            try {
                if (err != null) error = "Server chưa hỗ trợ thảo luận";
                else if (f.type() == MessageType.ERROR)
                    error = ErrorMessage.from(f).message();
                else items = JsonPayload.tree(f).path("items");
            } catch (Exception e) {
                error = e.getMessage();
            }
            JsonNode data = items;
            String msg = error;
            Platform.runLater(() -> {
                if (data != null) model.reset(data);
                status.setText(msg == null ? "" : msg);
                render();
            });
        });
    }

    private void onNew(Frame f) {
        try {
            JsonNode n = JsonPayload.tree(f);
            if (!lessonId.equals(n.path("lessonId").asText())) return;
            JsonNode item = n.path("item");
            Platform.runLater(() -> {
                model.upsert(item);
                render();
            });
        } catch (Exception ignored) {
            // khung hỏng thì bỏ
        }
    }

    private void send() {
        String text = input.getText().strip();
        if (text.isEmpty()) return;
        status.setText("Đang gửi…");
        var reply = client.expect(MessageType.ERROR);
        client.send(JsonPayload.frame(
                MessageType.DISCUSS_POST, Map.of("lessonId", lessonId, "text", text, "parentId", parentId)));
        // thành công thì DISCUSS_NEW tới qua onNew; lỗi (gửi dồn, quá dài) hiện ở đây
        input.clear();
        setReply(0, null);
        reply.orTimeout(1500, TimeUnit.MILLISECONDS)
                .whenComplete((f, err) -> Platform.runLater(() -> {
                    if (err == null && f != null) {
                        try {
                            status.setText(ErrorMessage.from(f).message());
                            input.setText(text);
                        } catch (Exception ignored) {
                            status.setText("Không gửi được");
                        }
                    } else {
                        status.setText("");
                    }
                }));
    }

    private void setReply(long id, String author) {
        parentId = id;
        replyTo.setVisible(id > 0);
        replyTo.setText(id > 0 ? "↪ Đang trả lời " + author + "   ✕" : "");
        if (id > 0) input.requestFocus();
    }

    private void render() {
        list.getChildren().clear();
        var threads = model.threads();
        if (threads.isEmpty()) {
            Label empty = new Label("Chưa có thảo luận nào. Hãy đặt câu hỏi đầu tiên!");
            empty.getStyleClass().add("dash-row-meta");
            list.getChildren().add(empty);
        }
        for (var t : threads) {
            VBox thread = new VBox(6, bubble(t.root(), true));
            for (JsonNode r : t.replies()) {
                Node b = bubble(r, false);
                HBox indent = new HBox(b);
                indent.getStyleClass().add("discuss-reply");
                HBox.setHgrow(b, Priority.ALWAYS);
                thread.getChildren().add(indent);
            }
            thread.getStyleClass().add("discuss-thread");
            list.getChildren().add(thread);
        }
        onCount.accept(model.size());
    }

    private Node bubble(JsonNode n, boolean root) {
        String author = n.path("author").asText();
        boolean teacher = !"STUDENT".equals(n.path("role").asText());
        Label avatar = new Label(HomeScreen.initial(author.isBlank() ? "?" : author));
        avatar.getStyleClass().addAll("dash-avatar", teacher ? "discuss-avatar-teacher" : "discuss-avatar-student");
        Label who = new Label(
                author + (teacher ? "  · Giáo viên" : "") + (n.path("mine").asBoolean() ? "  (bạn)" : ""));
        who.getStyleClass().add("dash-row-title");
        Label when = new Label(TIME.format(Instant.ofEpochMilli(n.path("at").asLong())));
        when.getStyleClass().add("dash-row-meta");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox head = new HBox(8, avatar, who, gap, when);
        head.setAlignment(Pos.CENTER_LEFT);
        Label text = new Label(n.path("text").asText());
        text.setWrapText(true);
        text.getStyleClass().add("discuss-text");
        HBox tools = new HBox(10);
        long id = n.path("id").asLong();
        if (root) {
            Button reply = new Button("Trả lời");
            reply.getStyleClass().add("dash-link");
            reply.setOnAction(e -> setReply(id, author));
            tools.getChildren().add(reply);
        }
        boolean hidden = n.path("hidden").asBoolean();
        if (staff) {
            Button hide = new Button(hidden ? "Hiện lại" : "Ẩn với học viên");
            hide.getStyleClass().add("dash-link");
            hide.setOnAction(e -> client.send(
                    MessageType.DISCUSS_HIDE,
                    new PayloadWriter().i64(id).bool(!hidden).toBytes()));
            tools.getChildren().add(hide);
        }
        VBox box = new VBox(6, head, text, tools);
        box.getStyleClass().addAll("discuss-bubble", teacher ? "discuss-bubble-teacher" : "discuss-bubble-student");
        if (hidden) box.getStyleClass().add("discuss-bubble-hidden");
        return box;
    }
}
