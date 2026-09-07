package com.giatieuviet.backend.internal;

import com.giatieuviet.backend.forecast.ForecastRefreshService;
import com.giatieuviet.backend.ingest.PriceIngestionService;
import com.giatieuviet.backend.ingest.WeatherIngestionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Triggers the daily jobs the backend used to run on its own cron. Now
 * called by a GitHub Actions schedule instead (ADR-0008): the hosted
 * backend sleeps when idle, so an in-process cron would silently stop
 * firing while asleep, and this way the process only needs to be awake for
 * the call.
 *
 * Covered by {@code SecurityConfig}'s existing {@code /internal/**} rule —
 * no separate auth wiring here.
 */
@RestController
@RequestMapping("/internal/v1/jobs")
public class JobTriggerController {

    private final PriceIngestionService priceIngestionService;
    private final WeatherIngestionService weatherIngestionService;
    private final ForecastRefreshService forecastRefreshService;

    public JobTriggerController(PriceIngestionService priceIngestionService,
                                 WeatherIngestionService weatherIngestionService,
                                 ForecastRefreshService forecastRefreshService) {
        this.priceIngestionService = priceIngestionService;
        this.weatherIngestionService = weatherIngestionService;
        this.forecastRefreshService = forecastRefreshService;
    }

    @PostMapping("/ingest-price")
    public Map<String, Integer> ingestPrice() {
        return Map.of("rowsWritten", priceIngestionService.ingestQuietly());
    }

    @PostMapping("/ingest-weather")
    public Map<String, Integer> ingestWeather() {
        return Map.of("rowsWritten", weatherIngestionService.ingestQuietly());
    }

    /**
     * No row count here: {@code refreshQuietly()} returns {@code void},
     * since a failed refresh keeps serving the previous run rather than
     * reporting a count of nothing changed. The caller only needs to know
     * the call landed.
     */
    @PostMapping("/refresh-forecast")
    public Map<String, Boolean> refreshForecast() {
        forecastRefreshService.refreshQuietly();
        return Map.of("triggered", true);
    }
}
