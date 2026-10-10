package labcast.client.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public final class LabCastApp extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("LabCast");
        stage.setScene(new Scene(new Label("LabCast — giai đoạn 0"), 480, 240));
        stage.show();
    }
}
