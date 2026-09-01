package com.giatieuviet.backend.service.stub;

import com.giatieuviet.backend.api.dto.PeriodStatResponse;
import com.giatieuviet.backend.api.dto.PricePointResponse;
import com.giatieuviet.backend.api.dto.RegionPriceResponse;
import com.giatieuviet.backend.api.dto.TodayPriceResponse;
import com.giatieuviet.backend.domain.Granularity;
import com.giatieuviet.backend.service.PriceService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * TEMPORARY in-memory data source, standing in for the PostgreSQL-backed
 * implementation until the database is wired up. It exists so the API
 * contract and the frontend can be developed against something real-shaped;
 * the numbers are illustrative, not observed.
 */
@Service
public class StubPriceService implements PriceService {

    private static final long TODAY_PRICE_VND = 148_700L;
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");
    private static final String TODAY_LABEL = "Hôm nay";

    @Override
    public TodayPriceResponse todayPrice() {
        return new TodayPriceResponse(
                TODAY_PRICE_VND,
                1_200L,
                0.82,
                LocalDate.now().format(DAY_LABEL),
                146_000L,
                152_000L,
                149_000L,
                "Bình quân 6 tỉnh trọng điểm");
    }

    @Override
    public List<RegionPriceResponse> regionPrices() {
        return List.of(
                new RegionPriceResponse("Đắk Lắk", 149_200L, 1_500L),
                new RegionPriceResponse("Đắk Nông", 148_900L, 1_100L),
                new RegionPriceResponse("Gia Lai", 147_600L, 900L),
                new RegionPriceResponse("Đồng Nai", 148_300L, -300L),
                new RegionPriceResponse("Bình Phước", 149_800L, 1_800L),
                new RegionPriceResponse("Bà Rịa - Vũng Tàu", 147_900L, 200L));
    }

    @Override
    public List<PricePointResponse> forecastSeries(Granularity granularity) {
        return switch (granularity) {
            case DAY -> buildSeries(15, 7, 130, offset -> LocalDate.now().plusDays(offset).format(DAY_LABEL));
            case WEEK -> buildSeries(10, 6, 620, offset -> LocalDate.now().plusWeeks(offset).format(DAY_LABEL));
            case MONTH -> buildSeries(12, 6, 1_650, offset -> monthLabel(LocalDate.now().plusMonths(offset)));
        };
    }

    @Override
    public List<PeriodStatResponse> periodStats() {
        return List.of(
                new PeriodStatResponse("30 ngày qua", 4.2, 6_100L),
                new PeriodStatResponse("90 ngày qua", 9.8, 13_300L),
                new PeriodStatResponse("1 năm qua", 18.5, 23_200L));
    }

    /**
     * Builds a series that drifts upward into today's known price, then keeps
     * drifting with a confidence band that widens the further out it forecasts.
     */
    private List<PricePointResponse> buildSeries(int historyCount, int forecastCount, int stepDrift,
            IntFunction<String> labelForOffset) {
        List<Long> history = new ArrayList<>(historyCount);
        double price = TODAY_PRICE_VND - (double) stepDrift * historyCount;
        for (int i = 0; i < historyCount; i++) {
            price += stepDrift + noise(i) * stepDrift * 2.5;
            history.add(Math.round(price));
        }
        // Anchor the last historical point exactly on today's published price.
        long shift = TODAY_PRICE_VND - history.get(historyCount - 1);

        List<PricePointResponse> points = new ArrayList<>(historyCount + forecastCount);
        for (int i = 0; i < historyCount; i++) {
            int offset = -(historyCount - 1 - i);
            long value = history.get(i) + shift;
            points.add(i == historyCount - 1
                    ? PricePointResponse.today(TODAY_LABEL, value)
                    : PricePointResponse.actual(labelForOffset.apply(offset), value));
        }

        double forecastPrice = TODAY_PRICE_VND;
        for (int i = 1; i <= forecastCount; i++) {
            forecastPrice += stepDrift * 1.1 + noise(historyCount + i) * stepDrift;
            long spread = (long) stepDrift * 2 * i;
            long median = Math.round(forecastPrice);
            points.add(PricePointResponse.forecast(labelForOffset.apply(i), median - spread, median, median + spread));
        }
        return points;
    }

    /** Deterministic pseudo-random noise in [-0.5, 0.5], so the series is stable across calls. */
    private static double noise(int seed) {
        double x = Math.sin(seed * 12.9898) * 43_758.5453;
        return (x - Math.floor(x)) - 0.5;
    }

    private static String monthLabel(LocalDate date) {
        return "T%d/%02d".formatted(date.getMonthValue(), date.getYear() % 100);
    }
}
