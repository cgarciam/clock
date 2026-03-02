package home.clock;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

// https://stackoverflow.com/questions/13227809/displaying-changing-values-in-javafx-label NOPMD
/**
 * Creates a digital clock display as a simple label. Format of the clock
 * display is hh:mm:ss aa, where: hh Hour in am/pm (1-12) mm Minute in hour ss
 * Second in minute aa AM/PM marker. Time is the system time for the local.
 * timezone.
 */
public class DigitalClock extends Label {
    /** Format string for date in clock: dd-MM-yyyy */
    protected static final String DATE_PATTERN = "dd-MM-yyyy";
    /** Time format pattern: HH:mm:ss (24-hour). */
    protected static final String TIME_PATTERN = "HH:mm:ss";
    /** Font family for text in UI. */
    protected static final String FONT_FAMILY = "Georgia";
    /** Font size for the clock label. */
    protected static final double FONT_SIZE = 14;

    /** Pre-built formatters — DateTimeFormatter is thread-safe and immutable. */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    /** Pre-built font — Font is immutable, no need to recreate it every second. */
    private static final Font CLOCK_FONT = Font.font(FONT_FAMILY, FontWeight.BOLD, FONT_SIZE);

    /**
     * Launches event handlers.
     */
    public DigitalClock() {
        super();
        setAlignment(Pos.CENTER);
        setFont(CLOCK_FONT);
        bindToTime();
    }

    // the digital clock updates once a second.
    private void bindToTime() {
        final Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(0), actionEvent -> updateText()),
                new KeyFrame(Duration.seconds(1)));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void updateText() {
        final LocalDateTime current = LocalDateTime.now();
        setText(current.format(TIME_FORMATTER) + "  " + current.format(DATE_FORMATTER));
    }

}