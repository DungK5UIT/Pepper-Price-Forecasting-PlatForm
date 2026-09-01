import type { PricePoint } from "@/lib/types";

const WIDTH = 1220;
const HEIGHT = 320;
const PAD_LEFT = 64;
const PAD_TOP = 20;
const PAD_BOTTOM = 30;
const GRID_LINES = 4;

function formatVnd(value: number): string {
  return value.toLocaleString("vi-VN");
}

function buildPath(coords: { x: number; y: number }[]): string {
  return coords.map((c, i) => `${i === 0 ? "M" : "L"}${c.x.toFixed(1)},${c.y.toFixed(1)}`).join(" ");
}

export function ForecastChart({ points }: { points: PricePoint[] }) {
  const values = points.flatMap((p) =>
    [p.actual, p.forecastQ10, p.forecastQ50, p.forecastQ90].filter(
      (v): v is number => typeof v === "number",
    ),
  );
  const min = Math.min(...values);
  const max = Math.max(...values);
  const domainPad = (max - min) * 0.12 || max * 0.05;
  const yMin = min - domainPad;
  const yMax = max + domainPad;

  const plotWidth = WIDTH - PAD_LEFT;
  const plotHeight = HEIGHT - PAD_TOP - PAD_BOTTOM;
  const step = points.length > 1 ? plotWidth / (points.length - 1) : 0;

  const xAt = (i: number) => PAD_LEFT + i * step;
  const yAt = (v: number) => PAD_TOP + plotHeight * (1 - (v - yMin) / (yMax - yMin));

  const todayIndex = points.findIndex((p) => p.isToday);

  const actualCoords = points
    .map((p, i) => (typeof p.actual === "number" ? { x: xAt(i), y: yAt(p.actual) } : null))
    .filter((c): c is { x: number; y: number } => c !== null);

  const forecastStart = todayIndex >= 0 ? todayIndex : points.findIndex((p) => p.forecastQ50 !== undefined);
  const forecastCoords = points
    .map((p, i) => (typeof p.forecastQ50 === "number" && i >= forecastStart ? { x: xAt(i), y: yAt(p.forecastQ50) } : null))
    .filter((c): c is { x: number; y: number } => c !== null);

  const upperCoords = points
    .map((p, i) => (typeof p.forecastQ90 === "number" && i >= forecastStart ? { x: xAt(i), y: yAt(p.forecastQ90) } : null))
    .filter((c): c is { x: number; y: number } => c !== null);
  const lowerCoords = points
    .map((p, i) => (typeof p.forecastQ10 === "number" && i >= forecastStart ? { x: xAt(i), y: yAt(p.forecastQ10) } : null))
    .filter((c): c is { x: number; y: number } => c !== null)
    .reverse();
  const bandPath =
    upperCoords.length > 0
      ? `${buildPath(upperCoords)} L${lowerCoords.map((c) => `${c.x.toFixed(1)},${c.y.toFixed(1)}`).join(" L")} Z`
      : "";

  const gridValues = Array.from({ length: GRID_LINES }, (_, i) => yMin + ((yMax - yMin) * (GRID_LINES - i)) / (GRID_LINES + 1));

  const labelEvery = Math.max(1, Math.ceil(points.length / 7));
  const lastPoint = points[points.length - 1];

  return (
    <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} width="100%" height="340" className="block overflow-visible">
      {gridValues.map((v) => (
        <g key={v}>
          <line x1={0} y1={yAt(v)} x2={WIDTH} y2={yAt(v)} stroke="var(--color-border)" strokeWidth={1} />
          <text x={0} y={yAt(v) - 6} fontSize={11} fill="var(--color-muted)">
            {formatVnd(Math.round(v))}
          </text>
        </g>
      ))}

      {todayIndex >= 0 && (
        <line
          x1={xAt(todayIndex)}
          y1={PAD_TOP - 10}
          x2={xAt(todayIndex)}
          y2={HEIGHT - PAD_BOTTOM + 10}
          stroke="#c9bfa8"
          strokeWidth={1.5}
          strokeDasharray="4 4"
        />
      )}

      {bandPath && <path d={bandPath} fill="var(--color-orange)" fillOpacity={0.14} stroke="none" />}

      <path d={buildPath(actualCoords)} fill="none" stroke="var(--color-forest)" strokeWidth={3} strokeLinecap="round" strokeLinejoin="round" />
      {forecastCoords.length > 1 && (
        <path
          d={buildPath(forecastCoords)}
          fill="none"
          stroke="var(--color-orange)"
          strokeWidth={2.5}
          strokeDasharray="7 6"
          strokeLinecap="round"
        />
      )}

      {actualCoords.length > 0 && (
        <circle
          cx={actualCoords[actualCoords.length - 1].x}
          cy={actualCoords[actualCoords.length - 1].y}
          r={5}
          fill="var(--color-forest)"
          stroke="var(--color-card)"
          strokeWidth={2}
        />
      )}
      {forecastCoords.length > 0 && (
        <>
          <circle
            cx={forecastCoords[forecastCoords.length - 1].x}
            cy={forecastCoords[forecastCoords.length - 1].y}
            r={5}
            fill="var(--color-orange)"
            stroke="var(--color-card)"
            strokeWidth={2}
          />
          {typeof lastPoint.forecastQ50 === "number" && (
            <text
              x={forecastCoords[forecastCoords.length - 1].x}
              y={forecastCoords[forecastCoords.length - 1].y - 12}
              textAnchor="end"
              fontSize={12}
              fontWeight={700}
              fill="var(--color-orange)"
            >
              {formatVnd(lastPoint.forecastQ50)}
            </text>
          )}
        </>
      )}

      {points.map((p, i) =>
        i % labelEvery === 0 || p.isToday || i === points.length - 1 ? (
          <text
            key={i}
            x={xAt(i)}
            y={HEIGHT - 6}
            textAnchor={i === points.length - 1 ? "end" : "middle"}
            fontSize={12}
            fontWeight={p.isToday ? 700 : 400}
            fill={p.isToday ? "var(--color-forest)" : "var(--color-muted)"}
          >
            {p.label}
          </text>
        ) : null,
      )}
    </svg>
  );
}
