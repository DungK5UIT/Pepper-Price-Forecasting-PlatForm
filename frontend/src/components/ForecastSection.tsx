"use client";

import { useState } from "react";
import type { ForecastSeries, Granularity, PeriodStat } from "@/lib/types";
import { ForecastChart } from "./ForecastChart";
import { TriangleUpIcon } from "./icons";

const GRANULARITY_LABEL: Record<Granularity, string> = {
  day: "Theo ngày",
  week: "Theo tuần",
  month: "Theo tháng",
};

const GRANULARITIES: Granularity[] = ["day", "week", "month"];

function formatVnd(value: number): string {
  return value.toLocaleString("vi-VN");
}

function formatPercent(value: number): string {
  const sign = value >= 0 ? "+" : "";
  return `${sign}${value.toString().replace(".", ",")}%`;
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
    <section id="du-bao" className="flex flex-col gap-5">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex flex-col gap-1">
          <span className="flex items-center gap-2 text-[11px] font-bold uppercase tracking-[0.2em] text-overline">
            <span className="h-px w-4 bg-orange-bright" />
            Biểu đồ
          </span>
          <h2 className="font-display text-[26px] font-bold text-forest">Diễn biến &amp; dự báo giá</h2>
        </div>

        <div className="flex gap-0.5 rounded-full bg-cream-alt p-1" role="tablist" aria-label="Chọn khoảng thời gian">
          {GRANULARITIES.map((g) => (
            <button
              key={g}
              type="button"
              role="tab"
              aria-selected={granularity === g}
              onClick={() => setGranularity(g)}
              className={
                granularity === g
                  ? "rounded-full bg-forest px-5.5 py-2.5 text-sm font-semibold text-cream-ink"
                  : "rounded-full px-5.5 py-2.5 text-sm font-semibold text-body"
              }
            >
              {GRANULARITY_LABEL[g]}
            </button>
          ))}
        </div>
      </div>

      <div className="flex flex-col gap-6 rounded-3xl border border-border bg-card px-10 py-9">
        <div className="flex flex-wrap gap-7">
          <span className="flex items-center gap-2 text-[13px] font-semibold text-body">
            <span className="h-3.5 w-3.5 rounded-full bg-forest" />
            Giá thực tế
          </span>
          <span className="flex items-center gap-2 text-[13px] font-semibold text-body">
            <span className="h-0 w-4 border-t-[2.5px] border-dashed border-orange" />
            Dự báo trung vị
          </span>
          <span className="flex items-center gap-2 text-[13px] font-semibold text-body">
            <span className="h-3.5 w-3.5 rounded bg-orange/20" />
            Khoảng tin cậy 10–90%
          </span>
        </div>

        <ForecastChart points={series[granularity]} />
      </div>

      <div className="grid grid-cols-1 gap-5 sm:grid-cols-3">
        {stats.map((stat) => (
          <div key={stat.label} className="flex flex-col gap-2.5 rounded-2xl border border-border bg-card px-6 py-5.5">
            <span className="text-xs font-medium uppercase tracking-[0.06em] text-muted">{stat.label}</span>
            <div className="flex flex-wrap items-baseline gap-2.5">
              <span className="font-display text-[26px] font-bold text-forest">
                {formatPercent(stat.changePercent)}
              </span>
              <span className="flex items-center gap-1 text-[13px] font-bold text-positive">
                <TriangleUpIcon />+{formatVnd(stat.changeVnd)} đ
              </span>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
