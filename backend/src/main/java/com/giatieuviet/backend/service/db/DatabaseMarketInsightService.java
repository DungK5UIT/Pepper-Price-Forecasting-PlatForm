package com.giatieuviet.backend.service.db;

import com.giatieuviet.backend.api.dto.MarketInsightResponse;
import com.giatieuviet.backend.persistence.MarketInsightRepository;
import com.giatieuviet.backend.service.MarketInsightService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/** Serves the most recently published commentary from the database. */
@Service
public class DatabaseMarketInsightService implements MarketInsightService {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");

    private final MarketInsightRepository insights;

    public DatabaseMarketInsightService(MarketInsightRepository insights) {
        this.insights = insights;
    }

    @Override
    @Transactional(readOnly = true)
    public MarketInsightResponse latestInsight() {
        return insights.findFirstByOrderByAsOfDateDesc()
                .map(insight -> new MarketInsightResponse(
                        insight.getInsightText(),
                        insight.getAsOfDate().format(DAY_LABEL)))
                .orElseThrow(() -> new IllegalStateException("No market insight has been published yet"));
    }
}
