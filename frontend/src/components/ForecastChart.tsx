"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { PricePoint } from "@/lib/types";
import { formatVnd } from "@/lib/format";

const DEFAULT_WIDTH = 960;
const PAD_LEFT = 58;
const PAD_RIGHT = 16;
const PAD_TOP = 24;
const PAD_BOTTOM = 28;
const TARGET_TICKS = 5;
const MIN_LABEL_SPACING = 84;

type Coord = { x: number; y: number };

function buildPath(coords: Coord[]): string {
  return coords.map((c, i) => `${i === 0 ? "M" : "L"}${c.x.toFixed(1)},${c.y.toFixed(1)}`).join(" ");
}

/** Round the tick step to 1/2/5 × 10^k so the axis reads in clean numbers. */
function niceTicks(min: number, max: number): number[] {
  const rough = (max - min) / (TARGET_TICKS - 1);
  const magnitude = 10 ** Math.floor(Math.log10(rough));
  const normalized = rough / magnitude;
  const step = (normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 5 ? 5 : 10) * magnitude;
  const ticks: number[] = [];
  for (let v = Math.ceil(min / step) * step; v <= max; v += step) {
    ticks.push(v);
  }
  return ticks;
}

function isNumber(v: number | undefined): v is number {
  return typeof v === "number";
}

