# Test Cases and Traceability

| Version | Date | Change |
|---|---|---|
| 1.0 | 2026-09-30 | First version: every requirement in [`requirements.md`](requirements.md) mapped to the tests that exist, and the gaps named. |

Each test case (`TC-xx`) verifies one requirement (`FR-xx` / `NFR-xx`). The
**Test** column names the real test method; **Missing** means the case is
specified here but no test exists yet — these are the next tests to write.

**Status on 2026-09-30:** backend 58 tests pass (local run 2026-09-30),
ML service 21 pass. Coverage: ML service 67% statements + branches
(`train.py` 0%, naive strategy untested); backend not measured yet (no
JaCoCo). Frontend has lint and build in CI, no tests.

| Status | Count |
|---|---|
| Covered | 44 |
| Partial — shape or one path only | 7 |
| Missing | 22 |
| **Total** | **73** |

## How to run

```bash
cd backend    && ./mvnw test                     # JUnit 5, Spring Boot slices, H2
cd ml-service && pytest                          # add --cov=ml_service --cov-branch for coverage
cd frontend   && npm run lint && npm run build
```

Test levels: **U** unit · **S** Spring slice (`@WebMvcTest`, service mocked)
· **I** integration (Spring context + H2, or FastAPI `TestClient`) · **M**
manual.

## Public API

Controller tests mock the service, so they prove the JSON shape and status
codes, not the numbers. The calculations in `DatabasePriceService` and
`DatabaseWeatherService` have no test yet.

| TC | Verifies | Test | Level | Status |
|---|---|---|---|---|
| TC-01 | FR-01 today's price shape | `PriceControllerTest › todayReturnsTheCurrentPrice` | S | Partial |
| TC-02 | FR-01 change %, 1 decimal; 0 when there is one observation | — | U/I | Missing |
| TC-03 | FR-01 range = forecast point nearest as-of + 7 days; = price when no forecast | — | I | Missing |
| TC-04 | FR-02 one entry per region, sorted | `PriceControllerTest › regionsReturnsOneEntryPerRegion` | S | Partial |
| TC-05 | FR-02 change against the region's own previous observation | — | I | Missing |
| TC-06 | FR-03 defaults to `day` | `PriceControllerTest › forecastDefaultsToDailyGranularity` | S | Covered |
| TC-07 | FR-03 unsupported granularity → 400 problem detail | `PriceControllerTest › forecastRejectsAnUnsupportedGranularity`, `GlobalExceptionHandlerTest › aValueTheDomainRefusesIsStillABadRequest` | S | Covered |
| TC-08 | FR-03 history bucketed (15 days / 10 ISO weeks / 12 months), last point `isToday` | — | I | Missing |
| TC-09 | FR-03 forecast points limited to 14 / 8 / 6 from the latest run | — | I | Missing |
| TC-10 | FR-04 stats shape | `PriceControllerTest › statsReturnsTheConfiguredPeriods` | S | Partial |
| TC-11 | FR-04 30/90/365-day change; 0 without a baseline | — | I | Missing |
| TC-12 | FR-05 days per province, hyphenated condition codes | `WeatherControllerTest › weatherReturnsDaysPerProvinceWithHyphenatedConditionCodes` | S | Covered |
| TC-13 | FR-05 forecast days flagged | `WeatherControllerTest › weatherMarksForecastDays` | S | Covered |
| TC-14 | FR-05 condition thresholds (10 mm / 25 km/h / 3 mm / 0.5 mm) and "Hôm nay" label | — | U | Missing |
| TC-15 | FR-06 commentary text and date label | `MarketInsightControllerTest › insightReturnsTextAndUpdatedLabel` | S | Partial |
| TC-16 | FR-07 unknown path → 404 | `GlobalExceptionHandlerTest › anUnknownPathIsNotFound` | I | Covered |
| TC-17 | FR-07 write → 401 before routing | `GlobalExceptionHandlerTest › aWriteToTheReadOnlyApiIsRefusedBeforeItIsRouted` | I | Covered |
| TC-18 | FR-07 unset fields omitted from JSON | `PriceControllerTest › forecastOmitsUnsetQuantilesOnHistoricalPoints` | S | Covered |

## Price collection

