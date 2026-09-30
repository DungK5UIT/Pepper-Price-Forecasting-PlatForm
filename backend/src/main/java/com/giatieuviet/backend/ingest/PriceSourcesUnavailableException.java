package com.giatieuviet.backend.ingest;

/**
 * No price source could be read, so nothing was stored for the day.
 *
 * Thrown rather than returning zero rows: the scheduler that triggers
 * collection only notices a failure through the HTTP status, and zero rows
 * looked exactly like success to it.
 */
public class PriceSourcesUnavailableException extends RuntimeException {

    /**
     * @param failures each source's name and why it could not be read, for
     *                 the operator reading the scheduler log
     */
    public PriceSourcesUnavailableException(String failures) {
        super("No price source could be read: " + failures);
    }
}
