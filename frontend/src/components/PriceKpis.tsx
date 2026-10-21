import type { TodayPrice } from "@/lib/types";
import { deltaDirection, formatSignedPercent, formatSignedVnd, formatVnd } from "@/lib/format";
import { DeltaBadge } from "./DeltaBadge";

function KpiCard({
  label,
  hint,
  children,
  footer,
}: {
  label: string;
  hint?: string;
  children: React.ReactNode;
  footer: React.ReactNode;
}) {
  return (
    <div className="card flex flex-col gap-3 p-5">
      <div className="flex items-center justify-between gap-2">
        <span className="eyebrow">{label}</span>
        {hint && <span className="rounded bg-subtle px-1.5 py-0.5 text-[11px] text-muted">{hint}</span>}
      </div>
      <div className="flex items-baseline gap-1.5">{children}</div>
      <div className="text-xs text-muted">{footer}</div>
    </div>
  );
}

function Value({ children }: { children: React.ReactNode }) {
  return <span className="text-3xl font-semibold tracking-tight tabular-nums">{children}</span>;
}

function Unit() {
  return <span className="text-sm text-muted">đ/kg</span>;
}

export function PriceKpis({ today }: { today: TodayPrice }) {
  const direction = deltaDirection(today.changeVnd);
  const medianDiff = today.forecastMedian - today.priceVnd;
  const medianPercent = today.priceVnd !== 0 ? (medianDiff / today.priceVnd) * 100 : 0;

  return (
    <section aria-label="Chỉ số chính" className="grid grid-cols-1 gap-4 md:grid-cols-3">
      <KpiCard
        label="Giá hôm nay"
        hint={today.asOfDate}
        footer={
          <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
            <DeltaBadge
              direction={direction}
              text={
                direction === "flat"
                  ? "Không đổi"
                  : `${formatSignedVnd(today.changeVnd)} đ (${formatSignedPercent(today.changePercent)})`
              }
            />
            so với hôm qua
          </span>
        }
      >
        <Value>{formatVnd(today.priceVnd)}</Value>
        <Unit />
      </KpiCard>

      <KpiCard
        label="Dự báo tuần tới (trung vị)"
        footer={
          <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
            <DeltaBadge
              direction={deltaDirection(medianDiff)}
              text={`${formatSignedVnd(medianDiff)} đ (${formatSignedPercent(medianPercent)})`}
            />
            so với hôm nay
          </span>
        }
      >
        <Value>{formatVnd(today.forecastMedian)}</Value>
        <Unit />
      </KpiCard>

      <KpiCard
        label="Khoảng dự báo 10–90%"
        footer={`Nguồn giá: ${today.sourceLabel.toLowerCase()}`}
      >
        <Value>
          {formatVnd(today.forecastLow)}
          <span className="px-1 text-muted">–</span>
          {formatVnd(today.forecastHigh)}
        </Value>
        <Unit />
      </KpiCard>
    </section>
  );
}
