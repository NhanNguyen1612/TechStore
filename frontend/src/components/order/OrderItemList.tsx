import { Link } from "react-router-dom";
import { formatCurrency } from "../../lib/format";
import type { OrderItem } from "../../types/api";

export function OrderItemList({
  items,
  reviewable = false,
}: {
  items: OrderItem[];
  reviewable?: boolean;
}) {
  return (
    <div className="card overflow-hidden">
      {items.map((item) => (
        <div
          key={item.id}
          className="flex flex-col gap-4 border-b border-ink/5 p-5 last:border-0 sm:flex-row sm:items-center"
        >
          <div className="h-24 w-24 shrink-0 overflow-hidden rounded-2xl bg-cream">
            {item.thumbnailUrl ? (
              <img
                src={item.thumbnailUrl}
                alt={item.productName}
                className="h-full w-full object-cover"
              />
            ) : (
              <div className="grid h-full place-items-center text-xs text-ink/35">
                Không có ảnh
              </div>
            )}
          </div>
          <div className="min-w-0 flex-1">
            <Link
              to={`/products/${item.productId}`}
              className="font-bold hover:text-teal"
            >
              {item.productName}
            </Link>
            <p className="mt-1 text-sm text-ink/45">{item.sku}</p>
            <p className="mt-2 text-sm">
              {formatCurrency(item.unitPrice)} × {item.quantity}
            </p>
            {reviewable && (
              <Link
                to={`/products/${item.productId}#reviews`}
                className="mt-3 inline-flex text-sm font-bold text-teal"
              >
                Đánh giá sản phẩm
              </Link>
            )}
          </div>
          <p className="font-display text-lg font-extrabold">
            {formatCurrency(item.subtotal)}
          </p>
        </div>
      ))}
    </div>
  );
}
