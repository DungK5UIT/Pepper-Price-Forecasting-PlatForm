package com.giatieuviet.backend.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ForecastRepository extends JpaRepository<Forecast, Long> {

    /** The most recent publication date for a granularity — older runs are kept but not served. */
    Optional<Forecast> findFirstByGranularityOrderByAsOfDateDesc(String granularity);

    List<Forecast> findByGranularityAndAsOfDateOrderByTargetDateAsc(String granularity, LocalDate asOfDate);

    /** Clears a run so it can be replaced rather than duplicated when regenerated. */
    void deleteByCommodityAndGranularityAndAsOfDate(String commodity, String granularity, LocalDate asOfDate);
}
