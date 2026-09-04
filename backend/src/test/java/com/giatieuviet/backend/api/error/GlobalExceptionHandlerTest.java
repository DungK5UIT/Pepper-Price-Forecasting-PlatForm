package com.giatieuviet.backend.api.error;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The statuses a caller sees when a request does not match anything.
 *
 * A catch-all {@code @ExceptionHandler(Exception.class)} silently absorbs the
 * exceptions Spring MVC raises for these cases, so every one of them came back
 * as 500 — a typo in a URL reported as a server fault, and a real fault
 * indistinguishable from one.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void anUnknownPathIsNotFound() {
        assertThat(mvc.get().uri("/api/v1/prices/nope")).hasStatus(404);
    }

    /**
     * Not 405: only {@code GET /api/**} is permitted, so a write falls through
     * to the closed-by-default rule and never reaches the dispatcher. That is
     * the intended shape while the API is read-only — adding a write endpoint
     * has to mean adding a rule for it (ADR-0006).
     */
    @Test
    void aWriteToTheReadOnlyApiIsRefusedBeforeItIsRouted() {
        assertThat(mvc.post().uri("/api/v1/prices/today")).hasStatus(401);
    }

    /** Still handled here, and still a 400 — the domain rejects the value. */
    @Test
    void aValueTheDomainRefusesIsStillABadRequest() {
        assertThat(mvc.get().uri("/api/v1/prices/forecast?granularity=fortnight"))
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Invalid request");
    }
}
