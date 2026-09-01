package com.giatieuviet.backend.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarketInsightRepository extends JpaRepository<MarketInsight, Long> {

    Optional<MarketInsight> findFirstByOrderByAsOfDateDesc();
}
