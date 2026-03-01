package home.clock;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;

/**
 * A modal digital clock window that stays always-on-top.
 * <p>
 * The clock is displayed in an undecorated, modal stage owned by a hidden
 * primary stage. Clicking on the time text logs the current displayed time.
 * </p>
 *
 * @author César García Mauricio & GitHub Copilot (enhanced with Claude Sonnet 4.6)
 */
// The comment size is justified by the complexity of the setup.
//@SuppressWarnings({ "PMD.CommentSize" })
@Slf4j
public class MainModal extends Application { // NOPMD.AtLeastOneConstructor
    /** Minimum window width in pixels. */
    private static final double MIN_WIDTH = 100;
    /** Minimum window height in pixels. */
    private static final double MIN_HEIGHT = 25;

    /**
     * Main entry point for the modal clock application.
     *
     * @param args command-line arguments forwarded to JavaFX
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
        // The primary stage must have a scene; without one some platforms
        // refuse to show any owned stages.
        final Scene hiddenScene = new Scene(new Group(), 1, 1);
        hiddenScene.setFill(Color.TRANSPARENT);
        primaryStage.setScene(hiddenScene);
        primaryStage.setAlwaysOnTop(true);
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setOpacity(0);
        primaryStage.show();

        final Stage clockStage = new Stage();
        clockStage.initStyle(StageStyle.TRANSPARENT);
        clockStage.initOwner(primaryStage);
        clockStage.setAlwaysOnTop(true);
        clockStage.initModality(Modality.WINDOW_MODAL);
        clockStage.setScene(setupScene());
        clockStage.setOnCloseRequest(event -> primaryStage.close());
        clockStage.showAndWait();
    }

    private Scene setupScene() {
        final DigitalClock clock = new DigitalClock();
        // Semi-transparent background with rounded corners.
        // The scene fill must be TRANSPARENT so the OS compositing reveals the
        // rounded shape; the visible background is painted entirely by CSS.
        clock.setStyle(
                "-fx-background-color: rgba(0,0,0,0.55);"
                + "-fx-background-radius: 8;"
                + "-fx-padding: 4 10 4 10;"
                + "-fx-text-fill: #e8e8e8;");

        final Scene scene = new Scene(clock, MIN_WIDTH, MIN_HEIGHT);
        scene.setFill(Color.TRANSPARENT);

        // --- drag-to-reposition ---
        final double[] dragDelta = new double[2];
        scene.setOnMousePressed(event -> {
            dragDelta[0] = scene.getWindow().getX() - event.getScreenX();
            dragDelta[1] = scene.getWindow().getY() - event.getScreenY();
        });
        scene.setOnMouseDragged(event -> {
            scene.getWindow().setX(event.getScreenX() + dragDelta[0]);
            scene.getWindow().setY(event.getScreenY() + dragDelta[1]);
        });

        scene.setOnMouseClicked(event -> {
            if (event.getTarget() instanceof final Text text && log.isInfoEnabled()) {
                log.info("Event time registered:\n{}", text.getText());
            }
        });
        return scene;
    }

}
