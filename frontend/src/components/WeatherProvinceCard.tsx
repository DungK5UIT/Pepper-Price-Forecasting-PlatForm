import type { ProvinceWeather } from "@/lib/types";
import { WeatherConditionIcon } from "./icons";

/** Rain below this still gets a visible sliver; above it bars scale to the week's wettest day. */
const MIN_RAIN_SCALE_MM = 10;

export function WeatherProvinceCard({ weather }: { weather: ProvinceWeather }) {
  const today = weather.days[0];
  const rainScale = Math.max(MIN_RAIN_SCALE_MM, ...weather.days.map((d) => d.rainMm));

  return (
    <div className="card overflow-hidden">
      <div className="flex items-center justify-between border-b border-line px-5 py-4">
        <div className="flex flex-col gap-0.5">
          <h2 className="text-sm font-semibold tracking-tight">{weather.province}</h2>
          <span className="text-xs text-muted">Hôm nay</span>
        </div>
        <div className="flex items-center gap-2.5">
          <WeatherConditionIcon
            condition={today.condition}
            width={22}
            height={22}
            className="text-muted"
            aria-hidden
          />
          <span className="text-2xl font-semibold tracking-tight tabular-nums">
            {Math.round(today.tempC)}°C
          </span>
        </div>
      </div>

      <table className="w-full text-xs">
        <caption className="sr-only">Thời tiết 7 ngày tại {weather.province}</caption>
        <thead className="sr-only">
          <tr>
            <th scope="col">Ngày</th>
            <th scope="col">Nhiệt độ</th>
            <th scope="col">Lượng mưa</th>
            <th scope="col">Loại số liệu</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-line">
          {weather.days.map((day, i) => (
            <tr key={day.label + i}>
              <th scope="row" className="w-20 whitespace-nowrap py-2.5 pl-5 text-left font-medium">
                {day.label}
              </th>
              <td className="py-2.5">
                <span className="flex items-center gap-2 tabular-nums text-body">
                  <WeatherConditionIcon
                    condition={day.condition}
                    width={16}
                    height={16}
                    className="text-muted"
                    aria-hidden
                  />
                  {Math.round(day.tempC)}°C
                </span>
              </td>
              <td className="py-2.5 pl-2">
                <span className="flex items-center justify-end gap-2 tabular-nums text-rain">
                  <span
                    className="h-1.5 rounded-full bg-rain/30"
                    style={{ width: `${Math.max(day.rainMm > 0 ? 4 : 0, (day.rainMm / rainScale) * 48)}px` }}
                    aria-hidden
                  />
                  {day.rainMm} mm
                </span>
              </td>
              <td className="py-2.5 pr-5 pl-2 text-right whitespace-nowrap">
                {day.isForecast && (
                  <span className="whitespace-nowrap rounded bg-subtle px-1.5 py-0.5 text-[11px] text-muted">Dự báo</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
