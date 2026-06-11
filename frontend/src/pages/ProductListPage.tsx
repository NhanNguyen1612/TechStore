import { Filter, Search, SlidersHorizontal } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { api, apiData } from "../lib/api";
import { getErrorMessage } from "../lib/format";
import type {
  Brand,
  PageResponse,
  ProductPage,
  ProductSort,
  ProductSummary,
} from "../types/api";
import { ProductCard } from "../components/ProductCard";
import { Pagination } from "../components/Pagination";
import { LoadingScreen } from "../components/LoadingScreen";

export function ProductListPage() {
  const [searchParams] = useSearchParams();
  const [products, setProducts] = useState<ProductPage | null>(null);
  const [facets, setFacets] = useState<ProductSummary[]>([]);
  const [brands, setBrands] = useState<Brand[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filters, setFilters] = useState(() => ({
    q: searchParams.get("q") ?? "",
    categoryId: searchParams.get("categoryId") ?? "",
    brandId: searchParams.get("brandId") ?? "",
    minPrice: searchParams.get("minPrice") ?? "",
    maxPrice: searchParams.get("maxPrice") ?? "",
    sort: (searchParams.get("sort") as ProductSort | null) ?? "NEWEST",
    page: 0,
  }));

  useEffect(() => {
    Promise.all([
      apiData<ProductPage>(api.get("/api/products", { params: { size: 100 } })),
      apiData<PageResponse<Brand>>(
        api.get("/api/brands", { params: { size: 100 } }),
      ),
    ])
      .then(([productPage, brandPage]) => {
        setFacets(productPage.content);
        setBrands(brandPage.content);
      })
      .catch(() => undefined);
  }, []);

  useEffect(() => {
    setLoading(true);
    setError("");
    const params = {
      q: filters.q || undefined,
      categoryId: filters.categoryId || undefined,
      brandId: filters.brandId || undefined,
      minPrice: filters.minPrice || undefined,
      maxPrice: filters.maxPrice || undefined,
      sort: filters.sort,
      page: filters.page,
      size: 12,
    };
    const endpoint = filters.q ? "/api/products/search" : "/api/products";
    apiData<ProductPage>(api.get(endpoint, { params }))
      .then((productPage) => {
        setProducts(productPage);
        setError("");
      })
      .catch((requestError) => setError(getErrorMessage(requestError)))
      .finally(() => setLoading(false));
  }, [filters]);

  const categories = useMemo(
    () =>
      Array.from(
        new Map(
          facets.map((product) => [
            product.categoryId,
            { id: product.categoryId, name: product.categoryName },
          ]),
        ).values(),
      ).sort((a, b) => a.name.localeCompare(b.name)),
    [facets],
  );

  const updateFilter = (key: keyof typeof filters, value: string | number) =>
    setFilters((current) => ({ ...current, [key]: value, page: 0 }));

  return (
    <section className="container-page py-10 sm:py-14">
      <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
        <div>
          <p className="text-sm font-bold uppercase tracking-[0.2em] text-teal">
            Khám phá sản phẩm
          </p>
          <h1 className="section-title mt-2">Sản phẩm cho mọi nhu cầu</h1>
        </div>
        <p className="text-sm text-ink/50">
          {products?.totalElements ?? 0} sản phẩm được tìm thấy
        </p>
      </div>

      <div className="card mt-8 grid gap-4 p-4 lg:grid-cols-[1.4fr_repeat(5,1fr)]">
        <label className="relative lg:col-span-1">
          <Search className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-ink/35" />
          <input
            className="field pl-12"
            value={filters.q}
            onChange={(event) => updateFilter("q", event.target.value)}
            placeholder="Tìm kiếm sản phẩm..."
          />
        </label>
        <select
          className="field"
          value={filters.categoryId}
          onChange={(event) => updateFilter("categoryId", event.target.value)}
        >
          <option value="">Tất cả danh mục</option>
          {categories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
        <select
          className="field"
          value={filters.brandId}
          onChange={(event) => updateFilter("brandId", event.target.value)}
        >
          <option value="">Tất cả thương hiệu</option>
          {brands.map((brand) => (
            <option key={brand.id} value={brand.id}>
              {brand.name}
            </option>
          ))}
        </select>
        <input
          className="field"
          type="number"
          min="0"
          value={filters.minPrice}
          onChange={(event) => updateFilter("minPrice", event.target.value)}
          placeholder="Giá thấp nhất"
        />
        <input
          className="field"
          type="number"
          min="0"
          value={filters.maxPrice}
          onChange={(event) => updateFilter("maxPrice", event.target.value)}
          placeholder="Giá cao nhất"
        />
        <select
          className="field"
          value={filters.sort}
          onChange={(event) =>
            updateFilter("sort", event.target.value as ProductSort)
          }
        >
          <option value="NEWEST">Mới nhất</option>
          <option value="PRICE_ASC">Giá: thấp đến cao</option>
          <option value="PRICE_DESC">Giá: cao đến thấp</option>
          <option value="BEST_SELLER">Bán chạy nhất</option>
        </select>
      </div>

      {error && (
        <p className="mt-6 rounded-2xl bg-red-50 p-4 text-red-700">{error}</p>
      )}
      {loading ? (
        <LoadingScreen label="Đang tìm sản phẩm" />
      ) : products?.content.length ? (
        <>
          <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {products.content.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
          <Pagination
            page={products.page}
            totalPages={products.totalPages}
            onChange={(page) => setFilters((current) => ({ ...current, page }))}
          />
        </>
      ) : (
        <div className="card mt-8 flex flex-col items-center py-16 text-center">
          <Filter className="h-10 w-10 text-teal" />
          <h2 className="mt-4 font-display text-xl font-bold">
            Không có sản phẩm phù hợp với bộ lọc
          </h2>
          <button
            className="btn-secondary mt-5"
            onClick={() =>
              setFilters({
                q: "",
                categoryId: "",
                brandId: "",
                minPrice: "",
                maxPrice: "",
                sort: "NEWEST",
                page: 0,
              })
            }
          >
            <SlidersHorizontal className="h-5 w-5" />
            Đặt lại bộ lọc
          </button>
        </div>
      )}
    </section>
  );
}
