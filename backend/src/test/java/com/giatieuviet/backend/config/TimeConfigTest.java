package com.giatieuviet.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the assumption every dated row in this platform rests on: "today"
 * means the Vietnamese trading day, not the host's calendar.
 *
 * These tests pass on a developer laptop in Vietnam whether or not the fix is
 * present, which is exactly why the failure they describe is worth pinning
 * down — it only appears once the code runs somewhere on UTC.
 */
@SpringBootTest
@ActiveProfiles("test")
class TimeConfigTest {

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private Clock clock;

    @Test
    void theInjectedClockIsPinnedToVietnam() {
        assertThat(clock.getZone()).isEqualTo(VIETNAM);
    }

    /**
     * 22:30 UTC is 05:30 the next morning in Vietnam — inside the window when
     * the collection jobs and a grower checking prices are both awake, and the
     * two calendars disagree.
     */
    @Test
    void aTradingDayIsNotTheHostsCalendarDay() {
        Instant duringTheVietnameseMorning = Instant.parse("2026-09-04T22:30:00Z");

        LocalDate onAUtcHost = LocalDate.now(Clock.fixed(duringTheVietnameseMorning, ZoneOffset.UTC));
        LocalDate inVietnam = LocalDate.now(Clock.fixed(duringTheVietnameseMorning, VIETNAM));

        assertThat(onAUtcHost).isEqualTo(LocalDate.of(2026, 9, 4));
        assertThat(inVietnam).isEqualTo(LocalDate.of(2026, 9, 5));
    }

    /**
     * The seven hours the two calendars disagree for. Every dated write in the
     * platform lands in this window daily: prices at 07:00, weather at 07:10,
     * the forecast run at 08:15.
     */
    @Test
    void theyAgreeForTheRestOfTheDay() {
        Instant duringTheVietnameseAfternoon = Instant.parse("2026-09-04T08:00:00Z");

        assertThat(LocalDate.now(Clock.fixed(duringTheVietnameseAfternoon, ZoneOffset.UTC)))
                .isEqualTo(LocalDate.now(Clock.fixed(duringTheVietnameseAfternoon, VIETNAM)));
    }
}
