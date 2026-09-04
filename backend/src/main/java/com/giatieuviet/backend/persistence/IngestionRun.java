package com.giatieuviet.backend.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One attempt at collecting data, successful or not.
 *
 * A collection job that quietly stops working is this platform's worst
 * failure mode: the API keeps serving the last good rows and nothing looks
 * broken until the forecast is weeks stale. Recording the attempt makes the
 * silence visible.
 */
@Entity
@Table(name = "ingestion_run")
public class IngestionRun {

    /** Nothing went wrong. */
    public static final String SUCCESS = "success";
    /** Data was written, but something is worth a look — a failover, a discrepancy. */
    public static final String PARTIAL = "partial";
    /** Nothing was written. */
    public static final String FAILED = "failed";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(nullable = false)
    private String status;

    @Column(name = "rows_written", nullable = false)
    private int rowsWritten;

    @Column
    private String detail;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at", nullable = false)
    private Instant finishedAt;

    protected IngestionRun() {
        // for JPA
    }

    public IngestionRun(String jobName, String status, int rowsWritten, String detail, Instant startedAt) {
        this.jobName = jobName;
        this.status = status;
        this.rowsWritten = rowsWritten;
        this.detail = detail;
        this.startedAt = startedAt;
        this.finishedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getJobName() {
        return jobName;
    }

    public String getStatus() {
        return status;
    }

    public int getRowsWritten() {
        return rowsWritten;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
