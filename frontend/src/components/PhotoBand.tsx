import Image from "next/image";
import vineCard from "@/assets/images/vine-card.jpg";
import hillsCard from "@/assets/images/hills-card.jpg";
import { ArrowRightIcon } from "./icons";

export function PhotoBand() {
  return (
    <section className="grid grid-cols-1 gap-5 lg:grid-cols-[1.6fr_1fr]">
      <div className="relative h-75 overflow-hidden rounded-[28px_28px_112px_28px]">
        <Image
          src={vineCard}
          alt="Dây tiêu Tây Nguyên"
          fill
          sizes="(min-width: 1024px) 60vw, 100vw"
          className="object-cover"
        />
        <div className="absolute inset-0 bg-linear-to-b from-forest-deep/0 from-40% to-forest-deep/85" />
        <div className="absolute bottom-6 left-7 flex flex-col gap-1">
          <span className="text-[10px] font-bold uppercase tracking-[0.2em] text-gold">
            Vườn tiêu Tây Nguyên
          </span>
          <span className="font-display text-[32px] font-bold text-white">Từ dây tiêu đến bàn cân</span>
        </div>
        <div className="absolute bottom-6 right-6 flex h-10 w-10 items-center justify-center rounded-full border-[0.8px] border-white/30 bg-white/10">
          <ArrowRightIcon className="text-white" />
        </div>
      </div>

      <div className="relative h-75 overflow-hidden rounded-[28px_112px_28px_28px]">
        <Image
          src={hillsCard}
          alt="Vùng trồng trọng điểm"
          fill
          sizes="(min-width: 1024px) 30vw, 100vw"
          className="object-cover"
        />
        <div className="absolute inset-0 bg-linear-to-b from-forest-deep/0 from-40% to-forest-deep/85" />
        <div className="absolute bottom-6 left-6 flex flex-col gap-1">
          <span className="text-[10px] font-bold uppercase tracking-[0.2em] text-gold">
            6 tỉnh trọng điểm
          </span>
          <span className="font-display text-[26px] font-bold text-white">Vùng nguyên liệu</span>
        </div>
      </div>
    </section>
  );
}
