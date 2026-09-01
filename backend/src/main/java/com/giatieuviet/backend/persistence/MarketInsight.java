package com.giatieuviet.backend.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** The narrative commentary published for a given day. */
@Entity
@Table(name = "market_insight")
public class MarketInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "as_of_date", nullable = false)
    private LocalDate asOfDate;

    @Column(name = "insight_text", nullable = false)
    private String insightText;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected MarketInsight() {
        // for JPA
    }

    public Long getId() {
        return id;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public String getInsightText() {
        return insightText;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
