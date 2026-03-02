package home.clock;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.OptionalLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * Maintains a running offset between the local JVM clock and true UTC using a
 * <strong>three-level fallback chain</strong>:
 *
 * <ol>
 *   <li><b>NTP UDP</b> ({@code pool.ntp.org:123}) — most precise (±1–50 ms),
 *       but UDP/123 is often blocked by firewalls.</li>
 *   <li><b>worldtimeapi.org HTTPS</b> — plain JSON over port 443, widely
 *       reachable. Precision ±200 ms (limited by HTTP round-trip).</li>
 *   <li><b>timeapi.io HTTPS</b> — second HTTP fallback in case the first is
 *       down or rate-limited.</li>
 *   <li><b>Local clock</b> — offset stays {@code 0}; the clock still works,
 *       it just loses NTP correction.</li>
 * </ol>
 *
 * <p>{@link #getAdjustedNow()} = {@code System.currentTimeMillis() + offsetMillis}
 * — no I/O on the JavaFX thread, safe to call every second.</p>
 *
 * @author César García Mauricio &amp; GitHub Copilot.
 */
@Slf4j
//@SuppressWarnings({ "PMD.LongVariable", "PMD.CommentSize", "PMD.OnlyOneReturn", "PMD.DoNotUseThreads" })
public class NtpSyncService {

    // ── NTP constants ──────────────────────────────────────────────────────────
    /** NTP server hostname. */
    private static final String NTP_HOST     = "pool.ntp.org";
    /** NTP server port. */
    private static final int    NTP_PORT     = 123;
    /** Timeout for NTP UDP requests in seconds. */
    private static final Duration NTP_TIMEOUT_MS = Duration.ofSeconds(3);

    // ── HTTP fallback endpoints ────────────────────────────────────────────────
    /** Returns JSON with field {@code "unixtime": <epoch seconds>}. */
    private static final String HTTP_URL_1 =
            "https://worldtimeapi.org/api/timezone/Etc/UTC";
    /** Returns JSON with field {@code "currentDateTime": "yyyy-MM-ddTHH:mm:ssZ"}. */
    private static final String HTTP_URL_2 =
            "https://timeapi.io/api/time/current/zone?timeZone=UTC";
    /** Timeout for HTTP requests. */
    private static final Duration HTTP_TIMEOUT   = Duration.ofSeconds(5);

    // ── Patterns for JSON parsing (no extra dependency needed) ─────────────────
    /** Matches {@code "unixtime":1234567890} in worldtimeapi response. */
    private static final Pattern UNIX_TIME_PATTERN =
            Pattern.compile("\"unixtime\"\\s*:\\s*(\\d+)");
    /** Matches {@code "dateTime":"2026-03-02T12:00:00..."} in timeapi.io response. */
    private static final Pattern DATE_TIME_PATTERN =
            Pattern.compile("\"dateTime\"\\s*:\\s*\"(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})");

    /** How often the background thread re-syncs. */
    private static final long RESYNC_INTERVAL_MINUTES = 30L;

    // ── State ──────────────────────────────────────────────────────────────────
    /**
     * Cached offset in ms: {@code trueTime − System.currentTimeMillis()}.
     * Written by the scheduler thread, read by the JavaFX thread.
     */
    private final AtomicLong offsetMillis = new AtomicLong(0L);
    /**
     * HTTP client shared by both HTTP fallbacks. Configured with a reasonable
     * timeout to avoid hanging the scheduler thread.
     */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();
    /** Background scheduler for periodic re-syncs. Runs as a daemon thread. */
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                final Thread thread = new Thread(r, "time-sync");
                thread.setDaemon(true);
                return thread;
            });

    // ── Constructor ────────────────────────────────────────────────────────────

    /**
     * Creates the service, runs an immediate sync, and schedules periodic
     * re-syncs every {@value #RESYNC_INTERVAL_MINUTES} minutes.
     */
    public NtpSyncService() {
        syncNow();
        scheduler.scheduleAtFixedRate(
                this::syncNow,
                RESYNC_INTERVAL_MINUTES,
                RESYNC_INTERVAL_MINUTES,
                TimeUnit.MINUTES);
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Returns the current time corrected by the last known offset.
     * No I/O — safe to call every second from the JavaFX animation thread.
     *
     * @return offset-corrected {@link LocalDateTime} in the system default zone
     */
    public LocalDateTime getAdjustedNow() {
        final long adjusted = System.currentTimeMillis() + offsetMillis.get();
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(adjusted), ZoneId.systemDefault());
    }

    /**
     * Returns the last computed offset in milliseconds
     * ({@code trueTime − localTime}). Positive = local clock is behind.
     *
     * @return offset in milliseconds
     */
    public long getOffsetMillis() {
        return offsetMillis.get();
    }

    /** Shuts down the background scheduler. Call when the application closes. */
    public void shutdown() {
        scheduler.shutdown();
    }

    // ── Sync chain ─────────────────────────────────────────────────────────────

    /**
     * Tries each time source in order. Stops as soon as one succeeds.
     * All failures are logged at WARN level; the winning source is logged at
     * DEBUG level.
     */
    private void syncNow() {
        if (tryNtpUdp())    { return; }
        if (tryHttp(HTTP_URL_1, "worldtimeapi.org", this::parseWorldTimeApi)) { return; }
        if (tryHttp(HTTP_URL_2, "timeapi.io",       this::parseTimeApiIo))    { return; }
        log.atWarn().addArgument(offsetMillis.get())
                .log("All time sources failed — using local clock (offset unchanged at {} ms)");
    }

    // ── Source 1: NTP UDP ──────────────────────────────────────────────────────

    private boolean tryNtpUdp() {
        try (NTPUDPClient client = new NTPUDPClient()) {
            client.setDefaultTimeout(NTP_TIMEOUT_MS);
            client.open();
            final InetAddress address = InetAddress.getByName(NTP_HOST);
            final TimeInfo info = client.getTime(address, NTP_PORT);
            info.computeDetails();
            applyOffset(info.getOffset(), "NTP UDP (" + NTP_HOST + ")");
            return true;
        } catch (final Exception ex) { // NOPMD.AvoidCatchingGenericException: We want to catch all exceptions from this source and fall back to HTTP.
            log.atWarn().setCause(ex).addArgument(NTP_HOST).addArgument(NTP_PORT)
                    .log("NTP UDP failed ({}:{}) — trying HTTP fallback");
            return false;
        }
    }

    // ── Source 2 & 3: HTTPS ────────────────────────────────────────────────────
    /** Functional interface for parsing HTTP response bodies. */
    @FunctionalInterface
    private interface BodyParser {
        /** Parses the response body and returns epoch milliseconds, or empty on failure. */
        OptionalLong parse(String body);
    }

    private boolean tryHttp(final String url, final String sourceName,
                             final BodyParser parser) {
        try {
            final long beforeMillis = System.currentTimeMillis();
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(HTTP_TIMEOUT)
                    .GET()
                    .build();
            final HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            final long afterMillis = System.currentTimeMillis();

            if (response.statusCode() != 200) { // NOPMD.AvoidLiteralsInIfCondition: HTTP status code.
                log.atWarn().addArgument(sourceName).addArgument(response.statusCode())
                        .log("{} returned HTTP {} — skipping", sourceName, response.statusCode());
                return false;
            }

            final OptionalLong epochMillis = parser.parse(response.body());
            if (epochMillis.isEmpty()) {
                log.atWarn().addArgument(sourceName).addArgument(response.body())
                        .log("{} — could not parse response body: {}");
                return false;
            }

            // Correct for ~half the round-trip time so we land close to the
            // true server time rather than the stale value from before the trip.
            final long halfRtt     = (afterMillis - beforeMillis) / 2;
            final long trueMillis  = epochMillis.getAsLong() + halfRtt;
            final long localMillis = (beforeMillis + afterMillis) / 2;
            applyOffset(trueMillis - localMillis, sourceName);
            return true;

        } catch (final Exception ex) { // NOSONAR S2142 NOPMD.AvoidCatchingGenericException: We want to catch all exceptions from this source and fall back to the next one.
            log.atWarn().setCause(ex).addArgument(sourceName)
                    .log("{} failed — trying next fallback", sourceName);
            return false;
        }
    }

    // ── JSON parsers ───────────────────────────────────────────────────────────

    /** Parses {@code "unixtime": 1234567890} → epoch millis. */
    private OptionalLong parseWorldTimeApi(final String body) {
        final Matcher matcher = UNIX_TIME_PATTERN.matcher(body);
        if (!matcher.find()) { return OptionalLong.empty(); }
        return OptionalLong.of(Long.parseLong(matcher.group(1)) * 1_000L);
    }

    /** Parses {@code "dateTime": "2026-03-02T12:00:00..."} → epoch millis. */
    private OptionalLong parseTimeApiIo(final String body) {
        final Matcher matcher = DATE_TIME_PATTERN.matcher(body);
        if (!matcher.find())  { return OptionalLong.empty(); }
        // Pattern captures up to seconds; parse as UTC instant.
        final String raw = matcher.group(1) + "Z";  // e.g. "2026-03-02T12:00:00Z"
        return OptionalLong.of(Instant.parse(raw).toEpochMilli());
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private void applyOffset(final long offset, final String source) {
        offsetMillis.set(offset);
        log.debug("Time sync OK via {} — offset: {} ms", source, offset);
    }

}