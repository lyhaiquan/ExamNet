package labcast.client.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import labcast.client.net.LcpClient;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;

/**
 * Trang "Thông báo bảo mật" của quản trị: yêu cầu quên mật khẩu đang chờ (đặt mật khẩu tạm ngay tại đây) và lịch sử
 * đổi / đặt lại mật khẩu, khoá tài khoản. Tự tải lại mỗi 5 giây. Mật khẩu không bao giờ hiện ở đây.
 */
final class NoticesScreen extends ScrollPane {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");

    private final LcpClient client;
    private final PasswordRequestsBanner pending;
    private final VBox rows = new VBox(6);
    private final Label empty = new Label("Chưa có sự kiện nào.");
    private final ComboBox<String> group = new ComboBox<>();
    private final Timeline timer;
    private final IntConsumer onBadge;
    private JsonNode events = JsonPayload.MAPPER.createArrayNode();
    private boolean loading;
    private boolean closed;

    NoticesScreen(LcpClient client, IntConsumer onBadge) {
        this.client = client;
        this.onBadge = onBadge;
        this.pending = new PasswordRequestsBanner(client, this::reload);
        getStyleClass().addAll("dash-scroll", "dash-admin");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);

        Label heading = new Label("Thông báo bảo mật");
        heading.getStyleClass().add("workspace-heading");
        Label hint = new Label("Ai bấm Quên mật khẩu, ai tự đổi hay bị đặt lại mật khẩu, ai vào tài khoản đang bị khoá."
                + " Mật khẩu người dùng tự đặt không hiện ở đâu cả.");
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);

        group.getItems().addAll(NoticeModel.GROUPS);
        group.setValue(NoticeModel.ALL);
        group.valueProperty().addListener((o, a, b) -> showEvents());
        Button markRead = new Button("Đánh dấu đã đọc");
        markRead.getStyleClass().add("students-refresh");
        markRead.setOnAction(e -> load(true));
        Button refresh = new Button("↻ Làm mới");
        refresh.getStyleClass().add("students-refresh");
        refresh.setOnAction(e -> load(false));
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Label historyTitle = new Label("Lịch sử gần đây");
        historyTitle.getStyleClass().add("pwd-requests-title");
        HBox bar = new HBox(10, historyTitle, gap, group, markRead, refresh);
        bar.setAlignment(Pos.CENTER_LEFT);

        empty.getStyleClass().add("muted");
        VBox page = new VBox(16, new VBox(6, heading, hint), pending, bar, rows);
        page.setPadding(new Insets(25, 30, 25, 30));
        page.getStyleClass().add("dash-page");
        setContent(page);

        timer = new Timeline(new KeyFrame(Duration.seconds(5), e -> load(false)));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
        load(true);
    }

    void stop() {
        closed = true;
        timer.stop();
    }

    private void reload() {
        load(false);
    }

    /** {@code markRead}: lần mở trang đầu tiên coi như đã xem. */
    private void load(boolean markRead) {
        if (loading || closed) return;
        loading = true;
        var reply = client.expect(MessageType.NOTICES_DATA, MessageType.ERROR);
        client.send(MessageType.NOTICES_REQ, new PayloadWriter().bool(markRead).toBytes());
        reply.orTimeout(10, TimeUnit.SECONDS).whenComplete((f, err) -> {
            JsonNode data = null;
            if (err == null && f.type() == MessageType.NOTICES_DATA) {
                try {
                    data = JsonPayload.tree(f);
                } catch (Exception ignored) {
                    // để trống
                }
            }
            JsonNode d = data;
            Platform.runLater(() -> {
                loading = false;
                if (d == null || closed) return;
                events = d.path("events");
                onBadge.accept(d.path("badge").asInt());
                pending.reload();
                showEvents();
            });
        });
    }

    private void showEvents() {
        rows.getChildren().clear();
        var list = NoticeModel.filter(events, group.getValue());
        if (list.isEmpty()) {
            rows.getChildren().add(empty);
            return;
        }
        for (JsonNode e : list) {
            Label what = new Label(NoticeModel.label(e.path("action").asText()));
            what.getStyleClass().add("pwd-requests-name");
            Label who = new Label(NoticeModel.describe(e));
            who.getStyleClass().add("pwd-requests-meta");
            Label when = new Label(
                    TIME.format(Instant.ofEpochMilli(e.path("ts").asLong()).atZone(ZoneId.systemDefault())));
            when.getStyleClass().add("pwd-requests-meta");
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            HBox row = new HBox(10, new VBox(1, what, who), gap, when);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("pwd-requests-row");
            rows.getChildren().add(row);
        }
    }
}