| TC | Verifies | Test | Level | Status |
|---|---|---|---|---|
| TC-19 | FR-08, FR-11 every region plus the national average stored | `PriceIngestionServiceTest › storesEveryRegionPlusTheNationalAverage` | I | Covered |
| TC-20 | FR-08 both sources parsed from saved pages | `PriceSourceParsingTest › readsEveryRegionFromGiaCaPhe`, `› readsEveryRegionFromGiaTieu`, `› theTwoSourcesAgreeOnTheSameDay` | U | Covered |
| TC-21 | FR-09 page without price rows refused | `PriceSourceParsingTest › refusesAPageItCannotRecognise` | U | Covered |
| TC-22 | FR-09 price outside 10,000–1,000,000 refused | — | U | Missing |
| TC-23 | FR-10 fallback recorded as `partial` | `PriceIngestionServiceTest › fallsBackToTheNextSourceAndSaysSoInTheRunLog` | I | Covered |
| TC-24 | FR-10 disagreement > 5% flagged, primary stored | `PriceIngestionServiceTest › flagsTheRunWhenTheSourcesDisagreeButStillStoresThePrimary` | I | Covered |
| TC-25 | FR-10 differing page dates flagged | `PriceIngestionServiceTest › flagsTheRunWhenOneSourceIsServingAnOlderDay` | I | Covered |
| TC-26 | FR-11 national = mean of regions | `PriceSourceParsingTest › averagesTheRegionsIntoTheNationalFigure` | U | Covered |
| TC-27 | FR-11 re-run corrects rows, no duplicates | `PriceIngestionServiceTest › rerunningTheSameDayCorrectsTheRowsRatherThanDuplicatingThem` | I | Covered |
| TC-28 | FR-12 no source → `failed` run, nothing stored | `PriceIngestionServiceTest › recordsAFailedRunAndStoresNothingWhenNoSourceCanBeRead` | I | Covered |
| TC-29 | FR-12 a collection run at startup does not fail the boot | `PriceIngestionServiceTest › aBrokenCollectionDoesNotPropagateToTheCaller` | I | Covered |
| TC-30 | FR-13 UTF-8 decoding of a response without charset | — | U | Missing |

## Weather collection

| TC | Verifies | Test | Level | Status |
|---|---|---|---|---|
| TC-31 | FR-14 parallel arrays zipped into days | `WeatherSourceTest › zipsTheParallelArraysIntoDays` | U | Covered |
| TC-32 | FR-14 all six provinces requested | `WeatherSourceTest › coversEveryProvinceThePlatformShows` | U | Covered |
| TC-33 | FR-15 only future days marked forecast | `WeatherSourceTest › marksOnlyFutureDaysAsForecast` | U | Covered |
| TC-34 | FR-15 forecast row replaced by the observation | `WeatherIngestionServiceTest › replacesYesterdaysForecastWithWhatActuallyHappened` | I | Covered |
| TC-35 | FR-15 rows stored per province and day | `WeatherIngestionServiceTest › storesEveryDayItIsGiven`, `› keepsProvincesApartOnTheSameDay` | I | Covered |
| TC-36 | FR-16 provider unreachable → `failed` run, not propagated | `WeatherIngestionServiceTest › recordsAFailedRunWhenTheProviderIsUnreachable`, `› aBrokenCollectionDoesNotPropagateToTheScheduler` | I | Covered |
| TC-37 | FR-16 one province failing stores none of the six | — (source is mocked in the service test) | U | Missing |

## Forecast refresh and generation

| TC | Verifies | Test | Level | Status |
|---|---|---|---|---|
| TC-38 | FR-17 request anchored on the latest observed price | `ForecastRefreshServiceTest › anchorsTheRequestOnTheLatestObservedPrice` | I | Covered |
| TC-39 | FR-18 every returned point stored | `ForecastRefreshServiceTest › storesEveryPointTheModelReturns` | I | Covered |
| TC-40 | FR-18 same-day re-run replaces the run | `ForecastRefreshServiceTest › rerunningReplacesTheDaysRunRatherThanDuplicatingIt` | I | Covered |
| TC-41 | FR-19 ML failure keeps the previous run | `ForecastRefreshServiceTest › aFailingMlServiceLeavesThePreviousRunInPlace` | I | Covered |
| TC-42 | FR-20 day, week and month returned | `test_api.py › test_forecast_returns_all_three_granularities` | I | Partial — GBM model only |
| TC-43 | FR-20 monthly targets on month ends | `test_api.py › test_monthly_targets_land_on_month_ends_after_the_as_of_date` | I | Covered — see RISK-04 |
| TC-44 | FR-20 q10 ≤ q50 ≤ q90 everywhere | `test_api.py › test_quantiles_are_ordered_everywhere` | I | Partial — GBM model only |
| TC-45 | FR-20 one horizon can be requested | `test_api.py › test_a_single_horizon_can_be_requested` | I | Covered |
| TC-46 | FR-20 interpolation: median path, √t band, weekly sampling, implied sigma | `test_interpolate.py` (7 tests) | U | Covered |
| TC-47 | FR-20 naive strategy: median = anchor, band from monthly sigma | — | U | Missing |
| TC-48 | FR-21 empty history → 422 | `test_api.py › test_rejects_a_request_without_history` | I | Partial — fails with the naive model (FR-21 Gap) |
| TC-49 | FR-21 non-positive price, horizon 3 → 422; no model → 503 | — | I | Missing |
| TC-50 | FR-22 health reports the model; `DEGRADED` without one; open without token | `test_api.py › test_health_reports_the_loaded_model`, `› test_reports_degraded_when_no_model_is_trained`, `› test_health_stays_open_with_no_token` | I | Covered |

