package home.clock;

import java.util.Calendar;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.util.Duration;

// https://stackoverflow.com/questions/13227809/displaying-changing-values-in-javafx-label
/**
 * Creates a digital clock display as a simple label. Format of the clock
 * display is hh:mm:ss aa, where: hh Hour in am/pm (1-12) mm Minute in hour ss
 * Second in minute aa AM/PM marker. Time is the system time for the local.
 * timezone.
 */
@SuppressWarnings({ "PMD.LawOfDemeter", "PMD.AddEmptyString" })
public class DigitalClock extends Label {

    /**
     * Launches event handlers.
     */
    public DigitalClock() {
        super();
        bindToTime();
    }

    // the digital clock updates once a second.
    private void bindToTime() {
        final Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(0), new EventHandler<ActionEvent>() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void handle(final ActionEvent actionEvent) {
                final Calendar time = Calendar.getInstance();
                final String hourString = StringUtilities.pad(2, ' ', time.get(Calendar.HOUR) == 0 ? "12" : time.get(Calendar.HOUR) + "");
                final String minuteString = StringUtilities.pad(2, '0', time.get(Calendar.MINUTE) + "");
                final String secondString = StringUtilities.pad(2, '0', time.get(Calendar.SECOND) + "");
                final String ampmString = time.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
                setText(hourString + ":" + minuteString + ":" + secondString + " " + ampmString);
            }
        }), new KeyFrame(Duration.seconds(1)));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

}

/**
 * Miscellaneous utilities.
 * 
 * @author CGM
 *
 */
final class StringUtilities {

    private StringUtilities() {
        // To PMD.
    }

    /**
     * Creates a string left padded to the specified width with the supplied padding
     * character.
     * 
     * @param fieldWidth
     *            the length of the resultant padded string.
     * @param padChar
     *            a character to use for padding the string.
     * @param text
     *            the string to be padded.
     * @return the padded string.
     */
    public static String pad(final int fieldWidth, final char padChar, final String text) {
        final StringBuilder strPadded = new StringBuilder();
        for (int i = text.length(); i < fieldWidth; i++) {
            strPadded.append(padChar);
        }
        strPadded.append(text);

        return strPadded.toString();
    }

}
