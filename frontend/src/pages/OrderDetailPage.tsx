import {
  ArrowLeft,
  ArrowUpRight,
  Banknote,
  CircleDollarSign,
  MapPin,
  Phone,
  UserRound,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { LoadingScreen } from "../components/LoadingScreen";
import { OrderItemList } from "../components/order/OrderItemList";
import { OrderStatusBadge } from "../components/order/OrderStatusBadge";
import { OrderSummary } from "../components/order/OrderSummary";
import { OrderTimeline } from "../components/order/OrderTimeline";
import { PaymentStatusBadge } from "../components/order/PaymentStatusBadge";
import { MomoQrPaymentModal } from "../components/payment/MomoQrPaymentModal";
import { formatDate, getErrorMessage } from "../lib/format";
import { orderService } from "../services/orderService";
import type { MomoPayment, OrderTracking } from "../types/api";

export function OrderDetailPage() {
  const { id } = useParams();
  const location = useLocation();
  const [order, setOrder] = useState<OrderTracking | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [momoPayment, setMomoPayment] = useState<MomoPayment | null>(null);

  const loadOrder = async () => {
    if (!id) return;
    try {
      setOrder(await orderService.getOrderTracking(id));
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  };

  useEffect(() => {
    loadOrder();
  }, [id]);

  const cancelOrder = async () => {
    if (!order) return;
    const reason = window.prompt(
      "Vui lòng nhập lý do hủy đơn hàng:",
      "Khách hàng hủy đơn",
    );
    if (reason === null) return;
    setBusy(true);
    setError("");
    try {
      await orderService.cancelOrder(order.orderId, reason);
      await loadOrder();
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  };

  const payWithMomo = async () => {
    if (!order) return;
    setBusy(true);
    setError("");
    try {
      const payment = await orderService.createMomoPayment(order.orderId);
      if (!payment.qrCodeUrl && !payment.payUrl && !payment.deeplink) {
        throw new Error("MoMo không trả về dữ liệu mã QR thanh toán");
      }
      setMomoPayment(payment);
      setBusy(false);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
      setBusy(false);
    }
  };

  if (!order && !error) return <LoadingScreen label="Đang tải đơn hàng" />;
  if (!order) {
    return <div className="container-page py-16 text-red-700">{error}</div>;
  }

  const canCancel = ["PENDING", "PENDING_PAYMENT"].includes(order.orderStatus);
  const canPay =
    order.paymentMethod === "MOMO" &&
    ["PENDING", "FAILED"].includes(order.paymentStatus) &&
    !["CANCELLED", "COMPLETED"].includes(order.orderStatus);

  return (
    <>
      <section className="container-page py-10 sm:py-14">
      {(location.state as { message?: string } | null)?.message && (
        <p className="mb-6 rounded-2xl bg-lime/40 p-4 font-semibold">
          {(location.state as { message: string }).message}
        </p>
      )}

      <Link
        to="/orders"
        className="inline-flex items-center gap-2 text-sm font-bold text-ink/55 hover:text-ink"
      >
        <ArrowLeft className="h-4 w-4" />
        Quay lại danh sách đơn
      </Link>

      <div className="mt-5 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="text-sm font-bold uppercase tracking-[0.2em] text-teal">
            Theo dõi đơn hàng
          </p>
          <h1 className="section-title mt-2">{order.orderCode}</h1>
          <p className="mt-2 text-sm text-ink/45">
            Đã đặt lúc {formatDate(order.createdAt)}
          </p>
        </div>
        <OrderStatusBadge status={order.orderStatus} />
      </div>

      {error && (
        <button
          type="button"
          onClick={() => setError("")}
          className="mt-6 w-full rounded-2xl bg-red-50 p-4 text-left text-red-700"
        >
          {error}
        </button>
      )}

      <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_360px]">
        <div className="grid gap-8">
          <section className="card p-6">
            <h2 className="font-display text-xl font-bold">
              Thông tin giao hàng
            </h2>
            <div className="mt-5 grid gap-4 sm:grid-cols-2">
              <Info icon={UserRound} label="Người nhận" value={order.customerName} />
              <Info icon={Phone} label="Số điện thoại" value={order.phone || "Chưa cung cấp"} />
              <div className="sm:col-span-2">
                <Info
                  icon={MapPin}
                  label="Địa chỉ giao hàng"
                  value={order.shippingAddress || "Chưa cung cấp"}
                />
              </div>
            </div>
            {order.note && (
              <div className="mt-4 rounded-2xl bg-cream p-4 text-sm">
                <p className="font-bold">Ghi chú đơn hàng</p>
                <p className="mt-1 text-ink/55">{order.note}</p>
              </div>
            )}
          </section>

          <section className="card p-6">
            <h2 className="font-display text-xl font-bold">Lịch sử đơn hàng</h2>
            <div className="mt-6">
              <OrderTimeline entries={order.timeline} />
            </div>
          </section>

          <section>
            <h2 className="mb-4 font-display text-xl font-bold">Sản phẩm</h2>
            <OrderItemList
              items={order.items}
              reviewable={order.orderStatus === "COMPLETED"}
            />
          </section>
        </div>

        <aside className="grid h-fit gap-5 lg:sticky lg:top-28">
          <OrderSummary
            quantity={order.items.reduce(
              (total, item) => total + item.quantity,
              0,
            )}
            total={order.totalAmount}
          />

          <div className="card p-6">
            <div className="flex items-center gap-3">
              {order.paymentMethod === "MOMO" ? (
                <CircleDollarSign className="h-6 w-6 text-teal" />
              ) : (
                <Banknote className="h-6 w-6 text-teal" />
              )}
              <div>
                <p className="font-bold">{order.paymentMethod}</p>
                <p className="text-xs text-ink/45">Phương thức thanh toán</p>
              </div>
            </div>
            <div className="mt-5">
              <PaymentStatusBadge status={order.paymentStatus} />
            </div>
            {order.transactionId && (
              <p className="mt-4 break-all text-xs text-ink/45">
                Mã giao dịch: {order.transactionId}
              </p>
            )}
            {canPay && (
              <button
                type="button"
                onClick={payWithMomo}
                disabled={busy}
                className="btn-primary mt-5 w-full"
              >
                {busy ? "Đang mở MoMo..." : "Thanh toán bằng MoMo"}
                <ArrowUpRight className="h-4 w-4" />
              </button>
            )}
            {canCancel && (
              <button
                type="button"
                onClick={cancelOrder}
                disabled={busy}
                className="btn-secondary mt-3 w-full text-coral"
              >
                Hủy đơn hàng
              </button>
            )}
          </div>
        </aside>
      </div>
      </section>
      {momoPayment && (
        <MomoQrPaymentModal
          payment={momoPayment}
          onClose={() => setMomoPayment(null)}
          onPaid={async () => {
            setMomoPayment(null);
            await loadOrder();
          }}
        />
      )}
    </>
  );
}

function Info({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof UserRound;
  label: string;
  value: string;
}) {
  return (
    <div className="flex gap-3 rounded-2xl bg-cream p-4">
      <Icon className="mt-0.5 h-5 w-5 shrink-0 text-teal" />
      <div className="min-w-0">
        <p className="text-xs font-bold uppercase tracking-wider text-ink/35">
          {label}
        </p>
        <p className="mt-1 break-words font-semibold">{value}</p>
      </div>
    </div>
  );
}
