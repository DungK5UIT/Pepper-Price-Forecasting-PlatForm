package com.giatieuviet.backend.service;

import com.giatieuviet.backend.api.dto.PeriodStatResponse;
import com.giatieuviet.backend.api.dto.PricePointResponse;
import com.giatieuviet.backend.api.dto.RegionPriceResponse;
import com.giatieuviet.backend.api.dto.TodayPriceResponse;
import com.giatieuviet.backend.domain.Granularity;

import java.util.List;

/**
 * Price and forecast data the public API serves. The implementation is the
 * seam where persistence lands: today it is an in-memory stub, later a
 * PostgreSQL-backed one, without controllers or DTOs changing.
 */
public interface PriceService {

    TodayPriceResponse todayPrice();

    List<RegionPriceResponse> regionPrices();

    List<PricePointResponse> forecastSeries(Granularity granularity);

    List<PeriodStatResponse> periodStats();
}
