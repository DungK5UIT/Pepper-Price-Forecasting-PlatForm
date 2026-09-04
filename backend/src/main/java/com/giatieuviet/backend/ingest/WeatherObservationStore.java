package com.giatieuviet.backend.ingest;

import com.giatieuviet.backend.persistence.WeatherObservation;
import com.giatieuviet.backend.persistence.WeatherObservationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persists a day's weather per province, correcting rows that already exist.
 *
 * Correction is the normal case here, not the exception: today's row arrives
 * first as a forecast and is overwritten with the measured value the next
 * day, when the same date comes back as history.
 */
@Component
public class WeatherObservationStore {

    private final WeatherObservationRepository observations;

    public WeatherObservationStore(WeatherObservationRepository observations) {
        this.observations = observations;
    }

    @Transactional
    public int store(List<WeatherSource.DailyWeather> days) {
        for (WeatherSource.DailyWeather day : days) {
            observations.findByProvinceAndObservedDate(day.province(), day.date())
                    .ifPresentOrElse(
                            existing -> existing.correctTo(
                                    day.tempC(), day.rainfallMm(), day.windSpeedKmh(), day.forecast()),
                            () -> observations.save(new WeatherObservation(
                                    day.province(), day.date(), day.tempC(), day.rainfallMm(),
                                    day.windSpeedKmh(), day.forecast())));
        }
        return days.size();
    }
}
