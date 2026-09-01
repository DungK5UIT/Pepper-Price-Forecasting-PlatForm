package com.giatieuviet.backend.domain;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Time bucket a price series is aggregated into. The wire codes are
 * lower-case ("day", "week", "month") because that is what the public API
 * exposes and what the frontend sends.
 */
public enum Granularity {

    DAY("day"),
    WEEK("week"),
    MONTH("month");

    private final String code;

    Granularity(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /**
     * @throws IllegalArgumentException if the code is not one of the supported values
     */
    public static Granularity fromCode(String code) {
        for (Granularity granularity : values()) {
            if (granularity.code.equalsIgnoreCase(code)) {
                return granularity;
            }
        }
        throw new IllegalArgumentException(
                "Unsupported granularity '%s'. Supported values: %s".formatted(code, supportedCodes()));
    }

    public static String supportedCodes() {
        return Arrays.stream(values()).map(Granularity::code).collect(Collectors.joining(", "));
    }
}
