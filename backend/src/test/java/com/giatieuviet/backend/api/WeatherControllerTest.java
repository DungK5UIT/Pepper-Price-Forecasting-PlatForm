package com.giatieuviet.backend.api;

import com.giatieuviet.backend.api.dto.ProvinceWeatherResponse;
import com.giatieuviet.backend.api.dto.WeatherDayResponse;
import com.giatieuviet.backend.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(WeatherController.class)
class WeatherControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private WeatherService weatherService;

    @Test
    void weatherReturnsDaysPerProvinceWithHyphenatedConditionCodes() {
        given(weatherService.provinceWeather()).willReturn(List.of(
                new ProvinceWeatherResponse("Đắk Lắk", List.of(
                        new WeatherDayResponse("Hôm nay", "cloud-rain", 27.5, 18, false),
                        new WeatherDayResponse("Th3", "cloud-sun", 26.0, 24, true)))));

        assertThat(mvc.get().uri("/api/v1/weather"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].days[0].condition").isEqualTo("cloud-rain");
    }

    @Test
    void weatherMarksForecastDays() {
        given(weatherService.provinceWeather()).willReturn(List.of(
                new ProvinceWeatherResponse("Gia Lai", List.of(
                        new WeatherDayResponse("Hôm nay", "sun", 28.0, 8, false),
                        new WeatherDayResponse("Th3", "cloud", 27.0, 12, true)))));

        assertThat(mvc.get().uri("/api/v1/weather"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].days[1].isForecast").isEqualTo(true);
    }
}
