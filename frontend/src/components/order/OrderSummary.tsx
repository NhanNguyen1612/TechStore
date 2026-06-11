import { formatCurrency } from "../../lib/format";

export function OrderSummary({
  quantity,
  total,
}: {
  quantity: number;
  total: number;
}) {
  return (
    <div className="card p-6">
      <h2 className="font-display text-xl font-bold">Tóm tắt đơn hàng</h2>
      <div className="mt-5 flex justify-between text-sm">
        <span className="text-ink/55">Sản phẩm</span>
        <span className="font-semibold">{quantity}</span>
      </div>
      <div className="mt-4 flex justify-between border-t border-ink/10 pt-4">
        <span className="font-bold">Tổng cộng</span>
        <span className="font-display text-xl font-extrabold">
          {formatCurrency(total)}
        </span>
      </div>
    </div>
  );
}
