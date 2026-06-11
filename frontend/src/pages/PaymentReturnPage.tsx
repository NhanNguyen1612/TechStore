import { CheckCircle2, CircleX, LoaderCircle } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { api, apiData } from "../lib/api";
import { getErrorMessage } from "../lib/format";
import { orderService } from "../services/orderService";
import type { MomoPayment, MomoReturn } from "../types/api";

export function PaymentReturnPage() {
  const [searchParams] = useSearchParams();
  const [result, setResult] = useState<MomoReturn | null>(null);
  const [payment, setPayment] = useState<MomoPayment | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    apiData<MomoReturn>(
      api.get("/api/payments/momo/return", {
        params: Object.fromEntries(searchParams.entries()),
      }),
    )
      .then(setResult)
      .catch((requestError) => setError(getErrorMessage(requestError)));
  }, [searchParams]);

  useEffect(() => {
    if (!result?.orderId || payment?.status === "PAID") return;
    let active = true;

    const refresh = async () => {
      try {
        const updated = await orderService.getPaymentStatus(result.orderId!);
        if (active) setPayment(updated);
      } catch (requestError) {
        if (active) setError(getErrorMessage(requestError));
      }
    };

    void refresh();
    const timer = window.setInterval(refresh, 1000);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, [payment?.status, result?.orderId]);

  const success =
    result?.signatureValid &&
    result.matched &&
    (result.paymentStatus === "PAID" || payment?.status === "PAID");
  const waiting =
    result?.signatureValid &&
    result.matched &&
    result.resultCode === 0 &&
    !success &&
    payment?.status !== "FAILED" &&
    payment?.status !== "CANCELLED";

  return (
    <section className="container-page py-16">
      <div className="card mx-auto max-w-xl p-8 text-center sm:p-12">
        {!result && !error || waiting ? (
          <LoaderCircle className="mx-auto h-12 w-12 animate-spin text-teal" />
        ) : success ? (
          <CheckCircle2 className="mx-auto h-14 w-14 text-teal" />
        ) : (
          <CircleX className="mx-auto h-14 w-14 text-coral" />
        )}
        <h1 className="mt-5 font-display text-3xl font-extrabold">
          {!result && !error
            ? "Đang xác minh thanh toán"
            : success
              ? "Thanh toán thành công"
              : waiting
                ? "Đang chờ MoMo xác nhận"
                : "Thanh toán chưa hoàn tất"}
        </h1>
        <p className="mt-3 leading-7 text-ink/55">
          {error ||
            (waiting
              ? "Thông tin chuyển hướng hợp lệ, nhưng đơn hàng chỉ được ghi nhận đã thanh toán sau khi TechStore nhận thông báo máy chủ đã xác minh từ MoMo."
              : result?.message) ||
            "Vui lòng chờ trong khi hệ thống xác nhận phản hồi từ MoMo."}
        </p>
        <div className="mt-8 flex flex-wrap justify-center gap-3">
          {result?.orderId && (
            <Link to={`/orders/${result.orderId}`} className="btn-primary">
              Xem đơn hàng
            </Link>
          )}
          <Link to="/orders" className="btn-secondary">
            Tất cả đơn hàng
          </Link>
        </div>
      </div>
    </section>
  );
}
