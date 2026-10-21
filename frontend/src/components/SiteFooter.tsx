import Link from "next/link";

export function SiteFooter() {
  return (
    <footer className="mt-auto border-t border-line">
      <div className="page-container flex flex-wrap items-center justify-between gap-3 py-6">
        <span className="text-xs text-muted">
          © {new Date().getFullYear()} Giá Tiêu Việt — Nền tảng dự báo giá tiêu Việt Nam
        </span>
        <nav className="flex gap-5 text-xs" aria-label="Chân trang">
          <Link href="/" className="text-muted hover:text-ink">
            Dự báo giá
          </Link>
          <Link href="/weather" className="text-muted hover:text-ink">
            Thời tiết
          </Link>
          <span className="text-muted/60" title="Sắp ra mắt">
            Phương pháp
          </span>
        </nav>
      </div>
    </footer>
  );
}
