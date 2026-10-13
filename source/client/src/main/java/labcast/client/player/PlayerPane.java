package labcast.client.player;

import java.util.Map;
import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import labcast.common.trace.Trace;

/**
 * Khung trình phát: chú thích màu, vùng vẽ các bộ vẽ, dòng thuyết minh, nút Phát/Dừng, thanh tua, tốc
 * độ, nút "tới bước quan trọng". Chỉ vẽ — mọi logic nằm ở {@link Player}. Màu và cỡ chữ ở {@code
 * labcast.css} (các lớp {@code legend}, {@code narration}, {@code player-controls}).
 */
public final class PlayerPane extends BorderPane {
    static final String PLAY = "▶ Phát";
    static final String PAUSE = "⏸ Dừng";

    private final VBox stage = new VBox(18);
    private final Label narration = new Label();
    private final Button playBtn = new Button(PLAY);
    private final Slider slider = new Slider(0, 1, 0);
    private final Label posLabel = new Label("0 / 0");
    private final ComboBox<String> speedBox =
            new ComboBox<>(FXCollections.observableArrayList("0,5×", "1×", "2×", "4×"));
    private Player player;
    private PlayerControls controls = PlayerControls.LOCAL;
    private boolean updatingSlider;

    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (player != null) {
                player.tick();
                String want = player.playing() ? PAUSE : PLAY;
                if (!want.equals(playBtn.getText())) {
                    playBtn.setText(want);
                }
            }
        }
    };

    public PlayerPane() {
        stage.getStyleClass().add("viz-stage");
        stage.setAlignment(Pos.CENTER);
        ScrollPane scroll = new ScrollPane(stage);
        scroll.setFitToWidth(true);
        // hình nhỏ hơn khung thì nằm giữa, lớn hơn thì cuộn được
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        narration.getStyleClass().add("narration");
        narration.setWrapText(true);
        narration.setMaxWidth(Double.MAX_VALUE);
        narration.setMinHeight(36);
        setNarration("");

        playBtn.setId("play");
        playBtn.getStyleClass().addAll("primary", "play");
        playBtn.setOnAction(e -> {
            if (player == null) {
                return;
            }
            if (player.playing()) {
                controls.pause(player);
            } else {
                controls.play(player);
            }
        });
        Button keyBtn = new Button("⏭");
        keyBtn.setTooltip(new Tooltip("Tới bước quan trọng kế tiếp"));
        keyBtn.setOnAction(e -> {
            if (player != null) {
                controls.nextKey(player);
            }
        });
        keyBtn.setMinWidth(Region.USE_PREF_SIZE);
        playBtn.setMinWidth(Region.USE_PREF_SIZE);
        speedBox.setMinWidth(78);
        speedBox.getSelectionModel().select(1);
        speedBox.setOnAction(e -> {
            if (player != null) {
                controls.speed(
                        player,
                        switch (speedBox.getSelectionModel().getSelectedIndex()) {
                            case 0 -> 0.5;
                            case 2 -> 2;
                            case 3 -> 4;
                            default -> 1;
                        });
            }
        });
        // Kéo thanh tua: chỉ tua khi thả tay, không tua liên tục trong lúc kéo
        slider.valueChangingProperty().addListener((obs, was, now) -> {
            if (!now && player != null) {
                controls.seek(player, (int) slider.getValue());
            }
        });
        slider.valueProperty().addListener((obs, o, n) -> {
            if (!updatingSlider && !slider.isValueChanging() && player != null) {
                controls.seek(player, n.intValue());
            }
        });
        posLabel.getStyleClass().add("position");
        HBox.setHgrow(slider, Priority.ALWAYS);
        HBox controls = new HBox(10, playBtn, keyBtn, slider, posLabel, speedBox);
        controls.getStyleClass().add("player-controls");
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox bottom = new VBox(0, narration, controls);
        bottom.getStyleClass().add("player-bottom");

        setTop(legend());
        setCenter(scroll);
        setBottom(bottom);
        timer.start();
    }

    /** Chú thích màu — spec §7.4: màn hình luôn có chú thích màu. */
    private static FlowPane legend() {
        FlowPane box = new FlowPane(14, 4);
        box.getStyleClass().add("legend");
        for (VizPalette.Entry e : VizPalette.LEGEND) {
            Rectangle sw = new Rectangle(11, 11, e.color());
            sw.setArcWidth(4);
            sw.setArcHeight(4);
            Label l = new Label(e.label(), sw);
            l.setGraphicTextGap(6);
            l.getStyleClass().add("legend-item");
            box.getChildren().add(l);
        }
        return box;
    }

    private void setNarration(String s) {
        boolean blank = s == null || s.isBlank();
        narration.setText(blank ? "" : s);
        narration.setVisible(!blank); // vẫn giữ chỗ để khung không nhảy lên xuống
    }

    /** Nạp trace mới cùng các bộ vẽ (khoá = tên biến). */
    public Player load(Trace trace, Map<String, View> views) {
        stage.getChildren().clear();
        for (View v : views.values()) {
            if (v.node() != null) {
                stage.getChildren().add(v.node());
            }
        }
        player = new Player(trace, views, System::currentTimeMillis);
        player.onNarration(s -> {
            narration.getStyleClass().remove("warn");
            setNarration(s);
        });
        player.onPosition(p -> {
            updatingSlider = true;
            slider.setMax(Math.max(1, trace.size() - 1));
            slider.setValue(Math.max(0, p));
            updatingSlider = false;
            posLabel.setText((p + 1) + " / " + trace.size());
        });
        narration.getStyleClass().remove("warn");
        if (trace.truncated()) {
            narration.getStyleClass().add("warn");
            setNarration("Trace bị cắt: code có thể lặp vô hạn hoặc chậm hơn mức cần thiết.");
        } else {
            setNarration("");
        }
        player.seek(-1);
        return player;
    }

    public Player player() {
        return player;
    }

    /** Đổi việc các nút làm (chiếu, theo giáo viên); null là điều khiển trình phát trên máy này. */
    public void setControls(PlayerControls c) {
        this.controls = c == null ? PlayerControls.LOCAL : c;
    }
}
