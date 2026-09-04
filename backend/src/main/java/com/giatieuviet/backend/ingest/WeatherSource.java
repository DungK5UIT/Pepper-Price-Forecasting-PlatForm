package com.giatieuviet.backend.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Reads the daily outlook per province from Open-Meteo.
 *
 * Open-Meteo is free and needs no key, which is why it was chosen over a
 * commercial provider: the platform has no billing story yet, and the
 * forecast only needs daily aggregates, not station-level detail.
 *
 * One day of history is requested alongside the forecast so a run that was
 * missed yesterday still fills yesterday's row.
 */
@Component
public class WeatherSource {

    private static final int FORECAST_DAYS = 7;
    private static final int PAST_DAYS = 1;
    private static final String DAILY_FIELDS = "temperature_2m_mean,precipitation_sum,windspeed_10m_max";
    /** Vietnam's zone, so a "day" in the response is a local day, not a UTC one. */
    private static final String TIMEZONE = "Asia/Bangkok";

    private final RestClient restClient;
    private final String baseUrl;

    public WeatherSource(RestClient.Builder builder,
                         @Value("${app.ingest.weather.base-url:https://api.open-meteo.com/v1/forecast}") String baseUrl) {
        this.restClient = builder.build();
        this.baseUrl = baseUrl;
    }

    /**
     * Six provinces in parallel rather than in sequence: one slow response
     * should not decide how long the whole job takes.
     *
     * A province that fails takes the whole call down. Weather is stored per
     * province per day and a partial set would look complete to the reader,
     * so the run is better recorded as failed and retried.
     */
    public List<DailyWeather> fetchAll(LocalDate today) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<List<DailyWeather>>> pending = PepperProvince.all().stream()
                    .map(province -> executor.submit(() -> fetch(province, today)))
                    .toList();

            List<DailyWeather> all = new ArrayList<>();
            for (Future<List<DailyWeather>> future : pending) {
                all.addAll(future.get());
            }
            return all;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while reading the weather forecast", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Could not read the weather forecast", exception.getCause());
        }
    }

    List<DailyWeather> fetch(PepperProvince province, LocalDate today) {
        String uri = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("latitude", province.latitude())
                .queryParam("longitude", province.longitude())
                .queryParam("daily", DAILY_FIELDS)
                .queryParam("timezone", TIMEZONE)
                .queryParam("forecast_days", FORECAST_DAYS)
                .queryParam("past_days", PAST_DAYS)
                .toUriString();

        Response response = restClient.get().uri(uri).retrieve().body(Response.class);
        if (response == null || response.daily() == null) {
            throw new IllegalStateException("Open-Meteo returned no daily data for " + province.displayName());
        }
        return response.daily().toDailyWeather(province.displayName(), today);
    }

    /** One province, one day. */
    public record DailyWeather(
            String province,
            LocalDate date,
            BigDecimal tempC,
            BigDecimal rainfallMm,
            BigDecimal windSpeedKmh,
            boolean forecast) {
    }

    /**
     * Open-Meteo answers with parallel arrays rather than a list of days, so
     * the fields are zipped back together here.
     */
    record Daily(
            List<LocalDate> time,
            @JsonProperty("temperature_2m_mean") List<BigDecimal> temperature,
            @JsonProperty("precipitation_sum") List<BigDecimal> precipitation,
            @JsonProperty("windspeed_10m_max") List<BigDecimal> windSpeed) {

        List<DailyWeather> toDailyWeather(String province, LocalDate today) {
            List<DailyWeather> days = new ArrayList<>(time.size());
            for (int i = 0; i < time.size(); i++) {
                LocalDate date = time.get(i);
                days.add(new DailyWeather(
                        province,
                        date,
                        temperature.get(i),
                        precipitation.get(i),
                        windSpeed.get(i),
                        date.isAfter(today)));
            }
            return days;
        }
    }

    record Response(Daily daily) {
    }
}
