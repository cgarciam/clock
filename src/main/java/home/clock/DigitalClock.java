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
import lombok.extern.slf4j.Slf4j;

// https://stackoverflow.com/questions/13227809/displaying-changing-values-in-javafx-label
/**
 * A digital clock displayed as a {@link Label}.
 * <p>
 * The default format is {@code HH:mm:ss} (24-hour). Optionally a date suffix
 * ({@code dd-MM-yyyy}) can be shown by setting {@link #WITH_DATE} to
 * {@code true}.
 * </p>
 * <p>
 * The timeline is aligned to the system clock's second boundary so the
 * display stays accurately synchronised with the OS clock. Time is further
 * corrected by an {@link NtpSyncService} that queries {@code pool.ntp.org}
 * on startup and every 30 minutes, eliminating the local-clock drift that
 * causes a multi-second offset versus a phone's NTP-synced time.
 * </p>
 * <p>
 * The rectangle size can be controlled via the parameterised constructor
 * {@link #DigitalClock(double, double)}. The font size is scaled
 * automatically so the text fills the available height, or set explicitly
 * via {@link #DigitalClock(double, double, double)}.
 * </p>
 *
 * @author César García Mauricio.
 */
@Slf4j
//@SuppressWarnings({ "PMD.LongVariable", "PMD.CommentSize" })
public class DigitalClock extends Label {
    /** Time format pattern: HH:mm:ss (24-hour). */
    protected static final String TIME_PATTERN = "HH:mm:ss";
    /** Time + date format pattern. */
    protected static final String TIME_DATE_PATTERN = "HH:mm:ss  dd-MM-yyyy";
    /** Font family for text in UI. */
    protected static final String FONT_FAMILY = "Georgia";
    /** Default preferred width in pixels. */
    protected static final double DEFAULT_WIDTH = 100;
    /** Default preferred height in pixels. */
    protected static final double DEFAULT_HEIGHT = 25;
    /** Default font size in points. */
    protected static final double DEFAULT_FONT_SIZE = 10;
    /**
     * Fraction of the height reserved for vertical padding inside the label.
     * The remaining fraction is available for the glyph ascent/descent.
     */
    private static final double HEIGHT_TO_FONT_RATIO = 0.65;
    /** Sentinel value meaning "derive font size from height automatically". */
    private static final double FONT_SIZE_AUTO = -1.0;
    /** Whether to append the current date after the time. */
    private static final boolean WITH_DATE = false;

    /**
     * Pre-built formatters — {@link DateTimeFormatter} is thread-safe and
     * immutable, so a single instance is shared across all updates.
     */
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern(TIME_PATTERN);
    /** Formatter for time + date display. */
    private static final DateTimeFormatter TIME_DATE_FORMATTER =
            DateTimeFormatter.ofPattern(TIME_DATE_PATTERN);

    /** Preferred width of the clock rectangle in pixels. */
    private final double preferredWidth;
    /** Preferred height of the clock rectangle in pixels. */
    private final double preferredHeight;

    /**
     * NTP synchronisation service. Queries {@code pool.ntp.org} once on
     * startup and every 30 minutes. {@link #updateDisplay()} reads the cached
     * offset — no blocking I/O on the JavaFX thread.
     */
    private final NtpSyncService ntpSyncService = new NtpSyncService();

    /**
     * Creates the digital clock label with default dimensions
     * ({@value #DEFAULT_WIDTH} × {@value #DEFAULT_HEIGHT} px) and starts
     * the time update loop.
     */
    public DigitalClock() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Creates the digital clock label sized to the given rectangle and starts
     * the time update loop. The font size is derived automatically from
     * {@code height} so the text fills the available vertical space.
     *
     * @param width  preferred width of the clock area in pixels  (must be &gt; 0)
     * @param height preferred height of the clock area in pixels (must be &gt; 0)
     */
    public DigitalClock(final double width, final double height) {
        this(width, height, FONT_SIZE_AUTO);
    }

    /**
     * Creates the digital clock label sized to the given rectangle with an
     * explicit font size, and starts the time update loop.
     *
     * @param width    preferred width of the clock area in pixels  (must be &gt; 0)
     * @param height   preferred height of the clock area in pixels (must be &gt; 0)
     * @param fontSize explicit font size in points (must be &gt; 0); pass
     *                 {@link #FONT_SIZE_AUTO} to derive the size automatically
     *                 from {@code height}
     */
//    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public DigitalClock(final double width, final double height, final double fontSize) {
        super();
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "DigitalClock dimensions must be positive, got: " + width + "×" + height);
        }
        if (fontSize != FONT_SIZE_AUTO && fontSize <= 0) {
            throw new IllegalArgumentException(
                    "DigitalClock fontSize must be positive, got: " + fontSize);
        }
        this.preferredWidth  = width;
        this.preferredHeight = height;

        setPrefSize(preferredWidth, preferredHeight);
        setMinSize(preferredWidth, preferredHeight);
        setMaxSize(preferredWidth, preferredHeight);
        setAlignment(Pos.CENTER);

        // Use explicit font size when provided; otherwise scale from height.
        final double resolvedFontSize = (fontSize == FONT_SIZE_AUTO)
                ? height * HEIGHT_TO_FONT_RATIO
                : fontSize;
        setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, resolvedFontSize));
        log.debug("DigitalClock created: {}×{}px, font size={}", width, height, resolvedFontSize);

        bindToTime();

        // Shut down the NTP background thread when the node leaves the scene.
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                log.debug("DigitalClock removed from scene - shutting down NtpSyncService.");
                ntpSyncService.shutdown();
            }
        });
    }

    /**
     * Starts a {@link Timeline} that updates the label every second,
     * aligned to the system clock's second boundary to prevent drift.
     */
    private void bindToTime() {
        // Initial update so the label is never blank.
        updateDisplay();

        // Calculate millis until the next whole second to align with system clock.
        final long nowMillis = System.currentTimeMillis();
        final long delayMillis = 1000 - (nowMillis % 1000);

        final Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(delayMillis), event -> {
                    updateDisplay();
                    // After the initial alignment, restart on a precise 1-second cadence.
                    startAlignedTimeline();
                })
        );
        timeline.setCycleCount(1);
        timeline.play();
    }

    /**
     * Starts an indefinite 1-second timeline that periodically re-syncs
     * to the system clock to compensate for accumulated drift.
     */
    private void startAlignedTimeline() {
        final Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(0), event -> updateDisplay()),
                new KeyFrame(Duration.seconds(1))
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    /**
     * Reads the NTP-corrected current time and updates the label text.
     * Falls back to the local clock automatically if NTP is unavailable
     * (the offset stays at zero in that case).
     */
    private void updateDisplay() {
        final LocalDateTime now = ntpSyncService.getAdjustedNow(); // NOPMD.LawOfDemeter
        final DateTimeFormatter formatter = WITH_DATE ? TIME_DATE_FORMATTER : TIME_FORMATTER;
        setText(now.format(formatter));
    }

}