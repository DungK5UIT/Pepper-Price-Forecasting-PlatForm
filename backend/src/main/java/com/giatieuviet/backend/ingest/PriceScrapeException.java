package com.giatieuviet.backend.ingest;

/**
 * A source responded, but not with something recognisable as a price table.
 *
 * Thrown rather than returning nothing on purpose: a site that has changed its
 * markup returns HTTP 200 and looks healthy, so an empty result would be
 * stored as "no prices today" and the gap would go unnoticed for weeks.
 */
public class PriceScrapeException extends RuntimeException {

    public PriceScrapeException(String message) {
        super(message);
    }

    public PriceScrapeException(String message, Throwable cause) {
        super(message, cause);
    }
}
