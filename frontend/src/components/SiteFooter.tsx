export function SiteFooter() {
  return (
    <footer className="flex flex-wrap items-center justify-between gap-4 border-t border-border px-16 py-8">
      <span className="text-[13px] text-muted">
        © {new Date().getFullYear()} Giá Tiêu Việt — Nền tảng dự báo giá tiêu Việt Nam
      </span>
      <div className="flex gap-6">
        <span className="text-[13px] text-body">Dự báo giá</span>
        <span className="text-[13px] text-body">Thời tiết</span>
        <span className="text-[13px] text-body">Phương pháp</span>
      </div>
    </footer>
  );
}
