package com.giatieuviet.backend.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A single observed price for one region on one day. Append-only. */
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
