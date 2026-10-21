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
    <main className="page-container flex flex-1 flex-col items-center justify-center gap-4 py-24 text-center">
      <span className="rounded-full border border-line bg-surface px-3 py-1 text-xs text-muted">
        Không tải được dữ liệu
      </span>
      <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">Chưa lấy được số liệu.</h1>
      <p className="max-w-md text-sm leading-relaxed text-muted">
        Máy chủ dữ liệu đang không phản hồi. Bạn thử tải lại sau ít phút, hoặc kiểm tra xem dịch vụ
        backend đã chạy chưa.
      </p>
      <button
        type="button"
        onClick={() => window.location.reload()}
        className="rounded-lg bg-ink px-4 py-2 text-sm font-medium text-canvas transition-opacity hover:opacity-90"
      >
        Tải lại trang
      </button>
    </main>
  );
}
