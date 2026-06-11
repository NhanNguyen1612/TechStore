import { Heart } from "lucide-react";
import { Link } from "react-router-dom";
import { EmptyState } from "../components/EmptyState";
import { ProductCard } from "../components/ProductCard";
import { useAppSelector } from "../store/hooks";
import type { ProductSummary } from "../types/api";

export function WishlistPage() {
  const items = useAppSelector((state) => state.wishlist.items);
  const products: ProductSummary[] = items.map((item) => ({
    id: item.productId,
    name: item.productName,
    slug: item.slug,
    sku: item.sku,
    price: item.price,
    stockQuantity: item.stockQuantity,
    soldCount: 0,
    thumbnailUrl: item.thumbnailUrl,
    categoryId: item.categoryId,
    categoryName: item.categoryName,
    brandId: item.brandId,
    brandName: item.brandName,
    createdAt: item.addedAt,
  }));

  return (
    <section className="container-page py-10 sm:py-14">
      <p className="text-sm font-bold uppercase tracking-[0.2em] text-teal">
        Sản phẩm đã lưu
      </p>
      <h1 className="section-title mt-2">Yêu thích</h1>
      {!products.length ? (
        <div className="mt-8">
          <EmptyState
            icon={Heart}
            title="Chưa có sản phẩm yêu thích"
            description="Nhấn biểu tượng trái tim để lưu sản phẩm yêu thích."
            action={
              <Link to="/products" className="btn-primary">
                Xem sản phẩm
              </Link>
            }
          />
        </div>
      ) : (
        <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {products.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}
    </section>
  );
}
