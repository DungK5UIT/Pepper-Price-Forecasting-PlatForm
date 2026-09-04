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
 * Weather for one province on one day. {@code isForecast} distinguishes a
 * predicted day from an observed one — both live here because the UI shows
 * them in one continuous list.
 */
@Entity
@Table(name = "weather_observation")
public class WeatherObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String province;

    @Column(name = "observed_date", nullable = false)
    private LocalDate observedDate;

    @Column(name = "temp_c")
    private BigDecimal tempC;

    @Column(name = "rainfall_mm")
    private BigDecimal rainfallMm;

    @Column(name = "wind_speed_kmh")
    private BigDecimal windSpeedKmh;

    @Column(name = "is_forecast", nullable = false)
    private boolean forecast;

    protected WeatherObservation() {
        // for JPA
    }

    public WeatherObservation(String province, LocalDate observedDate, BigDecimal tempC,
                              BigDecimal rainfallMm, BigDecimal windSpeedKmh, boolean forecast) {
        this.province = province;
        this.observedDate = observedDate;
        this.tempC = tempC;
        this.rainfallMm = rainfallMm;
        this.windSpeedKmh = windSpeedKmh;
        this.forecast = forecast;
    }

    /**
     * Replaces a day's figures with a later reading. Routine rather than
     * exceptional: a date first arrives as a forecast and is overwritten with
     * the measured value once it has passed.
     */
    public void correctTo(BigDecimal tempC, BigDecimal rainfallMm, BigDecimal windSpeedKmh, boolean forecast) {
        this.tempC = tempC;
        this.rainfallMm = rainfallMm;
        this.windSpeedKmh = windSpeedKmh;
        this.forecast = forecast;
    }

    public Long getId() {
        return id;
    }

    public String getProvince() {
        return province;
    }

    public LocalDate getObservedDate() {
        return observedDate;
    }

    public BigDecimal getTempC() {
        return tempC;
    }

    public BigDecimal getRainfallMm() {
        return rainfallMm;
    }

    public BigDecimal getWindSpeedKmh() {
        return windSpeedKmh;
    }

    public boolean isForecast() {
        return forecast;
    }
}
