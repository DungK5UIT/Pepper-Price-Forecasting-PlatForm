package com.giatieuviet.backend.api.dto;

/**
 * Mirrors {@code TodayPrice} in frontend/src/lib/types.ts.
 */
public record TodayPriceResponse(
        long priceVnd,
        long changeVnd,
        double changePercent,
        String asOfDate,
        long forecastLow,
        long forecastHigh,
        long forecastMedian,
        String sourceLabel) {
}
