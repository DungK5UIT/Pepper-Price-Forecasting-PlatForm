package com.giatieuviet.backend.internal;

import com.giatieuviet.backend.forecast.ForecastRefreshService;
import com.giatieuviet.backend.ingest.PriceIngestionService;
import com.giatieuviet.backend.ingest.PriceSourcesUnavailableException;
import com.giatieuviet.backend.ingest.WeatherIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * A controller slice: what is checked is that each endpoint calls the right
 * service method and reports its result. Security filters are off for the
 * same reason as every other slice test — access is covered against the real
 * filter chain in {@link com.giatieuviet.backend.config.SecurityConfigTest},
 * without actually running ingestion or a forecast refresh there.
 */
@WebMvcTest(JobTriggerController.class)
@AutoConfigureMockMvc(addFilters = false)
class JobTriggerControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private PriceIngestionService priceIngestionService;

    @MockitoBean
    private WeatherIngestionService weatherIngestionService;

    @MockitoBean
    private ForecastRefreshService forecastRefreshService;

    @Test
    void ingestPriceReportsRowsWritten() {
        given(priceIngestionService.ingest()).willReturn(7);

        assertThat(mvc.post().uri("/internal/v1/jobs/ingest-price"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.rowsWritten").isEqualTo(7);
    }

    @Test
    void ingestWeatherReportsRowsWritten() {
        given(weatherIngestionService.ingest()).willReturn(6);

        assertThat(mvc.post().uri("/internal/v1/jobs/ingest-weather"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.rowsWritten").isEqualTo(6);
    }

    @Test
    void refreshForecastTriggersAndAcknowledges() {
        assertThat(mvc.post().uri("/internal/v1/jobs/refresh-forecast"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.triggered").isEqualTo(true);

        verify(forecastRefreshService).refresh();
    }

    // TC-67: the scheduler runs curl --fail, so only a non-2xx status turns
    // its run red and gets the owner an email.

    @Test
    void aPriceRunWithNoReadableSourceIsABadGateway() {
        given(priceIngestionService.ingest()).willThrow(
                new PriceSourcesUnavailableException("primary: connection refused; backup: layout changed"));

        assertThat(mvc.post().uri("/internal/v1/jobs/ingest-price"))
                .hasStatus(HttpStatus.BAD_GATEWAY)
                .bodyJson()
                .extractingPath("$.detail").asString().contains("connection refused");
    }

    @Test
    void aFailedWeatherRunIsNotReportedAsSuccess() {
        given(weatherIngestionService.ingest()).willThrow(new IllegalStateException("Open-Meteo unreachable"));

        assertThat(mvc.post().uri("/internal/v1/jobs/ingest-weather")).hasStatus5xxServerError();
    }

    @Test
    void aFailedForecastRefreshIsNotReportedAsSuccess() {
        given(forecastRefreshService.refresh()).willThrow(new IllegalStateException("ML service unreachable"));

        assertThat(mvc.post().uri("/internal/v1/jobs/refresh-forecast")).hasStatus5xxServerError();
    }
}
