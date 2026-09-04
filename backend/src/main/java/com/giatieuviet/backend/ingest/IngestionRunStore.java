package com.giatieuviet.backend.ingest;

import com.giatieuviet.backend.persistence.IngestionRun;
import com.giatieuviet.backend.persistence.IngestionRunRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Writes the log of collection attempts, for every ingestion job. */
@Component
public class IngestionRunStore {

    private final IngestionRunRepository runs;

    public IngestionRunStore(IngestionRunRepository runs) {
        this.runs = runs;
    }

    /**
     * In its own transaction: a failed run has usually just rolled back the
     * work it was recording, and the record of the failure must survive that.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String jobName, String status, int rowsWritten, String detail, Instant startedAt) {
        runs.save(new IngestionRun(jobName, status, rowsWritten, detail, startedAt));
    }
}
