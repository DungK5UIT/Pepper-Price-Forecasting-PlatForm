package com.giatieuviet.backend.service;

import com.giatieuviet.backend.api.dto.MarketInsightResponse;

/**
 * The narrative market commentary shown alongside the numbers.
 */
public interface MarketInsightService {

    MarketInsightResponse latestInsight();
}
