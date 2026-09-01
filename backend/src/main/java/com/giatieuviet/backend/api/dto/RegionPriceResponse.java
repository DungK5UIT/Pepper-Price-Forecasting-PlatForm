package com.giatieuviet.backend.api.dto;

/**
 * Mirrors {@code RegionPrice} in frontend/src/lib/types.ts.
 */
public record RegionPriceResponse(String region, long priceVnd, long changeVnd) {
}
