import type { RegionPrice } from "@/lib/types";
import { deltaDirection, formatSignedVnd, formatVnd } from "@/lib/format";
import { DeltaBadge } from "./DeltaBadge";

export function RegionPriceTable({ regions }: { regions: RegionPrice[] }) {
  return (
    <div className="card overflow-hidden">
      <div className="flex items-center justify-between border-b border-line px-5 py-4">
        <h3 className="text-sm font-semibold tracking-tight">Giá theo vùng trồng</h3>
        <span className="text-xs text-muted">So với hôm qua</span>
      </div>
      <table className="w-full text-sm">
        <caption className="sr-only">Giá tiêu theo vùng trồng và mức thay đổi so với hôm qua</caption>
        <thead className="sr-only">
          <tr>
            <th scope="col">Vùng</th>
            <th scope="col">Giá (đ/kg)</th>
            <th scope="col">Thay đổi</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-line">
          {regions.map((region) => (
            <tr key={region.region} className="transition-colors hover:bg-subtle">
              <th scope="row" className="px-5 py-3 text-left font-medium">
                {region.region}
              </th>
              <td className="px-2 py-3 text-right tabular-nums">{formatVnd(region.priceVnd)} đ</td>
              <td className="py-3 pr-5 pl-2 text-right">
                <DeltaBadge direction={deltaDirection(region.changeVnd)} text={formatSignedVnd(region.changeVnd)} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
