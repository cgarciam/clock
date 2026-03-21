package home.clock;

import java.util.Calendar;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

/**
 * 
 * @author CGM
 *
 */
//@SuppressWarnings({ "PMD.LawOfDemeter", "PMD.CommentRequired" })
public class Clockwork {

    public final SimpleIntegerProperty hour = new SimpleIntegerProperty(0);
    public final SimpleIntegerProperty minute = new SimpleIntegerProperty(0);
    public final SimpleIntegerProperty second = new SimpleIntegerProperty(0);

    public Clockwork() {
        startTicking();
    }

    private void startTicking() {
        final Timeline timeline = new Timeline();
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1), updateTime()));
        timeline.play();
    }

    private EventHandler<ActionEvent> updateTime() {
        return eventHandler -> {
            final Calendar calendar = Calendar.getInstance(); // TODO NOPMD.ReplaceJavaUtilCalendar
            hour.set(calendar.get(Calendar.HOUR));
            minute.set(calendar.get(Calendar.MINUTE));
            second.set(calendar.get(Calendar.SECOND));
        };
    }

}
