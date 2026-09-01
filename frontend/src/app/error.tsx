"use client";

/**
 * Every page here renders backend data, so an unreachable or failing backend
 * is the realistic failure. Show that plainly and offer a retry instead of
 * letting the framework's default error screen through.
 *
 * The retry reloads rather than calling the boundary's `reset()`: the failure
 * happened while rendering on the server, and `reset()` only re-runs the
 * client render of the same failed payload.
 */
export default function Error() {
  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-6 px-16 py-24 text-center">
      <span className="flex items-center gap-3 text-[11px] font-bold uppercase tracking-[0.28em] text-overline">
        <span className="h-px w-10 bg-orange-bright" />
        Không tải được dữ liệu
      </span>
      <h1 className="font-display text-[48px] leading-[1] font-bold text-forest">
        Chưa lấy được số liệu.
      </h1>
      <p className="max-w-md text-base leading-[1.7] text-body">
        Máy chủ dữ liệu đang không phản hồi. Bạn thử tải lại sau ít phút, hoặc kiểm tra xem dịch vụ
        backend đã chạy chưa.
      </p>
      <button
        type="button"
        onClick={() => window.location.reload()}
        className="pill-btn bg-forest text-cream-ink"
      >
        Tải lại trang
      </button>
    </main>
  );
}
