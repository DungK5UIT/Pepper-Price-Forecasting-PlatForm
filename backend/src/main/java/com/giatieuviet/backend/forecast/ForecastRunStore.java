package com.giatieuviet.backend.forecast;

import com.giatieuviet.backend.persistence.Forecast;
import com.giatieuviet.backend.persistence.ForecastRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Persists a forecast run, replacing any earlier run for the same day.
 *
 * Separate from {@link ForecastRefreshService} so the transaction is entered
 * through Spring's proxy: a service calling its own {@code @Transactional}
 * method would bypass it and run without a transaction at all.
 */
@Component
public class ForecastRunStore {

    private final ForecastRepository forecasts;

    public ForecastRunStore(ForecastRepository forecasts) {
        this.forecasts = forecasts;
    }

    /**
     * Replaces the whole run atomically: a half-updated set of granularities
     * would show the chart two different forecasts at once.
     */
    @Transactional
    public int replaceRun(String commodity, LocalDate asOfDate, Map<String, List<Forecast>> byGranularity) {
        int stored = 0;
        for (Map.Entry<String, List<Forecast>> entry : byGranularity.entrySet()) {
            forecasts.deleteByCommodityAndGranularityAndAsOfDate(commodity, entry.getKey(), asOfDate);
            forecasts.flush();
            forecasts.saveAll(entry.getValue());
            stored += entry.getValue().size();
        }
        return stored;
    }
}
