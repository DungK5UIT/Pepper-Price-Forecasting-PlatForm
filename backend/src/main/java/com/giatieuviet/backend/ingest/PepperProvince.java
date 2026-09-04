package com.giatieuviet.backend.ingest;

import java.util.List;

/**
 * The provinces the platform tracks, with a representative coordinate each.
 *
 * These are the five regions the price sources quote, plus Đồng Nai, which
 * has no separate price quote but grows pepper and shares a weather system
 * with Bình Phước. The coordinate is the provincial centre, not a plantation:
 * growing areas are spread across each province, and the daily aggregate is
 * what the forecast would use.
 */
enum PepperProvince {

    DAK_LAK("Đắk Lắk", 12.6667, 108.0500),
    DAK_NONG("Đắk Nông", 12.0043, 107.6877),
    GIA_LAI("Gia Lai", 13.9833, 108.0000),
    DONG_NAI("Đồng Nai", 10.9500, 106.8167),
    BINH_PHUOC("Bình Phước", 11.5333, 106.9167),
    BA_RIA_VUNG_TAU("Bà Rịa - Vũng Tàu", 10.5000, 107.1667);

    private final String displayName;
    private final double latitude;
    private final double longitude;

    PepperProvince(String displayName, double latitude, double longitude) {
        this.displayName = displayName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /** Stored as-is: the same spelling the price sources and the UI use. */
    String displayName() {
        return displayName;
    }

    double latitude() {
        return latitude;
    }

    double longitude() {
        return longitude;
    }

    static List<PepperProvince> all() {
        return List.of(values());
    }
}
