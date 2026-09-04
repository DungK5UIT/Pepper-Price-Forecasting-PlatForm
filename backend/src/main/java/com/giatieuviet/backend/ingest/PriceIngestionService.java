package com.giatieuviet.backend.ingest;

import com.giatieuviet.backend.persistence.IngestionRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects today's domestic pepper prices and stores them.
 *
 * Every source is read, not just the first that works, because a site can
 * return HTTP 200 while serving a stale cache — comparing two readings is the
 * only cheap way to notice. The sources share an upstream (giacaphe.com
 * credits giatieu.com for its data), so agreement is weak evidence that a
 * price is right; disagreement is still strong evidence that something is
 * wrong, which is the direction that matters here.
 */
@Service
public class PriceIngestionService {

    public static final String JOB_NAME = "price_ingestion";

    private static final Logger log = LoggerFactory.getLogger(PriceIngestionService.class);

    /** Day-to-day moves are well under 5%; more than that between two sources means one is wrong. */
    private static final BigDecimal DISCREPANCY_THRESHOLD = new BigDecimal("0.05");

    private final List<PriceSource> sources;
    private final MarketPriceStore priceStore;
    private final IngestionRunStore runStore;
    private final boolean ingestOnStartup;

    /** Injected in {@code @Order} sequence: the first is the primary source. */
    public PriceIngestionService(List<PriceSource> sources, MarketPriceStore priceStore,
                                 IngestionRunStore runStore,
                                 @Value("${app.ingest.run-on-startup:false}") boolean ingestOnStartup) {
        this.sources = sources;
        this.priceStore = priceStore;
        this.runStore = runStore;
        this.ingestOnStartup = ingestOnStartup;
    }

    /**
     * Off by default. A process started at noon would otherwise collect
     * nothing until the next morning, losing the day entirely — but a boot
     * should not depend on two public websites being up, so it is opt-in.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ingestOnStartupIfEnabled() {
        if (ingestOnStartup) {
            ingestQuietly();
        }
    }

    /** Early enough that the forecast refresh, later the same morning, sees today's price. */
    @Scheduled(cron = "${app.ingest.price.cron:0 0 7 * * *}")
    public void ingestOnSchedule() {
        ingestQuietly();
    }

    /**
     * Collection failing is not a reason to fail a boot or kill the schedule:
     * yesterday's prices keep serving, and the run is logged as failed so the
     * gap is visible.
     */
    public int ingestQuietly() {
        try {
            return ingest();
        } catch (RuntimeException exception) {
            log.error("Price ingestion failed", exception);
            return 0;
        }
    }

    public int ingest() {
        Instant startedAt = Instant.now();
        List<Reading> readings = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (PriceSource source : sources) {
            try {
                readings.add(new Reading(source.name(), source.fetch()));
            } catch (RuntimeException exception) {
                failures.add(source.name() + ": " + exception.getMessage());
            }
        }

        if (readings.isEmpty()) {
            String detail = String.join("; ", failures);
            log.error("No price source could be read: {}", detail);
            runStore.record(JOB_NAME, IngestionRun.FAILED, 0, detail, startedAt);
            return 0;
        }

        Reading used = readings.get(0);
        String note = concerns(used, readings, failures);

        int written = priceStore.store(
                used.page().pageDate(),
                used.page().regions(),
                used.page().nationalAveragePrice(),
                used.sourceName());

        String status = note == null ? IngestionRun.SUCCESS : IngestionRun.PARTIAL;
        runStore.record(JOB_NAME, status, written, note, startedAt);
        log.info("Stored {} price rows for {} from {}{}",
                written, used.page().pageDate(), used.sourceName(), note == null ? "" : " (" + note + ")");
        return written;
    }

    /**
     * @return what a human should know about this run, or null if it was
     *         unremarkable
     */
    private String concerns(Reading used, List<Reading> readings, List<String> failures) {
        List<String> notes = new ArrayList<>();
        if (!failures.isEmpty()) {
            notes.add("Fell back to %s; unavailable: %s".formatted(used.sourceName(), String.join("; ", failures)));
        }
        for (Reading other : readings.subList(1, readings.size())) {
            if (!other.page().pageDate().equals(used.page().pageDate())) {
                notes.add("%s is dated %s but %s is dated %s — one of them is stale".formatted(
                        used.sourceName(), used.page().pageDate(),
                        other.sourceName(), other.page().pageDate()));
            }
            BigDecimal usedAverage = used.page().nationalAveragePrice();
            BigDecimal difference = usedAverage.subtract(other.page().nationalAveragePrice()).abs()
                    .divide(usedAverage, 4, RoundingMode.HALF_UP);
            if (difference.compareTo(DISCREPANCY_THRESHOLD) > 0) {
                notes.add("%s and %s disagree by %s%% (%s vs %s); stored %s".formatted(
                        used.sourceName(), other.sourceName(),
                        difference.movePointRight(2).setScale(1, RoundingMode.HALF_UP),
                        usedAverage.toPlainString(), other.page().nationalAveragePrice().toPlainString(),
                        used.sourceName()));
            }
        }
        return notes.isEmpty() ? null : String.join(". ", notes);
    }

    private record Reading(String sourceName, PriceSource.PricePage page) {
    }
}
