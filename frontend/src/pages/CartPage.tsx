import { Minus, Plus, ShoppingCart, Trash2 } from "lucide-react";
import { Link } from "react-router-dom";
import { EmptyState } from "../components/EmptyState";
import { formatCurrency } from "../lib/format";
import { clearCart, removeCartItem, updateCartItem } from "../store/cartSlice";
import { useAppDispatch, useAppSelector } from "../store/hooks";

export function CartPage() {
  const dispatch = useAppDispatch();
  const { data: cart, loading, error } = useAppSelector((state) => state.cart);

  return (
    <section className="container-page py-10 sm:py-14">
      <div className="flex items-end justify-between gap-4">
        <div>
          <p className="text-sm font-bold uppercase tracking-[0.2em] text-teal">
            Sản phẩm bạn đã chọn
          </p>
          <h1 className="section-title mt-2">Giỏ hàng</h1>
        </div>
        {cart.items.length > 0 && (
          <button
            className="text-sm font-bold text-coral"
            onClick={() => dispatch(clearCart())}
          >
            Xóa giỏ hàng
          </button>
        )}
      </div>

      {error && (
        <p className="mt-6 rounded-2xl bg-red-50 p-4 text-red-700">{error}</p>
      )}

      {!cart.items.length ? (
        <div className="mt-8">
          <EmptyState
            icon={ShoppingCart}
            title="Giỏ hàng đang trống"
            description="Khám phá sản phẩm và thêm những món phù hợp vào giỏ hàng."
            action={
              <Link to="/products" className="btn-primary">
                Khám phá sản phẩm
              </Link>
            }
          />
        </div>
      ) : (
        <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_360px]">
          <div className="grid gap-4">
            {cart.items.map((item) => (
              <article
                key={item.id}
                className="card flex flex-col gap-5 p-5 sm:flex-row sm:items-center"
              >
                <Link
                  to={`/products/${item.productId}`}
                  className="h-28 w-full shrink-0 overflow-hidden rounded-2xl bg-cream sm:w-32"
                >
                  {item.thumbnailUrl ? (
                    <img
                      src={item.thumbnailUrl}
                      alt={item.productName}
                      className="h-full w-full object-cover"
                    />
                  ) : (
                    <div className="grid h-full place-items-center font-bold text-ink/15">
                      TS
                    </div>
                  )}
                </Link>
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-bold uppercase tracking-wider text-ink/40">
                    {item.sku}
                  </p>
                  <Link
                    to={`/products/${item.productId}`}
                    className="mt-1 block font-display text-lg font-bold"
                  >
                    {item.productName}
                  </Link>
                  <p className="mt-2 text-sm text-ink/50">
                    {formatCurrency(item.unitPrice)} mỗi sản phẩm
                  </p>
                  {!item.available && (
                    <p className="mt-2 text-sm font-semibold text-coral">
                      Sản phẩm này không còn khả dụng.
                    </p>
                  )}
                </div>
                <div className="flex items-center justify-between gap-4 sm:flex-col sm:items-end">
                  <p className="font-display text-lg font-extrabold">
                    {formatCurrency(item.subtotal)}
                  </p>
                  <div className="flex items-center gap-2">
                    <div className="flex items-center rounded-full border border-ink/10">
                      <button
                        className="p-2.5"
                        disabled={loading || item.quantity <= 1}
                        onClick={() =>
                          dispatch(
                            updateCartItem({
                              productId: item.productId,
                              quantity: item.quantity - 1,
                            }),
                          )
                        }
                      >
                        <Minus className="h-4 w-4" />
                      </button>
                      <span className="w-8 text-center text-sm font-bold">
                        {item.quantity}
                      </span>
                      <button
                        className="p-2.5"
                        disabled={
                          loading || item.quantity >= item.availableStock
                        }
                        onClick={() =>
                          dispatch(
                            updateCartItem({
                              productId: item.productId,
                              quantity: item.quantity + 1,
                            }),
                          )
                        }
                      >
                        <Plus className="h-4 w-4" />
                      </button>
                    </div>
                    <button
                      className="rounded-full p-2.5 text-coral hover:bg-red-50"
                      onClick={() => dispatch(removeCartItem(item.productId))}
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </article>
            ))}
          </div>

          <aside className="card h-fit p-6 lg:sticky lg:top-28">
            <h2 className="font-display text-xl font-bold">Tóm tắt đơn hàng</h2>
            <div className="mt-6 grid gap-3 text-sm">
              <div className="flex justify-between">
                <span className="text-ink/55">Sản phẩm</span>
                <span className="font-semibold">{cart.totalQuantity}</span>
              </div>
              <div className="flex justify-between border-t border-ink/10 pt-4">
                <span className="font-bold">Tổng cộng</span>
                <span className="font-display text-xl font-extrabold">
                  {formatCurrency(cart.totalAmount)}
                </span>
              </div>
            </div>
            <Link
              to="/checkout"
              className="btn-primary mt-6 w-full"
              aria-disabled={cart.items.some((item) => !item.available)}
            >
              Tiếp tục thanh toán
            </Link>
          </aside>
        </div>
      )}
    </section>
  );
}
