package com.giatieuviet.backend.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One predicted point: what the model published on {@code asOfDate} for
 * {@code targetDate}, as a median with a q10–q90 band.
 */
@Entity
@Table(name = "forecast")
public class Forecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String commodity;

    @Column(nullable = false)
    private String granularity;

    @Column(name = "as_of_date", nullable = false)
    private LocalDate asOfDate;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "predicted_price_q10")
    private BigDecimal predictedPriceQ10;

    @Column(name = "predicted_price_q50", nullable = false)
    private BigDecimal predictedPriceQ50;

    @Column(name = "predicted_price_q90")
    private BigDecimal predictedPriceQ90;

    @Column(name = "model_version")
    private String modelVersion;

    protected Forecast() {
        // for JPA
    }

    public Forecast(String commodity, String granularity, LocalDate asOfDate, LocalDate targetDate,
            BigDecimal predictedPriceQ10, BigDecimal predictedPriceQ50, BigDecimal predictedPriceQ90,
            String modelVersion) {
        this.commodity = commodity;
        this.granularity = granularity;
        this.asOfDate = asOfDate;
        this.targetDate = targetDate;
        this.predictedPriceQ10 = predictedPriceQ10;
        this.predictedPriceQ50 = predictedPriceQ50;
        this.predictedPriceQ90 = predictedPriceQ90;
        this.modelVersion = modelVersion;
    }

    public Long getId() {
        return id;
    }

    public String getCommodity() {
        return commodity;
    }

    public String getGranularity() {
        return granularity;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public BigDecimal getPredictedPriceQ10() {
        return predictedPriceQ10;
    }

    public BigDecimal getPredictedPriceQ50() {
        return predictedPriceQ50;
    }

    public BigDecimal getPredictedPriceQ90() {
        return predictedPriceQ90;
    }

    public String getModelVersion() {
        return modelVersion;
    }
}
