package com.giatieuviet.backend.internal;

import com.giatieuviet.backend.forecast.ForecastRefreshService;
import com.giatieuviet.backend.ingest.PriceIngestionService;
import com.giatieuviet.backend.ingest.WeatherIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
        given(priceIngestionService.ingestQuietly()).willReturn(7);

        assertThat(mvc.post().uri("/internal/v1/jobs/ingest-price"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.rowsWritten").isEqualTo(7);
    }

    @Test
    void ingestWeatherReportsRowsWritten() {
        given(weatherIngestionService.ingestQuietly()).willReturn(6);

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

        verify(forecastRefreshService).refreshQuietly();
    }
}
