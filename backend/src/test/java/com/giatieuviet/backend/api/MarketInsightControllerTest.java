package com.giatieuviet.backend.api;

import com.giatieuviet.backend.api.dto.MarketInsightResponse;
import com.giatieuviet.backend.service.MarketInsightService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * A controller slice: what is checked is the JSON this endpoint produces.
 * Security filters are off because {@code @WebMvcTest} would otherwise apply
 * Spring Security's defaults rather than this project's rules — access is
 * covered against the real filter chain in
 * {@link com.giatieuviet.backend.config.SecurityConfigTest}.
 */
@WebMvcTest(MarketInsightController.class)
@AutoConfigureMockMvc(addFilters = false)
class MarketInsightControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private MarketInsightService marketInsightService;

    @Test
    void insightReturnsTextAndUpdatedLabel() {
        given(marketInsightService.latestInsight())
                .willReturn(new MarketInsightResponse("Giá tiêu tiếp tục tăng nhẹ.", "08:00, 01/09"));

        assertThat(mvc.get().uri("/api/v1/market-insight"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.updatedAtLabel").isEqualTo("08:00, 01/09");
    }
}
