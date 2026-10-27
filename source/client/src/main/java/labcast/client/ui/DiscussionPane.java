package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
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
 * Khung "Thảo luận" bên phải màn bài: bình luận của lớp đang chọn, lọc Tất cả / Theo dõi, sắp Mới nhất / Hữu ích
 * nhất, thích, trả lời thu gọn (hiện 2, còn lại "Xem thêm"), nhận bình luận mới ngay khi bạn cùng lớp gửi. Học
 * viên không dán được chữ vào ô gõ (như ô code) để thảo luận không thành chỗ dán lời giải; giáo viên ẩn/hiện
 * được từng bình luận.
 */
final class DiscussionPane extends VBox {
    private static final int SHOWN_REPLIES = 2;

    private final LcpClient client;
    private final String lessonId;
    private final boolean staff;
    private final DiscussionModel model;
    private final VBox list = new VBox(12);
    private final TextArea input = new TextArea();
    private final Label replyTo = new Label();
    private final Label status = new Label();
    private final Label title = new Label("Thảo luận");
    private final Consumer<Frame> onNew = this::onNew;
    private final IntConsumer onUnread;
    private final Set<Long> expanded = new HashSet<>();
    private boolean onlyFollowing;
    private DiscussionModel.Sort sort = DiscussionModel.Sort.NEWEST;
    private boolean open;
    private int unread;
    private long parentId;

