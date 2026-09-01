package com.giatieuviet.backend.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {

    /** The nationwide series, oldest first — the basis for the chart and the period stats. */
    List<MarketPrice> findByRegionOrderByObservedDateAsc(String region);

    /** Everything except the nationwide series, i.e. the per-region rows. */
    List<MarketPrice> findByRegionNotOrderByObservedDateAsc(String region);
}
