import {
  ArrowRight,
  BadgeCheck,
  Flame,
  Headphones,
  Package,
  ShieldCheck,
  ShoppingBag,
  Truck,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ProductCard } from "../components/ProductCard";
import { api, apiData } from "../lib/api";
import { formatCurrency, getErrorMessage } from "../lib/format";
import type {
  Brand,
  PageResponse,
  ProductPage,
  ProductSummary,
} from "../types/api";

const PRODUCTS_PER_BRAND = 4;
const TOP_SELLER_SCAN_SIZE = 50;

export function HomePage() {
  const [topSellers, setTopSellers] = useState<ProductSummary[]>([]);
  const [catalogProducts, setCatalogProducts] = useState<ProductSummary[]>([]);
  const [brands, setBrands] = useState<Brand[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([
      apiData<ProductPage>(
        api.get("/api/products", {
          params: { sort: "BEST_SELLER", page: 0, size: TOP_SELLER_SCAN_SIZE },
        }),
      ),
      apiData<ProductPage>(
        api.get("/api/products", {
          params: { sort: "NEWEST", page: 0, size: 100 },
        }),
      ),
      apiData<PageResponse<Brand>>(
        api.get("/api/brands", {
          params: { page: 0, size: 100, sortBy: "name", direction: "ASC" },
        }),
      ),
    ])
      .then(([bestSellerPage, catalogPage, brandPage]) => {
        setTopSellers(
          bestSellerPage.content
            .filter((product) => product.soldCount > 0)
            .slice(0, 4),
        );
        setCatalogProducts(catalogPage.content);
        setBrands(brandPage.content);
      })
      .catch((requestError) => setError(getErrorMessage(requestError)))
      .finally(() => setLoading(false));
  }, []);

  const featuredProduct = topSellers[0];
  const brandSections = useMemo(
    () => groupByBrand(brands, catalogProducts),
    [brands, catalogProducts],
  );

  return (
    <>
      <HeroBanner product={featuredProduct} />

      <section className="container-page -mt-5 relative z-10 grid gap-3 sm:grid-cols-3">
        <Benefit
          icon={Truck}
          title="Giao hàng nhanh"
          text="Tồn kho minh bạch và dễ dàng theo dõi đơn hàng"
        />
        <Benefit
          icon={ShieldCheck}
          title="Thanh toán an toàn"
          text="Tài khoản và thanh toán được bảo vệ"
        />
        <Benefit
          icon={Headphones}
          title="Hỗ trợ trực tuyến"
          text="Trò chuyện trực tiếp với đội ngũ hỗ trợ"
        />
      </section>

      <main className="pb-20">
        <section className="container-page py-16">
          <SectionHeading
            eyebrow="Được yêu thích nhất"
            title="Sản phẩm bán chạy"
            description="Xếp hạng theo đơn hàng hoàn tất và số lượng bán thực tế."
            href="/products?sort=BEST_SELLER"
          />

          {error && (
            <p className="mt-7 rounded-2xl bg-red-50 p-4 text-red-700">
              {error}
            </p>
          )}

          {loading ? (
            <ProductGridSkeleton />
          ) : topSellers.length ? (
            <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
              {topSellers.map((product, index) => (
                <div key={product.id} className="relative">
                  <span className="absolute -left-2 -top-3 z-10 inline-flex h-10 min-w-10 items-center justify-center rounded-full bg-ink px-3 font-display text-sm font-extrabold text-lime shadow-lg">
                    #{index + 1}
                  </span>
                  <ProductCard product={product} showSoldCount />
                </div>
              ))}
            </div>
          ) : (
            <EmptyTopSellers />
          )}
        </section>

        {brandSections.map((brand, index) => (
          <BrandSection key={brand.id} brand={brand} index={index} />
        ))}
      </main>
    </>
  );
}

