package com.giatieuviet.backend.forecast;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Calls the Python ML service's internal API (ADR-0002: the backend
 * orchestrates, the ML service computes).
 *
 * The call is synchronous, as `docs/architecture/overview.md` says it should
 * be until there is a measured reason for a queue.
 */
@Component
public class MlForecastClient {

    private final RestClient restClient;

    public MlForecastClient(RestClient.Builder builder, @Value("${app.ml-service.base-url}") String baseUrl) {
        // Pinned to HTTP/1.1: the JDK client otherwise opens with an h2c upgrade
        // request, which uvicorn rejects — and the body is lost with it.
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    public ForecastRun generate(ForecastRequest request) {
        return restClient.post()
                .uri("/internal/v1/forecast")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ForecastRun.class);
    }

    public record PriceObservation(LocalDate date, double priceVnd) {
    }

    public record ForecastRequest(
            LocalDate asOfDate,
            double anchorPrice,
            List<PriceObservation> history,
            int horizonMonths) {
    }

    public record ForecastPoint(LocalDate targetDate, double q10, double q50, double q90, boolean interpolated) {
    }

    /** Points are keyed by granularity: "day", "week", "month". */
    public record ForecastRun(
            String modelVersion,
            LocalDate asOfDate,
            String strategy,
            Map<String, List<ForecastPoint>> points) {
    }
}
