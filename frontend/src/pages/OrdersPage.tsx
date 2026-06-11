import { Eye, PackageSearch, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { EmptyState } from "../components/EmptyState";
import {
  OrderFilterTabs,
  type OrderFilter,
} from "../components/order/OrderFilterTabs";
import { OrderStatusBadge } from "../components/order/OrderStatusBadge";
import { PaymentStatusBadge } from "../components/order/PaymentStatusBadge";
import { api, apiData } from "../lib/api";
import { formatCurrency, formatDate, getErrorMessage } from "../lib/format";
import { orderService } from "../services/orderService";
import { useAppSelector } from "../store/hooks";
import type { OrderSummary } from "../types/api";

export function OrdersPage() {
  const navigate = useNavigate();
  const user = useAppSelector((state) => state.auth.user);
  const [orders, setOrder] = useState<OrderSummary[]>([]);
  const [filter, setFilter] = useState<OrderFilter>("ALL");
  const [orderCode, setOrderCode] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const request =
      user?.role === "ROLE_CUSTOMER"
        ? orderService.getMyOrder()
        : apiData<OrderSummary[]>(api.get("/api/orders"));
    request
      .then(setOrder)
      .catch((requestError) => setError(getErrorMessage(requestError)))
      .finally(() => setLoading(false));
  }, [user?.role]);

  const filteredOrder = useMemo(
    () =>
      orders.filter((order) => {
        if (filter === "ALL") return true;
        if (filter === "SHIPPING") {
          return ["SHIPPING", "DELIVERED"].includes(order.status);
        }
        return order.status === filter;
      }),
    [filter, orders],
  );

  const openTracking = () => {
    const code = orderCode.trim();
    if (code) navigate(`/order-tracking/${encodeURIComponent(code)}`);
  };

  return (
    <section className="container-page py-10 sm:py-14">
      <div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-end">
        <div>
          <p className="text-sm font-bold uppercase tracking-[0.2em] text-teal">
            Lịch sử mua hàng
          </p>
          <h1 className="section-title mt-2">Đơn hàng của bạn</h1>
          <p className="mt-2 text-sm text-ink/50">
            Theo dõi thanh toán, xử lý và giao hàng tại một nơi.
          </p>
        </div>
        <div className="flex w-full max-w-md gap-2">
          <input
            className="field"
            value={orderCode}
            onChange={(event) => setOrderCode(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") openTracking();
            }}
            placeholder="Nhập mã đơn hàng"
          />
          <button
            type="button"
            onClick={openTracking}
            disabled={!orderCode.trim()}
            className="btn-primary px-4"
            aria-label="Tra cứu đơn hàng"
          >
            <Search className="h-5 w-5" />
          </button>
        </div>
      </div>

      <div className="mt-8">
        <OrderFilterTabs value={filter} onChange={setFilter} />
      </div>

      {error && (
        <p className="mt-6 rounded-2xl bg-red-50 p-4 text-red-700">{error}</p>
      )}

      {!loading && filteredOrder.length === 0 ? (
        <div className="mt-8">
          <EmptyState
            icon={PackageSearch}
            title={orders.length ? "Không có đơn hàng phù hợp" : "Chưa có đơn hàng"}
            description={
              orders.length
                ? "Chọn trạng thái khác để xem các đơn hàng còn lại."
                : "Đơn hàng sẽ xuất hiện tại đây sau khi bạn đặt hàng."
            }
            action={
              !orders.length ? (
                <Link to="/products" className="btn-primary">
                  Bắt đầu mua sắm
                </Link>
              ) : undefined
            }
          />
        </div>
      ) : (
        <div className="mt-6 grid gap-4">
          {filteredOrder.map((order) => (
            <article
              key={order.id}
              className="card grid gap-5 p-5 md:grid-cols-[1.4fr_1fr_1fr_auto] md:items-center"
            >
              <div>
                <Link
                  to={`/orders/${order.id}`}
                  className="font-display text-lg font-bold hover:text-teal"
                >
                  {order.orderCode}
                </Link>
                <p className="mt-1 text-sm text-ink/45">
                  {formatDate(order.createdAt)} · {order.totalQuantity} sản phẩm
                </p>
              </div>
              <div className="grid gap-2">
                <p className="text-xs font-bold uppercase tracking-wider text-ink/35">
                  Đơn hàng
                </p>
                <OrderStatusBadge status={order.status} />
              </div>
              <div className="grid gap-2">
                <p className="text-xs font-bold uppercase tracking-wider text-ink/35">
                  {order.paymentMethod}
                </p>
                <PaymentStatusBadge status={order.paymentStatus} />
              </div>
              <div className="md:text-right">
                <p className="font-display text-lg font-extrabold">
                  {formatCurrency(order.totalAmount)}
                </p>
                <Link
                  to={`/orders/${order.id}`}
                  className="btn-secondary mt-3 !px-4 !py-2 text-sm"
                >
                  <Eye className="h-4 w-4" />
                  Xem đơn hàng
                </Link>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