## Model training

| TC | Verifies | Test | Level | Status |
|---|---|---|---|---|
| TC-51 | FR-23 monthly aggregation, lags, seasonality, target, latest row | `test_features.py` (5 tests) | U | Covered |
| TC-52 | FR-23 training refuses to run without the internal credential | — | U | Missing |
| TC-53 | FR-24 backtest scores both strategies and keeps the lower mean pinball | — | U | Missing |

## Access control

| TC | Verifies | Test | Level | Status |
|---|---|---|---|---|
| TC-54 | FR-25 public API and health open | `SecurityConfigTest › thePublicApiStaysOpen`, `› healthIsReadableAnonymously` | I | Covered |
| TC-55 | FR-25 health breakdown hidden from anonymous callers | — | I | Missing |
| TC-56 | FR-26 internal endpoint: anonymous, wrong password, right credential | `SecurityConfigTest › theInternalEndpointRefusesAnAnonymousCaller`, `› theInternalEndpointRefusesAWrongPassword`, `› theInternalEndpointServesTheMlServiceCredential` | I | Covered |
| TC-57 | FR-26 unlisted endpoint closed; job triggers closed to anonymous | `SecurityConfigTest › anUnlistedActuatorEndpointIsClosedByDefault`, `› theJobTriggerEndpointsRefuseAnAnonymousCaller` | I | Covered |
| TC-58 | FR-20 ML forecast refuses a caller without the token | `test_api.py › test_forecast_refuses_a_caller_with_no_token` | I | Covered |
| TC-59 | FR-27 services refuse to start without their credentials | — | I | Missing |
| TC-60 | FR-28 CORS: allowed origin gets headers, other origins do not | — | S | Missing |

## Operations

| TC | Verifies | Test | Level | Status |
|---|---|---|---|---|
| TC-61 | FR-29 every attempt logged with status | covered by TC-23…TC-29, TC-36 | I | Covered |
| TC-62 | FR-30 `UP` while jobs succeed; `STALE` past the threshold; one quiet job is enough | `IngestionHealthIndicatorTest` (3 tests) | I | Covered |
| TC-63 | FR-30 `failed` runs do not count; `partial` runs do | `IngestionHealthIndicatorTest › aRunThatCollectedNothingDoesNotCountAsRecentActivity`, `› aPartialRunStillCountsAsCollected` | I | Covered |
| TC-64 | FR-31 job endpoints trigger the services | `JobTriggerControllerTest` (3 tests) | S | Covered |
| TC-65 | FR-31 scheduled run order and wake-up | Manual: run the workflow with *Run workflow* and read its log | M | Covered — manual |
| TC-66 | FR-32 trading day pinned to Vietnam, not the host | `TimeConfigTest` (3 tests) | U | Covered |
| TC-67 | NFR-03 a failed job returns non-2xx so the scheduler run fails | `JobTriggerControllerTest › aPriceRunWithNoReadableSourceIsABadGateway`, `› aFailedWeatherRunIsNotReportedAsSuccess`, `› aFailedForecastRefreshIsNotReportedAsSuccess` | S | Covered |
| TC-68 | NFR-04 outbound call times out instead of hanging | — | I | Missing |
| TC-69 | NFR-09 shipped model beats or equals the baseline | Evidence: `ml-service/ml_service/artifacts/metrics.json` | M | Covered — manual |

## Website

| TC | Verifies | Test | Level | Status |
|---|---|---|---|---|
| TC-70 | FR-33 dashboard renders every section from API data | — | Component/E2E | Missing |
| TC-71 | FR-33 day/week/month switch changes the chart series | — | Component | Missing |
| TC-72 | FR-34 weather page renders six provinces, forecast days marked | — | Component | Missing |
| TC-73 | FR-35 backend failure shows the error page with reload | — | E2E | Missing |

## What to write next

In order of risk (see [`risks.md`](risks.md)):

1. TC-68 — together with the fix for RISK-02.
2. TC-02, TC-03, TC-05, TC-08, TC-09, TC-11, TC-14 — the calculations the
   public sees, now untested; add JaCoCo to measure C0/C1.
3. TC-47, TC-48 with the naive model, TC-53 — the model that actually ships.
4. TC-22, TC-37, TC-55, TC-60 — cheap guards on existing behaviour.
5. TC-70 – TC-73 — introduce a frontend test runner.
