package com.giatieuviet.backend.api;

import com.giatieuviet.backend.api.dto.MarketInsightResponse;
import com.giatieuviet.backend.service.MarketInsightService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/market-insight")
public class MarketInsightController {

    private final MarketInsightService marketInsightService;

    public MarketInsightController(MarketInsightService marketInsightService) {
        this.marketInsightService = marketInsightService;
    }

    @GetMapping
    public MarketInsightResponse insight() {
        return marketInsightService.latestInsight();
    }
}
