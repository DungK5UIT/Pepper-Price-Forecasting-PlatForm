package com.giatieuviet.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The line this configuration draws: the published read API stays open, the
 * internal and management surfaces do not.
 *
 * Run against the real filter chain rather than a slice, because the risk
 * being closed here is a rule that does not match the URL it was meant to
 * cover — which a mocked chain would never show.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    private static final String USERNAME = "ml-service";
    private static final String PASSWORD = "test-only-password";

    @Autowired
    private MockMvcTester mvc;

    private static String basic(String username, String password) {
        return "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    /** Endpoints that answer on an empty database, so this stays about access. */
    @Test
    void thePublicApiStaysOpen() {
        assertThat(mvc.get().uri("/api/v1/prices/regions")).hasStatusOk();
        assertThat(mvc.get().uri("/api/v1/weather")).hasStatusOk();
    }

    /** An uptime check has to work without a credential. */
    @Test
    void healthIsReadableAnonymously() {
        assertThat(mvc.get().uri("/actuator/health")).hasStatusOk();
    }

    /**
     * The gap this configuration exists to close: one unauthenticated request
     * used to return the whole price series.
     */
    @Test
    void theInternalEndpointRefusesAnAnonymousCaller() {
        assertThat(mvc.get().uri("/internal/v1/price-history")).hasStatus(401);
    }

    @Test
    void theInternalEndpointRefusesAWrongPassword() {
        assertThat(mvc.get().uri("/internal/v1/price-history")
                .header(HttpHeaders.AUTHORIZATION, basic(USERNAME, "not-the-password")))
                .hasStatus(401);
    }

    @Test
    void theInternalEndpointServesTheMlServiceCredential() {
        assertThat(mvc.get().uri("/internal/v1/price-history")
                .header(HttpHeaders.AUTHORIZATION, basic(USERNAME, PASSWORD)))
                .hasStatusOk();
    }

    /**
     * Anything not named in the configuration is closed rather than open: a
     * new endpoint should have to be let out deliberately instead of
     * inheriting access by sitting outside the listed patterns.
     */
    @Test
    void anUnlistedActuatorEndpointIsClosedByDefault() {
        assertThat(mvc.get().uri("/actuator/env")).hasStatus(401);
    }
}
