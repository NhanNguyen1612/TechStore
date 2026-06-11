import {
  BarChart3,
  CircleDollarSign,
  Package,
  RefreshCw,
  Settings2,
  ShoppingCart,
  TrendingUp,
  UsersRound,
} from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { EmptyState } from "../components/EmptyState";
import { LoadingScreen } from "../components/LoadingScreen";
import { api, apiData } from "../lib/api";
import { orderStatusLabel } from "../lib/constants";
import { formatCurrency, getErrorMessage } from "../lib/format";
import type {
  AdminCustomersAnalytics,
  AdminOrderAnalytics,
  AdminOverview,
  AdminProductsAnalytics,
  AdminRevenue,
  OrderStatus,
} from "../types/api";

const chartColors = [
  "#15231f",
  "#0f766e",
  "#d7f64b",
  "#ff6b4a",
  "#94a3b8",
  "#f59e0b",
  "#8b5cf6",
  "#06b6d4",
];

type Grouping = "DAILY" | "MONTHLY" | "YEARLY";

const groupingLabel: Record<Grouping, string> = {
  DAILY: "hàng ngày",
  MONTHLY: "hàng tháng",
  YEARLY: "hàng năm",
};

export function DashboardPage() {
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [grouping, setGrouping] = useState<Grouping>("DAILY");
  const [data, setData] = useState<{
    overview: AdminOverview;
    revenue: AdminRevenue;
    orders: AdminOrderAnalytics;
    products: AdminProductsAnalytics;
    customers: AdminCustomersAnalytics;
  } | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    const params = { from: from || undefined, to: to || undefined };
    Promise.all([
      apiData<AdminOverview>(api.get("/api/admin/dashboard/overview")),
      apiData<AdminRevenue>(
        api.get("/api/admin/dashboard/revenue", {
          params: { ...params, type: grouping },
        }),
      ),
      apiData<AdminOrderAnalytics>(
        api.get("/api/admin/dashboard/orders", { params }),
      ),
      apiData<AdminProductsAnalytics>(
        api.get("/api/admin/dashboard/products", {
          params: { limit: 8, lowStockThreshold: 5 },
        }),
      ),
      apiData<AdminCustomersAnalytics>(
        api.get("/api/admin/dashboard/customers", { params: { limit: 8 } }),
      ),
    ])
      .then(([overview, revenue, orders, products, customers]) => {
        if (!active) return;
        setData({ overview, revenue, orders, products, customers });
        setError("");
      })
      .catch((requestError) => {
        if (active) setError(getErrorMessage(requestError));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [from, to, grouping, refreshKey]);

  useEffect(() => {
    const refresh = () => setRefreshKey((value) => value + 1);
    const interval = window.setInterval(refresh, 30_000);
    window.addEventListener("focus", refresh);
    return () => {
      window.clearInterval(interval);
      window.removeEventListener("focus", refresh);
    };
  }, []);

  if (loading && !data) return <LoadingScreen label="Đang tải bảng thống kê" />;

  const orderChart = data?.orders.byStatus.filter((item) => item.count > 0) ?? [];

  return (
    <section className="container-page py-10 sm:py-14">
      <div className="flex flex-col justify-between gap-6 xl:flex-row xl:items-end">
        <div>
          <p className="text-sm font-bold uppercase tracking-[0.2em] text-teal">
            Thống kê quản trị
          </p>
          <h1 className="section-title mt-2">Bảng điều khiển kinh doanh</h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-ink/55">
            Doanh thu, đơn hàng, khách hàng và tồn kho được tổng hợp từ dữ liệu hệ thống.
          </p>
        </div>
        <div className="flex flex-wrap items-end gap-3">
          <label className="min-w-40 flex-1">
            <span className="label">Từ ngày</span>
            <input
              className="field"
              type="date"
              value={from}
              max={to || undefined}
              onChange={(event) => setFrom(event.target.value)}
            />
          </label>
          <label className="min-w-40 flex-1">
            <span className="label">Đến ngày</span>
            <input
              className="field"
              type="date"
              value={to}
              min={from || undefined}
              onChange={(event) => setTo(event.target.value)}
            />
          </label>
          <label className="min-w-36 flex-1">
            <span className="label">Nhóm theo</span>
            <select
              className="field"
              value={grouping}
              onChange={(event) => setGrouping(event.target.value as Grouping)}
            >
              <option value="DAILY">Theo ngày</option>
              <option value="MONTHLY">Theo tháng</option>
              <option value="YEARLY">Theo năm</option>
            </select>
          </label>
          <button
            type="button"
            className="btn-secondary whitespace-nowrap"
            disabled={loading}
            onClick={() => setRefreshKey((value) => value + 1)}
          >
            <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            Làm mới
          </button>
          <Link to="/admin" className="btn-primary whitespace-nowrap">
            <Settings2 className="h-4 w-4" />
            Quản lý cửa hàng
          </Link>
        </div>
      </div>

      {error && (
        <p className="mt-6 rounded-2xl bg-red-50 p-4 text-red-700">{error}</p>
      )}

      {data && (
        <>
          <div className="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <Metric
              icon={CircleDollarSign}
              label="Tổng doanh thu"
              value={formatCurrency(data.overview.totalRevenue)}
              detail={`${formatCurrency(data.overview.monthRevenue)} trong tháng này`}
            />
            <Metric
              icon={ShoppingCart}
              label="Tổng đơn hàng"
              value={data.overview.totalOrder.toLocaleString()}
              detail={`${data.overview.unpaidOrder} đơn đang chờ thanh toán`}
            />
            <Metric
              icon={UsersRound}
              label="Người dùng"
              value={data.overview.totalUsers.toLocaleString()}
              detail={`${data.customers.newThisMonth} khách hàng mới trong tháng này`}
            />
            <Metric
              icon={Package}
              label="Sản phẩm"
              value={data.overview.totalProducts.toLocaleString()}
              detail={`${data.products.lowStock.length} sản phẩm sắp hết hàng`}
            />
          </div>

          <div className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <MiniMetric
              label="Doanh thu hôm nay"
              value={formatCurrency(data.overview.todayRevenue)}
            />
            <MiniMetric
              label="Đơn đã thanh toán"
              value={data.overview.paidOrder.toLocaleString()}
            />
            <MiniMetric
              label="Tỷ lệ hủy"
              value={`${data.overview.cancellationRate}%`}
            />
            <MiniMetric
              label="MoMo thành công"
              value={`${data.overview.momoSuccessRate}%`}
            />
          </div>

          <p className="mt-5 rounded-2xl bg-white/65 px-4 py-3 text-sm text-ink/55">
            Doanh thu COD được ghi nhận khi đơn hàng được xác nhận. Doanh thu MoMo được ghi nhận sau khi thanh toán thành công.
          </p>

          <div className="mt-6 grid gap-6 xl:grid-cols-[1.35fr_0.65fr]">
            <ChartCard title={`Xu hướng doanh thu ${groupingLabel[grouping]}`}>
              {data.revenue.series.length ? (
                <ResponsiveContainer width="100%" height={310}>
                  <LineChart data={data.revenue.series}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="period" tick={{ fontSize: 12 }} />
                    <YAxis
                      width={76}
                      tick={{ fontSize: 12 }}
                      tickFormatter={compactMoney}
                    />
                    <Tooltip
                      formatter={(value) => formatCurrency(Number(value))}
                    />
                    <Line
                      type="monotone"
                      dataKey="revenue"
                      stroke="#0f766e"
                      strokeWidth={3}
                      dot={{ fill: "#d7f64b", strokeWidth: 2 }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              ) : (
                <ChartEmpty label="Không có doanh thu trong khoảng thời gian này" />
              )}
            </ChartCard>

            <ChartCard title="Doanh thu theo phương thức thanh toán">
              <ResponsiveContainer width="100%" height={310}>
                <BarChart
                  data={[
                    { name: "MoMo", value: data.revenue.momoRevenue },
                    { name: "COD", value: data.revenue.codRevenue },
                  ]}
                >
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="name" />
                  <YAxis width={72} tickFormatter={compactMoney} />
                  <Tooltip formatter={(value) => formatCurrency(Number(value))} />
                  <Bar dataKey="value" radius={[12, 12, 0, 0]}>
                    <Cell fill="#0f766e" />
                    <Cell fill="#d7f64b" />
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </ChartCard>
          </div>

          <div className="mt-6 grid gap-6 xl:grid-cols-2">
            <ChartCard title="Đơn hàng theo trạng thái">
              {orderChart.length ? (
                <div className="grid items-center gap-4 md:grid-cols-[1fr_180px]">
                  <ResponsiveContainer width="100%" height={300}>
                    <PieChart>
                      <Pie
                        data={orderChart}
                        dataKey="count"
                        nameKey="label"
                        innerRadius={66}
                        outerRadius={108}
                        paddingAngle={3}
                      >
                        {orderChart.map((item, index) => (
                          <Cell
                            key={item.label}
                            fill={chartColors[index % chartColors.length]}
                          />
                        ))}
                      </Pie>
                      <Tooltip />
                    </PieChart>
                  </ResponsiveContainer>
                  <div className="space-y-2">
                    {orderChart.map((item, index) => (
                      <div
                        key={item.label}
                        className="flex items-center justify-between gap-3 text-xs"
                      >
                        <span className="flex items-center gap-2">
                          <span
                            className="h-2.5 w-2.5 rounded-full"
                            style={{
                              background:
                                chartColors[index % chartColors.length],
                            }}
                          />
                          {orderStatusLabel[item.label as OrderStatus] ??
                            item.label.replaceAll("_", " ")}
                        </span>
                        <strong>{item.count}</strong>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <ChartEmpty label="Không có đơn hàng trong khoảng thời gian này" />
              )}
            </ChartCard>

            <ChartCard title="Hoạt động đặt hàng">
              {data.orders.monthly.length ? (
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={data.orders.monthly}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="period" />
                    <YAxis allowDecimals={false} width={40} />
                    <Tooltip />
                    <Bar dataKey="orders" fill="#15231f" radius={[10, 10, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <ChartEmpty label="Chưa có hoạt động đặt hàng" />
              )}
            </ChartCard>
          </div>

          <div className="mt-6 grid gap-6 xl:grid-cols-2">
            <TableCard title="Sản phẩm bán chạy" icon={Package}>
              {data.products.bestSellers.length ? (
                data.products.bestSellers.map((product, index) => (
                  <div
                    key={product.productId}
                    className="grid grid-cols-[36px_1fr_auto] items-center gap-3 border-b border-ink/5 py-4 last:border-0"
                  >
                    <Rank value={index + 1} />
                    <div className="min-w-0">
                      <p className="truncate font-bold">{product.name}</p>
                      <p className="text-xs text-ink/45">
                        {product.sold} đã bán · {product.sku}
                      </p>
                    </div>
                    <p className="text-sm font-bold">{product.stock} còn hàng</p>
                  </div>
                ))
              ) : (
                <SmallEmpty label="Chưa có sản phẩm được bán" />
              )}
            </TableCard>

            <TableCard title="Khách hàng hàng đầu" icon={UsersRound}>
              {data.customers.topBySpending.length ? (
                data.customers.topBySpending.map((customer, index) => (
                  <div
                    key={customer.customerId}
                    className="grid grid-cols-[36px_1fr_auto] items-center gap-3 border-b border-ink/5 py-4 last:border-0"
                  >
                    <Rank value={index + 1} />
                    <div className="min-w-0">
                      <p className="truncate font-bold">{customer.name}</p>
                      <p className="truncate text-xs text-ink/45">
                        {customer.email} · {customer.orders} đơn hàng
                      </p>
                    </div>
                    <p className="text-sm font-bold">
                      {formatCurrency(customer.spending)}
                    </p>
                  </div>
                ))
              ) : (
                <SmallEmpty label="Chưa có đơn hàng của khách hàng" />
              )}
            </TableCard>
          </div>
        </>
      )}
    </section>
  );
}

function Metric({
  icon: Icon,
  label,
  value,
  detail,
}: {
  icon: typeof CircleDollarSign;
  label: string;
  value: string;
  detail: string;
}) {
  return (
    <div className="card p-6">
      <span className="inline-grid rounded-2xl bg-lime p-3">
        <Icon className="h-5 w-5" />
      </span>
      <p className="mt-5 text-sm font-semibold text-ink/50">{label}</p>
      <p className="mt-1 font-display text-2xl font-extrabold">{value}</p>
      <p className="mt-2 text-xs text-ink/45">{detail}</p>
    </div>
  );
}

function MiniMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-black/5 bg-white/70 px-5 py-4">
      <p className="text-xs font-semibold uppercase tracking-wider text-ink/40">
        {label}
      </p>
      <p className="mt-1 font-display text-lg font-extrabold">{value}</p>
    </div>
  );
}

function ChartCard({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="card min-w-0 p-5 sm:p-6">
      <h2 className="font-display text-xl font-bold">{title}</h2>
      <div className="mt-4 min-w-0">{children}</div>
    </div>
  );
}

function ChartEmpty({ label }: { label: string }) {
  return (
    <div className="grid h-[300px] place-items-center rounded-2xl bg-cream/55 text-center">
      <div>
        <BarChart3 className="mx-auto h-8 w-8 text-teal/50" />
        <p className="mt-3 text-sm font-semibold text-ink/45">{label}</p>
      </div>
    </div>
  );
}

function TableCard({
  title,
  icon: Icon,
  children,
}: {
  title: string;
  icon: typeof Package;
  children: React.ReactNode;
}) {
  return (
    <div className="card p-6">
      <div className="flex items-center gap-3">
        <Icon className="h-5 w-5 text-teal" />
        <h2 className="font-display text-xl font-bold">{title}</h2>
      </div>
      <div className="mt-3">{children}</div>
    </div>
  );
}

function Rank({ value }: { value: number }) {
  return (
    <span className="grid h-8 w-8 place-items-center rounded-full bg-cream text-xs font-bold">
      {value}
    </span>
  );
}

function SmallEmpty({ label }: { label: string }) {
  return (
    <div className="py-5">
      <EmptyState
        icon={TrendingUp}
        title={label}
        description="Báo cáo sẽ tự động hiển thị khi có dữ liệu."
      />
    </div>
  );
}

function compactMoney(value: number) {
  if (value >= 1_000_000_000) return `${value / 1_000_000_000} tỷ`;
  if (value >= 1_000_000) return `${value / 1_000_000} tr`;
  if (value >= 1_000) return `${value / 1_000} nghìn`;
  return `${value}`;
}
