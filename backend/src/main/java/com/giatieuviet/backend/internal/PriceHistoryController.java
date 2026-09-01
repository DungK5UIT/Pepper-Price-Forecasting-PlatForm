package com.giatieuviet.backend.internal;

import com.giatieuviet.backend.persistence.MarketPrice;
import com.giatieuviet.backend.persistence.MarketPriceRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Bulk price history for the ML service's training run.
 *
 * Internal, not part of the public contract in docs/api/README.md: the ML
 * service has no database access of its own (ADR-0003), so training data
 * reaches it through the backend. CORS is scoped to /api/**, so a browser
 * cannot reach this.
 */
@RestController
@RequestMapping("/internal/v1/price-history")
public class PriceHistoryController {

    private final MarketPriceRepository marketPrices;

    public PriceHistoryController(MarketPriceRepository marketPrices) {
        this.marketPrices = marketPrices;
    }

    @GetMapping
    public List<PriceObservation> history(@RequestParam(defaultValue = "national") String region) {
        return marketPrices.findByRegionOrderByObservedDateAsc(region).stream()
                .map(price -> new PriceObservation(price.getObservedDate(), price.getPriceVndPerKg().doubleValue()))
                .toList();
    }

    /** Deliberately the same shape the ML service accepts, so it passes straight through. */
    public record PriceObservation(LocalDate date, double priceVnd) {
    }
}
