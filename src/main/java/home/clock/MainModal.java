package home.clock;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@Slf4j
public class MainModal extends Application {
    /** Standard Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    /** Window width in pixels. */
    private static final double WIDTH = 60;
    /** Window height in pixels. */
    private static final double HEIGHT = 20;

    /**
     * Main entry point for application - example of modal window.
     * 
     * @param args
     */
    public static void main(final String... args) {
        if (log.isDebugEnabled()) {
            log.debug("Starting application...");
        }
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
        stage.setScene(setUpScene());
        stage.showAndWait();
    }

    private Scene setUpScene() {
        final Scene scene = new Scene(new DigitalClock(), WIDTH, HEIGHT);
        scene.setOnMouseClicked(event -> {
            if (event.getTarget() instanceof Text && LOGGER.isInfoEnabled()) {
                LOGGER.info("Event time registered:\n{}", Text.class.cast(event.getTarget()).getText());
            }
        });
        return scene;
    }

}
