import { SiteHeader } from "@/components/SiteHeader";
import { SiteFooter } from "@/components/SiteFooter";
import { HeroSection } from "@/components/HeroSection";
import { PhotoBand } from "@/components/PhotoBand";
import { ForecastSection } from "@/components/ForecastSection";
import { RegionPriceTable } from "@/components/RegionPriceTable";
import { WeatherSnapshot } from "@/components/WeatherSnapshot";
import { MarketInsightCard } from "@/components/MarketInsightCard";
import {
  getForecastSeries,
  getMarketInsight,
  getPeriodStats,
  getProvinceWeather,
  getRegionPrices,
  getTodayPrice,
} from "@/lib/api";

/**
 * Rendered per request rather than prerendered at build.
 *
 * The page reads everything from the backend, so a statically generated build
 * would need the backend reachable from wherever the image is built — a CI
 * runner, usually, where it is not. Trading build-time rendering for a query
 * per request is cheap here: the backend is one hop away on the internal
 * network and the queries are small reads.
 */
export const dynamic = "force-dynamic";

export default async function DashboardPage() {
  const [today, series, stats, regions, provinces, insight] = await Promise.all([
    getTodayPrice(),
    getForecastSeries(),
    getPeriodStats(),
    getRegionPrices(),
    getProvinceWeather(),
    getMarketInsight(),
  ]);

  return (
    <>
      <SiteHeader active="/" />
      <HeroSection today={today} />
      <main className="flex flex-col gap-10 px-16 py-14">
        <PhotoBand />
        <ForecastSection series={series} stats={stats} />
        <section className="grid grid-cols-1 items-start gap-5 lg:grid-cols-[1.3fr_1fr]">
          <RegionPriceTable regions={regions} />
          <WeatherSnapshot provinces={provinces} />
        </section>
        <MarketInsightCard insight={insight} />
      </main>
      <SiteFooter />
    </>
  );
}
