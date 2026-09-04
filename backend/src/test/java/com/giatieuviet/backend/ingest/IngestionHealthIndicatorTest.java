package com.giatieuviet.backend.ingest;

import com.giatieuviet.backend.persistence.IngestionRun;
import com.giatieuviet.backend.persistence.IngestionRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs from a fixed clock rather than backdated rows: the run log stamps
 * itself with the moment it is written, so "a day has passed" is expressed by
 * moving the reader's clock forward instead of lying about when the run
 * happened.
 */
@SpringBootTest
@ActiveProfiles("test")
class IngestionHealthIndicatorTest {

    private static final Duration TOLERATED = Duration.ofHours(26);

    @Autowired
    private IngestionRunRepository runs;

    @BeforeEach
    void clearPreviousRuns() {
        runs.deleteAll();
    }

    private IngestionHealthIndicator readingAt(Instant now) {
        return new IngestionHealthIndicator(runs, Clock.fixed(now, ZoneOffset.UTC), TOLERATED);
    }

    private void record(String jobName, String status) {
        runs.save(new IngestionRun(jobName, status, 6, null, Instant.now()));
    }

    private void recordBothJobs(String status) {
        record(PriceIngestionService.JOB_NAME, status);
        record(WeatherIngestionService.JOB_NAME, status);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> detailFor(Health health, String jobName) {
        return (Map<String, Object>) health.getDetails().get(jobName);
    }

    @Test
    void isUpWhileBothJobsKeepSucceeding() {
        recordBothJobs(IngestionRun.SUCCESS);

        Health health = readingAt(Instant.now()).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(detailFor(health, PriceIngestionService.JOB_NAME))
                .containsEntry("state", "fresh")
                .containsEntry("rowsWritten", 6);
    }

    @Test
    void goesStaleWhenNoRunHasSucceededSinceTheThreshold() {
        recordBothJobs(IngestionRun.SUCCESS);

        Health health = readingAt(Instant.now().plus(Duration.ofHours(27))).health();

        assertThat(health.getStatus()).isEqualTo(IngestionHealthIndicator.STALE);
        assertThat(detailFor(health, PriceIngestionService.JOB_NAME)).containsEntry("state", "stale");
    }

    /**
     * The point of the check: prices arriving does not prove weather is, and a
     * single quiet job has to be enough to raise the alarm.
     */
    @Test
    void oneQuietJobIsEnoughToRaiseTheAlarm() {
        record(PriceIngestionService.JOB_NAME, IngestionRun.SUCCESS);

        Health health = readingAt(Instant.now()).health();

        assertThat(health.getStatus()).isEqualTo(IngestionHealthIndicator.STALE);
        assertThat(detailFor(health, PriceIngestionService.JOB_NAME)).containsEntry("state", "fresh");
        assertThat(detailFor(health, WeatherIngestionService.JOB_NAME))
                .containsEntry("state", "never succeeded");
    }

    /**
     * A failed run wrote nothing, so it must not read as recent activity —
     * otherwise a job failing every morning would look perfectly healthy.
     */
    @Test
    void aRunThatCollectedNothingDoesNotCountAsRecentActivity() {
        recordBothJobs(IngestionRun.FAILED);

        assertThat(readingAt(Instant.now()).health().getStatus())
                .isEqualTo(IngestionHealthIndicator.STALE);
    }

    /** A partial run stored the day's data; it is flagged for a human, not a gap. */
    @Test
    void aPartialRunStillCountsAsCollected() {
        recordBothJobs(IngestionRun.PARTIAL);

        Health health = readingAt(Instant.now()).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(detailFor(health, PriceIngestionService.JOB_NAME))
                .containsEntry("lastStatus", IngestionRun.PARTIAL);
    }
}
