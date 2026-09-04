package com.giatieuviet.backend.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {

    /** The nationwide series, oldest first — the basis for the chart and the period stats. */
    List<MarketPrice> findByRegionOrderByObservedDateAsc(String region);

    /** Everything except the nationwide series, i.e. the per-region rows. */
    List<MarketPrice> findByRegionNotOrderByObservedDateAsc(String region);

    /** The row the daily collection either corrects or creates. */
    Optional<MarketPrice> findByCommodityAndRegionAndObservedDate(
            String commodity, String region, LocalDate observedDate);
}
