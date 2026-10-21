import Link from "next/link";
import type { ProvinceWeather } from "@/lib/types";
import { ArrowRightIcon, WeatherConditionIcon } from "./icons";

export function WeatherSnapshot({ provinces }: { provinces: ProvinceWeather[] }) {
  return (
    <div className="card overflow-hidden">
      <div className="flex items-center justify-between border-b border-line px-5 py-4">
        <h3 className="text-sm font-semibold tracking-tight">Thời tiết vùng trồng</h3>
        <Link href="/weather" className="flex items-center gap-1 text-xs font-medium text-accent hover:underline">
          Xem 7 ngày
          <ArrowRightIcon width={12} height={12} aria-hidden />
        </Link>
      </div>
      <ul className="divide-y divide-line">
        {provinces.map((p) => {
          const todayInfo = p.days[0];
          return (
            <li key={p.province} className="flex items-center justify-between px-5 py-3 text-sm">
              <span className="flex items-center gap-2.5 font-medium">
                <WeatherConditionIcon
                  condition={todayInfo.condition}
                  width={16}
                  height={16}
                  className="text-muted"
                  aria-hidden
                />
                {p.province}
              </span>
              <span className="tabular-nums text-body">
                {Math.round(todayInfo.tempC)}°C
                <span className="px-1.5 text-line">|</span>
                <span className="text-rain">{todayInfo.rainMm} mm</span>
              </span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
