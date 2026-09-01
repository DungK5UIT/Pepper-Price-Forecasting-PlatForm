package com.giatieuviet.backend.forecast;

import com.giatieuviet.backend.persistence.Forecast;
import com.giatieuviet.backend.persistence.MarketPrice;
import com.giatieuviet.backend.persistence.MarketPriceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Asks the ML service for a new forecast run and hands it to
 * {@link ForecastRunStore} to persist.
 *
 * The backend owns persistence, so the ML service returns numbers and this
 * class decides what becomes a row (ADR-0002/0003).
 */
@Service
public class ForecastRefreshService {

    private static final Logger log = LoggerFactory.getLogger(ForecastRefreshService.class);

    private static final String COMMODITY = "black_pepper";
    private static final String NATIONAL_REGION = "national";
    private static final int HORIZON_MONTHS = 2;

    private final MarketPriceRepository marketPrices;
    private final MlForecastClient mlForecastClient;
    private final ForecastRunStore runStore;
    private final boolean refreshOnStartup;

    public ForecastRefreshService(
            MarketPriceRepository marketPrices,
            MlForecastClient mlForecastClient,
            ForecastRunStore runStore,
            @Value("${app.forecast.refresh-on-startup:false}") boolean refreshOnStartup) {
        this.marketPrices = marketPrices;
        this.mlForecastClient = mlForecastClient;
        this.runStore = runStore;
        this.refreshOnStartup = refreshOnStartup;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartupIfEnabled() {
        if (refreshOnStartup) {
            refreshQuietly();
        }
    }

    @Scheduled(cron = "${app.forecast.refresh-cron:0 15 8 * * *}")
    public void refreshOnSchedule() {
        refreshQuietly();
    }

    /**
     * A forecast that cannot be regenerated is not a reason to fail: the last
     * stored run keeps serving, so the public API stays up while the ML
     * service is unavailable.
     */
    public void refreshQuietly() {
        try {
            log.info("Stored {} forecast points", refresh());
        } catch (RuntimeException exception) {
            log.error("Forecast refresh failed; keeping the previously stored run", exception);
        }
    }

    public int refresh() {
        List<MarketPrice> history = marketPrices.findByRegionOrderByObservedDateAsc(NATIONAL_REGION);
        if (history.isEmpty()) {
            log.warn("No nationwide price history to forecast from");
            return 0;
        }

        MarketPrice latest = history.get(history.size() - 1);
        // The run is dated today even though the anchor is the latest observed
        // price, which is routinely yesterday's — as-of is when we forecast.
        LocalDate asOfDate = LocalDate.now();

        MlForecastClient.ForecastRun run = mlForecastClient.generate(new MlForecastClient.ForecastRequest(
                asOfDate,
                latest.getPriceVndPerKg().doubleValue(),
                history.stream()
                        .map(price -> new MlForecastClient.PriceObservation(
                                price.getObservedDate(), price.getPriceVndPerKg().doubleValue()))
                        .toList(),
                HORIZON_MONTHS));

        Map<String, List<Forecast>> byGranularity = new LinkedHashMap<>();
        run.points().forEach((granularity, points) -> {
            List<Forecast> rows = new ArrayList<>(points.size());
            for (MlForecastClient.ForecastPoint point : points) {
                rows.add(new Forecast(
                        COMMODITY,
                        granularity,
                        run.asOfDate(),
                        point.targetDate(),
                        BigDecimal.valueOf(point.q10()),
                        BigDecimal.valueOf(point.q50()),
                        BigDecimal.valueOf(point.q90()),
                        run.modelVersion()));
            }
            byGranularity.put(granularity, rows);
        });

        return runStore.replaceRun(COMMODITY, run.asOfDate(), byGranularity);
    }
}
