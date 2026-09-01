/**
 * Number formatting shared by every place that shows a price or a change.
 * Prices are whole VND; the backend never sends formatted strings.
 */

export type DeltaDirection = "up" | "down" | "flat";

export function deltaDirection(value: number): DeltaDirection {
  if (value > 0) return "up";
  if (value < 0) return "down";
  return "flat";
}

export function formatVnd(value: number): string {
  return value.toLocaleString("vi-VN");
}

/** Signed for changes: "+1.500", "-300", "0" — the minus comes from the number itself. */
export function formatSignedVnd(value: number): string {
  return `${value > 0 ? "+" : ""}${formatVnd(value)}`;
}

/** Vietnamese decimal comma, e.g. "+4,2%" / "-1,5%" / "0%". */
export function formatSignedPercent(value: number): string {
  const formatted = value.toLocaleString("vi-VN", { maximumFractionDigits: 1 });
  return `${value > 0 ? "+" : ""}${formatted}%`;
}
