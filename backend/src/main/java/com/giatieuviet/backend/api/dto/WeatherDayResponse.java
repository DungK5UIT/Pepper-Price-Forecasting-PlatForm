package com.giatieuviet.backend.api.dto;

/**
 * Mirrors {@code WeatherDay} in frontend/src/lib/types.ts. {@code condition}
 * carries the hyphenated wire code of
 * {@link com.giatieuviet.backend.domain.WeatherCondition}.
 *
 * One temperature per day, not a min/max range: that is what the upstream
 * weather source records.
 */
public record WeatherDayResponse(
        String label,
        String condition,
        double tempC,
        double rainMm,
        boolean isForecast) {
}
