package com.giatieuviet.backend.ingest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * A public site publishing today's domestic pepper prices by region.
 *
 * There is more than one implementation so a single site being down or
 * restructured does not cost the platform a day of history. They are not
 * independent measurements, though — see
 * {@code docs/adr/0005-price-and-weather-ingestion.md}.
 */
public interface PriceSource {

    /** Stored on every row as its provenance, so a bad day can be traced to a site. */
    String name();

    /**
     * @throws PriceScrapeException if the response cannot be parsed
     * @throws RuntimeException     if the site cannot be reached
     */
    PricePage fetch();

    record RegionPrice(String region, BigDecimal priceVndPerKg) {
    }

    /** What one site published for one day. */
    record PricePage(LocalDate pageDate, List<RegionPrice> regions) {

        /**
         * The nationwide figure the forecast is anchored on. The sources
         * publish regions only, so it is the mean across them — which is what
         * the earlier prototype stored, and keeps the series continuous.
         */
        public BigDecimal nationalAveragePrice() {
            return regions.stream()
                    .map(RegionPrice::priceVndPerKg)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(regions.size()), 2, RoundingMode.HALF_UP);
        }
    }
}
