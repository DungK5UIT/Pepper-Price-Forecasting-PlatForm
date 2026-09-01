package com.giatieuviet.backend.domain;

/**
 * Weather condition as rendered by the frontend's icon set. The wire codes
 * are hyphenated ("cloud-sun"), so they are carried explicitly rather than
 * derived from the enum constant name.
 */
public enum WeatherCondition {

    SUN("sun"),
    CLOUD("cloud"),
    CLOUD_SUN("cloud-sun"),
    CLOUD_RAIN("cloud-rain"),
    WIND("wind");

    private final String code;

    WeatherCondition(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
