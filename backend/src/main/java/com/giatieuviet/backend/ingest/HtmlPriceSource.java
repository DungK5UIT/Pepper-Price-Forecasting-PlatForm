package com.giatieuviet.backend.ingest;

import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * Shared plumbing for the sources that scrape an HTML page: fetch the bytes,
 * hand them to the subclass as text, and refuse prices that cannot be real.
 *
 * Parsing is separated from fetching so the parsers can be tested against
 * saved pages without touching the network.
 */
abstract class HtmlPriceSource implements PriceSource {

    /** Identifies this platform to the sites it reads, so operators can see who is calling. */
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; GiaTieuVietBot/1.0; +https://github.com/DungK5UIT/Pepper-Price-Forecasting-PlatForm)";

    /**
     * Domestic pepper has traded between roughly 35.000 and 200.000 đ/kg over
     * the last decade. A figure outside this band means the parse latched onto
     * the wrong number — a percentage change, an ad, a world price in USD —
     * and storing it would corrupt the series the forecast is anchored on.
     */
    private static final BigDecimal MIN_PLAUSIBLE_PRICE = BigDecimal.valueOf(10_000);
    private static final BigDecimal MAX_PLAUSIBLE_PRICE = BigDecimal.valueOf(1_000_000);

    private final RestClient restClient;
    private final String url;

    protected HtmlPriceSource(RestClient.Builder builder, String url) {
        this.restClient = builder.build();
        this.url = url;
    }

    @Override
    public PricePage fetch() {
        PricePage page = parse(download());
        page.regions().forEach(HtmlPriceSource::requirePlausible);
        return page;
    }

    /** @param html the page as text; implementations parse, never fetch */
    abstract PricePage parse(String html);

    /**
     * Read as bytes and decode as UTF-8 explicitly. Both sites serve
     * {@code Content-Type: text/html} with no charset, and the String
     * converter then falls back to ISO-8859-1 — which turns "Đắk Lắk" into
     * mojibake that would be stored as a region name.
     */
    private String download() {
        byte[] body = restClient.get()
                .uri(url)
                .header("User-Agent", USER_AGENT)
                .retrieve()
                .body(byte[].class);
        if (body == null) {
            throw new PriceScrapeException("Empty response from " + url);
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private static void requirePlausible(RegionPrice price) {
        if (price.priceVndPerKg().compareTo(MIN_PLAUSIBLE_PRICE) < 0
                || price.priceVndPerKg().compareTo(MAX_PLAUSIBLE_PRICE) > 0) {
            throw new PriceScrapeException(
                    "Implausible price for %s: %s đ/kg — the page layout has probably changed"
                            .formatted(price.region(), price.priceVndPerKg().toPlainString()));
        }
    }
}
