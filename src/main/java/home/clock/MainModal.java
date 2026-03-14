package home.clock;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
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
    /** Default window width in pixels (used when {@code --width} is not supplied). */
    private static final double DEFAULT_WIDTH = DigitalClock.DEFAULT_WIDTH;
    /** Default window height in pixels (used when {@code --height} is not supplied). */
    private static final double DEFAULT_HEIGHT = DigitalClock.DEFAULT_HEIGHT;
    /** Default font size in points (used when {@code --fontSize} is not supplied). */
    private static final double DEFAULT_FONT_SIZE = DigitalClock.DEFAULT_FONT_SIZE;
    /**
     * Interval in seconds between periodic always-on-top re-assertions.
     * The OS window manager may silently demote the window from the topmost
     * z-order; toggling the property forces a re-evaluation.
     */
    private static final int ALWAYS_ON_TOP_REASSERT_SECONDS = 5; // NOPMD.LongVariable

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
        final double width  = parseParam("width",  DEFAULT_WIDTH);
        final double height = parseParam("height", DEFAULT_HEIGHT);
        final double fontSize = parseParam("fontSize", DEFAULT_FONT_SIZE);

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
        clockStage.setScene(setupScene(width, height, fontSize));
        clockStage.setOnCloseRequest(event -> primaryStage.close());

        enforceAlwaysOnTop(primaryStage, clockStage);

        clockStage.showAndWait();
    }

    /**
     * Reads a named parameter (e.g. {@code --width=200}) from the JavaFX
     * {@link javafx.application.Application.Parameters} and parses it as a
     * positive {@code double}. Falls back to {@code defaultValue} when the
     * parameter is absent or cannot be parsed.
     *
     * @param name         parameter name (without the {@code --} prefix)
     * @param defaultValue value to use when the parameter is missing or invalid
     * @return the parsed value, or {@code defaultValue}
     */
//    @SuppressWarnings("PMD.OnlyOneReturn")
    private double parseParam(final String name, final double defaultValue) {
        final String raw = getParameters().getNamed().get(name);
        if (raw == null) {
            return defaultValue;
        }
        try {
            final double value = Double.parseDouble(raw);
            if (value > 0) {
                return value;
            }
            log.warn("Parameter --{}={} is not positive; using default {}", name, raw, defaultValue);
        } catch (final NumberFormatException ex) {
            log.warn("Parameter --{}={} is not a valid number; using default {}", name, raw, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Builds the clock scene sized to the given rectangle.
     *
     * @param width  scene width in pixels
     * @param height scene height in pixels
     * @param fontSize font size in points
     * @return the configured {@link Scene}
     */
    private Scene setupScene(final double width, final double height, final double fontSize) {
        final DigitalClock clock = new DigitalClock(width, height, fontSize);
        // Semi-transparent background with rounded corners.
        // The scene fill must be TRANSPARENT so the OS compositing reveals the
        // rounded shape; the visible background is painted entirely by CSS.
        clock.setStyle(
                "-fx-background-color: rgba(0,0,0,0.55);"
                + "-fx-background-radius: 8;"
                + "-fx-padding: 4 10 4 10;"
                + "-fx-text-fill: #e8e8e8;");

        final Scene scene = new Scene(clock, width, height);
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

    /**
     * Sets up two mechanisms to keep both stages above all other windows:
     * <ol>
     *   <li>A {@link Timeline} that periodically toggles {@code alwaysOnTop}
     *       off and back on, forcing the OS to re-evaluate the z-order.</li>
     *   <li>A focus-change listener on the clock stage that immediately
     *       re-asserts topmost status when focus is lost (e.g. user clicks
     *       another application).</li>
     * </ol>
     *
     * @param primaryStage the hidden owner stage
     * @param clockStage   the visible clock stage
     */
    private void enforceAlwaysOnTop(final Stage primaryStage,
                                     final Stage clockStage) {
        // --- Periodic re-assertion via Timeline ---
        final Timeline topEnforcer = new Timeline(
                new KeyFrame(Duration.seconds(ALWAYS_ON_TOP_REASSERT_SECONDS), event -> {
                    reassertAlwaysOnTop(primaryStage);
                    reassertAlwaysOnTop(clockStage);
                })
        );
        topEnforcer.setCycleCount(Animation.INDEFINITE);
        topEnforcer.play();

        // --- Immediate re-assertion when focus is lost ---
        clockStage.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (Boolean.FALSE.equals(isNowFocused)) {
                log.debug("Clock stage lost focus — re-asserting alwaysOnTop");
                reassertAlwaysOnTop(primaryStage);
                reassertAlwaysOnTop(clockStage);
            }
        });
    }

    /**
     * Toggles {@code alwaysOnTop} off then on for the given stage.
     * This two-step switch forces the underlying platform window manager
     * (e.g. Windows' {@code SetWindowPos(HWND_TOPMOST, …)}) to re-apply
     * the topmost flag even if it was silently cleared.
     *
     * @param stage the stage whose topmost status should be refreshed
     */
    private static void reassertAlwaysOnTop(final Stage stage) {
        stage.setAlwaysOnTop(false);
        stage.setAlwaysOnTop(true);
    }

}
