package home.clock;

import javafx.application.Application;
import javafx.scene.Scene;
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

        final Stage dialog = new Stage();

        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.initOwner(primaryStage);
        dialog.setAlwaysOnTop(true);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setScene(new Scene(new DigitalClock(), 60, 20));
        dialog.showAndWait();
    }

}
