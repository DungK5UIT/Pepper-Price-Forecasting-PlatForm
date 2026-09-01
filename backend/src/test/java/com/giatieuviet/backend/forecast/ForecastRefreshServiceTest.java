package com.giatieuviet.backend.forecast;

import com.giatieuviet.backend.persistence.Forecast;
import com.giatieuviet.backend.persistence.ForecastRepository;
import com.giatieuviet.backend.persistence.MarketPriceRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

/**
 * Exercises the refresh against the in-memory database with the ML service
 * mocked — what matters here is what ends up stored, not the model itself.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ForecastRefreshServiceTest {

    private static final LocalDate TODAY = LocalDate.now();

    @Autowired
    private ForecastRefreshService refreshService;

    @Autowired
    private ForecastRepository forecasts;

    @Autowired
    private MarketPriceRepository marketPrices;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private MlForecastClient mlForecastClient;

    @BeforeEach
    void seedPriceHistory() {
        forecasts.deleteAll();
        marketPrices.deleteAll();
        entityManager.createNativeQuery("""
                insert into market_price (commodity, region, price_vnd_per_kg, source, observed_date)
                values ('black_pepper', 'national', 135000, 'test', :first),
                       ('black_pepper', 'national', 135700, 'test', :second)
                """)
                .setParameter("first", TODAY.minusDays(2))
                .setParameter("second", TODAY.minusDays(1))
                .executeUpdate();
    }

    private MlForecastClient.ForecastRun run(String modelVersion, double median) {
        return new MlForecastClient.ForecastRun(
                modelVersion,
                TODAY,
                "gbm",
                Map.of(
                        "month", List.of(new MlForecastClient.ForecastPoint(
                                TODAY.plusDays(30), median - 5_000, median, median + 5_000, false)),
                        "day", List.of(new MlForecastClient.ForecastPoint(
                                TODAY.plusDays(1), median - 500, median, median + 500, true))));
    }

    @Test
    void storesEveryPointTheModelReturns() {
        given(mlForecastClient.generate(any())).willReturn(run("gbm-test", 137_000));

        int stored = refreshService.refresh();

        assertThat(stored).isEqualTo(2);
        assertThat(forecasts.findByGranularityAndAsOfDateOrderByTargetDateAsc("month", TODAY))
                .singleElement()
                .satisfies(forecast -> {
                    assertThat(forecast.getModelVersion()).isEqualTo("gbm-test");
                    assertThat(forecast.getPredictedPriceQ50()).isEqualByComparingTo(BigDecimal.valueOf(137_000));
                });
    }

    @Test
    void anchorsTheRequestOnTheLatestObservedPrice() {
        given(mlForecastClient.generate(any())).willReturn(run("gbm-test", 137_000));

        refreshService.refresh();

        var captor = org.mockito.ArgumentCaptor.forClass(MlForecastClient.ForecastRequest.class);
        org.mockito.Mockito.verify(mlForecastClient).generate(captor.capture());
        assertThat(captor.getValue().anchorPrice()).isEqualTo(135_700.0);
        assertThat(captor.getValue().history()).hasSize(2);
    }

    @Test
    void rerunningReplacesTheDaysRunRatherThanDuplicatingIt() {
        given(mlForecastClient.generate(any())).willReturn(run("gbm-first", 137_000));
        refreshService.refresh();

        given(mlForecastClient.generate(any())).willReturn(run("gbm-second", 140_000));
        refreshService.refresh();

        List<Forecast> monthly = forecasts.findByGranularityAndAsOfDateOrderByTargetDateAsc("month", TODAY);
        assertThat(monthly).singleElement()
                .extracting(Forecast::getModelVersion)
                .isEqualTo("gbm-second");
    }

    @Test
    void aFailingMlServiceLeavesThePreviousRunInPlace() {
        given(mlForecastClient.generate(any())).willReturn(run("gbm-first", 137_000));
        refreshService.refresh();

        willThrow(new IllegalStateException("ml service down")).given(mlForecastClient).generate(any());
        refreshService.refreshQuietly();

        assertThat(forecasts.findByGranularityAndAsOfDateOrderByTargetDateAsc("month", TODAY))
                .singleElement()
                .extracting(Forecast::getModelVersion)
                .isEqualTo("gbm-first");
    }
}
