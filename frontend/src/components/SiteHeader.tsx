import Link from "next/link";

const NAV_LINKS = [
  { href: "/", label: "Dự báo giá", implemented: true },
  { href: "/weather", label: "Thời tiết", implemented: true },
  { href: "/methodology", label: "Phương pháp", implemented: false },
] as const;

export function SiteHeader({ active }: { active: "/" | "/weather" }) {
  return (
    <header className="flex items-center justify-between border-b border-forest/15 px-16 py-6">
      <Link href="/" className="flex items-center gap-3">
        <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
          <circle cx="20" cy="20" r="20" fill="var(--color-forest)" />
          <path
            d="M20 30V14M20 14C20 14 14 15 13 21C13 21 18 22 20 18M20 14C20 14 26 15 27 21C27 21 22 22 20 18"
            stroke="var(--color-cream-ink)"
            strokeWidth="1.6"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
        <span className="flex flex-col leading-tight">
          <span className="font-display text-[21px] font-bold text-forest">Giá Tiêu Việt</span>
          <span className="text-[10px] font-semibold uppercase tracking-[0.12em] text-body">
            Nền tảng dự báo giá tiêu
          </span>
        </span>
      </Link>

      <nav className="flex items-center gap-10">
        {NAV_LINKS.map((link) =>
          link.implemented ? (
            <Link
              key={link.href}
              href={link.href}
              className={
                link.href === active
                  ? "text-sm font-bold text-forest"
                  : "text-sm font-medium text-body hover:text-forest"
              }
            >
              {link.label}
            </Link>
          ) : (
            <span key={link.href} className="text-sm font-medium text-body/50" title="Sắp ra mắt">
              {link.label}
            </span>
          ),
        )}
      </nav>

      <div className="flex items-center gap-2 rounded-full bg-forest px-[18px] py-[10px] text-xs font-bold uppercase tracking-[0.06em] text-cream-ink">
        <span className="h-[7px] w-[7px] rounded-full bg-gold" />
        Cập nhật 08:00 hôm nay
      </div>
    </header>
  );
}
