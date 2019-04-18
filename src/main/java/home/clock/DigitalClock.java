package home.clock;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.util.Duration;

// https://stackoverflow.com/questions/13227809/displaying-changing-values-in-javafx-label NOPMD
/**
 * Creates a digital clock display as a simple label. Format of the clock
 * display is hh:mm:ss aa, where: hh Hour in am/pm (1-12) mm Minute in hour ss
 * Second in minute aa AM/PM marker. Time is the system time for the local.
 * timezone.
 */
@SuppressWarnings({ "PMD.LawOfDemeter", "PMD.AddEmptyString" })
public class DigitalClock extends Label {
    /** Format string for date in clock: dd-MM-yyyy */
    protected static final String FORMATTER;

    static {
        FORMATTER = "dd-MM-yyyy";
    }

    /**
     * Launches event handlers.
     */
    public DigitalClock() {
        super();
        setAlignment(Pos.CENTER);
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
                final DateTime current = new DateTime();
                final String hour =   StringUtils.leftPad(current.getHourOfDay() == 0 ? "12" : current.getHourOfDay() + "", 2, '0');
                final String minute = StringUtils.leftPad(current.getMinuteOfHour() + "", 2, '0');
                final String second = StringUtils.leftPad(current.getSecondOfMinute() + "", 2, '0');
                final String date = current.toString(FORMATTER);
                setText(hour + ":" + minute + ":" + second + " " + date);
            }
        }), new KeyFrame(Duration.seconds(1)));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

}