export function ForecastChart({ points }: { points: PricePoint[] }) {
  const wrapperRef = useRef<HTMLDivElement>(null);
  const [width, setWidth] = useState(DEFAULT_WIDTH);
  const [active, setActive] = useState<number | null>(null);

  useEffect(() => {
    const el = wrapperRef.current;
    if (!el) return;
    const observer = new ResizeObserver(([entry]) => setWidth(Math.round(entry.contentRect.width)));
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  const compact = width < 520;
  const height = compact ? 260 : 340;

  const values = points.flatMap((p) =>
    [p.actual, p.forecastQ10, p.forecastQ50, p.forecastQ90].filter(isNumber),
  );
  const min = Math.min(...values);
  const max = Math.max(...values);
  const domainPad = (max - min) * 0.12 || max * 0.05;
  const yMin = min - domainPad;
  const yMax = max + domainPad;

  const plotLeft = PAD_LEFT;
  const plotRight = width - PAD_RIGHT;
  const plotHeight = height - PAD_TOP - PAD_BOTTOM;
  const step = points.length > 1 ? (plotRight - plotLeft) / (points.length - 1) : 0;

  const xAt = (i: number) => plotLeft + i * step;
  const yAt = (v: number) => PAD_TOP + plotHeight * (1 - (v - yMin) / (yMax - yMin));

  const todayIndex = points.findIndex((p) => p.isToday);
  const forecastStart =
    todayIndex >= 0 ? todayIndex : points.findIndex((p) => p.forecastQ50 !== undefined);

  const coordsOf = (pick: (p: PricePoint) => number | undefined, from = 0): Coord[] =>
    points.flatMap((p, i) => {
      const v = pick(p);
      return isNumber(v) && i >= from ? [{ x: xAt(i), y: yAt(v) }] : [];
    });

  const actualCoords = coordsOf((p) => p.actual);
  const forecastCoords = coordsOf((p) => p.forecastQ50, forecastStart);
  const upperCoords = coordsOf((p) => p.forecastQ90, forecastStart);
  const lowerCoords = coordsOf((p) => p.forecastQ10, forecastStart).reverse();
  const bandPath =
    upperCoords.length > 0 && lowerCoords.length > 0
      ? `${buildPath(upperCoords)} ${lowerCoords.map((c) => `L${c.x.toFixed(1)},${c.y.toFixed(1)}`).join(" ")} Z`
      : "";

  const ticks = niceTicks(yMin, yMax);

  const maxLabels = Math.max(2, Math.floor((plotRight - plotLeft) / MIN_LABEL_SPACING));
  const labelEvery = Math.max(1, Math.ceil(points.length / maxLabels));
  const showLabel = (i: number) =>
    points[i].isToday ||
    (i === points.length - 1 && (todayIndex < 0 || Math.abs(i - todayIndex) > 1)) ||
    (i % labelEvery === 0 &&
      (todayIndex < 0 || Math.abs(i - todayIndex) > 1) &&
      (labelEvery === 1 || points.length - 1 - i >= labelEvery));

  const lastActual = actualCoords[actualCoords.length - 1];
  const lastForecast = forecastCoords[forecastCoords.length - 1];
  const lastPoint = points[points.length - 1];

  const indexFromPointer = useCallback(
    (clientX: number, target: Element) => {
      if (points.length === 0) return null;
      const rect = target.getBoundingClientRect();
      const x = clientX - rect.left;
      const raw = step > 0 ? Math.round((x - PAD_LEFT) / step) : 0;
      return Math.min(points.length - 1, Math.max(0, raw));
    },
    [points.length, step],
  );

  const onKeyDown = (e: React.KeyboardEvent) => {
    if (e.key !== "ArrowLeft" && e.key !== "ArrowRight") return;
    e.preventDefault();
    const delta = e.key === "ArrowRight" ? 1 : -1;
    setActive((cur) => {
      const base = cur ?? (todayIndex >= 0 ? todayIndex : points.length - 1);
      return Math.min(points.length - 1, Math.max(0, base + delta));
    });
  };

  const activePoint = active !== null ? points[active] : null;
  const tooltipOnLeft = active !== null && xAt(active) > width / 2;

  const lastActualValue = points.map((p) => p.actual).filter(isNumber).at(-1);
  const summary = [
    "Biểu đồ giá tiêu và dự báo.",
    isNumber(lastActualValue) ? `Giá thực tế gần nhất ${formatVnd(lastActualValue)} đồng.` : "",
    isNumber(lastPoint?.forecastQ50) ? `Dự báo cuối kỳ ${formatVnd(lastPoint.forecastQ50)} đồng.` : "",
    "Dùng phím mũi tên trái phải để xem từng điểm.",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <div
      ref={wrapperRef}
      className="relative w-full rounded-md"
      tabIndex={0}
      role="group"
      aria-label={summary}
      onKeyDown={onKeyDown}
      onBlur={() => setActive(null)}
    >
      <svg
        width={width}
        height={height}
        className="block touch-pan-y select-none"
        aria-hidden
        onPointerMove={(e) => setActive(indexFromPointer(e.clientX, e.currentTarget))}
        onPointerDown={(e) => setActive(indexFromPointer(e.clientX, e.currentTarget))}
        onPointerLeave={(e) => {
          if (e.pointerType === "mouse") setActive(null);
        }}
      >
        {/* Forecast zone: everything right of "today" is a projection, not data. */}
        {todayIndex >= 0 && (
          <>
            <rect
              x={xAt(todayIndex)}
              y={PAD_TOP}
              width={Math.max(0, plotRight - xAt(todayIndex))}
              height={plotHeight}
              fill="var(--color-accent)"
              fillOpacity={0.04}
            />
            <text x={xAt(todayIndex) + 8} y={PAD_TOP + 12} fontSize={11} fontWeight={500} fill="var(--color-accent)">
              Dự báo
            </text>
          </>
        )}

        {ticks.map((v) => (
          <g key={v}>
            <line
              x1={plotLeft}
              y1={yAt(v)}
              x2={plotRight}
              y2={yAt(v)}
              stroke="var(--color-line)"
              strokeWidth={1}
              strokeDasharray="2 4"
            />
            <text x={plotLeft - 10} y={yAt(v) + 4} textAnchor="end" fontSize={11} fill="var(--color-muted)">
              {formatVnd(Math.round(v))}
            </text>
          </g>
        ))}

        {todayIndex >= 0 && (
          <line
            x1={xAt(todayIndex)}
            y1={PAD_TOP}
            x2={xAt(todayIndex)}
            y2={height - PAD_BOTTOM}
            stroke="var(--color-muted)"
            strokeOpacity={0.5}
            strokeWidth={1}
          />
        )}

        {bandPath && <path d={bandPath} fill="var(--color-accent)" fillOpacity={0.14} />}

        {actualCoords.length > 0 && (
          <path
            d={buildPath(actualCoords)}
            fill="none"
            stroke="var(--color-ink)"
            strokeWidth={2}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        )}
        {forecastCoords.length > 1 && (
          <path
            d={buildPath(forecastCoords)}
            fill="none"
            stroke="var(--color-accent)"
            strokeWidth={2}
            strokeDasharray="5 4"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        )}

        {lastActual && (
          <circle cx={lastActual.x} cy={lastActual.y} r={4} fill="var(--color-ink)" stroke="var(--color-surface)" strokeWidth={2} />
        )}
        {lastForecast && (
          <>
            <circle cx={lastForecast.x} cy={lastForecast.y} r={4} fill="var(--color-accent)" stroke="var(--color-surface)" strokeWidth={2} />
            {isNumber(lastPoint?.forecastQ50) && active === null && (
              <text
                x={lastForecast.x}
                y={lastForecast.y - 14}
                textAnchor="end"
                fontSize={12}
                fontWeight={600}
                fill="var(--color-accent)"
                stroke="var(--color-surface)"
                strokeWidth={4}
                paintOrder="stroke"
              >
                {formatVnd(lastPoint.forecastQ50)}
              </text>
            )}
          </>
        )}

        {points.map((p, i) =>
          showLabel(i) ? (
            <text
              key={i}
              x={xAt(i)}
              y={height - 8}
              textAnchor={i === points.length - 1 ? "end" : i === 0 ? "start" : "middle"}
              fontSize={11}
              fontWeight={p.isToday ? 600 : 400}
              fill={p.isToday ? "var(--color-ink)" : "var(--color-muted)"}
            >
              {p.label}
            </text>
          ) : null,
        )}

        {active !== null && activePoint && (
          <g>
            <line
              x1={xAt(active)}
              y1={PAD_TOP}
              x2={xAt(active)}
              y2={height - PAD_BOTTOM}
              stroke="var(--color-muted)"
              strokeWidth={1}
            />
            {isNumber(activePoint.actual) && (
              <circle cx={xAt(active)} cy={yAt(activePoint.actual)} r={4.5} fill="var(--color-ink)" stroke="var(--color-surface)" strokeWidth={2} />
            )}
            {isNumber(activePoint.forecastQ50) && (
              <circle cx={xAt(active)} cy={yAt(activePoint.forecastQ50)} r={4.5} fill="var(--color-accent)" stroke="var(--color-surface)" strokeWidth={2} />
            )}
          </g>
        )}
      </svg>

      {active !== null && activePoint && (
        <div
          className="pointer-events-none absolute top-2 z-10 min-w-52 rounded-lg border border-line bg-surface px-3 py-2.5 text-xs shadow-lg"
          style={{
            left: xAt(active),
            transform: `translateX(${tooltipOnLeft ? "calc(-100% - 12px)" : "12px"})`,
          }}
          role="status"
        >
          <div className="mb-1.5 font-medium text-ink">{activePoint.label}</div>
          <dl className="flex flex-col gap-1">
            {isNumber(activePoint.actual) && (
              <TooltipRow swatch="bg-ink" label="Thực tế" value={`${formatVnd(activePoint.actual)} đ`} />
            )}
            {isNumber(activePoint.forecastQ50) && (
              <TooltipRow swatch="bg-accent" label="Dự báo" value={`${formatVnd(activePoint.forecastQ50)} đ`} />
            )}
            {isNumber(activePoint.forecastQ10) && isNumber(activePoint.forecastQ90) && (
              <TooltipRow
                swatch="bg-accent/25"
                label="Khoảng 10–90%"
                value={`${formatVnd(activePoint.forecastQ10)} – ${formatVnd(activePoint.forecastQ90)}`}
              />
            )}
          </dl>
        </div>
      )}
    </div>
  );
}

function TooltipRow({ swatch, label, value }: { swatch: string; label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <dt className="flex items-center gap-1.5 whitespace-nowrap text-muted">
        <span className={`h-2 w-2 shrink-0 rounded-full ${swatch}`} aria-hidden />
        {label}
      </dt>
      <dd className="whitespace-nowrap font-medium tabular-nums text-ink">{value}</dd>
    </div>
  );
}
