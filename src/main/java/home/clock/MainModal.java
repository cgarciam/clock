package home.clock;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * 
 * @author cesar CGM
 *
 */
// @Sll4j
public class MainModal extends Application {
    /** Standard logger. */
    private static final Logger LOGGER;

    static {
        LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    }

    /**
     * Main entry point for application - example of modal window.
     * 
     * @param args
     */
    public static void main(final String... args) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("HW!");
        }
        launch(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Stage primaryStage) {

        primaryStage.setAlwaysOnTop(true);

        final Stage dialog = new Stage();

        dialog.initOwner(primaryStage);
        dialog.setTitle("My Clock");
        dialog.setAlwaysOnTop(true);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.showAndWait();
    }

}
