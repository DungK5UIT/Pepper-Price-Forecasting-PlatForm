package com.giatieuviet.backend.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface WeatherObservationRepository extends JpaRepository<WeatherObservation, Long> {

    List<WeatherObservation> findByObservedDateGreaterThanEqualOrderByProvinceAscObservedDateAsc(LocalDate from);
}
