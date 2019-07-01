package home.clock;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author cesar CGM
 *
 */
@SuppressWarnings({ "PMD.AtLeastOneConstructor", "PMD.DataflowAnomalyAnalysis" })
@Slf4j
public class MainModal extends Application {
    /** Window width in pixels. */
    private static final double WIDTH = 150;
    /** Window height in pixels. */
    private static final double HEIGHT = 20;

    /**
     * Main entry point for application - example of modal window.
     * 
     * @param args
     */
    public static void main(final String... args) {
        log.debug("Starting application...");
        launch(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Stage primaryStage) {
        primaryStage.setAlwaysOnTop(true);
        primaryStage.initStyle(StageStyle.UNDECORATED);

        final Stage stage = new Stage();

        stage.initStyle(StageStyle.UNDECORATED);
        stage.initOwner(primaryStage);
        stage.setAlwaysOnTop(true);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setScene(setupScene());
        stage.showAndWait();
    }

    private Scene setupScene() {
        final Scene scene = new Scene(new DigitalClock(), WIDTH, HEIGHT);
        scene.setOnMouseClicked(event -> {
            if (event.getTarget() instanceof Text) {
                log.info("Event time registered:\n{}", Text.class.cast(event.getTarget()).getText());
            }
        });
        return scene;
    }

}
