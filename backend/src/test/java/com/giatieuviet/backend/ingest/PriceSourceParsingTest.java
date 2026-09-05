package com.giatieuviet.backend.ingest;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Parsing is checked against pages saved from the real sites on 2026-09-03,
 * trimmed to the heading and the table. When a site changes its markup these
 * tests keep passing — the fixture is frozen — so they are not a monitor;
 * they pin down what the parsers do with markup of a known shape, and give a
 * place to reproduce a break once one is reported.
 */
class PriceSourceParsingTest {

    private static final Clock VIETNAM = Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final GiaCaPhePriceSource giaCaPhe =
            new GiaCaPhePriceSource(RestClient.builder(), "https://example.invalid", VIETNAM);
    private final GiaTieuPriceSource giaTieu =
            new GiaTieuPriceSource(RestClient.builder(), "https://example.invalid", VIETNAM);

    private static String fixture(String name) {
        try (var stream = new ClassPathResource("ingest/" + name).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Test
    void readsEveryRegionFromGiaCaPhe() {
        PriceSource.PricePage page = giaCaPhe.parse(fixture("giacaphe-2026-09-03.html"));

        assertThat(page.pageDate()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(page.regions()).extracting(PriceSource.RegionPrice::region)
                .containsExactly("Gia Lai", "Bà Rịa - Vũng Tàu", "Đắk Lắk", "Bình Phước", "Đắk Nông");
        assertThat(page.regions().get(2).priceVndPerKg()).isEqualByComparingTo("136500");
    }

    @Test
    void readsEveryRegionFromGiaTieu() {
        PriceSource.PricePage page = giaTieu.parse(fixture("giatieu-2026-09-03.html"));

        assertThat(page.pageDate()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(page.regions()).extracting(PriceSource.RegionPrice::region)
                .containsExactly("Gia Lai", "Bà Rịa - Vũng Tàu", "Đắk Lắk", "Bình Phước", "Đắk Nông");
        // Thousands separators, unlike giacaphe.com's data-price attribute.
        assertThat(page.regions().get(4).priceVndPerKg()).isEqualByComparingTo("137000");
    }

    /**
     * Both sites republish the same figures — giacaphe.com credits giatieu.com
     * as its data source — which is exactly why the cross-check in
     * {@link PriceIngestionService} treats agreement as weak evidence.
     */
    @Test
    void theTwoSourcesAgreeOnTheSameDay() {
        assertThat(giaCaPhe.parse(fixture("giacaphe-2026-09-03.html")).nationalAveragePrice())
                .isEqualByComparingTo(giaTieu.parse(fixture("giatieu-2026-09-03.html")).nationalAveragePrice());
    }

    @Test
    void averagesTheRegionsIntoTheNationalFigure() {
        // (135000 + 135000 + 136500 + 135000 + 137000) / 5
        assertThat(giaCaPhe.parse(fixture("giacaphe-2026-09-03.html")).nationalAveragePrice())
                .isEqualByComparingTo(new BigDecimal("135700.00"));
    }

    @Test
    void refusesAPageItCannotRecognise() {
        assertThatThrownBy(() -> giaCaPhe.parse("<html><body>Bảo trì hệ thống</body></html>"))
                .isInstanceOf(PriceScrapeException.class)
                .hasMessageContaining("No price rows found");

        assertThatThrownBy(() -> giaTieu.parse("<html><body>Bảo trì hệ thống</body></html>"))
                .isInstanceOf(PriceScrapeException.class)
                .hasMessageContaining("No price rows found");
    }
}