function HeroBanner({ product }: { product?: ProductSummary }) {
  return (
    <section className="container-page pt-7 sm:pt-10">
      <div className="relative isolate overflow-hidden rounded-[2.5rem] bg-ink px-6 py-10 text-white shadow-soft sm:px-10 lg:min-h-[540px] lg:px-16 lg:py-14">
        <div className="absolute -right-20 -top-24 h-96 w-96 rounded-full bg-lime/20 blur-3xl" />
        <div className="absolute -bottom-40 left-1/4 h-96 w-96 rounded-full bg-coral/20 blur-3xl" />
        <div className="relative grid h-full gap-10 lg:grid-cols-[1.05fr_.95fr] lg:items-center">
          <div className="max-w-2xl">
            <span className="inline-flex items-center gap-2 rounded-full bg-lime px-4 py-2 text-xs font-extrabold uppercase tracking-[0.14em] text-ink">
              <Flame className="h-4 w-4" />
              Bán chạy nhất
            </span>
            <h1 className="mt-6 font-display text-4xl font-extrabold leading-[1.02] tracking-[-0.04em] sm:text-6xl lg:text-7xl">
              Công nghệ tốt hơn.
              <span className="block text-lime">Tốt hơn mỗi ngày.</span>
            </h1>
            <p className="mt-6 max-w-xl text-base leading-7 text-white/65 sm:text-lg">
              {product
                ? "Sản phẩm bán chạy nhất hiện tại dựa trên các đơn hàng đã hoàn tất."
                : "Khám phá thương hiệu uy tín và lựa chọn công nghệ phù hợp với nhu cầu của bạn."}
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                to="/products"
                className="inline-flex items-center justify-center gap-2 rounded-full bg-lime px-6 py-3.5 font-bold text-ink transition hover:scale-[1.02]"
              >
                Mua ngay
                <ArrowRight className="h-5 w-5" />
              </Link>
              <Link
                to="/products?sort=BEST_SELLER"
                className="inline-flex items-center justify-center rounded-full border border-white/20 bg-white/10 px-6 py-3.5 font-bold text-white backdrop-blur transition hover:bg-white/15"
              >
                Khám phá sản phẩm bán chạy
              </Link>
            </div>
            <div className="mt-9 flex flex-wrap gap-x-6 gap-y-3 text-sm text-white/65">
              <span className="inline-flex items-center gap-2">
                <BadgeCheck className="h-4 w-4 text-lime" />
                Sản phẩm chính hãng
              </span>
              <span className="inline-flex items-center gap-2">
                <ShoppingBag className="h-4 w-4 text-lime" />
                Hàng có sẵn
              </span>
            </div>
          </div>

          <div className="relative mx-auto w-full max-w-[520px]">
            <div className="absolute inset-8 rounded-full bg-lime/25 blur-3xl" />
            <div className="relative overflow-hidden rounded-[2rem] border border-white/15 bg-white p-5 text-ink shadow-2xl sm:p-7">
              <div className="flex items-center justify-between gap-3">
                <span className="rounded-full bg-coral px-3 py-1.5 text-xs font-extrabold uppercase tracking-wider text-white">
                  #1 BÁN CHẠY NHẤT
                </span>
                {product && (
                  <span className="text-sm font-bold text-ink/45">
                    {product.soldCount} đã bán
                  </span>
                )}
              </div>
              <Link
                to={product ? `/products/${product.id}` : "/products"}
                className="mt-5 block"
              >
                <div className="grid aspect-[5/3] place-items-center overflow-hidden rounded-3xl bg-gradient-to-br from-cream via-white to-lime/15">
                  {product?.thumbnailUrl ? (
                    <img
                      src={product.thumbnailUrl}
                      alt={product.name}
                      className="h-full w-full object-contain p-4 transition duration-500 hover:scale-105"
                    />
                  ) : (
                    <Package className="h-24 w-24 text-ink/10" />
                  )}
                </div>
                <p className="mt-5 text-xs font-bold uppercase tracking-[0.16em] text-teal">
                  {product
                    ? `${product.brandName} / ${product.categoryName}`
                    : "Bộ sưu tập nổi bật"}
                </p>
                <div className="mt-2 flex items-end justify-between gap-4">
                  <h2 className="font-display text-xl font-extrabold sm:text-2xl">
                    {product?.name ?? "Tìm thiết bị yêu thích tiếp theo của bạn"}
                  </h2>
                  {product && (
                    <span className="shrink-0 font-display text-lg font-extrabold">
                      {formatCurrency(product.price)}
                    </span>
                  )}
                </div>
              </Link>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function BrandSection({
  brand,
  index,
}: {
  brand: BrandGroup;
  index: number;
}) {
  return (
    <section className={index % 2 ? "bg-white/65" : ""}>
      <div className="container-page py-10 sm:py-14">
        <SectionHeading
          eyebrow="Thương hiệu"
          title={brand.name}
          description={`${brand.total} sản phẩm đến từ ${brand.name}.`}
          href={`/products?brandId=${brand.id}`}
        />
        <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {brand.products.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      </div>
    </section>
  );
}

function SectionHeading({
  icon: Icon,
  eyebrow,
  title,
  description,
  href,
}: {
  icon?: typeof Package;
  eyebrow: string;
  title: string;
  description: string;
  href: string;
}) {
  return (
    <div className="flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
      <div>
        <p className="flex items-center gap-2 text-xs font-extrabold uppercase tracking-[0.2em] text-teal">
          {Icon && (
            <span className="grid h-8 w-8 place-items-center rounded-xl bg-lime text-ink">
              <Icon className="h-4 w-4" />
            </span>
          )}
          {eyebrow}
        </p>
        <h2 className="mt-3 font-display text-3xl font-extrabold tracking-tight sm:text-4xl">
          {title}
        </h2>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-ink/50">
          {description}
        </p>
      </div>
      <Link
        to={href}
        className="inline-flex shrink-0 items-center gap-2 font-bold text-teal transition hover:gap-3"
      >
        Xem tất cả
        <ArrowRight className="h-4 w-4" />
      </Link>
    </div>
  );
}

function Benefit({
  icon: Icon,
  title,
  text,
}: {
  icon: typeof Truck;
  title: string;
  text: string;
}) {
  return (
    <div className="flex items-center gap-4 rounded-3xl border border-black/5 bg-white p-5 shadow-lg">
      <span className="grid h-12 w-12 shrink-0 place-items-center rounded-2xl bg-lime">
        <Icon className="h-5 w-5" />
      </span>
      <div>
        <p className="font-display font-bold">{title}</p>
        <p className="mt-0.5 text-xs text-ink/45">{text}</p>
      </div>
    </div>
  );
}

function ProductGridSkeleton() {
  return (
    <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
      {Array.from({ length: 4 }, (_, index) => (
        <div
          key={index}
          className="h-[390px] animate-pulse rounded-3xl bg-white shadow-sm"
        />
      ))}
    </div>
  );
}

function EmptyTopSellers() {
  return (
    <div className="mt-8 rounded-[2rem] border border-dashed border-ink/15 bg-white p-10 text-center">
      <Package className="mx-auto h-10 w-10 text-teal" />
      <h3 className="mt-4 font-display text-xl font-bold">
        Chưa ghi nhận lượt bán
      </h3>
      <p className="mt-2 text-sm text-ink/50">
        Sản phẩm bán chạy sẽ xuất hiện khi khách hàng hoàn tất đơn hàng.
      </p>
      <Link
        to="/products"
        className="mt-5 inline-flex items-center gap-2 font-bold text-teal"
      >
        Xem tất cả sản phẩm
        <ArrowRight className="h-4 w-4" />
      </Link>
    </div>
  );
}

interface BrandGroup {
  id: number;
  name: string;
  total: number;
  products: ProductSummary[];
}

function groupByBrand(
  brands: Brand[],
  products: ProductSummary[],
): BrandGroup[] {
  const groups = new Map<number, BrandGroup>();

  products.forEach((product) => {
    const current = groups.get(product.brandId);
    if (current) {
      current.total += 1;
      if (current.products.length < PRODUCTS_PER_BRAND) {
        current.products.push(product);
      }
      return;
    }

    groups.set(product.brandId, {
      id: product.brandId,
      name: product.brandName,
      total: 1,
      products: [product],
    });
  });

  return brands
    .filter((brand) => groups.has(brand.id))
    .map((brand) => groups.get(brand.id)!);
}
