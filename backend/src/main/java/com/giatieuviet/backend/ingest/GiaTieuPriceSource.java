package com.giatieuviet.backend.ingest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * giatieu.com — the fallback, used when the primary source fails.
 *
 * robots.txt re-verified 2026-09-04: {@code User-agent: *} disallows only
 * /gia/ and /quotes/ (prefix rules, so /gia-tieu-hom-nay/ is not covered), and
 * sets no crawl delay.
 *
 * The markup it parses:
 * <pre>
 *   &lt;tr&gt;&lt;td&gt;Đắk Lắk&lt;/td&gt;&lt;td class="td-v-gia"&gt;136,500&lt;/td&gt;...
 * </pre>
 * The date comes from {@code <time datetime="YYYY-MM-DDTHH:MM:SS+07:00">}.
 */
@Component
@Order(2)
class GiaTieuPriceSource extends HtmlPriceSource {

    static final String SOURCE_NAME = "giatieu.com/gia-tieu-hom-nay";

    private static final Pattern PAGE_DATE = Pattern.compile(
            "<time\\s+datetime=['\"](\\d{4})-(\\d{2})-(\\d{2})T");

    private static final Pattern ROW = Pattern.compile(
            "<tr>\\s*<td>([^<]+)</td>\\s*<td\\s+class=['\"]td-v-gia['\"]>([\\d,]+)</td>");

    GiaTieuPriceSource(RestClient.Builder builder,
                       @Value("${app.ingest.price.giatieu-url:https://giatieu.com/gia-tieu-hom-nay/}") String url) {
        super(builder, url);
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
            regions.add(new RegionPrice(
                    rows.group(1).strip(),
                    new BigDecimal(rows.group(2).replace(",", ""))));
        }
        if (regions.isEmpty()) {
            throw new PriceScrapeException(
                    "No price rows found on " + SOURCE_NAME + " — the page layout has probably changed");
        }
        return new PricePage(parsePageDate(html), regions);
    }

    private static LocalDate parsePageDate(String html) {
        Matcher date = PAGE_DATE.matcher(html);
        if (!date.find()) {
            return LocalDate.now();
        }
        return LocalDate.of(
                Integer.parseInt(date.group(1)),
                Integer.parseInt(date.group(2)),
                Integer.parseInt(date.group(3)));
    }
}
