package com.giatieuviet.backend.service.db;

import com.giatieuviet.backend.api.dto.PeriodStatResponse;
import com.giatieuviet.backend.api.dto.PricePointResponse;
import com.giatieuviet.backend.api.dto.RegionPriceResponse;
import com.giatieuviet.backend.api.dto.TodayPriceResponse;
import com.giatieuviet.backend.domain.Granularity;
import com.giatieuviet.backend.persistence.Forecast;
import com.giatieuviet.backend.persistence.ForecastRepository;
import com.giatieuviet.backend.persistence.MarketPrice;
import com.giatieuviet.backend.persistence.MarketPriceRepository;
import com.giatieuviet.backend.service.PriceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Serves prices and forecasts from the database.
 *
 * The nationwide series (region {@link #NATIONAL_REGION}) is the headline
 * number and the basis of the chart; the other regions are shown as a
 * breakdown. Everything is anchored on the latest observed date rather than
 * today's date, because a day's prices are collected during that day and the
 * newest row is routinely yesterday's.
 */
@Service
public class DatabasePriceService implements PriceService {

    private static final String NATIONAL_REGION = "national";
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");
    private static final String TODAY_LABEL = "Hôm nay";

    /** How far ahead "next week" looks when quoting a range on the headline price. */
    private static final int HEADLINE_FORECAST_HORIZON_DAYS = 7;

    private static final Map<Granularity, Integer> HISTORY_POINTS =
            Map.of(Granularity.DAY, 15, Granularity.WEEK, 10, Granularity.MONTH, 12);
    private static final Map<Granularity, Integer> FORECAST_POINTS =
            Map.of(Granularity.DAY, 14, Granularity.WEEK, 8, Granularity.MONTH, 6);

    private final MarketPriceRepository marketPrices;
    private final ForecastRepository forecasts;

    public DatabasePriceService(MarketPriceRepository marketPrices, ForecastRepository forecasts) {
        this.marketPrices = marketPrices;
        this.forecasts = forecasts;
    }

    @Override
    @Transactional(readOnly = true)
    public TodayPriceResponse todayPrice() {
        List<MarketPrice> national = nationalSeries();
        if (national.isEmpty()) {
            throw new IllegalStateException("No nationwide prices have been ingested yet");
        }

        MarketPrice latest = national.get(national.size() - 1);
        long price = toVnd(latest.getPriceVndPerKg());
        long change = national.size() > 1 ? price - toVnd(national.get(national.size() - 2).getPriceVndPerKg()) : 0L;
        double changePercent = change == 0 || price == change
                ? 0.0
                : round1((double) change / (price - change) * 100);

        Forecast headline = headlineForecast(latest.getObservedDate());

        return new TodayPriceResponse(
                price,
                change,
                changePercent,
                latest.getObservedDate().format(DAY_LABEL),
                headline == null ? price : toVnd(headline.getPredictedPriceQ10()),
                headline == null ? price : toVnd(headline.getPredictedPriceQ90()),
                headline == null ? price : toVnd(headline.getPredictedPriceQ50()),
                latest.getSource());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegionPriceResponse> regionPrices() {
        Map<String, List<MarketPrice>> byRegion = new LinkedHashMap<>();
        for (MarketPrice price : marketPrices.findByRegionNotOrderByObservedDateAsc(NATIONAL_REGION)) {
            byRegion.computeIfAbsent(price.getRegion(), region -> new ArrayList<>()).add(price);
        }

        return byRegion.values().stream()
                .map(series -> {
                    MarketPrice latest = series.get(series.size() - 1);
                    long price = toVnd(latest.getPriceVndPerKg());
                    long change = series.size() > 1
                            ? price - toVnd(series.get(series.size() - 2).getPriceVndPerKg())
                            : 0L;
                    return new RegionPriceResponse(latest.getRegion(), price, change);
                })
                .sorted(Comparator.comparing(RegionPriceResponse::region))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PricePointResponse> forecastSeries(Granularity granularity) {
        List<PricePointResponse> points = new ArrayList<>(historyPoints(granularity));
        points.addAll(forecastPoints(granularity));
        return points;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PeriodStatResponse> periodStats() {
        List<MarketPrice> national = nationalSeries();
        if (national.isEmpty()) {
            return List.of();
        }

        MarketPrice latest = national.get(national.size() - 1);
        return List.of(
                periodStat("30 ngày qua", national, latest, 30),
                periodStat("90 ngày qua", national, latest, 90),
                periodStat("1 năm qua", national, latest, 365));
    }

    private List<MarketPrice> nationalSeries() {
        return marketPrices.findByRegionOrderByObservedDateAsc(NATIONAL_REGION);
    }

    /** The forecast point closest to a week out, from the most recent published run. */
    private Forecast headlineForecast(LocalDate anchor) {
        List<Forecast> daily = latestRun(Granularity.DAY);
        LocalDate target = anchor.plusDays(HEADLINE_FORECAST_HORIZON_DAYS);
        return daily.stream()
                .min(Comparator.comparingLong(f -> Math.abs(f.getTargetDate().toEpochDay() - target.toEpochDay())))
                .orElse(null);
    }

    private List<Forecast> latestRun(Granularity granularity) {
        return forecasts.findFirstByGranularityOrderByAsOfDateDesc(granularity.code())
                .map(latest -> forecasts.findByGranularityAndAsOfDateOrderByTargetDateAsc(
                        granularity.code(), latest.getAsOfDate()))
                .orElseGet(List::of);
    }

    /**
     * Historical points, bucketed to the requested granularity. The most recent
     * one is marked as today's anchor and repeats its value as the forecast
     * quantiles, so the actual line and the forecast line meet there.
     */
    private List<PricePointResponse> historyPoints(Granularity granularity) {
        List<MarketPrice> national = nationalSeries();
        if (national.isEmpty()) {
            return List.of();
        }

        Map<LocalDate, List<MarketPrice>> buckets = new LinkedHashMap<>();
        for (MarketPrice price : national) {
            buckets.computeIfAbsent(bucketOf(price.getObservedDate(), granularity), key -> new ArrayList<>())
                    .add(price);
        }

        List<Map.Entry<LocalDate, List<MarketPrice>>> recent = buckets.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();
        int keep = HISTORY_POINTS.getOrDefault(granularity, 15);
        recent = recent.subList(Math.max(0, recent.size() - keep), recent.size());

        List<PricePointResponse> points = new ArrayList<>(recent.size());
        for (int i = 0; i < recent.size(); i++) {
            Map.Entry<LocalDate, List<MarketPrice>> bucket = recent.get(i);
            long value = averageVnd(bucket.getValue());
            boolean isLast = i == recent.size() - 1;
            points.add(isLast
                    ? PricePointResponse.today(TODAY_LABEL, value)
                    : PricePointResponse.actual(labelOf(bucket.getKey(), granularity), value));
        }
        return points;
    }

    private List<PricePointResponse> forecastPoints(Granularity granularity) {
        return latestRun(granularity).stream()
                .limit(FORECAST_POINTS.getOrDefault(granularity, 14))
                .map(forecast -> PricePointResponse.forecast(
                        labelOf(forecast.getTargetDate(), granularity),
                        toVnd(forecast.getPredictedPriceQ10()),
                        toVnd(forecast.getPredictedPriceQ50()),
                        toVnd(forecast.getPredictedPriceQ90())))
                .toList();
    }

    private PeriodStatResponse periodStat(String label, List<MarketPrice> series, MarketPrice latest, int daysBack) {
        LocalDate cutoff = latest.getObservedDate().minusDays(daysBack);
        Optional<MarketPrice> baseline = series.stream()
                .filter(price -> !price.getObservedDate().isAfter(cutoff))
                .max(Comparator.comparing(MarketPrice::getObservedDate));

        if (baseline.isEmpty()) {
            return new PeriodStatResponse(label, 0.0, 0L);
        }

        long baselinePrice = toVnd(baseline.get().getPriceVndPerKg());
        long change = toVnd(latest.getPriceVndPerKg()) - baselinePrice;
        double percent = baselinePrice == 0 ? 0.0 : round1((double) change / baselinePrice * 100);
        return new PeriodStatResponse(label, percent, change);
    }

    /** Collapses a date onto the first day of its day/week/month bucket. */
    private static LocalDate bucketOf(LocalDate date, Granularity granularity) {
        return switch (granularity) {
            case DAY -> date;
            case WEEK -> date.with(IsoFields.WEEK_BASED_YEAR, date.get(IsoFields.WEEK_BASED_YEAR))
                    .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
                    .with(java.time.DayOfWeek.MONDAY);
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    private static String labelOf(LocalDate date, Granularity granularity) {
        return granularity == Granularity.MONTH
                ? "T%d/%02d".formatted(date.getMonthValue(), date.getYear() % 100)
                : date.format(DAY_LABEL);
    }

    private static long averageVnd(List<MarketPrice> prices) {
        BigDecimal total = prices.stream()
                .map(MarketPrice::getPriceVndPerKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return toVnd(total.divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP));
    }

    private static long toVnd(BigDecimal amount) {
        return amount == null ? 0L : amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
