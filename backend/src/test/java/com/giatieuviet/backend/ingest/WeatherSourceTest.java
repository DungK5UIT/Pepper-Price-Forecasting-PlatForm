package com.giatieuviet.backend.ingest;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Open-Meteo returns parallel arrays rather than a list of days; what is
 * checked here is that they are zipped back together against the right dates,
 * and that whether a day is a forecast or a measurement is decided from the
 * date rather than trusted from the response.
 */
class WeatherSourceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 4);

    /** Yesterday, today, tomorrow — the shape of a real reply, trimmed to three days. */
    private static final String THREE_DAYS = """
            {"daily": {
               "time": ["2026-09-03", "2026-09-04", "2026-09-05"],
               "temperature_2m_mean": [24.5, 24.2, 24.4],
               "precipitation_sum": [10.5, 19.7, 18.4],
               "windspeed_10m_max": [18.5, 18.6, 12.6]
            }}""";

    @Test
    void zipsTheParallelArraysIntoDays() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("latitude=12.6667")))
                .andExpect(queryParam("timezone", "Asia/Bangkok"))
                .andRespond(withSuccess(THREE_DAYS, MediaType.APPLICATION_JSON));

        WeatherSource source = new WeatherSource(builder, "https://open-meteo.test/v1/forecast");
        List<WeatherSource.DailyWeather> days = source.fetch(PepperProvince.DAK_LAK, TODAY);

        assertThat(days).hasSize(3);
        assertThat(days.get(1).province()).isEqualTo("Đắk Lắk");
        assertThat(days.get(1).date()).isEqualTo(TODAY);
        assertThat(days.get(1).tempC()).isEqualByComparingTo("24.2");
        assertThat(days.get(1).rainfallMm()).isEqualByComparingTo("19.7");
        assertThat(days.get(1).windSpeedKmh()).isEqualByComparingTo("18.6");
        server.verify();
    }

    @Test
    void marksOnlyFutureDaysAsForecast() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(containsString("latitude=")))
                .andRespond(withSuccess(THREE_DAYS, MediaType.APPLICATION_JSON));

        List<WeatherSource.DailyWeather> days =
                new WeatherSource(builder, "https://open-meteo.test/v1/forecast")
                        .fetch(PepperProvince.DAK_LAK, TODAY);

        // Today counts as measured, not predicted: its daily aggregate is
        // already settled by the time the job runs the next morning.
        assertThat(days).extracting(WeatherSource.DailyWeather::forecast)
                .containsExactly(false, false, true);
    }

    @Test
    void coversEveryProvinceThePlatformShows() {
        assertThat(PepperProvince.all()).extracting(PepperProvince::displayName)
                .containsExactlyInAnyOrder("Đắk Lắk", "Đắk Nông", "Gia Lai",
                        "Đồng Nai", "Bình Phước", "Bà Rịa - Vũng Tàu");
    }
}
