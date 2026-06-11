import {
  BadgeCheck,
  Heart,
  ImagePlus,
  Minus,
  Plus,
  ShoppingBag,
} from "lucide-react";
import { type FormEvent, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import clsx from "clsx";
import { api, apiData } from "../lib/api";
import { formatCurrency, formatDate, getErrorMessage } from "../lib/format";
import { addToCart } from "../store/cartSlice";
import { useAppDispatch, useAppSelector } from "../store/hooks";
import { addWishlist, removeWishlist } from "../store/wishlistSlice";
import type { ProductDetail, ProductReviews } from "../types/api";
import { LoadingScreen } from "../components/LoadingScreen";
import { RatingStars } from "../components/RatingStars";

export function ProductDetailPage() {
  const { id } = useParams();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const user = useAppSelector((state) => state.auth.user);
  const wishlisted = useAppSelector((state) =>
    state.wishlist.items.some((item) => item.productId === Number(id)),
  );
  const [product, setProduct] = useState<ProductDetail | null>(null);
  const [reviews, setReviews] = useState<ProductReviews | null>(null);
  const [selectedImage, setSelectedImage] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reviewForm, setReviewForm] = useState({
    rating: 5,
    comment: "",
    images: [] as File[],
  });
  const [reviewMessage, setReviewMessage] = useState("");

  const load = async () => {
    if (!id) return;
    try {
      const [productData, reviewData] = await Promise.all([
        apiData<ProductDetail>(api.get(`/api/products/${id}`)),
        apiData<ProductReviews>(api.get(`/api/reviews/product/${id}`)),
      ]);
      setProduct(productData);
      setReviews(reviewData);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [id]);

  const submitReview = async (event: FormEvent) => {
    event.preventDefault();
    if (!product) return;
    try {
      if (reviewForm.images.length) {
        const form = new FormData();
        form.append(
          "request",
          new Blob(
            [
              JSON.stringify({
                productId: product.id,
                rating: reviewForm.rating,
                comment: reviewForm.comment,
              }),
            ],
            { type: "application/json" },
          ),
          "request.json",
        );
        reviewForm.images.forEach((image) => form.append("images", image));
        await api.post("/api/reviews", form);
      } else {
        await api.post("/api/reviews", {
          productId: product.id,
          rating: reviewForm.rating,
          comment: reviewForm.comment,
        });
      }
      setReviewMessage("Đánh giá đã được gửi và đang chờ quản trị viên duyệt.");
      setReviewForm({ rating: 5, comment: "", images: [] });
    } catch (requestError) {
      setReviewMessage(getErrorMessage(requestError));
    }
  };

  if (loading) return <LoadingScreen label="Đang tải sản phẩm" />;
  if (!product) {
    return (
      <div className="container-page py-16 text-center text-red-700">
        {error || "Không tìm thấy sản phẩm"}
      </div>
    );
  }

  const images = product.images.length
    ? product.images
    : [{ id: 0, url: "", sortOrder: 0, primary: true }];

  return (
    <section className="container-page py-10 sm:py-14">
      <div className="grid gap-8 lg:grid-cols-2">
        <div>
          <div className="card aspect-square overflow-hidden bg-white">
            {images[selectedImage]?.url ? (
              <img
                src={images[selectedImage].url}
                alt={product.name}
                className="h-full w-full object-contain p-6"
              />
            ) : (
              <div className="grid h-full place-items-center font-display text-7xl font-black text-ink/10">
                TS
              </div>
            )}
          </div>
          {product.images.length > 1 && (
            <div className="mt-4 flex gap-3 overflow-x-auto pb-2">
              {product.images.map((image, index) => (
                <button
                  key={image.id}
                  onClick={() => setSelectedImage(index)}
                  className={clsx(
                    "h-20 w-20 shrink-0 overflow-hidden rounded-2xl border-2 bg-white",
                    index === selectedImage
                      ? "border-teal"
                      : "border-transparent",
                  )}
                >
                  <img
                    src={image.url}
                    alt=""
                    className="h-full w-full object-cover"
                  />
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="lg:pl-6">
          <div className="flex flex-wrap gap-2 text-xs font-bold uppercase tracking-wider">
            <span className="rounded-full bg-lime px-3 py-1.5">
              {product.brandName}
            </span>
            <span className="rounded-full bg-white px-3 py-1.5">
              {product.categoryName}
            </span>
          </div>
          <h1 className="mt-5 font-display text-4xl font-extrabold tracking-tight sm:text-5xl">
            {product.name}
          </h1>
          <div className="mt-4 flex items-center gap-3">
            <RatingStars value={Math.round(reviews?.averageRating ?? 0)} />
            <span className="text-sm text-ink/50">
              {reviews?.averageRating ?? 0} ({reviews?.totalReviews ?? 0} đánh giá)
            </span>
          </div>
          <p className="mt-6 font-display text-3xl font-extrabold text-teal">
            {formatCurrency(product.price)}
          </p>
          <p className="mt-6 whitespace-pre-line leading-7 text-ink/60">
            {product.description || "Sản phẩm chưa có mô tả."}
          </p>

          <div className="mt-8 flex flex-wrap items-center gap-3">
            <div className="flex items-center rounded-full border border-ink/10 bg-white p-1">
              <button
                className="rounded-full p-3 hover:bg-cream"
                onClick={() => setQuantity((value) => Math.max(1, value - 1))}
              >
                <Minus className="h-4 w-4" />
              </button>
              <span className="w-12 text-center font-bold">{quantity}</span>
              <button
                className="rounded-full p-3 hover:bg-cream"
                onClick={() =>
                  setQuantity((value) =>
                    Math.min(product.stockQuantity, value + 1),
                  )
                }
              >
                <Plus className="h-4 w-4" />
              </button>
            </div>
            <button
              className="btn-primary flex-1 sm:flex-none"
              disabled={product.stockQuantity < 1}
              onClick={() => {
                if (!user) return navigate("/login");
                if (user.role !== "ROLE_CUSTOMER") return;
                dispatch(addToCart({ productId: product.id, quantity }));
              }}
            >
              <ShoppingBag className="h-5 w-5" />
              Thêm vào giỏ hàng
            </button>
            <button
              className={clsx(
                "btn-secondary !px-4",
                wishlisted && "!border-coral !text-coral",
              )}
              onClick={() => {
                if (!user) return navigate("/login");
                if (user.role !== "ROLE_CUSTOMER") return;
                if (wishlisted) {
                  dispatch(removeWishlist(product.id));
                } else {
                  dispatch(addWishlist(product.id));
                }
              }}
            >
              <Heart
                className={clsx("h-5 w-5", wishlisted && "fill-current")}
              />
            </button>
          </div>
          <p className="mt-4 text-sm font-medium text-ink/50">
            {product.stockQuantity > 0
              ? `${product.stockQuantity} sản phẩm còn hàng`
              : "Sản phẩm hiện đã hết hàng"}
          </p>
        </div>
      </div>

      <div className="mt-16 grid gap-8 lg:grid-cols-[1fr_.72fr]">
        <div>
          <h2 className="section-title">Đánh giá của khách hàng</h2>
          <div className="mt-6 grid gap-4">
            {reviews?.reviews.length ? (
              reviews.reviews.map((review) => (
                <article key={review.id} className="card p-6">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <p className="font-bold">{review.userName}</p>
                      <div className="mt-1 flex items-center gap-2">
                        <RatingStars value={review.rating} size="sm" />
                        {review.verifiedPurchase && (
                          <span className="inline-flex items-center gap-1 text-xs font-semibold text-teal">
                            <BadgeCheck className="h-4 w-4" />
                            Đã mua hàng
                          </span>
                        )}
                      </div>
                    </div>
                    <time className="text-xs text-ink/40">
                      {formatDate(review.createdAt)}
                    </time>
                  </div>
                  <p className="mt-4 leading-7 text-ink/65">{review.comment}</p>
                  {review.images.length > 0 && (
                    <div className="mt-4 flex gap-3 overflow-x-auto">
                      {review.images.map((image) => (
                        <img
                          key={image.id}
                          src={image.url}
                          alt=""
                          className="h-24 w-24 rounded-2xl object-cover"
                        />
                      ))}
                    </div>
                  )}
                </article>
              ))
            ) : (
              <div className="card p-8 text-center text-ink/50">
                Chưa có đánh giá nào được duyệt.
              </div>
            )}
          </div>
        </div>

        {user?.role === "ROLE_CUSTOMER" && (
          <form onSubmit={submitReview} className="card h-fit p-6">
            <h2 className="font-display text-xl font-bold">Viết đánh giá</h2>
            <p className="mt-2 text-sm leading-6 text-ink/50">
              Chỉ sản phẩm đã giao mới có thể đánh giá. Đánh giá mới cần quản trị viên duyệt.
            </p>
            <div className="mt-5">
              <label className="label">Số sao</label>
              <RatingStars
                value={reviewForm.rating}
                onChange={(rating) =>
                  setReviewForm((current) => ({ ...current, rating }))
                }
              />
            </div>
            <div className="mt-5">
              <label className="label">Nội dung đánh giá</label>
              <textarea
                className="field min-h-32 resize-y"
                required
                maxLength={2000}
                value={reviewForm.comment}
                onChange={(event) =>
                  setReviewForm((current) => ({
                    ...current,
                    comment: event.target.value,
                  }))
                }
              />
            </div>
            <label className="mt-5 flex cursor-pointer items-center gap-3 rounded-2xl border border-dashed border-ink/20 p-4 text-sm font-semibold hover:border-teal">
              <ImagePlus className="h-5 w-5 text-teal" />
              {reviewForm.images.length
                ? `${reviewForm.images.length} image(s) selected`
                : "Thêm tối đa 5 hình ảnh"}
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                multiple
                className="hidden"
                onChange={(event) =>
                  setReviewForm((current) => ({
                    ...current,
                    images: Array.from(event.target.files ?? []).slice(0, 5),
                  }))
                }
              />
            </label>
            {reviewMessage && (
              <p className="mt-4 text-sm font-medium text-teal">
                {reviewMessage}
              </p>
            )}
            <button className="btn-primary mt-5 w-full">Gửi đánh giá</button>
          </form>
        )}
      </div>
    </section>
  );
}
