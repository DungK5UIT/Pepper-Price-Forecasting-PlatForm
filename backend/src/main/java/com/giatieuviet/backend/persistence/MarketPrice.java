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
 * A single observed price for one region on one day.
 *
 * One row per region per day: a later collection run for the same day
 * corrects the row through {@link #correctTo} rather than adding a second
 * one, so the chart never sees a day twice.
 */
@Entity
@Table(name = "market_price")
public class MarketPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String commodity;

    @Column(nullable = false)
    private String region;

    @Column(name = "price_vnd_per_kg", nullable = false)
    private BigDecimal priceVndPerKg;

    @Column(nullable = false)
    private String source;

    @Column(name = "observed_date", nullable = false)
    private LocalDate observedDate;

    protected MarketPrice() {
        // for JPA
    }

    public MarketPrice(String commodity, String region, BigDecimal priceVndPerKg, String source,
                       LocalDate observedDate) {
        this.commodity = commodity;
        this.region = region;
        this.priceVndPerKg = priceVndPerKg;
        this.source = source;
        this.observedDate = observedDate;
    }

    /**
     * Overwrites today's figure with a later reading. The source comes along
     * with the price: which site the number that is actually stored came from
     * is the part worth knowing.
     */
    public void correctTo(BigDecimal priceVndPerKg, String source) {
        this.priceVndPerKg = priceVndPerKg;
        this.source = source;
    }

    public Long getId() {
        return id;
    }

    public String getCommodity() {
        return commodity;
    }

    public String getRegion() {
        return region;
    }

    public BigDecimal getPriceVndPerKg() {
        return priceVndPerKg;
    }

    public String getSource() {
        return source;
    }

    public LocalDate getObservedDate() {
        return observedDate;
    }
}
