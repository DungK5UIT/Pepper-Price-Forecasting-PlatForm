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
} from "@/lib/mock-data";

export default function DashboardPage() {
  const today = getTodayPrice();
  const series = getForecastSeries();
  const stats = getPeriodStats();
  const regions = getRegionPrices();
  const provinces = getProvinceWeather();
  const insight = getMarketInsight();

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
