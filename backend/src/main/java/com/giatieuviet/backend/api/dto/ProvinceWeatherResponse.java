package com.giatieuviet.backend.api.dto;

import java.util.List;

/**
 * Mirrors {@code ProvinceWeather} in frontend/src/lib/types.ts.
 */
public record ProvinceWeatherResponse(String province, List<WeatherDayResponse> days) {
}
