import Link from "next/link";
import type { ProvinceWeather } from "@/lib/types";
import { ArrowRightIcon, WeatherConditionIcon } from "./icons";

function HillsBanner() {
  return (
    <svg viewBox="0 0 400 56" width="100%" height="52" preserveAspectRatio="none" className="block">
      <rect width="400" height="56" fill="#eff5f0" />
      <circle cx="42" cy="18" r="10" fill="var(--color-gold)" fillOpacity={0.7} />
      <path
        d="M0 43C50 32 100 41 150 35C200 28 250 41 300 35C340 30 370 38 400 32V56H0Z"
        fill="#cfe0d2"
      />
      <path
        d="M0 52C50 45 100 50 150 46C200 42 250 50 300 46C340 43 370 48 400 44V56H0Z"
        fill="var(--color-forest)"
        fillOpacity={0.9}
      />
    </svg>
  );
}

export function WeatherSnapshot({ provinces }: { provinces: ProvinceWeather[] }) {
  return (
    <div className="flex flex-col overflow-hidden rounded-3xl border border-border bg-card">
      <HillsBanner />
      <div className="flex flex-col gap-5 px-8 py-6.5">
        <div className="flex items-center justify-between">
          <h3 className="font-display text-xl font-bold text-forest">Thời tiết vùng trồng</h3>
          <Link href="/weather" className="flex items-center gap-1 text-[13px] font-bold text-orange">
            Xem 7 ngày
            <ArrowRightIcon width={14} height={14} />
          </Link>
        </div>
        <div className="flex flex-col">
          {provinces.map((p, i) => {
            const todayInfo = p.days[0];
            return (
              <div
                key={p.province}
                className={
                  "flex items-center justify-between py-3" +
                  (i < provinces.length - 1 ? " border-b border-cream-alt" : "")
                }
              >
                <span className="flex items-center gap-2.5 text-sm font-semibold text-forest">
                  <span className="flex h-6.5 w-6.5 shrink-0 items-center justify-center rounded-full bg-badge-bg">
                    <WeatherConditionIcon condition={todayInfo.condition} width={16} height={16} className="text-forest" />
                  </span>
                  {p.province}
                </span>
                <span className="text-[13px] tabular-nums text-body">
                  {Math.round(todayInfo.tempC)}°C ·{" "}
                  <span className="text-rain">{todayInfo.rainMm}mm</span>
                </span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
