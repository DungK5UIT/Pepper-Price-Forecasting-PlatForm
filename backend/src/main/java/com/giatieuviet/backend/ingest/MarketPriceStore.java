package com.giatieuviet.backend.ingest;

import com.giatieuviet.backend.persistence.MarketPrice;
import com.giatieuviet.backend.persistence.MarketPriceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Persists a day's prices, correcting rows that already exist.
 *
 * Separate from {@link PriceIngestionService} so the transaction is entered
 * through Spring's proxy — a service calling its own {@code @Transactional}
 * method would bypass it entirely.
 */
@Component
public class MarketPriceStore {

    static final String COMMODITY = "black_pepper";
    /** The nationwide series the forecast anchors on. */
    static final String NATIONAL_REGION = "national";

    private final MarketPriceRepository marketPrices;

    public MarketPriceStore(MarketPriceRepository marketPrices) {
        this.marketPrices = marketPrices;
    }

    /**
     * Idempotent by (commodity, region, date): running the job twice in a day
     * corrects the rows rather than duplicating them, so a retry after a
     * partial failure is always safe.
     *
     * @return how many rows were written — inserted or corrected
     */
    @Transactional
    public int store(LocalDate observedDate, List<PriceSource.RegionPrice> regions,
                     BigDecimal nationalAverage, String source) {
        int written = 0;
        for (PriceSource.RegionPrice region : regions) {
            written += upsert(region.region(), region.priceVndPerKg(), observedDate, source);
        }
        written += upsert(NATIONAL_REGION, nationalAverage, observedDate, source);
        return written;
    }

    private int upsert(String region, BigDecimal price, LocalDate observedDate, String source) {
        marketPrices.findByCommodityAndRegionAndObservedDate(COMMODITY, region, observedDate)
                .ifPresentOrElse(
                        existing -> existing.correctTo(price, source),
                        () -> marketPrices.save(
                                new MarketPrice(COMMODITY, region, price, source, observedDate)));
        return 1;
    }
}
