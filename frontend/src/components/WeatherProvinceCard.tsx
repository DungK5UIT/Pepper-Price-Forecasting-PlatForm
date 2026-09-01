import type { ProvinceWeather } from "@/lib/types";
import { WeatherConditionIcon } from "./icons";

function HillsBanner() {
  return (
    <svg viewBox="0 0 400 60" width="100%" height="56" preserveAspectRatio="none" className="block">
      <rect width="400" height="60" fill="#eff5f0" />
      <circle cx="358" cy="18" r="10" fill="var(--color-gold)" fillOpacity={0.7} />
      <path
        d="M0 46C50 34 100 44 150 37C200 30 250 44 300 37C340 31 370 40 400 34V60H0Z"
        fill="#cfe0d2"
      />
      <path
        d="M0 56C50 48 100 54 150 49C200 44 250 54 300 49C340 45 370 51 400 47V60H0Z"
        fill="var(--color-forest)"
        fillOpacity={0.9}
      />
    </svg>
  );
}

export function WeatherProvinceCard({ weather }: { weather: ProvinceWeather }) {
  const today = weather.days[0];

  return (
    <div className="flex flex-col overflow-hidden rounded-3xl border border-border bg-card">
      <HillsBanner />
      <div className="flex flex-col gap-4.5 px-7 py-5.5">
        <div className="flex items-center justify-between">
          <div className="flex flex-col gap-0.5">
            <span className="font-display text-[19px] font-semibold text-forest">{weather.province}</span>
            <span className="text-xs text-muted">Hôm nay</span>
          </div>
          <div className="flex items-center gap-2">
            <WeatherConditionIcon condition={today.condition} width={26} height={26} className="text-forest" />
            <span className="font-display text-[26px] font-semibold text-forest">{today.tempMax}°C</span>
          </div>
        </div>
        <div className="h-px bg-cream-alt" />
        <div>
          {weather.days.map((day, i) => (
            <div
              key={day.label + i}
              className="grid grid-cols-[64px_22px_1fr_auto_44px] items-center gap-2.5 border-b border-cream-alt py-2.25 last:border-none"
            >
              <span className="text-xs font-bold text-forest">{day.label}</span>
              <WeatherConditionIcon condition={day.condition} width={18} height={18} className="text-body" />
              <span className="text-xs text-body">
                {day.tempMin}–{day.tempMax}°C
              </span>
              <span className="justify-self-end text-xs text-rain">{day.rainMm}mm</span>
              {day.isForecast ? (
                <span className="justify-self-end rounded-full bg-cream-alt px-1.75 py-0.5 text-[10px] text-muted">
                  DB
                </span>
              ) : (
                <span />
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
