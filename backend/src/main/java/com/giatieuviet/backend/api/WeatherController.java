package com.giatieuviet.backend.api;

import com.giatieuviet.backend.api.dto.ProvinceWeatherResponse;
import com.giatieuviet.backend.service.WeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public List<ProvinceWeatherResponse> weather() {
        return weatherService.provinceWeather();
    }
}
