package com.giatieuviet.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * The clock, as an injectable bean.
 *
 * Anything that decides whether something is "too old" needs to be testable
 * without waiting, and {@code Instant.now()} scattered through the code is
 * not.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
