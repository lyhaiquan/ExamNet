package labcast.client.ui;

import java.util.function.Function;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import labcast.client.cast.CastSession;
import labcast.client.player.Player;
import labcast.client.player.PlayerControls;
import labcast.client.player.PlayerPane;
import labcast.client.views.Views;
import labcast.common.trace.Trace;

/**
 * Màn học viên xem giáo viên chiếu (ĐG1). Mặc định theo giáo viên: cùng bước, cùng lúc với cả lớp. Học
 * viên bấm bất kỳ nút nào của trình phát là tách ra tự xem; nút "Theo giáo viên" về lại đúng bước đang
 * chiếu.
 */
public final class CastView extends BorderPane implements CastSession.Screen {
    private final CastSession session;
    private final Function<String, String> lessonName;
    private final PlayerPane player = new PlayerPane();
    private final Label title = new Label("Giáo viên đang chiếu");
    private final Label live = new Label();
    private final Label follow = new Label();
    private final Button rejoin = new Button("⟲ Theo giáo viên");
    private boolean ended;

    /**
     * @param lessonName tên bài theo mã bài (traceId), để hiện tiêu đề
     * @param onClose nút "Đóng"
     */
    public CastView(CastSession session, Function<String, String> lessonName, Runnable onClose) {
        this.session = session;
        this.lessonName = lessonName;
        getStyleClass().add("cast-view");
        setId("cast-view");

        live.getStyleClass().add("source-chip");
        follow.managedProperty().bind(follow.visibleProperty());
        title.getStyleClass().add("cast-title");
        follow.setId("follow-state");
        rejoin.setId("rejoin");
        rejoin.getStyleClass().add("primary");
        rejoin.managedProperty().bind(rejoin.visibleProperty());
        rejoin.setOnAction(e -> session.rejoin());
        Button close = new Button("✕ Đóng");
        close.setOnAction(e -> onClose.run());
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox header = new HBox(10, live, title, gap, follow, rejoin, close);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("cast-header");
        setTop(header);

        player.getStyleClass().add("cast-body");
        BorderPane.setMargin(player, new Insets(0, 20, 20, 20));
        setCenter(player);

        // Học viên tự bấm: tách khỏi giáo viên trước, rồi mới điều khiển trình phát trên máy mình
        player.setControls(new PlayerControls() {
            @Override
            public void play(Player p) {
                session.detach();
                PlayerControls.LOCAL.play(p);
            }

            @Override
            public void pause(Player p) {
                session.detach();
                PlayerControls.LOCAL.pause(p);
            }

            @Override
            public void seek(Player p, int k) {
                session.detach();
                PlayerControls.LOCAL.seek(p, k);
            }

            @Override
            public void speed(Player p, double x) {
                session.detach();
                PlayerControls.LOCAL.speed(p, x);
            }

            @Override
            public void nextKey(Player p) {
                session.detach();
                PlayerControls.LOCAL.nextKey(p);
            }
        });
        session.onChange(this::refresh);
        refresh();
    }

    @Override
    public Player show(String traceId, Trace trace) {
        ended = false;
        title.setText(lessonName.apply(traceId));
        Player p = player.load(trace, Views.create(trace.header().path("views"), "v"));
        refresh();
        return p;
    }

    @Override
    public void stopped() {
        ended = true;
        refresh();
    }

    private void refresh() {
        follow.getStyleClass().removeAll("detached", "ended");
        live.getStyleClass().removeAll("casting", "none");
        rejoin.setVisible(false);
        follow.setVisible(true);
        boolean over = ended || !session.active();
        live.setText(over ? "ĐÃ DỪNG CHIẾU" : "📡 ĐANG CHIẾU");
        live.getStyleClass().add(over ? "none" : "casting");
        if (over) {
            follow.setVisible(false); // trình phát giữ bước cuối, học viên tự xem lại được
        } else if (!session.ready()) {
            follow.setText("Đang nhận animation…");
        } else if (session.following()) {
            follow.setText("Đang theo giáo viên");
        } else {
            follow.setText("Em đang tự xem");
            follow.getStyleClass().add("detached");
            rejoin.setVisible(true);
        }
        if (!follow.getStyleClass().contains("follow-chip")) {
            follow.getStyleClass().add("follow-chip");
        }
    }

    public PlayerPane playerPane() {
        return player;
    }
}
