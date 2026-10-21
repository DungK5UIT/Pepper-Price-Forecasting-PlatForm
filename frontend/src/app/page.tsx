import { SiteHeader } from "@/components/SiteHeader";
import { SiteFooter } from "@/components/SiteFooter";
import { PriceKpis } from "@/components/PriceKpis";
import { ForecastSection } from "@/components/ForecastSection";
import { RegionPriceTable } from "@/components/RegionPriceTable";
import { WeatherSnapshot } from "@/components/WeatherSnapshot";
import {
  getForecastSeries,
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
  const [today, series, stats, regions, provinces] = await Promise.all([
    getTodayPrice(),
    getForecastSeries(),
    getPeriodStats(),
    getRegionPrices(),
    getProvinceWeather(),
  ]);

  return (
    <>
      <SiteHeader active="/" />
      <main className="page-container flex flex-col gap-6 py-8 sm:py-10">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">Giá tiêu hôm nay</h1>
          <p className="max-w-2xl text-sm leading-relaxed text-muted">
            Dự báo giá tiêu Việt Nam theo ngày, tuần, tháng từ chuỗi giá lịch sử, kèm khoảng dao
            động có thể xảy ra. Giá và thời tiết vùng trồng được thu thập mỗi sáng.
          </p>
        </div>

        <PriceKpis today={today} />
        <ForecastSection series={series} stats={stats} />
        <section className="grid grid-cols-1 items-start gap-4 lg:grid-cols-[1.3fr_1fr]">
          <RegionPriceTable regions={regions} />
          <WeatherSnapshot provinces={provinces} />
        </section>
      </main>
      <SiteFooter />
    </>
  );
}
