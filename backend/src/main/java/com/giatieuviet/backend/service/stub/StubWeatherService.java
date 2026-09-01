package com.giatieuviet.backend.service.stub;

import com.giatieuviet.backend.api.dto.ProvinceWeatherResponse;
import com.giatieuviet.backend.api.dto.WeatherDayResponse;
import com.giatieuviet.backend.domain.WeatherCondition;
import com.giatieuviet.backend.service.WeatherService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.giatieuviet.backend.domain.WeatherCondition.CLOUD;
import static com.giatieuviet.backend.domain.WeatherCondition.CLOUD_RAIN;
import static com.giatieuviet.backend.domain.WeatherCondition.CLOUD_SUN;
import static com.giatieuviet.backend.domain.WeatherCondition.SUN;

/**
 * TEMPORARY in-memory data source, standing in for the PostgreSQL-backed
 * implementation until the database is wired up. The figures are illustrative
 * of the rainy season in the growing provinces, not observed readings.
 */
@Service
public class StubWeatherService implements WeatherService {

    private static final List<String> DAY_LABELS = List.of("Hôm nay", "Th3", "Th4", "Th5", "Th6", "Th7", "CN");

    @Override
    public List<ProvinceWeatherResponse> provinceWeather() {
        return List.of(
                province("Đắk Lắk", List.of(
                        day(CLOUD_RAIN, 24, 31, 18), day(CLOUD_RAIN, 23, 29, 24), day(CLOUD_SUN, 24, 30, 6),
                        day(CLOUD_RAIN, 23, 28, 20), day(CLOUD, 24, 30, 9), day(CLOUD_RAIN, 23, 29, 22),
                        day(CLOUD_SUN, 24, 31, 5))),
                province("Đắk Nông", List.of(
                        day(CLOUD_RAIN, 23, 29, 22), day(CLOUD_RAIN, 22, 28, 26), day(CLOUD, 23, 29, 10),
                        day(CLOUD_RAIN, 22, 27, 19), day(CLOUD_SUN, 23, 28, 8), day(CLOUD_RAIN, 22, 28, 21),
                        day(CLOUD_SUN, 23, 29, 7))),
                province("Gia Lai", List.of(
                        day(CLOUD_SUN, 24, 32, 8), day(CLOUD, 24, 31, 12), day(SUN, 25, 32, 4),
                        day(CLOUD_RAIN, 24, 30, 15), day(CLOUD_SUN, 24, 31, 6), day(CLOUD, 24, 30, 11),
                        day(SUN, 25, 32, 3))),
                province("Đồng Nai", List.of(
                        day(CLOUD_RAIN, 25, 33, 15), day(CLOUD_RAIN, 25, 32, 18), day(CLOUD_SUN, 26, 33, 6),
                        day(CLOUD_RAIN, 25, 31, 20), day(CLOUD_SUN, 26, 33, 5), day(CLOUD, 25, 32, 14),
                        day(SUN, 26, 33, 4))),
                province("Bình Phước", List.of(
                        day(CLOUD_SUN, 25, 34, 3), day(CLOUD, 25, 33, 7), day(SUN, 26, 34, 2),
                        day(CLOUD, 25, 32, 10), day(CLOUD_SUN, 26, 34, 3), day(CLOUD, 25, 33, 6),
                        day(SUN, 26, 34, 2))),
                province("Bà Rịa - Vũng Tàu", List.of(
                        day(SUN, 26, 32, 1), day(CLOUD_SUN, 26, 31, 3), day(SUN, 27, 33, 0),
                        day(CLOUD, 26, 31, 5), day(CLOUD_SUN, 27, 32, 2), day(CLOUD, 26, 31, 4),
                        day(SUN, 27, 33, 1))));
    }

    /** Labels the days and marks everything after the first one as forecast. */
    private static ProvinceWeatherResponse province(String name, List<DayReading> readings) {
        List<WeatherDayResponse> days = new ArrayList<>(readings.size());
        for (int i = 0; i < readings.size(); i++) {
            DayReading reading = readings.get(i);
            days.add(new WeatherDayResponse(
                    DAY_LABELS.get(i),
                    reading.condition().code(),
                    reading.tempMin(),
                    reading.tempMax(),
                    reading.rainMm(),
                    i > 0));
        }
        return new ProvinceWeatherResponse(name, days);
    }

    private static DayReading day(WeatherCondition condition, int tempMin, int tempMax, double rainMm) {
        return new DayReading(condition, tempMin, tempMax, rainMm);
    }

    private record DayReading(WeatherCondition condition, int tempMin, int tempMax, double rainMm) {
    }
}
