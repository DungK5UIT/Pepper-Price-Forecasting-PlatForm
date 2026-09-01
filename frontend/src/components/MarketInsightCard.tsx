import Image from "next/image";
import peppercornBg from "@/assets/images/peppercorn-bg.jpg";
import type { MarketInsight } from "@/lib/types";

export function MarketInsightCard({ insight }: { insight: MarketInsight }) {
  return (
    <section className="relative flex flex-col gap-4 overflow-hidden rounded-[24px_24px_96px_24px] px-12 py-10 text-cream-ink">
      <Image src={peppercornBg} alt="" fill className="object-cover" aria-hidden />
      <div className="absolute inset-0 bg-linear-to-r from-forest-deep/95 from-32% to-forest-deep/55" />

      <span className="relative flex items-center gap-2 text-[11px] font-bold uppercase tracking-[0.2em] text-gold">
        <span className="h-px w-4 bg-gold" />
        Nhận định thị trường
      </span>
      <p className="relative max-w-3xl font-display text-xl leading-[1.7] font-semibold italic text-cream-ink">
        {insight.text}
      </p>
      <span className="relative text-xs text-cream-ink/60">
        Nhận định được tạo tự động từ mô hình phân tích dữ liệu · Không phải khuyến nghị đầu tư ·
        Cập nhật {insight.updatedAtLabel}
      </span>
    </section>
  );
}
