import Image from "next/image";
import type { Metadata } from "next";
import { SiteHeader } from "@/components/SiteHeader";
import { SiteFooter } from "@/components/SiteFooter";
import { WeatherProvinceCard } from "@/components/WeatherProvinceCard";
import { getProvinceWeather } from "@/lib/api";
import hillsBanner from "@/assets/images/hills-banner.jpg";

export const metadata: Metadata = {
  title: "Thời tiết vùng trồng tiêu — Giá Tiêu Việt",
  description: "Dự báo 7 ngày nhiệt độ, lượng mưa và gió tại 6 tỉnh trồng tiêu trọng điểm Việt Nam.",
};

export default async function WeatherPage() {
  const provinces = await getProvinceWeather();

  return (
    <>
      <SiteHeader active="/weather" />
      <main className="flex flex-col gap-9 px-16 py-14">
        <div className="relative h-55 overflow-hidden rounded-[32px_32px_128px_32px]">
          <Image
            src={hillsBanner}
            alt="Vùng trồng tiêu Tây Nguyên"
            fill
            sizes="100vw"
            className="object-cover"
            priority
          />
          <div className="absolute inset-0 bg-linear-to-b from-forest-deep/5 from-55% to-forest-deep/60" />
        </div>

        <section className="flex max-w-3xl flex-col gap-3.5">
          <span className="flex items-center gap-2 text-[11px] font-bold uppercase tracking-[0.28em] text-overline">
            <span className="h-px w-10 bg-orange-bright" />
            Vùng trồng trọng điểm
          </span>
          <h1 className="font-display text-[56px] leading-[0.96] font-bold tracking-[-0.02em] text-forest">
            Thời tiết vùng trồng tiêu.
          </h1>
          <p className="text-base leading-[1.7] text-body">
            Theo dõi nhiệt độ, lượng mưa và gió tại 6 tỉnh trồng tiêu trọng điểm. Đây là một trong
            các yếu tố đầu vào của mô hình dự báo giá — mưa lớn hoặc hạn hán kéo dài ảnh hưởng trực
            tiếp đến sản lượng và tiến độ thu hoạch.
          </p>
        </section>

        <section className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
          {provinces.map((weather) => (
            <WeatherProvinceCard key={weather.province} weather={weather} />
          ))}
        </section>

        <div className="flex items-center gap-2.5 text-xs text-muted">
          <span className="rounded-full bg-cream-alt px-2 py-0.5 text-muted">DB</span>
          Dự báo — ngày chưa có số liệu quan trắc thực tế
        </div>
      </main>
      <SiteFooter />
    </>
  );
}
