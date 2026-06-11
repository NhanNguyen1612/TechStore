import clsx from "clsx";
import { orderStatusLabel } from "../../lib/constants";
import type { OrderStatus } from "../../types/api";

export type OrderFilter = "ALL" | OrderStatus;

const filters: Array<{ value: OrderFilter; label: string }> = [
  { value: "ALL", label: "Tất cả" },
  ...(
    [
      "PENDING",
      "PENDING_PAYMENT",
      "PAID",
      "SHIPPING",
      "COMPLETED",
      "CANCELLED",
    ] as OrderStatus[]
  ).map((value) => ({ value, label: orderStatusLabel[value] })),
];

export function OrderFilterTabs({
  value,
  onChange,
}: {
  value: OrderFilter;
  onChange: (value: OrderFilter) => void;
}) {
  return (
    <div className="flex gap-2 overflow-x-auto pb-2">
      {filters.map((filter) => (
        <button
          key={filter.value}
          type="button"
          onClick={() => onChange(filter.value)}
          className={clsx(
            "shrink-0 rounded-full px-4 py-2 text-sm font-bold transition",
            value === filter.value
              ? "bg-ink text-lime"
              : "bg-white text-ink/55 hover:text-ink",
          )}
        >
          {filter.label}
        </button>
      ))}
    </div>
  );
}
