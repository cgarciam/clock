package home.clock;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;

/**
 * @author CGM
 *
 */
@SuppressWarnings({ /* "restriction", */ "PMD.LawOfDemeter", "PMD.CommentRequired" })
@Slf4j
public class AppMainClock extends Application {
    private static final double UNIT = 100;
    private final transient Clockwork clockwork = new Clockwork();

    /**
     * main access point.
     * 
     * @param args
     */
    public static void main(final String... args) {
        if (log.isDebugEnabled()) {
            log.debug("Hello World!");
        }
        launch(args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Stage stage) {
        final Group root = new Group();

        root.getChildren().add(outerRim());
        root.getChildren().add(minuteHand());
        root.getChildren().add(hourHand());
        root.getChildren().add(secondsHand());
        root.getChildren().add(tickMarks());
        root.getChildren().add(centerPoint());

        setUpMouseForScaleAndMove(stage, root);
        final Scene scene = makeATransparentScene(root);
        makeATransparentStage(stage, scene);
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private Node outerRim() {
        final Stop[] stops = new Stop[4];

        stops[0] = new Stop(0.8, Color.WHITE);
        stops[1] = new Stop(0.9, Color.BLACK);
        stops[2] = new Stop(0.95, Color.WHITE);
        stops[3] = new Stop(1.0, Color.BLACK);

        final double focusAngle = 0;
        final double focusDistance = 0;
        final Paint fill = new RadialGradient(focusAngle, focusDistance, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                stops);

        final Circle circle = new Circle();

        circle.setFill(fill);
        circle.setRadius(UNIT);
        circle.setCenterX(UNIT);
        circle.setCenterY(UNIT);

        return circle;
    }

    private Node tickMarks() {
        final Group tickMarkGroup = new Group();
        for (int n = 0; n < 12; n++) {
            tickMarkGroup.getChildren().add(tickMark(n));
        }
        return tickMarkGroup;
    }

    private Node tickMark(final int nMark) {
        final Line line = new Line(UNIT, UNIT * 0.12, UNIT, UNIT * (nMark % 3 == 0 ? 0.3 : 0.2));
        final Rotate rotate = new Rotate();
        rotate.setPivotX(UNIT);
        rotate.setPivotY(UNIT);
        rotate.setAngle(360 / 12 * nMark);
        line.getTransforms().add(rotate);
        line.setStrokeWidth(2);
        return line;
    }

    private Node centerPoint() {
        final Circle center = new Circle();

        center.setFill(Color.BLACK);
        center.setRadius(0.05 * UNIT);
        center.setCenterX(UNIT);
        center.setCenterY(UNIT);

        return center;
    }

    private void setUpMouseForScaleAndMove(final Stage stage, final Parent root) {
        root.onMouseDraggedProperty().set(moveWhenDragging(stage));
        root.onScrollProperty().set(scaleWhenScrolling(stage, root));
    }

    private EventHandler<MouseEvent> moveWhenDragging(final Stage stage) {
        return new EventHandler<MouseEvent>() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void handle(final MouseEvent mouseEvent) {
                stage.setX(mouseEvent.getScreenX() - stage.getWidth() / 2);
                stage.setY(mouseEvent.getScreenY() - stage.getHeight() / 2);
            }
        };
    }

    private EventHandler<ScrollEvent> scaleWhenScrolling(final Stage stage, final Parent root) {
        return new EventHandler<ScrollEvent>() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void handle(final ScrollEvent scrollEvent) {
                final double scroll = scrollEvent.getDeltaY();
                root.setScaleX(root.getScaleX() + scroll / 100);
                root.setScaleY(root.getScaleY() + scroll / 100);
                root.setTranslateX(root.getTranslateX() + scroll);
                root.setTranslateY(root.getTranslateY() + scroll);
                stage.sizeToScene();
            }
        };
    }

    private Scene makeATransparentScene(final Parent root) {
        final Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        return scene;
    }

    private void makeATransparentStage(final Stage stage, final Scene scene) {
        stage.setScene(scene);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.show();
    }

    private Node hourHand() {
        final Rotate rotate = rotationAroundCenter();
        rotate.angleProperty().bind(clockwork.hour.multiply(360 / 12));
        return hand(UNIT * 0.4, Color.BLACK, rotate);
    }

    private Node minuteHand() {
        final Rotate rotate = rotationAroundCenter();
        rotate.angleProperty().bind(clockwork.minute.multiply(360 / 60));
        return hand(UNIT * 0.2, Color.BLACK, rotate);
    }

    private Node secondsHand() {
        final Rotate rotate = rotationAroundCenter();
        rotate.angleProperty().bind(clockwork.second.multiply(360 / 60));
        final Line line = new Line(UNIT, UNIT * 1.1, UNIT, UNIT * 0.2);
        line.getTransforms().add(rotate);
        return line;
    }

    private Rotate rotationAroundCenter() {
        final Rotate rotate = new Rotate();

        rotate.setPivotX(UNIT);
        rotate.setPivotY(UNIT);

        return rotate;
    }

    private Node hand(final double stretchRelativeToRim, final Color color, final Rotate rotate) {
        final Path hand = new Path();

        hand.setFill(color);
        hand.setStroke(Color.TRANSPARENT);
        hand.getElements().add(new MoveTo(UNIT, UNIT));
        hand.getElements().add(new LineTo(UNIT * 0.9, UNIT * 0.9));
        hand.getElements().add(new LineTo(UNIT, stretchRelativeToRim));
        hand.getElements().add(new LineTo(UNIT * 1.1, UNIT * 0.9));
        hand.getElements().add(new LineTo(UNIT, UNIT));

        hand.getTransforms().add(rotate);

        return hand;
    }

}
