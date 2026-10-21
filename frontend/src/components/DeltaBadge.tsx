import type { DeltaDirection } from "@/lib/format";
import { DeltaIcon } from "./icons";

const TONE: Record<DeltaDirection, string> = {
  up: "bg-positive-soft text-positive",
  down: "bg-negative-soft text-negative",
  flat: "bg-subtle text-muted",
};

/** A price movement: icon + text, never colour alone. */
export function DeltaBadge({ direction, text }: { direction: DeltaDirection; text: string }) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-md px-1.5 py-0.5 text-xs font-medium tabular-nums ${TONE[direction]}`}
    >
      <DeltaIcon direction={direction} width={10} height={10} aria-hidden />
      {text}
    </span>
  );
}
