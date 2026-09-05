package com.giatieuviet.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * The clock, as an injectable bean, pinned to an explicit zone.
 *
 * Two reasons it is a bean rather than {@code LocalDate.now()} at each call
 * site. Anything deciding whether something is "too old" has to be testable
 * without waiting. And "today" is a business fact here, not a property of
 * whatever machine happens to be running: prices are quoted for a Vietnamese
 * trading day and the collection jobs are scheduled against it.
 *
 * A container defaults to UTC. On UTC, every hour between midnight and 07:00
 * local is still the previous calendar day, so an unpinned clock would date
 * the day's prices to yesterday, mark the wrong day as forecast rather than
 * observed, and show a grower "Hôm nay" above yesterday's weather — quietly,
 * with nothing failing.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock(@Value("${app.time-zone}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
