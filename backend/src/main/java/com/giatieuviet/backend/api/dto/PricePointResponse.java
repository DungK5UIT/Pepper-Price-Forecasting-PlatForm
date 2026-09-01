package com.giatieuviet.backend.api.dto;

/**
 * Mirrors {@code PricePoint} in frontend/src/lib/types.ts. Historical points
 * carry {@code actual}; forecast points carry the quantile fields. The point
 * marked {@code isToday} carries both, so the chart's actual line and forecast
 * line meet there.
 */
public record PricePointResponse(
        String label,
        Boolean isToday,
        Long actual,
        Long forecastQ10,
        Long forecastQ50,
        Long forecastQ90) {

    public static PricePointResponse actual(String label, long actual) {
        return new PricePointResponse(label, null, actual, null, null, null);
    }

    public static PricePointResponse today(String label, long actual) {
        return new PricePointResponse(label, true, actual, actual, actual, actual);
    }

    public static PricePointResponse forecast(String label, long q10, long q50, long q90) {
        return new PricePointResponse(label, null, null, q10, q50, q90);
    }
}
