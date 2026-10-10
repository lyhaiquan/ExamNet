package labcast.client.player;

import java.util.Map;
import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import labcast.common.trace.Trace;

/**
 * Khung trình phát: vùng vẽ các bộ vẽ, dòng thuyết minh, nút Phát/Dừng, thanh tua, tốc độ, nút "tới
 * bước quan trọng". Chỉ vẽ — mọi logic nằm ở {@link Player}.
 */
public final class PlayerPane extends BorderPane {
    private final VBox stage = new VBox(16);
    private final Label narration = new Label(" ");
    private final Button playBtn = new Button("▶ Phát");
    private final Slider slider = new Slider(0, 1, 0);
    private final Label posLabel = new Label("0 / 0");
    private final ComboBox<String> speedBox =
            new ComboBox<>(FXCollections.observableArrayList("0,5×", "1×", "2×", "4×"));
    private Player player;
    private boolean updatingSlider;

    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (player != null) {
                player.tick();
                playBtn.setText(player.playing() ? "⏸ Dừng" : "▶ Phát");
            }
        }
    };

    public PlayerPane() {
        stage.setPadding(new Insets(12));
        stage.setAlignment(Pos.TOP_LEFT);
        narration.setWrapText(true);
        narration.setStyle("-fx-font-size: 14px; -fx-text-fill: #1f4e79;");

        Button keyBtn = new Button("⏭ Bước quan trọng");
        keyBtn.setOnAction(e -> {
            if (player != null) {
                player.nextKeyStep();
            }
        });
        playBtn.setOnAction(e -> {
            if (player == null) {
                return;
            }
            if (player.playing()) {
                player.pause();
            } else {
                player.play();
            }
        });
        speedBox.getSelectionModel().select(1);
        speedBox.setOnAction(e -> {
            if (player != null) {
                player.speed(
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
                player.seek((int) slider.getValue());
            }
        });
        slider.valueProperty().addListener((obs, o, n) -> {
            if (!updatingSlider && !slider.isValueChanging() && player != null) {
                player.seek(n.intValue());
            }
        });
        HBox.setHgrow(slider, Priority.ALWAYS);
        HBox controls = new HBox(8, playBtn, keyBtn, slider, posLabel, speedBox);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(8));

        setCenter(stage);
        setBottom(new VBox(4, narration, controls));
        timer.start();
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
        player.onNarration(s -> narration.setText(s));
        player.onPosition(p -> {
            updatingSlider = true;
            slider.setMax(Math.max(1, trace.size() - 1));
            slider.setValue(Math.max(0, p));
            updatingSlider = false;
            posLabel.setText((p + 1) + " / " + trace.size());
        });
        narration.setText(
                trace.truncated() ? "Trace bị cắt: code có thể lặp vô hạn hoặc chậm hơn mức cần thiết." : " ");
        player.seek(-1);
        return player;
    }

    public Player player() {
        return player;
    }
}
