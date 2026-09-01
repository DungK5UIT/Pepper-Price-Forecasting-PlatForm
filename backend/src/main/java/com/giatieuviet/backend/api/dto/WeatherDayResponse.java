package com.giatieuviet.backend.api.dto;

/**
 * Mirrors {@code WeatherDay} in frontend/src/lib/types.ts. {@code condition}
 * carries the hyphenated wire code of
 * {@link com.giatieuviet.backend.domain.WeatherCondition}.
 */
public record WeatherDayResponse(
        String label,
        String condition,
        int tempMin,
        int tempMax,
        double rainMm,
        boolean isForecast) {
}
