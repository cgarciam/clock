package home.clock;

/**
 * Plain (non-JavaFX) entry point that delegates to {@link MainModal}.
 * <p>
 * When the application JARs live on the <em>classpath</em> rather than the
 * module path, the JVM refuses to launch a class that directly extends
 * {@link javafx.application.Application}.  This thin launcher avoids that
 * restriction by keeping the JavaFX type reference one call-frame away.
 * </p>
 */
public class Launcher {

    private Launcher() { }

    /**
     * Main entry point.
     *
     * @param args command-line arguments forwarded to JavaFX
     */
    public static void main(final String... args) {
        MainModal.main(args);
    }
}
