import Link from "next/link";

const NAV_LINKS = [
  { href: "/", label: "Dự báo giá", implemented: true },
  { href: "/weather", label: "Thời tiết", implemented: true },
  { href: "/methodology", label: "Phương pháp", implemented: false },
] as const;

export function SiteHeader({ active }: { active: "/" | "/weather" }) {
  return (
    <header className="sticky top-0 z-20 border-b border-line bg-canvas/85 backdrop-blur">
      <div className="page-container flex h-14 items-center justify-between gap-4">
        <div className="flex items-center gap-6 sm:gap-8">
          <Link href="/" className="flex items-center gap-2.5" aria-label="Giá Tiêu Việt — trang chủ">
            <svg width="26" height="26" viewBox="0 0 40 40" fill="none" aria-hidden>
              <rect width="40" height="40" rx="10" fill="var(--color-ink)" />
              <path
                d="M20 30V14M20 14C20 14 14 15 13 21C13 21 18 22 20 18M20 14C20 14 26 15 27 21C27 21 22 22 20 18"
                stroke="var(--color-canvas)"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
            <span className="text-sm font-semibold tracking-tight">Giá Tiêu Việt</span>
          </Link>

          <nav className="flex items-center gap-1" aria-label="Điều hướng chính">
            {NAV_LINKS.map((link) =>
              link.implemented ? (
                <Link
                  key={link.href}
                  href={link.href}
                  aria-current={link.href === active ? "page" : undefined}
                  className={
                    "rounded-md px-2.5 py-1.5 text-sm transition-colors " +
                    (link.href === active
                      ? "bg-subtle font-medium text-ink"
                      : "text-muted hover:bg-subtle hover:text-ink")
                  }
                >
                  {link.label}
                </Link>
              ) : (
                <span
                  key={link.href}
                  className="hidden cursor-not-allowed rounded-md px-2.5 py-1.5 text-sm text-muted/60 sm:inline"
                  title="Sắp ra mắt"
                >
                  {link.label}
                </span>
              ),
            )}
          </nav>
        </div>

        <div className="hidden items-center gap-2 rounded-full border border-line bg-surface px-3 py-1 text-xs text-muted sm:flex">
          <span className="h-1.5 w-1.5 rounded-full bg-positive" aria-hidden />
          Cập nhật hằng ngày
        </div>
      </div>
    </header>
  );
}
