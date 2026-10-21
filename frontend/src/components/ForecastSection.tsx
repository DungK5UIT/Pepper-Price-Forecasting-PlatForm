"use client";

import { useState } from "react";
import type { ForecastSeries, Granularity, PeriodStat } from "@/lib/types";
import { deltaDirection, formatSignedPercent, formatSignedVnd } from "@/lib/format";
import { ForecastChart } from "./ForecastChart";
import { DeltaBadge } from "./DeltaBadge";

const GRANULARITY_LABEL: Record<Granularity, string> = {
  day: "Ngày",
  week: "Tuần",
  month: "Tháng",
};

const GRANULARITIES: Granularity[] = ["day", "week", "month"];

function LegendItem({ children, swatch }: { children: React.ReactNode; swatch: React.ReactNode }) {
  return (
    <span className="flex items-center gap-2 text-xs text-muted">
      {swatch}
      {children}
    </span>
  );
}

export function ForecastSection({
  series,
  stats,
}: {
  series: ForecastSeries;
  stats: PeriodStat[];
}) {
  const [granularity, setGranularity] = useState<Granularity>("day");

  return (
    <section id="du-bao" className="flex scroll-mt-20 flex-col gap-4" aria-labelledby="du-bao-title">
      <div className="card p-4 sm:p-6">
        <div className="mb-5 flex flex-wrap items-start justify-between gap-4">
          <div className="flex flex-col gap-1">
            <h2 id="du-bao-title" className="text-base font-semibold tracking-tight">
              Diễn biến &amp; dự báo giá
            </h2>
            <p className="text-xs text-muted">Rê chuột hoặc chạm vào biểu đồ để xem giá từng thời điểm.</p>
          </div>

          <div
            className="flex gap-0.5 rounded-lg bg-subtle p-0.5"
            role="tablist"
            aria-label="Chọn khoảng thời gian"
          >
            {GRANULARITIES.map((g) => (
              <button
                key={g}
                type="button"
                role="tab"
                aria-selected={granularity === g}
                onClick={() => setGranularity(g)}
                className={
                  "rounded-md px-3.5 py-1.5 text-sm font-medium transition-colors " +
                  (granularity === g
                    ? "bg-surface text-ink shadow-sm ring-1 ring-line"
                    : "text-muted hover:text-ink")
                }
              >
                {GRANULARITY_LABEL[g]}
              </button>
            ))}
          </div>
        </div>

        <div className="mb-3 flex flex-wrap gap-x-5 gap-y-1.5">
          <LegendItem swatch={<span className="h-0.5 w-4 rounded bg-ink" />}>Giá thực tế</LegendItem>
          <LegendItem swatch={<span className="h-0 w-4 border-t-2 border-dashed border-accent" />}>
            Dự báo trung vị
          </LegendItem>
          <LegendItem swatch={<span className="h-3 w-4 rounded-sm bg-accent/20" />}>
            Khoảng tin cậy 10–90%
          </LegendItem>
        </div>

        <ForecastChart points={series[granularity]} />
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {stats.map((stat) => (
          <div key={stat.label} className="card flex flex-col gap-2 p-5">
            <span className="eyebrow">{stat.label}</span>
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5">
              <span className="text-2xl font-semibold tracking-tight tabular-nums">
                {formatSignedPercent(stat.changePercent)}
              </span>
              <DeltaBadge
                direction={deltaDirection(stat.changeVnd)}
                text={`${formatSignedVnd(stat.changeVnd)} đ`}
              />
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
