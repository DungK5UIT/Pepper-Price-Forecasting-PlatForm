package com.giatieuviet.backend.ingest;

import com.giatieuviet.backend.persistence.IngestionRun;
import com.giatieuviet.backend.persistence.IngestionRunRepository;
import com.giatieuviet.backend.persistence.MarketPrice;
import com.giatieuviet.backend.persistence.MarketPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises collection against the in-memory database with the websites
 * replaced by stubs — what matters here is what ends up stored and what the
 * run log says about it, not the parsing, which
 * {@link PriceSourceParsingTest} covers.
 *
 * Deliberately not {@code @Transactional}: the run log is written in its own
 * transaction precisely so it survives a rollback, and a test that rolled
 * everything back would not see that.
 */
@SpringBootTest
@ActiveProfiles("test")
class PriceIngestionServiceTest {

    private static final LocalDate PAGE_DATE = LocalDate.of(2026, 9, 3);

    @Autowired
    private MarketPriceStore priceStore;

    @Autowired
    private IngestionRunStore runStore;

    @Autowired
    private MarketPriceRepository marketPrices;

    @Autowired
    private IngestionRunRepository ingestionRuns;

    @BeforeEach
    void clearPreviousRuns() {
        marketPrices.deleteAll();
        ingestionRuns.deleteAll();
    }

    /** A stub site: either it publishes a page, or it is broken. */
    private record StubSource(String name, PriceSource.PricePage page, RuntimeException failure)
            implements PriceSource {

        @Override
        public PriceSource.PricePage fetch() {
            if (failure != null) {
                throw failure;
            }
            return page;
        }
    }

    private static PriceSource working(String name, LocalDate date, String... prices) {
        List<PriceSource.RegionPrice> regions = List.of(
                new PriceSource.RegionPrice("Đắk Lắk", new BigDecimal(prices[0])),
                new PriceSource.RegionPrice("Gia Lai", new BigDecimal(prices[1])));
        return new StubSource(name, new PriceSource.PricePage(date, regions), null);
    }

    private static PriceSource broken(String name, String message) {
        return new StubSource(name, null, new PriceScrapeException(message));
    }

    private PriceIngestionService serviceReading(PriceSource... sources) {
        return new PriceIngestionService(List.of(sources), priceStore, runStore, false);
    }

    private IngestionRun lastRun() {
        return ingestionRuns.findByJobNameOrderByFinishedAtDesc(PriceIngestionService.JOB_NAME).get(0);
    }

    @Test
    void storesEveryRegionPlusTheNationalAverage() {
        int written = serviceReading(working("primary", PAGE_DATE, "136000", "134000")).ingest();

        assertThat(written).isEqualTo(3);
        assertThat(marketPrices.findAll()).extracting(MarketPrice::getRegion)
                .containsExactlyInAnyOrder("Đắk Lắk", "Gia Lai", "national");
        assertThat(marketPrices.findByCommodityAndRegionAndObservedDate("black_pepper", "national", PAGE_DATE))
                .get()
                .satisfies(price -> {
                    assertThat(price.getPriceVndPerKg()).isEqualByComparingTo("135000.00");
                    assertThat(price.getSource()).isEqualTo("primary");
                });
        assertThat(lastRun().getStatus()).isEqualTo(IngestionRun.SUCCESS);
        assertThat(lastRun().getDetail()).isNull();
    }

    @Test
    void rerunningTheSameDayCorrectsTheRowsRatherThanDuplicatingThem() {
        serviceReading(working("primary", PAGE_DATE, "136000", "134000")).ingest();
        serviceReading(working("primary", PAGE_DATE, "138000", "136000")).ingest();

        assertThat(marketPrices.findAll()).hasSize(3);
        assertThat(marketPrices.findByCommodityAndRegionAndObservedDate("black_pepper", "Đắk Lắk", PAGE_DATE))
                .get()
                .satisfies(price -> assertThat(price.getPriceVndPerKg()).isEqualByComparingTo("138000"));
    }

    @Test
    void fallsBackToTheNextSourceAndSaysSoInTheRunLog() {
        int written = serviceReading(
                broken("primary", "layout changed"),
                working("backup", PAGE_DATE, "136000", "134000")).ingest();

        assertThat(written).isEqualTo(3);
        assertThat(marketPrices.findAll()).allSatisfy(price ->
                assertThat(price.getSource()).isEqualTo("backup"));
        assertThat(lastRun().getStatus()).isEqualTo(IngestionRun.PARTIAL);
        assertThat(lastRun().getDetail()).contains("Fell back to backup").contains("layout changed");
    }

    @Test
    void flagsTheRunWhenTheSourcesDisagreeButStillStoresThePrimary() {
        serviceReading(
                working("primary", PAGE_DATE, "136000", "134000"),
                working("backup", PAGE_DATE, "100000", "100000")).ingest();

        assertThat(marketPrices.findByCommodityAndRegionAndObservedDate("black_pepper", "national", PAGE_DATE))
                .get()
                .satisfies(price -> assertThat(price.getPriceVndPerKg()).isEqualByComparingTo("135000"));
        assertThat(lastRun().getStatus()).isEqualTo(IngestionRun.PARTIAL);
        assertThat(lastRun().getDetail()).contains("disagree by 25.9%");
    }

    @Test
    void flagsTheRunWhenOneSourceIsServingAnOlderDay() {
        serviceReading(
                working("primary", PAGE_DATE, "136000", "134000"),
                working("backup", PAGE_DATE.minusDays(3), "136000", "134000")).ingest();

        assertThat(lastRun().getStatus()).isEqualTo(IngestionRun.PARTIAL);
        assertThat(lastRun().getDetail()).contains("stale");
    }

    @Test
    void recordsAFailedRunAndStoresNothingWhenNoSourceCanBeRead() {
        int written = serviceReading(
                broken("primary", "connection refused"),
                broken("backup", "layout changed")).ingest();

        assertThat(written).isZero();
        assertThat(marketPrices.findAll()).isEmpty();
        assertThat(lastRun().getStatus()).isEqualTo(IngestionRun.FAILED);
        assertThat(lastRun().getDetail()).contains("connection refused").contains("layout changed");
    }

    @Test
    void aBrokenCollectionDoesNotPropagateToTheCaller() {
        PriceIngestionService service = serviceReading(broken("primary", "connection refused"));

        assertThat(service.ingestQuietly()).isZero();
        assertThat(lastRun().getStatus()).isEqualTo(IngestionRun.FAILED);
    }
}
