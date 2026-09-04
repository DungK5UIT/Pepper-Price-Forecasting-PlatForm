package com.giatieuviet.backend.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IngestionRunRepository extends JpaRepository<IngestionRun, Long> {

    List<IngestionRun> findByJobNameOrderByFinishedAtDesc(String jobName);

    /**
     * The last run of this job that actually wrote something — the health
     * check asks for anything but {@code failed}, since a partial run still
     * collected the day's data.
     */
    Optional<IngestionRun> findFirstByJobNameAndStatusNotOrderByFinishedAtDesc(String jobName, String status);
}
