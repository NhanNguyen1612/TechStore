import { Banknote, CheckCircle2, Smartphone } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import clsx from "clsx";
import { MomoQrPaymentModal } from "../components/payment/MomoQrPaymentModal";
import { formatCurrency, getErrorMessage } from "../lib/format";
import { orderService } from "../services/orderService";
import { resetCart } from "../store/cartSlice";
import { useAppDispatch, useAppSelector } from "../store/hooks";
import type { MomoPayment, OrderDetail } from "../types/api";

export function CheckoutPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const cart = useAppSelector((state) => state.cart.data);
  const user = useAppSelector((state) => state.auth.user);
  const [paymentMethod, setPaymentMethod] = useState<"COD" | "MOMO">("COD");
  const [delivery, setDelivery] = useState({
    recipientName: user?.fullName ?? "",
    phone: user?.phone ?? "",
    shippingAddress: "",
    note: "",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [momoPayment, setMomoPayment] = useState<MomoPayment | null>(null);
  const [pendingMomoOrder, setPendingMomoOrder] =
    useState<OrderDetail | null>(null);

  const checkout = async () => {
    setLoading(true);
    setError("");
    let order = pendingMomoOrder;
    try {
      if (!order) {
        if (
          !delivery.recipientName.trim() ||
          !delivery.phone.trim() ||
          !delivery.shippingAddress.trim()
        ) {
          throw new Error(
            "Vui lòng nhập tên người nhận, số điện thoại và địa chỉ giao hàng",
          );
        }
        order = await orderService.createOrder({
          ...delivery,
          recipientName: delivery.recipientName.trim(),
          phone: delivery.phone.trim(),
          shippingAddress: delivery.shippingAddress.trim(),
          note: delivery.note.trim(),
          paymentMethod,
        });
      }

      if (order.paymentMethod === "MOMO") {
        setPendingMomoOrder(order);
        dispatch(resetCart());
        const payment = await orderService.createMomoPayment(order.id);
        if (!payment.qrCodeUrl && !payment.payUrl && !payment.deeplink) {
          throw new Error("MoMo không trả về dữ liệu mã QR thanh toán");
        }
        setMomoPayment(payment);
        return;
      }

      dispatch(resetCart());
      navigate(`/orders/${order.id}`, {
        state: { message: "Đặt hàng thành công. Bạn sẽ thanh toán khi nhận hàng." },
      });
    } catch (requestError) {
      const message = getErrorMessage(requestError);
      setError(
        order?.paymentMethod === "MOMO"
          ? `Đơn hàng ${order.orderCode} đã được tạo nhưng chưa thể tạo mã QR MoMo. ${message}`
          : message,
      );
    } finally {
      setLoading(false);
    }
  };

  const summaryProducts = pendingMomoOrder?.items ?? cart.items;
  const summaryTotal = pendingMomoOrder?.totalAmount ?? cart.totalAmount;

  if (!summaryProducts.length && !momoPayment) {
    return (
      <section className="container-page py-16">
        <div className="card p-10 text-center">
          <CheckCircle2 className="mx-auto h-10 w-10 text-teal" />
          <h1 className="mt-4 font-display text-2xl font-bold">
            Giỏ hàng đang trống
          </h1>
          <Link to="/products" className="btn-primary mt-6">
            Xem sản phẩm
          </Link>
          <Link to="/orders" className="btn-secondary mt-3">
            Xem đơn hàng
          </Link>
        </div>
      </section>
    );
  }

  return (
    <>
      <section className="container-page py-10 sm:py-14">
      <p className="text-sm font-bold uppercase tracking-[0.2em] text-teal">
        Bước cuối cùng
      </p>
      <h1 className="section-title mt-2">Thanh toán</h1>
      <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_380px]">
        <div className="grid gap-6">
          <div className="card p-6">
            <h2 className="font-display text-xl font-bold">
              Thông tin giao hàng
            </h2>
            <div className="mt-5 grid gap-4 sm:grid-cols-2">
              <Field
                label="Tên người nhận"
                value={delivery.recipientName}
                onChange={(value) =>
                  setDelivery({ ...delivery, recipientName: value })
                }
              />
              <Field
                label="Số điện thoại"
                value={delivery.phone}
                onChange={(value) =>
                  setDelivery({ ...delivery, phone: value })
                }
              />
              <div className="sm:col-span-2">
                <Field
                  label="Địa chỉ giao hàng"
                  value={delivery.shippingAddress}
                  onChange={(value) =>
                    setDelivery({ ...delivery, shippingAddress: value })
                  }
                />
              </div>
              <div className="sm:col-span-2">
                <label className="label">Ghi chú đơn hàng</label>
                <textarea
                  className="field min-h-24 resize-y"
                  value={delivery.note}
                  maxLength={1000}
                  onChange={(event) =>
                    setDelivery({ ...delivery, note: event.target.value })
                  }
                  placeholder="Hướng dẫn giao hàng (không bắt buộc)"
                />
              </div>
            </div>
          </div>

          <div className="card p-6">
            <h2 className="font-display text-xl font-bold">Phương thức thanh toán</h2>
            <div className="mt-5 grid gap-4 sm:grid-cols-2">
              <PaymentChoice
                active={paymentMethod === "COD"}
                icon={Banknote}
                title="Thanh toán khi nhận hàng"
                text="Đặt hàng và thanh toán khi nhận sản phẩm."
                onClick={() => setPaymentMethod("COD")}
                disabled={Boolean(pendingMomoOrder)}
              />
              <PaymentChoice
                active={paymentMethod === "MOMO"}
                icon={Smartphone}
                title="Ví MoMo"
                text="Hiển thị mã QR và chờ MoMo xác nhận."
                onClick={() => setPaymentMethod("MOMO")}
                disabled={Boolean(pendingMomoOrder)}
              />
            </div>
            {pendingMomoOrder && (
              <p className="mt-4 rounded-2xl bg-amber-50 p-3 text-sm text-amber-800">
                Đơn hàng {pendingMomoOrder.orderCode} đã được tạo. Hãy thử thanh toán lại mà không tạo thêm đơn hàng.
              </p>
            )}
          </div>
        </div>

        <aside className="card h-fit p-6 lg:sticky lg:top-28">
          <h2 className="font-display text-xl font-bold">Tóm tắt đơn hàng</h2>
          <div className="mt-5 grid gap-4">
            {summaryProducts.map((item) => (
              <div key={item.id} className="flex justify-between gap-4 text-sm">
                <span className="text-ink/60">
                  {item.productName} × {item.quantity}
                </span>
                <span className="font-semibold">
                  {formatCurrency(item.subtotal)}
                </span>
              </div>
            ))}
          </div>
          <div className="mt-5 flex justify-between border-t border-ink/10 pt-5">
            <span className="font-bold">Tổng cộng</span>
            <span className="font-display text-xl font-extrabold">
              {formatCurrency(summaryTotal)}
            </span>
          </div>
          {error && (
            <div className="mt-4 rounded-2xl bg-red-50 p-3 text-sm text-red-700">
              <p>{error}</p>
              {pendingMomoOrder && (
                <Link
                  to={`/orders/${pendingMomoOrder.id}`}
                  className="mt-2 inline-flex font-bold underline"
                >
                  Xem đơn hàng đã tạo
                </Link>
              )}
            </div>
          )}
          <button
            disabled={
              loading ||
              (!pendingMomoOrder &&
                cart.items.some((item) => !item.available))
            }
            onClick={checkout}
            className="btn-primary mt-6 w-full"
          >
            {loading
              ? pendingMomoOrder
                ? "Đang tạo mã QR..."
                : "Đang tạo đơn hàng..."
              : pendingMomoOrder
                ? "Tạo lại mã QR MoMo"
                : paymentMethod === "MOMO"
                  ? "Thanh toán bằng MoMo"
                  : "Đặt hàng"}
          </button>
        </aside>
      </div>
      </section>
      {momoPayment && (
        <MomoQrPaymentModal
          payment={momoPayment}
          onClose={() =>
            navigate(`/orders/${momoPayment.orderId}`, { replace: true })
          }
          onPaid={(payment) =>
            navigate(`/orders/${payment.orderId}`, {
              replace: true,
              state: { message: "Thanh toán MoMo đã được xác nhận thành công." },
            })
          }
        />
      )}
    </>
  );
}

function PaymentChoice({
  active,
  icon: Icon,
  title,
  text,
  onClick,
  disabled = false,
}: {
  active: boolean;
  icon: typeof Banknote;
  title: string;
  text: string;
  onClick: () => void;
  disabled?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={clsx(
        "rounded-3xl border-2 p-5 text-left transition",
        active
          ? "border-teal bg-teal/5"
          : "border-ink/10 bg-white hover:border-ink/25",
        disabled && "cursor-not-allowed opacity-60",
      )}
    >
      <Icon className={clsx("h-6 w-6", active && "text-teal")} />
      <p className="mt-4 font-bold">{title}</p>
      <p className="mt-1 text-sm leading-6 text-ink/50">{text}</p>
    </button>
  );
}

function Field({
  label,
  value,
  onChange,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label>
      <span className="label">{label}</span>
      <input
        className="field"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        required
      />
    </label>
  );
}
