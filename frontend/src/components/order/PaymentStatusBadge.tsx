import clsx from "clsx";
import type { OrderPaymentStatus } from "../../types/api";

const labels: Record<OrderPaymentStatus, string> = {
  UNPAID: "Chưa thanh toán",
  PENDING: "Chờ xử lý",
  PAID: "Đã thanh toán",
  FAILED: "Thất bại",
  CANCELLED: "Đã hủy",
  REFUNDED: "Đã hoàn tiền",
};

const tones: Record<OrderPaymentStatus, string> = {
  UNPAID: "bg-ink/5 text-ink/60",
  PENDING: "bg-amber-100 text-amber-800",
  PAID: "bg-lime/50 text-ink",
  FAILED: "bg-red-100 text-red-700",
  CANCELLED: "bg-red-100 text-red-700",
  REFUNDED: "bg-sky-100 text-sky-800",
};

export function PaymentStatusBadge({
  status,
}: {
  status: OrderPaymentStatus;
}) {
  return (
    <span
      className={clsx(
        "inline-flex w-fit rounded-full px-3 py-1.5 text-xs font-bold",
        tones[status],
      )}
    >
      {labels[status]}
    </span>
  );
}
