package com.giatieuviet.backend.ingest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * giacaphe.com — the primary source, tried first.
 *
 * robots.txt re-verified 2026-09-04: the {@code User-agent: *} block disallows
 * /apps/, /media/, /quotes/, /service/, /ads/, /tools/, /files/, /services/,
 * /khachhangnhantin/, /dn/, /csdl/, /widgets/, /report/, /price/, /cdn-cgi/
 * and /live-quotes/ — not this path — and sets no crawl delay. The job reads
 * one page a day.
 *
 * The markup it parses:
 * <pre>
 *   &lt;td class='gnd_market'&gt;Đắk Lắk&lt;/td&gt;
 *   &lt;td class='gnd-gia' data-price='136500'&gt;136,500&lt;/td&gt;
 * </pre>
 * The date comes from the {@code <h1>}: "Giá tiêu hôm nay ngày DD/MM/YYYY".
 */
@Component
@Order(1)
class GiaCaPhePriceSource extends HtmlPriceSource {

    static final String SOURCE_NAME = "giacaphe.com/gia-tieu-hom-nay";

    private static final Pattern PAGE_DATE = Pattern.compile(
            "Giá tiêu hôm nay ngày\\s*(\\d{1,2})/(\\d{1,2})/(\\d{4})");

    private static final Pattern ROW = Pattern.compile(
            "<td\\s+class=['\"]gnd_market['\"]>([^<]+)</td>\\s*"
                    + "<td\\s+class=['\"]gnd-gia['\"]\\s+data-price=['\"](\\d+)['\"]");

    GiaCaPhePriceSource(RestClient.Builder builder,
                        @Value("${app.ingest.price.giacaphe-url:https://giacaphe.com/gia-tieu-hom-nay/}") String url,
                        Clock clock) {
        super(builder, url, clock);
    }

    @Override
    public String name() {
        return SOURCE_NAME;
    }

    @Override
    PricePage parse(String html) {
        List<RegionPrice> regions = new ArrayList<>();
        Matcher rows = ROW.matcher(html);
        while (rows.find()) {
            regions.add(new RegionPrice(rows.group(1).strip(), new BigDecimal(rows.group(2))));
        }
        if (regions.isEmpty()) {
            throw new PriceScrapeException(
                    "No price rows found on " + SOURCE_NAME + " — the page layout has probably changed");
        }
        return new PricePage(pageDate(html), regions);
    }

    /**
     * Falls back to today when the heading is missing. The page is published
     * each morning, so a missing date is far more likely to be a heading
     * reword than a stale page — and the plausibility check still guards the
     * numbers themselves.
     */
    private LocalDate pageDate(String html) {
        Matcher date = PAGE_DATE.matcher(html);
        if (!date.find()) {
            return today();
        }
        return LocalDate.of(
                Integer.parseInt(date.group(3)),
                Integer.parseInt(date.group(2)),
                Integer.parseInt(date.group(1)));
    }
}