    /**
     * @param onClose nút ✕ ở góc khung
     * @param onUnread số bình luận mới tới khi khung đang đóng (0 khi mở)
     */
    DiscussionPane(LcpClient client, String lessonId, boolean staff, Runnable onClose, IntConsumer onUnread) {
        this.client = client;
        this.lessonId = lessonId;
        this.staff = staff;
        this.onUnread = onUnread;
        this.model = new DiscussionModel(staff);
        getStyleClass().add("discuss-pane");
        setSpacing(10);
        setMinWidth(300);

        title.getStyleClass().add("discuss-title");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Button close = new Button("✕");
        close.getStyleClass().add("discuss-close");
        close.setTooltip(new Tooltip("Đóng thảo luận"));
        close.setOnAction(e -> onClose.run());
        HBox head = new HBox(8, title, gap, close);
        head.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup tabs = new ToggleGroup();
        ToggleButton all = tab("Tất cả", tabs, false);
        ToggleButton follow = tab("Theo dõi", tabs, true);
        all.setSelected(true);
        ComboBox<String> sortBox = new ComboBox<>();
        sortBox.getItems().addAll("Mới nhất", "Hữu ích nhất");
        sortBox.setValue("Mới nhất");
        sortBox.getStyleClass().add("discuss-sort");
        sortBox.setOnAction(e -> {
            sort = "Hữu ích nhất".equals(sortBox.getValue())
                    ? DiscussionModel.Sort.USEFUL
                    : DiscussionModel.Sort.NEWEST;
            render();
        });
        Region gap2 = new Region();
        HBox.setHgrow(gap2, Priority.ALWAYS);
        HBox filters = new HBox(4, all, follow, gap2, sortBox);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.getStyleClass().add("discuss-filters");

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("discuss-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        input.setPromptText("Viết bình luận…");
        input.setTooltip(new Tooltip(
                staff
                        ? "Enter để gửi · Shift+Enter xuống dòng"
                        : "Enter để gửi · Shift+Enter xuống dòng · không dán được chữ, đừng gửi lời giải"));
        input.setWrapText(true);
        // mới vào một dòng; gõ nhiều dòng (Shift+Enter) thì cao dần tới 4 dòng
        input.setPrefRowCount(1);
        input.textProperty()
                .addListener((o, a, b) -> input.setPrefRowCount(Math.max(
                        1, Math.min(4, (int) b.chars().filter(ch -> ch == '\n').count() + 1))));
        input.getStyleClass().add("discuss-input");
        HBox.setHgrow(input, Priority.ALWAYS);
        if (!staff) blockPaste(input);
        input.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && !e.isShiftDown()) {
                e.consume(); // Enter gửi, Shift+Enter xuống dòng
                send();
            }
        });
        Button send = new Button();
        send.setGraphic(icon(SEND, "discuss-icon-send"));
        send.getStyleClass().add("discuss-send");
        send.setTooltip(new Tooltip("Gửi (Enter) · Shift+Enter để xuống dòng"));
        send.setOnAction(e -> send());
        replyTo.getStyleClass().add("discuss-reply-to");
        replyTo.managedProperty().bind(replyTo.visibleProperty());
        replyTo.setVisible(false);
        replyTo.setOnMouseClicked(e -> setReply(0, null));
        status.getStyleClass().add("dash-row-meta");
        status.managedProperty().bind(status.visibleProperty());
        status.visibleProperty().bind(status.textProperty().isNotEmpty());
        HBox box = new HBox(8, input, send);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("discuss-input-box");
        VBox composer = new VBox(6, replyTo, status, box);
        composer.getStyleClass().add("discuss-composer");
        getChildren().addAll(head, filters, scroll, composer);

        client.on(MessageType.DISCUSS_NEW, onNew);
        load();
    }

    private ToggleButton tab(String text, ToggleGroup group, boolean following) {
        ToggleButton t = new ToggleButton(text);
        t.setToggleGroup(group);
        t.getStyleClass().add("discuss-tab");
        t.setOnAction(e -> {
            if (!t.isSelected()) t.setSelected(true);
            onlyFollowing = following;
            render();
        });
        return t;
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

    /** Khung đang hiện hay không; mở thì xoá đếm chưa đọc. */
    void setOpen(boolean open) {
        this.open = open;
        if (open) {
            unread = 0;
            onUnread.accept(0);
        }
    }

    int count() {
        return model.size();
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
                // khung chưa mở: nút thảo luận hiện số bình luận đang có
                if (!open) onUnread.accept(unread = model.size());
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
                boolean fresh = model.upsert(item);
                if (fresh && !open && !item.path("mine").asBoolean()) onUnread.accept(++unread);
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
        if (parentId > 0) expanded.add(parentId); // thấy ngay câu trả lời vừa gửi
        input.clear();
        setReply(0, null);
        // thành công thì DISCUSS_NEW tới qua onNew; lỗi (gửi dồn, quá dài) hiện ở đây
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
        title.setText("Thảo luận (" + model.size() + ")");
        list.getChildren().clear();
        var threads = model.threads(onlyFollowing, sort);
        if (threads.isEmpty()) {
            Label empty = new Label(
                    onlyFollowing
                            ? "Bạn chưa hỏi hay trả lời luồng nào ở bài này."
                            : "Chưa có thảo luận nào. Hãy đặt câu hỏi đầu tiên!");
            empty.setWrapText(true);
            empty.getStyleClass().add("dash-row-meta");
            list.getChildren().add(empty);
        }
        for (var t : threads) list.getChildren().add(thread(t));
    }

    private Node thread(DiscussionModel.Thread t) {
        long rootId = t.root().path("id").asLong();
        long rootAuthor = t.root().path("authorId").asLong();
        VBox box = new VBox(8, comment(t.root(), true, t.replies().size(), false));
        box.getStyleClass().add("discuss-thread");
        if (!t.replies().isEmpty()) {
            VBox replies = new VBox(8);
            replies.getStyleClass().add("discuss-replies");
            boolean all = expanded.contains(rootId);
            int n = all
                    ? t.replies().size()
                    : Math.min(SHOWN_REPLIES, t.replies().size());
            for (int i = 0; i < n; i++) {
                JsonNode r = t.replies().get(i);
                replies.getChildren()
                        .add(comment(r, false, 0, r.path("authorId").asLong() == rootAuthor));
            }
            int more = t.replies().size() - n;
            if (more > 0 || all && t.replies().size() > SHOWN_REPLIES) {
                Button toggle = new Button(more > 0 ? "Xem thêm " + more + " phản hồi" : "Thu gọn phản hồi");
                toggle.getStyleClass().add("dash-link");
                toggle.setOnAction(e -> {
                    if (!expanded.remove(rootId)) expanded.add(rootId);
                    render();
                });
                replies.getChildren().add(toggle);
            }
            box.getChildren().add(replies);
        }
        return box;
    }

    private Node comment(JsonNode n, boolean root, int replyCount, boolean threadAuthor) {
        String author = n.path("author").asText();
        boolean teacher = !"STUDENT".equals(n.path("role").asText());
        Label avatar = new Label(HomeScreen.initial(author.isBlank() ? "?" : author));
        avatar.getStyleClass().addAll("dash-avatar", teacher ? "discuss-avatar-teacher" : "discuss-avatar-student");
        Label who = new Label(author + (n.path("mine").asBoolean() ? " (bạn)" : ""));
        who.getStyleClass().add("discuss-name");
        Label when = new Label(ago(n.path("at").asLong()));
        when.getStyleClass().add("discuss-time");
        HBox nameLine = new HBox(6, who);
        nameLine.setAlignment(Pos.CENTER_LEFT);
        if (teacher) nameLine.getChildren().add(tag("Giáo viên", "discuss-tag-teacher"));
        if (threadAuthor) nameLine.getChildren().add(tag("Tác giả", "discuss-tag-author"));
        VBox names = new VBox(0, nameLine, when);
        HBox head = new HBox(8, avatar, names);
        head.setAlignment(Pos.CENTER_LEFT);

        Label text = new Label(n.path("text").asText());
        text.setWrapText(true);
        text.getStyleClass().add("discuss-text");

        long id = n.path("id").asLong();
        boolean liked = n.path("liked").asBoolean();
        Button like = new Button(Integer.toString(n.path("likes").asInt()));
        like.setGraphic(icon(THUMB, "discuss-icon"));
        like.getStyleClass().addAll("discuss-action", liked ? "discuss-liked" : "discuss-like");
        like.setTooltip(new Tooltip(liked ? "Bỏ thích" : "Thích: bình luận hữu ích"));
        like.setOnAction(e -> client.send(
                MessageType.DISCUSS_LIKE,
                new PayloadWriter().i64(id).bool(!liked).toBytes()));
        HBox actions = new HBox(10, like);
        actions.setAlignment(Pos.CENTER_LEFT);
        if (root) {
            Button reply = new Button(Integer.toString(replyCount));
            reply.setGraphic(icon(CHAT, "discuss-icon"));
            reply.getStyleClass().add("discuss-action");
            reply.setTooltip(new Tooltip("Trả lời"));
            reply.setOnAction(e -> setReply(id, author));
            actions.getChildren().add(reply);
        }
        boolean hidden = n.path("hidden").asBoolean();
        if (staff) {
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            Button hide = new Button(hidden ? "Hiện lại" : "Ẩn");
            hide.getStyleClass().add("dash-link");
            hide.setTooltip(new Tooltip(hidden ? "Hiện lại cho học viên" : "Ẩn với học viên (lộ lời giải, lạc đề…)"));
            hide.setOnAction(e -> client.send(
                    MessageType.DISCUSS_HIDE,
                    new PayloadWriter().i64(id).bool(!hidden).toBytes()));
            actions.getChildren().addAll(gap, hide);
        }
        VBox box = new VBox(6, head, text, actions);
        box.getStyleClass().addAll("discuss-comment", root ? "discuss-root" : "discuss-reply-item");
        if (hidden) box.getStyleClass().add("discuss-hidden");
        return box;
    }

    static final String THUMB = "M7 10 V20 H3 V10 Z M7 10 L11 3 C12.5 3 13.2 4 13 5.5 L12.5 9 H19"
            + " C20.3 9 21.2 10.2 20.9 11.4 L19.3 18.3 C19 19.3 18.2 20 17.2 20 H7";
    static final String CHAT = "M4 5 H20 V16 H10 L6 19.5 V16 H4 Z";
    static final String SEND = "M3 11.5 L21 3.5 L13.5 21 L11 13.5 Z";

    static javafx.scene.shape.SVGPath icon(String path, String style) {
        javafx.scene.shape.SVGPath p = new javafx.scene.shape.SVGPath();
        p.setContent(path);
        p.getStyleClass().add(style);
        return p;
    }

    private static Label tag(String text, String style) {
        Label l = new Label(text);
        l.getStyleClass().addAll("discuss-tag", style);
        return l;
    }

    private static String ago(long ts) {
        long min = Math.max(0, (System.currentTimeMillis() - ts) / 60_000);
        if (min < 1) return "vừa xong";
        if (min < 60) return min + " phút trước";
        if (min < 24 * 60) return (min / 60) + " giờ trước";
        return (min / (24 * 60)) + " ngày trước";
    }
}
