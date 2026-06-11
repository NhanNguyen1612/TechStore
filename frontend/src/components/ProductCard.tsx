import { Heart, ShoppingBag } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import clsx from "clsx";
import { formatCurrency } from "../lib/format";
import { addToCart } from "../store/cartSlice";
import { useAppDispatch, useAppSelector } from "../store/hooks";
import { addWishlist, removeWishlist } from "../store/wishlistSlice";
import type { ProductSummary } from "../types/api";

export function ProductCard({
  product,
  showSoldCount = false,
}: {
  product: ProductSummary;
  showSoldCount?: boolean;
}) {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const user = useAppSelector((state) => state.auth.user);
  const wishlisted = useAppSelector((state) =>
    state.wishlist.items.some((item) => item.productId === product.id),
  );
  const canShop = user?.role === "ROLE_CUSTOMER";

  const requireCustomer = () => {
    if (!user) {
      navigate("/login");
      return false;
    }
    return canShop;
  };

  return (
    <article className="group overflow-hidden rounded-3xl border border-black/5 bg-white shadow-sm transition hover:-translate-y-1 hover:shadow-soft">
      <div className="relative aspect-[4/3] overflow-hidden bg-gradient-to-br from-cream to-white">
        <Link to={`/products/${product.id}`}>
          {product.thumbnailUrl ? (
            <img
              src={product.thumbnailUrl}
              alt={product.name}
              className="h-full w-full object-cover transition duration-500 group-hover:scale-105"
            />
          ) : (
            <div className="flex h-full items-center justify-center text-5xl font-black text-ink/10">
              TS
            </div>
          )}
        </Link>
        <button
          onClick={() => {
            if (!requireCustomer()) return;
            if (wishlisted) {
              dispatch(removeWishlist(product.id));
            } else {
              dispatch(addWishlist(product.id));
            }
          }}
          className={clsx(
            "absolute right-3 top-3 rounded-full p-2.5 shadow-sm backdrop-blur transition",
            wishlisted
              ? "bg-coral text-white"
              : "bg-white/85 text-ink hover:bg-white",
          )}
          aria-label={wishlisted ? "Xóa khỏi yêu thích" : "Thêm vào yêu thích"}
        >
          <Heart className={clsx("h-5 w-5", wishlisted && "fill-current")} />
        </button>
      </div>
      <div className="p-5">
        <div className="mb-2 flex items-center justify-between gap-3 text-xs font-semibold uppercase tracking-wider text-ink/45">
          <span>{product.brandName}</span>
          <span>{product.categoryName}</span>
        </div>
        <Link
          to={`/products/${product.id}`}
          className="font-display text-lg font-bold leading-snug hover:text-teal"
        >
          {product.name}
        </Link>
        <div className="mt-4 flex items-end justify-between gap-3">
          <div>
            <p className="font-display text-xl font-extrabold">
              {formatCurrency(product.price)}
            </p>
            <p className="mt-1 text-xs text-ink/45">
              {showSoldCount && product.soldCount > 0
                ? `${product.soldCount} đã bán`
                : product.stockQuantity > 0
                  ? `${product.stockQuantity} còn hàng`
                  : "Hết hàng"}
            </p>
          </div>
          <button
            disabled={product.stockQuantity < 1}
            onClick={() => {
              if (!requireCustomer()) return;
              dispatch(addToCart({ productId: product.id, quantity: 1 }));
            }}
            className="rounded-full bg-lime p-3 text-ink transition hover:scale-105 disabled:opacity-40"
            aria-label="Thêm vào giỏ hàng"
          >
            <ShoppingBag className="h-5 w-5" />
          </button>
        </div>
      </div>
    </article>
  );
}
