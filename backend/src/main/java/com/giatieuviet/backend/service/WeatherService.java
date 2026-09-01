package com.giatieuviet.backend.service;

import com.giatieuviet.backend.api.dto.ProvinceWeatherResponse;

import java.util.List;

/**
 * Weather in the pepper-growing provinces, one entry per province with the
 * current day first followed by the forecast days.
 */
public interface WeatherService {

    List<ProvinceWeatherResponse> provinceWeather();
}
