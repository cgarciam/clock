package home.clock;

import java.util.Calendar;

import org.apache.commons.lang3.StringUtils;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
                final String hourString   = StringUtils.leftPad(time.get(Calendar.HOUR) == 0 ? "12" : time.get(Calendar.HOUR) + "", 2, ' ');
                final String minuteString = StringUtils.leftPad(time.get(Calendar.MINUTE) + "", 2, '0');
                final String secondString = StringUtils.leftPad(time.get(Calendar.SECOND) + "", 2, '0');
                final String ampmString = time.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
                setText(hourString + ":" + minuteString + ":" + secondString + " " + ampmString);
            }
        }), new KeyFrame(Duration.seconds(1)));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

}
