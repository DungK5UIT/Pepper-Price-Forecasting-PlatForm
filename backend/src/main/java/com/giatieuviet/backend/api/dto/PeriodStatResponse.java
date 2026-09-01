package com.giatieuviet.backend.api.dto;

/**
 * Mirrors {@code PeriodStat} in frontend/src/lib/types.ts.
 */
public record PeriodStatResponse(String label, double changePercent, long changeVnd) {
}
