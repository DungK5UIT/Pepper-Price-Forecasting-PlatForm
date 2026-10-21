import type { RegionPrice } from "@/lib/types";
import { deltaDirection, formatSignedVnd, formatVnd } from "@/lib/format";
import { DeltaIcon, LeafIcon } from "./icons";

function HillsBanner() {
  return (
    <svg viewBox="0 0 400 56" width="100%" height="52" preserveAspectRatio="none" className="block">
      <rect width="400" height="56" fill="#eff5f0" />
      <circle cx="358" cy="18" r="10" fill="var(--color-gold)" fillOpacity={0.7} />
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

export function RegionPriceTable({ regions }: { regions: RegionPrice[] }) {
  return (
    <div className="flex flex-col overflow-hidden rounded-3xl border border-border bg-card">
      <HillsBanner />
      <div className="flex flex-col gap-5 px-8 py-6.5">
        <div className="flex items-center justify-between">
          <h3 className="font-display text-xl font-bold text-forest">Giá theo vùng trồng</h3>
          <span className="text-xs text-muted">So với hôm qua</span>
        </div>
        <div className="flex flex-col">
          {regions.map((region, i) => {
            const direction = deltaDirection(region.changeVnd);
            return (
              <div
                key={region.region}
                className={
                  "grid grid-cols-[1.3fr_auto_auto] items-center gap-4 py-3.5" +
                  (i < regions.length - 1 ? " border-b border-cream-alt" : "")
                }
              >
                <span className="flex items-center gap-2.5">
                  <span className="flex h-6.5 w-6.5 shrink-0 items-center justify-center rounded-full bg-badge-bg">
                    <LeafIcon width={13} height={13} className="text-forest" />
                  </span>
                  <span className="text-[15px] font-semibold text-forest">{region.region}</span>
                </span>
                <span className="text-[15px] font-bold tabular-nums text-forest">
                  {formatVnd(region.priceVnd)} đ
                </span>
                <span
                  className={
                    "flex items-center justify-self-end gap-1 text-[13px] font-bold " +
                    (direction === "down" ? "text-negative" : direction === "up" ? "text-positive" : "text-muted")
                  }
                >
                  <DeltaIcon direction={direction} />
                  {formatSignedVnd(region.changeVnd)}
                </span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
