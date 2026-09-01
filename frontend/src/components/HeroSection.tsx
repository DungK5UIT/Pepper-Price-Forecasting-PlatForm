import Link from "next/link";
import type { TodayPrice } from "@/lib/types";
import { deltaDirection, formatSignedPercent, formatSignedVnd, formatVnd } from "@/lib/format";
import { DeltaIcon } from "./icons";

/** Gold reads as a rise on the dark card; a fall needs its own tone there. */
const DELTA_TONE = {
  up: "bg-gold/18 text-gold",
  down: "bg-negative-on-dark/18 text-negative-on-dark",
  flat: "bg-cream-ink/12 text-cream-ink/80",
} as const;

export function HeroSection({ today }: { today: TodayPrice }) {
  const direction = deltaDirection(today.changeVnd);

  return (
    <section className="relative grid grid-cols-1 items-center gap-14 overflow-hidden border-b border-forest/15 px-16 py-16 lg:grid-cols-[1.05fr_0.95fr] lg:py-18">
      <div
        className="pointer-events-none absolute -left-35 -top-15 h-80 w-80 rounded-full bg-blob-sage blur-3xl"
        aria-hidden
      />
      <div
        className="pointer-events-none absolute -right-25 -bottom-20 h-95 w-95 rounded-full bg-blob-apricot opacity-35 blur-3xl"
        aria-hidden
      />
      <div className="grain-overlay" aria-hidden />

      <div className="relative flex max-w-xl flex-col gap-7">
        <span className="flex items-center gap-3 text-[11px] font-bold uppercase tracking-[0.28em] text-overline">
          <span className="h-px w-10 bg-orange-bright" />
          Cập nhật 08:00 · {today.asOfDate}
        </span>
        <h1 className="font-display text-[64px] leading-[0.94] font-bold tracking-[-0.03em] text-forest sm:text-[74px]">
          Giá tiêu hôm nay,
          <span className="block text-orange">rõ trong từng con số.</span>
        </h1>
        <p className="max-w-lg text-[17px] leading-[1.7] text-body">
          Dự báo giá tiêu Việt Nam theo ngày, tuần, tháng — kết hợp giá lịch sử, thời tiết vùng
          trồng và tỷ giá USD/VND, cập nhật mỗi sáng.
        </p>
        <div className="flex flex-wrap items-center gap-3.5">
          <a href="#du-bao" className="pill-btn bg-forest text-cream-ink">
            Xem dự báo chi tiết
          </a>
          <Link href="/weather" className="pill-btn border-[0.8px] border-forest/30 bg-cream-ink/60 text-forest">
            Thời tiết vùng trồng
          </Link>
        </div>
      </div>

      <div className="relative ml-auto flex w-full max-w-115 flex-col gap-5.5 rounded-[32px_32px_128px_32px] bg-linear-to-br from-forest to-forest-deep p-10 text-cream-ink">
        <span className="text-[11px] font-bold uppercase tracking-[0.2em] text-gold">
          Giá tiêu hôm nay
        </span>
        <div className="flex flex-wrap items-end gap-3">
          <span className="font-display text-[64px] leading-none font-bold">
            {formatVnd(today.priceVnd)}
          </span>
          <span className="pb-2.5 text-lg font-medium text-[#cfe0c6]">đ/kg</span>
        </div>
        <span
          className={`flex w-fit items-center gap-1.5 rounded-full px-3.5 py-2 text-[13px] font-bold ${DELTA_TONE[direction]}`}
        >
          <DeltaIcon direction={direction} />
          {direction === "flat"
            ? "Không đổi so với hôm qua"
            : `${formatSignedVnd(today.changeVnd)} đ (${formatSignedPercent(today.changePercent)}) so với hôm qua`}
        </span>
        <div className="h-px bg-cream-ink/15" />
        <p className="text-sm leading-[1.65] text-[#cfe0c6]">
          Dự kiến tuần tới{" "}
          <strong className="text-cream-ink">
            {formatVnd(today.forecastLow)} – {formatVnd(today.forecastHigh)} đ/kg
          </strong>
          , trung vị <strong className="text-cream-ink">{formatVnd(today.forecastMedian)} đ/kg</strong>.
        </p>
        <span className="text-xs text-cream-ink/55">Nguồn: {today.sourceLabel.toLowerCase()}</span>
      </div>
    </section>
  );
}
