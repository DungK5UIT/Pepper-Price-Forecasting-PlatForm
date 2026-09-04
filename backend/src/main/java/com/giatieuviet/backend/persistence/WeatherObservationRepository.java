package com.giatieuviet.backend.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeatherObservationRepository extends JpaRepository<WeatherObservation, Long> {

    List<WeatherObservation> findByObservedDateGreaterThanEqualOrderByProvinceAscObservedDateAsc(LocalDate from);

    /** The row the daily collection either corrects or creates. */
    Optional<WeatherObservation> findByProvinceAndObservedDate(String province, LocalDate observedDate);
}
