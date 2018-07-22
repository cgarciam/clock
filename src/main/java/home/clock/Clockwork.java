package home.clock;

import java.util.Calendar;

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
@SuppressWarnings({ /* "restriction", */ "PMD.LawOfDemeter", "PMD.CommentRequired" })
public class Clockwork {

    public final transient SimpleIntegerProperty hour = new SimpleIntegerProperty(0);
    public final transient SimpleIntegerProperty minute = new SimpleIntegerProperty(0);
    public final transient SimpleIntegerProperty second = new SimpleIntegerProperty(0);

    public Clockwork() {
        startTicking();
    }

    private void startTicking() {
        final Timeline timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1), updateTime()));
        timeline.play();
    }

    private EventHandler<ActionEvent> updateTime() {
        return EventHandler -> {
            final Calendar calendar = Calendar.getInstance();
            hour.set(calendar.get(Calendar.HOUR));
            minute.set(calendar.get(Calendar.MINUTE));
            second.set(calendar.get(Calendar.SECOND));
        };
    }

}
