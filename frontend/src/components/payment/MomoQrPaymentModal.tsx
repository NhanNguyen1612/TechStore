import {
  CheckCircle2,
  Clock3,
  ExternalLink,
  LoaderCircle,
  RefreshCw,
  X,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { QRCodeSVG } from "qrcode.react";
import { formatCurrency, getErrorMessage } from "../../lib/format";
import { orderService } from "../../services/orderService";
import type { MomoPayment } from "../../types/api";

interface MomoQrPaymentModalProps {
  payment: MomoPayment;
  onClose: () => void;
  onPaid: (payment: MomoPayment) => void;
}

export function MomoQrPaymentModal({
  payment,
  onClose,
  onPaid,
}: MomoQrPaymentModalProps) {
  const [current, setCurrent] = useState(payment);
  const [remaining, setRemaining] = useState(() => secondsLeft(payment));
  const [retrying, setRetrying] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    setCurrent(payment);
    setRemaining(secondsLeft(payment));
  }, [payment]);

  useEffect(() => {
    if (current.status !== "PENDING") return;
    let active = true;

    const refresh = async () => {
      try {
        const updated = await orderService.getPaymentStatus(current.orderId);
        if (!active) return;
        setCurrent(updated);
        setRemaining(secondsLeft(updated));
        if (updated.status === "PAID") onPaid(updated);
      } catch (requestError) {
        if (active) setError(getErrorMessage(requestError));
      }
    };

    const timer = window.setInterval(() => {
      setRemaining(secondsLeft(current));
      void refresh();
    }, 1000);

    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, [current.orderId, current.status, current.expiresAt, onPaid]);

  const qrValue = useMemo(
    () => current.qrCodeUrl || current.payUrl || current.deeplink || "",
    [current.deeplink, current.payUrl, current.qrCodeUrl],
  );
  const paid = current.status === "PAID";
  const expired =
    !paid &&
    (remaining <= 0 ||
      current.status === "FAILED" ||
      current.status === "CANCELLED");

  const retry = async () => {
    setRetrying(true);
    setError("");
    try {
      const nextPayment = await orderService.createMomoPayment(current.orderId);
      setCurrent(nextPayment);
      setRemaining(secondsLeft(nextPayment));
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setRetrying(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 grid place-items-center bg-ink/65 p-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-labelledby="momo-payment-title"
    >
      <div className="relative w-full max-w-lg rounded-[2rem] bg-white p-6 shadow-2xl sm:p-8">
        <button
          type="button"
          onClick={onClose}
          className="absolute right-5 top-5 rounded-full p-2 text-ink/45 hover:bg-cream hover:text-ink"
          aria-label="Đóng thanh toán"
        >
          <X className="h-5 w-5" />
        </button>

        {paid ? (
          <div className="py-8 text-center">
            <CheckCircle2 className="mx-auto h-16 w-16 text-teal" />
            <h2
              id="momo-payment-title"
              className="mt-5 font-display text-3xl font-extrabold"
            >
              Thanh toán đã được xác nhận
            </h2>
            <p className="mt-3 text-ink/55">
              MoMo đã xác nhận tiền được chuyển vào tài khoản người bán.
            </p>
          </div>
        ) : (
          <>
            <p className="text-xs font-extrabold uppercase tracking-[0.2em] text-[#a50064]">
              Thanh toán MoMo
            </p>
            <h2
              id="momo-payment-title"
              className="mt-2 pr-10 font-display text-2xl font-extrabold"
            >
              Quét mã để thanh toán {formatCurrency(current.amount)}
            </h2>
            <p className="mt-2 text-sm text-ink/50">
              Đơn hàng {current.orderCode}. Thanh toán chỉ được xác nhận sau khi TechStore nhận thông báo hợp lệ từ MoMo.
            </p>

            <div className="mt-6 grid place-items-center rounded-3xl bg-[#fff5fb] p-6">
              {!expired && qrValue ? (
                <div className="rounded-2xl bg-white p-4 shadow-sm">
                  <QRCodeSVG
                    value={qrValue}
                    size={220}
                    level="M"
                    marginSize={1}
                  />
                </div>
              ) : (
                <div className="grid h-[252px] w-[252px] place-items-center rounded-2xl border-2 border-dashed border-[#a50064]/20 bg-white px-8 text-center">
                  <div>
                    <Clock3 className="mx-auto h-10 w-10 text-[#a50064]" />
                    <p className="mt-3 font-display text-xl font-bold">
                      Mã QR đã hết hạn
                    </p>
                    <p className="mt-2 text-sm text-ink/50">
                      Tạo mã QR mới để tiếp tục thanh toán.
                    </p>
                  </div>
                </div>
              )}
            </div>

            <div className="mt-5 flex items-center justify-center gap-2 text-sm font-bold">
              {expired ? (
                <span className="text-coral">Phiên thanh toán đã hết hạn</span>
              ) : (
                <>
                  <LoaderCircle className="h-4 w-4 animate-spin text-teal" />
                  <span>Đang chờ MoMo xác nhận</span>
                  <span className="rounded-full bg-cream px-2.5 py-1 tabular-nums">
                    00:{String(remaining).padStart(2, "0")}
                  </span>
                </>
              )}
            </div>
          </>
        )}

        {error && (
          <p className="mt-4 rounded-2xl bg-red-50 p-3 text-sm text-red-700">
            {error}
          </p>
        )}

        <div className="mt-6 grid gap-3 sm:grid-cols-2">
          <Link to={`/orders/${current.orderId}`} className="btn-secondary">
            Xem đơn hàng
          </Link>
          {paid ? (
            <button type="button" onClick={onClose} className="btn-primary">
              Hoàn tất
            </button>
          ) : expired ? (
            <button
              type="button"
              onClick={retry}
              disabled={retrying}
              className="btn-primary"
            >
              {retrying ? (
                <LoaderCircle className="h-4 w-4 animate-spin" />
              ) : (
                <RefreshCw className="h-4 w-4" />
              )}
              Tạo mã QR mới
            </button>
          ) : (
            <a
              href={current.deeplink || current.payUrl || undefined}
              target="_blank"
              rel="noreferrer"
              className="btn-primary"
            >
              Mở MoMo
              <ExternalLink className="h-4 w-4" />
            </a>
          )}
        </div>
      </div>
    </div>
  );
}

function secondsLeft(payment: MomoPayment) {
  if (!payment.expiresAt) return 0;
  return Math.max(
    0,
    Math.ceil((new Date(payment.expiresAt).getTime() - Date.now()) / 1000),
  );
}
