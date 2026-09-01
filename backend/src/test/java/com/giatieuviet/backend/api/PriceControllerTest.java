package com.giatieuviet.backend.api;

import com.giatieuviet.backend.api.dto.PeriodStatResponse;
import com.giatieuviet.backend.api.dto.PricePointResponse;
import com.giatieuviet.backend.api.dto.RegionPriceResponse;
import com.giatieuviet.backend.api.dto.TodayPriceResponse;
import com.giatieuviet.backend.domain.Granularity;
import com.giatieuviet.backend.service.PriceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(PriceController.class)
class PriceControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private PriceService priceService;

    @Test
    void todayReturnsTheCurrentPrice() {
        given(priceService.todayPrice()).willReturn(new TodayPriceResponse(
                148_700L, 1_200L, 0.82, "01/09", 146_000L, 152_000L, 149_000L, "Bình quân 6 tỉnh trọng điểm"));

        assertThat(mvc.get().uri("/api/v1/prices/today"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.priceVnd").isEqualTo(148_700);
    }

    @Test
    void regionsReturnsOneEntryPerRegion() {
        given(priceService.regionPrices()).willReturn(List.of(
                new RegionPriceResponse("Đắk Lắk", 149_200L, 1_500L),
                new RegionPriceResponse("Đồng Nai", 148_300L, -300L)));

        assertThat(mvc.get().uri("/api/v1/prices/regions"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[1].changeVnd").isEqualTo(-300);
    }

    @Test
    void forecastDefaultsToDailyGranularity() {
        given(priceService.forecastSeries(Granularity.DAY))
                .willReturn(List.of(PricePointResponse.today("Hôm nay", 148_700L)));

        assertThat(mvc.get().uri("/api/v1/prices/forecast"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].isToday").isEqualTo(true);
    }

    @Test
    void forecastOmitsUnsetQuantilesOnHistoricalPoints() {
        given(priceService.forecastSeries(Granularity.WEEK))
                .willReturn(List.of(PricePointResponse.actual("25/08", 147_500L)));

        assertThat(mvc.get().uri("/api/v1/prices/forecast").param("granularity", "week"))
                .hasStatusOk()
                .bodyJson()
                .doesNotHavePath("$[0].forecastQ50");
    }

    @Test
    void forecastRejectsAnUnsupportedGranularity() {
        assertThat(mvc.get().uri("/api/v1/prices/forecast").param("granularity", "bogus"))
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.detail").asString().contains("day, week, month");
    }

    @Test
    void statsReturnsTheConfiguredPeriods() {
        given(priceService.periodStats()).willReturn(List.of(
                new PeriodStatResponse("30 ngày qua", 4.2, 6_100L),
                new PeriodStatResponse("90 ngày qua", 9.8, 13_300L),
                new PeriodStatResponse("1 năm qua", 18.5, 23_200L)));

        assertThat(mvc.get().uri("/api/v1/prices/stats"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.length()").isEqualTo(3);
    }
}
