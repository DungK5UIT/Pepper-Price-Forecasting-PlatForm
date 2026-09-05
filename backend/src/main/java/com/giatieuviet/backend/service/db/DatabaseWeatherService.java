package com.giatieuviet.backend.service.db;

import com.giatieuviet.backend.api.dto.ProvinceWeatherResponse;
import com.giatieuviet.backend.api.dto.WeatherDayResponse;
import com.giatieuviet.backend.domain.WeatherCondition;
import com.giatieuviet.backend.persistence.WeatherObservation;
import com.giatieuviet.backend.persistence.WeatherObservationRepository;
import com.giatieuviet.backend.service.WeatherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serves the weather outlook per growing province from the database.
 *
 * The upstream source records temperature, rainfall and wind — not a
 * condition — so the icon shown for a day is derived here from those numbers.
 */
@Service
public class DatabaseWeatherService implements WeatherService {

    private static final int DAYS_SHOWN = 7;

    /** Rain thresholds in millimetres, chosen to match how the icons read at a glance. */
    private static final double RAINY_MM = 10.0;
    private static final double SHOWERY_MM = 3.0;
    private static final double DRIZZLE_MM = 0.5;
    private static final double WINDY_KMH = 25.0;

    private static final Map<DayOfWeek, String> WEEKDAY_LABELS = Map.of(
            DayOfWeek.MONDAY, "Th2",
            DayOfWeek.TUESDAY, "Th3",
            DayOfWeek.WEDNESDAY, "Th4",
            DayOfWeek.THURSDAY, "Th5",
            DayOfWeek.FRIDAY, "Th6",
            DayOfWeek.SATURDAY, "Th7",
            DayOfWeek.SUNDAY, "CN");

    private final WeatherObservationRepository observations;
    private final Clock clock;

    public DatabaseWeatherService(WeatherObservationRepository observations, Clock clock) {
        this.observations = observations;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProvinceWeatherResponse> provinceWeather() {
        List<WeatherObservation> upcoming = observations
                .findByObservedDateGreaterThanEqualOrderByProvinceAscObservedDateAsc(LocalDate.now(clock));

        Map<String, List<WeatherObservation>> byProvince = new LinkedHashMap<>();
        for (WeatherObservation observation : upcoming) {
            byProvince.computeIfAbsent(observation.getProvince(), province -> new ArrayList<>())
                    .add(observation);
        }

        return byProvince.entrySet().stream()
                .map(entry -> new ProvinceWeatherResponse(entry.getKey(), daysOf(entry.getValue())))
                .toList();
    }

    private static List<WeatherDayResponse> daysOf(List<WeatherObservation> observations) {
        List<WeatherDayResponse> days = new ArrayList<>();
        for (int i = 0; i < Math.min(DAYS_SHOWN, observations.size()); i++) {
            WeatherObservation observation = observations.get(i);
            days.add(new WeatherDayResponse(
                    i == 0 ? "Hôm nay" : WEEKDAY_LABELS.get(observation.getObservedDate().getDayOfWeek()),
                    conditionOf(observation).code(),
                    toDouble(observation.getTempC()),
                    toDouble(observation.getRainfallMm()),
                    observation.isForecast()));
        }
        return days;
    }

    private static WeatherCondition conditionOf(WeatherObservation observation) {
        double rain = toDouble(observation.getRainfallMm());
        double wind = toDouble(observation.getWindSpeedKmh());

        if (rain >= RAINY_MM) {
            return WeatherCondition.CLOUD_RAIN;
        }
        if (wind >= WINDY_KMH) {
            return WeatherCondition.WIND;
        }
        if (rain >= SHOWERY_MM) {
            return WeatherCondition.CLOUD;
        }
        if (rain >= DRIZZLE_MM) {
            return WeatherCondition.CLOUD_SUN;
        }
        return WeatherCondition.SUN;
    }

    private static double toDouble(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }
}
