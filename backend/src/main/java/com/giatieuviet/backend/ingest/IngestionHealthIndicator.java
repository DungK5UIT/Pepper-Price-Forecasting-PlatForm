package com.giatieuviet.backend.ingest;

import com.giatieuviet.backend.persistence.IngestionRun;
import com.giatieuviet.backend.persistence.IngestionRunRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reports whether the collection jobs are still doing their work.
 *
 * ADR-0005 called this the platform's worst failure mode and left it open:
 * a job that quietly stops still leaves an API serving yesterday's rows, so
 * nothing looks broken while the forecast goes stale. The run log recorded
 * the evidence; this reads it.
 *
 * The status is {@link #STALE} rather than {@code DOWN} on purpose. The
 * process is fine — it answers requests, its database is reachable, the
 * stored forecast still serves. Reporting DOWN would tell an orchestrator to
 * restart or drain an instance that is working, and restarting it collects
 * nothing. So {@code /actuator/health} keeps returning 200 and a monitor has
 * to read the body, which is the honest shape of the problem: this is a
 * data-freshness alarm, not a liveness one.
 */
@Component("ingestion")
public class IngestionHealthIndicator implements HealthIndicator {

    /** Registered in {@code management.endpoint.health.status.order} so it outranks UP. */
    static final Status STALE = new Status("STALE", "A collection job has not succeeded recently");

    private static final List<String> JOBS =
            List.of(PriceIngestionService.JOB_NAME, WeatherIngestionService.JOB_NAME);

    private final IngestionRunRepository runs;
    private final Clock clock;
    private final Duration tolerated;

    public IngestionHealthIndicator(
            IngestionRunRepository runs,
            Clock clock,
            @Value("${app.ingest.staleness-threshold:PT26H}") Duration tolerated) {
        this.runs = runs;
        this.clock = clock;
        this.tolerated = tolerated;
    }

    @Override
    public Health health() {
        Instant deadline = clock.instant().minus(tolerated);
        Health.Builder health = Health.up();
        boolean anyStale = false;

        for (String job : JOBS) {
            // A partial run still wrote rows — it is flagged for a human, not
            // a gap in the data. Only a failed run collected nothing.
            Optional<IngestionRun> last =
                    runs.findFirstByJobNameAndStatusNotOrderByFinishedAtDesc(job, IngestionRun.FAILED);

            Map<String, Object> detail = new LinkedHashMap<>();
            if (last.isEmpty()) {
                detail.put("state", "never succeeded");
                anyStale = true;
            } else {
                IngestionRun run = last.get();
                boolean stale = run.getFinishedAt().isBefore(deadline);
                anyStale |= stale;
                detail.put("state", stale ? "stale" : "fresh");
                detail.put("lastSucceededAt", run.getFinishedAt().toString());
                detail.put("lastStatus", run.getStatus());
                detail.put("rowsWritten", run.getRowsWritten());
                if (run.getDetail() != null) {
                    detail.put("note", run.getDetail());
                }
            }
            health.withDetail(job, detail);
        }

        health.withDetail("toleratedAge", tolerated.toString());
        return (anyStale ? health.status(STALE) : health).build();
    }
}
