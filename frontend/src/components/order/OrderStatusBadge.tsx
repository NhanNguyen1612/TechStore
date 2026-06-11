import clsx from "clsx";
import { orderStatusLabel } from "../../lib/constants";
import type { OrderStatus } from "../../types/api";

const tones: Record<OrderStatus, string> = {
  PENDING: "bg-amber-100 text-amber-800",
  PENDING_PAYMENT: "bg-orange-100 text-orange-800",
  PAID: "bg-sky-100 text-sky-800",
  CONFIRMED: "bg-indigo-100 text-indigo-800",
  SHIPPING: "bg-violet-100 text-violet-800",
  DELIVERED: "bg-teal/10 text-teal",
  COMPLETED: "bg-lime/50 text-ink",
  CANCELLED: "bg-red-100 text-red-700",
};

export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  return (
    <span
      className={clsx(
        "inline-flex w-fit rounded-full px-3 py-1.5 text-xs font-bold",
        tones[status],
      )}
    >
      {orderStatusLabel[status]}
    </span>
  );
}
