package com.giatieuviet.backend.api;

import com.giatieuviet.backend.api.dto.PeriodStatResponse;
import com.giatieuviet.backend.api.dto.PricePointResponse;
import com.giatieuviet.backend.api.dto.RegionPriceResponse;
import com.giatieuviet.backend.api.dto.TodayPriceResponse;
import com.giatieuviet.backend.domain.Granularity;
import com.giatieuviet.backend.service.PriceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/prices")
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping("/today")
    public TodayPriceResponse today() {
        return priceService.todayPrice();
    }

    @GetMapping("/regions")
    public List<RegionPriceResponse> regions() {
        return priceService.regionPrices();
    }

    /**
     * @param granularity one of {@code day}, {@code week}, {@code month};
     *                    anything else is rejected as a 400 by
     *                    {@link com.giatieuviet.backend.api.error.GlobalExceptionHandler}
     */
    @GetMapping("/forecast")
    public List<PricePointResponse> forecast(@RequestParam(defaultValue = "day") String granularity) {
        return priceService.forecastSeries(Granularity.fromCode(granularity));
    }

    @GetMapping("/stats")
    public List<PeriodStatResponse> stats() {
        return priceService.periodStats();
    }
}
