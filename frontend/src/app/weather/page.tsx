import type { Metadata } from "next";
import { SiteHeader } from "@/components/SiteHeader";
import { SiteFooter } from "@/components/SiteFooter";
import { WeatherProvinceCard } from "@/components/WeatherProvinceCard";
import { getProvinceWeather } from "@/lib/api";

export const metadata: Metadata = {
  title: "Thời tiết vùng trồng tiêu — Giá Tiêu Việt",
  description: "Dự báo 7 ngày nhiệt độ, lượng mưa và gió tại 6 tỉnh trồng tiêu trọng điểm Việt Nam.",
};

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

export default async function WeatherPage() {
  const provinces = await getProvinceWeather();

  return (
    <>
      <SiteHeader active="/weather" />
      <main className="page-container flex flex-col gap-6 py-8 sm:py-10">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">Thời tiết vùng trồng tiêu</h1>
          <p className="max-w-2xl text-sm leading-relaxed text-muted">
            Theo dõi nhiệt độ, lượng mưa và gió tại 6 tỉnh trồng tiêu trọng điểm — mưa lớn hoặc hạn
            hán kéo dài ảnh hưởng trực tiếp đến sản lượng và tiến độ thu hoạch. Mô hình dự báo giá
            hiện chưa dùng dữ liệu thời tiết; nó sẽ được đưa vào khi tích lũy đủ lịch sử.
          </p>
        </div>

        <section className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {provinces.map((weather) => (
            <WeatherProvinceCard key={weather.province} weather={weather} />
          ))}
        </section>

        <p className="flex items-center gap-2 text-xs text-muted">
          <span className="rounded bg-subtle px-1.5 py-0.5 text-[11px]">Dự báo</span>
          Ngày chưa có số liệu quan trắc thực tế.
        </p>
      </main>
      <SiteFooter />
    </>
  );
}
