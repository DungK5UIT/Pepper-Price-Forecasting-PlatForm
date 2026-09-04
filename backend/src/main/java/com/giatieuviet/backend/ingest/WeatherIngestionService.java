package com.giatieuviet.backend.ingest;

import com.giatieuviet.backend.persistence.IngestionRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Collects the weather outlook for the growing provinces and stores it.
 *
 * The platform shows this to growers directly, and it is the input the
 * forecasting model cannot use yet: weather history only starts in August
 * 2026, against three and a half years of prices. Collecting it daily from
 * here is what eventually makes it usable — see
 * {@code docs/adr/0004-forecasting-model.md}.
 */
@Service
public class WeatherIngestionService {

    public static final String JOB_NAME = "weather_ingestion";

    private static final Logger log = LoggerFactory.getLogger(WeatherIngestionService.class);

    private final WeatherSource weatherSource;
    private final WeatherObservationStore observationStore;
    private final IngestionRunStore runStore;
    private final boolean ingestOnStartup;

    public WeatherIngestionService(WeatherSource weatherSource, WeatherObservationStore observationStore,
                                   IngestionRunStore runStore,
                                   @Value("${app.ingest.run-on-startup:false}") boolean ingestOnStartup) {
        this.weatherSource = weatherSource;
        this.observationStore = observationStore;
        this.runStore = runStore;
        this.ingestOnStartup = ingestOnStartup;
    }

    @Order(2)
    @EventListener(ApplicationReadyEvent.class)
    public void ingestOnStartupIfEnabled() {
        if (ingestOnStartup) {
            ingestQuietly();
        }
    }

    @Scheduled(cron = "${app.ingest.weather.cron:0 10 7 * * *}")
    public void ingestOnSchedule() {
        ingestQuietly();
    }

    /** Weather being unavailable leaves the price series, and the forecast, untouched. */
    public int ingestQuietly() {
        try {
            return ingest();
        } catch (RuntimeException exception) {
            log.error("Weather ingestion failed", exception);
            return 0;
        }
    }

    public int ingest() {
        Instant startedAt = Instant.now();
        LocalDate today = LocalDate.now();

        List<WeatherSource.DailyWeather> days;
        try {
            days = weatherSource.fetchAll(today);
        } catch (RuntimeException exception) {
            runStore.record(JOB_NAME, IngestionRun.FAILED, 0, exception.getMessage(), startedAt);
            throw exception;
        }

        int written = observationStore.store(days);
        runStore.record(JOB_NAME, IngestionRun.SUCCESS, written, null, startedAt);
        log.info("Stored {} weather rows across {} provinces", written, PepperProvince.all().size());
        return written;
    }
}
