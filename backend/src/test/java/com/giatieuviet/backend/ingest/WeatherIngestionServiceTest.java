package com.giatieuviet.backend.ingest;

import com.giatieuviet.backend.persistence.IngestionRun;
import com.giatieuviet.backend.persistence.IngestionRunRepository;
import com.giatieuviet.backend.persistence.WeatherObservation;
import com.giatieuviet.backend.persistence.WeatherObservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

/**
 * Exercises collection against the in-memory database with Open-Meteo mocked.
 *
 * Not {@code @Transactional}, for the same reason as
 * {@link PriceIngestionServiceTest}: the run log is written in its own
 * transaction so it outlives a rollback.
 */
@SpringBootTest
@ActiveProfiles("test")
class WeatherIngestionServiceTest {

    private static final LocalDate TODAY = LocalDate.now();

    @Autowired
    private WeatherIngestionService ingestionService;

    @Autowired
    private WeatherObservationRepository observations;

    @Autowired
    private IngestionRunRepository ingestionRuns;

    @MockitoBean
    private WeatherSource weatherSource;

    @BeforeEach
    void clearPreviousRuns() {
        observations.deleteAll();
        ingestionRuns.deleteAll();
    }

    private static WeatherSource.DailyWeather day(LocalDate date, String rainfall, boolean forecast) {
        return new WeatherSource.DailyWeather("Đắk Lắk", date, new BigDecimal("24.2"),
                new BigDecimal(rainfall), new BigDecimal("18.6"), forecast);
    }

    private IngestionRun lastRun() {
        return ingestionRuns.findByJobNameOrderByFinishedAtDesc(WeatherIngestionService.JOB_NAME).get(0);
    }

    @Test
    void storesEveryDayItIsGiven() {
        given(weatherSource.fetchAll(any())).willReturn(List.of(
                day(TODAY, "19.7", false),
                day(TODAY.plusDays(1), "18.4", true)));

        assertThat(ingestionService.ingest()).isEqualTo(2);
        assertThat(observations.findAll()).hasSize(2);
        assertThat(lastRun().getStatus()).isEqualTo(IngestionRun.SUCCESS);
    }

    /**
     * The case that matters most: a date arrives as a forecast, then comes
     * back the next day as a measurement and has to overwrite rather than
     * accumulate.
     */
    @Test
    void replacesYesterdaysForecastWithWhatActuallyHappened() {
        given(weatherSource.fetchAll(any())).willReturn(List.of(day(TODAY, "19.7", true)));
        ingestionService.ingest();

        given(weatherSource.fetchAll(any())).willReturn(List.of(day(TODAY, "24.1", false)));
        ingestionService.ingest();

        assertThat(observations.findAll()).singleElement().satisfies(observation -> {
            assertThat(observation.getRainfallMm()).isEqualByComparingTo("24.1");
            assertThat(observation.isForecast()).isFalse();
        });
    }

    @Test
    void recordsAFailedRunWhenTheProviderIsUnreachable() {
        willThrow(new IllegalStateException("open-meteo unreachable")).given(weatherSource).fetchAll(any());

        assertThatThrownBy(() -> ingestionService.ingest()).isInstanceOf(IllegalStateException.class);

        assertThat(observations.findAll()).isEmpty();
        assertThat(lastRun().getStatus()).isEqualTo(IngestionRun.FAILED);
        assertThat(lastRun().getDetail()).contains("open-meteo unreachable");
    }

    @Test
    void aBrokenCollectionDoesNotPropagateToTheScheduler() {
        willThrow(new IllegalStateException("open-meteo unreachable")).given(weatherSource).fetchAll(any());

        assertThat(ingestionService.ingestQuietly()).isZero();
        assertThat(lastRun().getStatus()).isEqualTo(IngestionRun.FAILED);
    }

    @Test
    void keepsProvincesApartOnTheSameDay() {
        given(weatherSource.fetchAll(any())).willReturn(List.of(
                day(TODAY, "19.7", false),
                new WeatherSource.DailyWeather("Gia Lai", TODAY, new BigDecimal("22.0"),
                        new BigDecimal("5.0"), new BigDecimal("9.0"), false)));

        ingestionService.ingest();

        assertThat(observations.findAll()).extracting(WeatherObservation::getProvince)
                .containsExactlyInAnyOrder("Đắk Lắk", "Gia Lai");
    }
}
