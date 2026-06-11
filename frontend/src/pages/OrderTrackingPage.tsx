import { Search } from "lucide-react";
import { type FormEvent, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { LoadingScreen } from "../components/LoadingScreen";
import { OrderStatusBadge } from "../components/order/OrderStatusBadge";
import { OrderTimeline } from "../components/order/OrderTimeline";
import { PaymentStatusBadge } from "../components/order/PaymentStatusBadge";
import { formatCurrency, getErrorMessage } from "../lib/format";
import { orderService } from "../services/orderService";
import type { OrderTracking } from "../types/api";

export function OrderTrackingPage() {
  const { orderCode = "" } = useParams();
  const navigate = useNavigate();
  const [query, setQuery] = useState(orderCode);
  const [tracking, setTracking] = useState<OrderTracking | null>(null);
  const [loading, setLoading] = useState(Boolean(orderCode));
  const [error, setError] = useState("");

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError("");
      try {
        const order = await orderService.getOrderByCode(orderCode);
        setTracking(await orderService.getOrderTracking(order.id));
      } catch (requestError) {
        setTracking(null);
        setError(getErrorMessage(requestError));
      } finally {
        setLoading(false);
      }
    };
    setQuery(orderCode);
    if (orderCode) {
      load();
    } else {
      setTracking(null);
      setError("");
      setLoading(false);
    }
  }, [orderCode]);

  const search = (event: FormEvent) => {
    event.preventDefault();
    const code = query.trim();
    if (code) navigate(`/order-tracking/${encodeURIComponent(code)}`);
  };

  return (
    <section className="container-page py-10 sm:py-14">
      <div className="mx-auto max-w-4xl">
        <p className="text-center text-sm font-bold uppercase tracking-[0.2em] text-teal">
          Tra cứu nhanh
        </p>
        <h1 className="section-title mt-2 text-center">Tra cứu đơn hàng</h1>
        <form onSubmit={search} className="mx-auto mt-6 flex max-w-xl gap-2">
          <input
            className="field"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Nhập mã đơn hàng"
          />
          <button className="btn-primary px-5" aria-label="Tìm đơn hàng">
            <Search className="h-5 w-5" />
          </button>
        </form>

        {loading ? (
          <LoadingScreen label="Đang tải thông tin đơn hàng" />
        ) : error ? (
          <p className="mt-8 rounded-2xl bg-red-50 p-4 text-center text-red-700">
            {error}
          </p>
        ) : tracking ? (
          <div className="mt-8 grid gap-6">
            <div className="card flex flex-col justify-between gap-5 p-6 sm:flex-row sm:items-center">
              <div>
                <p className="font-display text-xl font-extrabold">
                  {tracking.orderCode}
                </p>
                <p className="mt-1 text-sm text-ink/45">
                  {tracking.customerName} {" · "}
                  {formatCurrency(tracking.totalAmount)}
                </p>
              </div>
              <div className="flex flex-wrap gap-2">
                <OrderStatusBadge status={tracking.orderStatus} />
                <PaymentStatusBadge status={tracking.paymentStatus} />
              </div>
            </div>
            <div className="card p-6">
              <OrderTimeline entries={tracking.timeline} />
            </div>
          </div>
        ) : null}
      </div>
    </section>
  );
}
