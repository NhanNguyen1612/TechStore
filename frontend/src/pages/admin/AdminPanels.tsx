import {
  Check,
  Eye,
  EyeOff,
  PackagePlus,
  Plus,
  Save,
  Send,
  Trash2,
  UserPlus,
  XCircle,
} from "lucide-react";
import { useMemo, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { api } from "../../lib/api";
import {
  adminStatusLabel,
  orderStatusLabel,
  paymentStatusLabel,
  roleLabel,
} from "../../lib/constants";
import { formatCurrency, formatDate } from "../../lib/format";
import type {
  AdminConversation,
  AdminCoupon,
  AdminNotification,
  AdminOrder,
  AdminPage,
  AdminPayment,
  AdminProduct,
  AdminReview,
  AdminTaxonomy,
  AdminUser,
  CouponType,
  OrderStatus,
  PaymentStatus,
  Role,
} from "../../types/api";
import {
  AdminTable,
  buildQuery,
  ErrorBanner,
  FieldLabel,
  FormCard,
  PageControls,
  PanelHeader,
  runMutation,
  StatusBadge,
  TableShell,
  Toolbar,
  useAdminPage,
} from "./AdminCommon";

const roles: Role[] = ["ROLE_ADMIN", "ROLE_STAFF", "ROLE_CUSTOMER"];
const paymentStatuses: PaymentStatus[] = [
  "PENDING",
  "PAID",
  "FAILED",
  "CANCELLED",
  "REFUNDED",
];
const orderStatuses: OrderStatus[] = [
  "PENDING",
  "PENDING_PAYMENT",
  "PAID",
  "CONFIRMED",
  "SHIPPING",
  "DELIVERED",
  "COMPLETED",
  "CANCELLED",
];

export function UsersPanel() {
  const [search, setSearch] = useState("");
  const [role, setRole] = useState("");
  const [active, setActive] = useState("");
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
  const endpoint = useMemo(
    () =>
      `/api/admin/users?${buildQuery({
        search,
        role,
        active,
        page,
        size: 20,
        sort: "createdAt,desc",
      })}`,
    [active, page, role, search],
  );
  const result = useAdminPage<AdminUser>(endpoint);

  const mutate = (request: Promise<unknown>) =>
    runMutation(request, result.reload, result.setError);

  return (
    <>
      <PanelHeader
        eyebrow="Tài khoản và phân quyền"
        title="Người dùng"
        description="Tìm kiếm tài khoản, thay đổi vai trò, kiểm soát truy cập và tạo tài khoản nhân viên hoặc khách hàng."
        actions={
          <button className="btn-primary" onClick={() => setCreating(true)}>
            <UserPlus className="h-4 w-4" />
            Thêm người dùng
          </button>
        }
      />
      {creating && (
        <FormCard title="Tạo người dùng" onClose={() => setCreating(false)}>
          <CreateUserForm
            onCreated={() => {
              setCreating(false);
              result.reload();
            }}
            onError={result.setError}
          />
        </FormCard>
      )}
      <Toolbar>
        <FieldLabel label="Tìm kiếm">
          <input
            className="field"
            placeholder="Tên, email hoặc số điện thoại"
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(0);
            }}
          />
        </FieldLabel>
        <FieldLabel label="Vai trò">
          <select
            className="field"
            value={role}
            onChange={(event) => {
              setRole(event.target.value);
              setPage(0);
            }}
          >
            <option value="">Tất cả vai trò</option>
            {roles.map((value) => (
              <option key={value} value={value}>
                {roleLabel[value]}
              </option>
            ))}
          </select>
        </FieldLabel>
        <FieldLabel label="Trạng thái">
          <select
            className="field"
            value={active}
            onChange={(event) => {
              setActive(event.target.value);
              setPage(0);
            }}
          >
            <option value="">Tất cả trạng thái</option>
            <option value="true">Đang hoạt động</option>
            <option value="false">Đã vô hiệu hóa</option>
          </select>
        </FieldLabel>
      </Toolbar>
      <ErrorBanner error={result.error} onRetry={result.reload} />
      <TableShell
        loading={result.loading}
        empty={!result.data?.content.length}
      >
        <AdminTable
          headers={[
            "Người dùng",
            "Số điện thoại",
            "Vai trò",
            "Trạng thái",
            "Ngày tạo",
            "Thao tác",
          ]}
        >
          {result.data?.content.map((user) => (
            <tr key={user.id}>
              <td className="px-5 py-4">
                <p className="font-bold">{user.fullName}</p>
                <p className="mt-1 text-xs text-ink/45">{user.email}</p>
              </td>
              <td className="px-5 py-4 text-ink/60">{user.phone || "—"}</td>
              <td className="px-5 py-4">
                <select
                  className="rounded-xl border border-ink/10 bg-white px-3 py-2 text-xs font-bold"
                  value={user.role}
                  onChange={(event) =>
                    mutate(
                      api.put(`/api/admin/users/${user.id}/role`, {
                        role: event.target.value,
                      }),
                    )
                  }
                >
                  {roles.map((value) => (
                    <option key={value} value={value}>
                      {roleLabel[value]}
                    </option>
                  ))}
                </select>
              </td>
              <td className="px-5 py-4">
                <StatusBadge value={user.enabled ? "ACTIVE" : "DISABLED"} />
              </td>
              <td className="px-5 py-4 text-xs text-ink/45">
                {formatDate(user.createdAt)}
              </td>
              <td className="px-5 py-4">
                <div className="flex gap-2">
                  <button
                    className="btn-secondary !px-3 !py-2 text-xs"
                    onClick={() =>
                      mutate(
                        api.put(`/api/admin/users/${user.id}/status`, {
                          active: !user.enabled,
                        }),
                      )
                    }
                  >
                    {user.enabled ? "Vô hiệu hóa" : "Kích hoạt"}
                  </button>
                  <DeleteButton
                    label="người dùng"
                    onDelete={() =>
                      mutate(api.delete(`/api/admin/users/${user.id}`))
                    }
                  />
                </div>
              </td>
            </tr>
          ))}
        </AdminTable>
      </TableShell>
      {result.data && (
        <PageControls
          {...result.data}
          onChange={(value) => setPage(value)}
        />
      )}
    </>
  );
}

function CreateUserForm({
  onCreated,
  onError,
}: {
  onCreated: () => void;
  onError: (value: string) => void;
}) {
  const [form, setForm] = useState({
    email: "",
    password: "",
    fullName: "",
    phone: "",
    role: "ROLE_STAFF" as Role,
    enabled: true,
  });
  const submit = (event: FormEvent) => {
    event.preventDefault();
    runMutation(api.post("/api/admin/users", form), onCreated, onError);
  };
  return (
    <form onSubmit={submit} className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      <Input
        label="Họ và tên"
        value={form.fullName}
        onChange={(value) => setForm({ ...form, fullName: value })}
        required
      />
      <Input
        label="Email"
        type="email"
        value={form.email}
        onChange={(value) => setForm({ ...form, email: value })}
        required
      />
      <Input
        label="Số điện thoại"
        value={form.phone}
        onChange={(value) => setForm({ ...form, phone: value })}
      />
      <Input
        label="Mật khẩu"
        type="password"
        value={form.password}
        onChange={(value) => setForm({ ...form, password: value })}
        required
      />
      <FieldLabel label="Vai trò">
        <select
          className="field"
          value={form.role}
          onChange={(event) =>
            setForm({ ...form, role: event.target.value as Role })
          }
        >
          {roles.map((value) => (
            <option key={value} value={value}>
              {roleLabel[value]}
            </option>
          ))}
        </select>
      </FieldLabel>
      <div className="flex items-end">
        <button className="btn-primary w-full" type="submit">
          <Plus className="h-4 w-4" />
          Tạo người dùng
        </button>
      </div>
    </form>
  );
}

export function ProductsPanel() {
  const [search, setSearch] = useState("");
  const [active, setActive] = useState("");
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
  const [stockDrafts, setStockDrafts] = useState<Record<number, string>>({});
  const endpoint = useMemo(
    () =>
      `/api/admin/products?${buildQuery({
        search,
        active,
        page,
        size: 20,
        sort: "createdAt,desc",
      })}`,
    [active, page, search],
  );
  const result = useAdminPage<AdminProduct>(endpoint);
  const categories = useAdminPage<AdminTaxonomy>(
    "/api/admin/categories?page=0&size=100&sort=name,asc",
  );
  const brands = useAdminPage<AdminTaxonomy>(
    "/api/admin/brands?page=0&size=100&sort=name,asc",
  );
  const mutate = (request: Promise<unknown>) =>
    runMutation(request, result.reload, result.setError);

  return (
    <>
      <PanelHeader
        eyebrow="Quản lý danh mục sản phẩm"
        title="Sản phẩm"
        description="Tạo sản phẩm kèm hình ảnh, quản lý hiển thị và điều chỉnh tồn kho."
        actions={
          <button className="btn-primary" onClick={() => setCreating(true)}>
            <PackagePlus className="h-4 w-4" />
            Thêm sản phẩm
          </button>
        }
      />
      {creating && (
        <FormCard title="Tạo sản phẩm" onClose={() => setCreating(false)}>
          <CreateProductForm
            categories={categories.data?.content ?? []}
            brands={brands.data?.content ?? []}
            onCreated={() => {
              setCreating(false);
              result.reload();
            }}
            onError={result.setError}
          />
        </FormCard>
      )}
      <Toolbar>
        <FieldLabel label="Tìm kiếm">
          <input
            className="field"
            placeholder="Tên sản phẩm hoặc SKU"
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(0);
            }}
          />
        </FieldLabel>
        <FieldLabel label="Trạng thái">
          <select
            className="field"
            value={active}
            onChange={(event) => {
              setActive(event.target.value);
              setPage(0);
            }}
          >
            <option value="">Tất cả sản phẩm</option>
            <option value="true">Đang hoạt động</option>
            <option value="false">Ngừng hoạt động</option>
          </select>
        </FieldLabel>
      </Toolbar>
      <ErrorBanner error={result.error} onRetry={result.reload} />
      <TableShell
        loading={result.loading}
        empty={!result.data?.content.length}
      >
        <AdminTable
          headers={[
            "Sản phẩm",
            "Danh mục",
            "Giá bán",
            "Tồn kho",
            "Trạng thái",
            "Thao tác",
          ]}
        >
          {result.data?.content.map((product) => (
            <tr key={product.id}>
              <td className="px-5 py-4">
                <div className="flex items-center gap-3">
                  <div className="h-12 w-12 overflow-hidden rounded-2xl bg-cream">
                    {product.thumbnailUrl && (
                      <img
                        src={product.thumbnailUrl}
                        alt=""
                        className="h-full w-full object-cover"
                      />
                    )}
                  </div>
                  <div>
                    <p className="font-bold">{product.name}</p>
                    <p className="mt-1 text-xs text-ink/45">
                      {product.sku} · {product.soldCount} đã bán
                    </p>
                  </div>
                </div>
              </td>
              <td className="px-5 py-4">
                <p className="font-semibold">{product.categoryName}</p>
                <p className="text-xs text-ink/45">{product.brandName}</p>
              </td>
              <td className="px-5 py-4 font-bold">
                {formatCurrency(product.price)}
              </td>
              <td className="px-5 py-4">
                <div className="flex items-center gap-2">
                  <input
                    aria-label={`Tồn kho của ${product.name}`}
                    className="w-20 rounded-xl border border-ink/10 px-3 py-2"
                    type="number"
                    min="0"
                    value={stockDrafts[product.id] ?? product.stockQuantity}
                    onChange={(event) =>
                      setStockDrafts({
                        ...stockDrafts,
                        [product.id]: event.target.value,
                      })
                    }
                  />
                  <button
                    aria-label={`Lưu tồn kho của ${product.name}`}
                    className="rounded-xl bg-ink p-2 text-white"
                    onClick={() =>
                      mutate(
                        api.put(`/api/admin/products/${product.id}/stock`, {
                          stockQuantity: Number(
                            stockDrafts[product.id] ?? product.stockQuantity,
                          ),
                        }),
                      )
                    }
                  >
                    <Save className="h-4 w-4" />
                  </button>
                </div>
              </td>
              <td className="px-5 py-4">
                <StatusBadge value={product.active ? "ACTIVE" : "DISABLED"} />
              </td>
              <td className="px-5 py-4">
                <div className="flex gap-2">
                  <button
                    className="btn-secondary !px-3 !py-2 text-xs"
                    onClick={() =>
                      mutate(
                        api.put(`/api/admin/products/${product.id}/status`, {
                          active: !product.active,
                        }),
                      )
                    }
                  >
                    {product.active ? "Ẩn" : "Hiển thị"}
                  </button>
                  <DeleteButton
                    label="sản phẩm"
                    onDelete={() =>
                      mutate(api.delete(`/api/admin/products/${product.id}`))
                    }
                  />
                </div>
              </td>
            </tr>
          ))}
        </AdminTable>
      </TableShell>
      {result.data && (
        <PageControls {...result.data} onChange={(value) => setPage(value)} />
      )}
    </>
  );
}

function CreateProductForm({
  categories,
  brands,
  onCreated,
  onError,
}: {
  categories: AdminTaxonomy[];
  brands: AdminTaxonomy[];
  onCreated: () => void;
  onError: (value: string) => void;
}) {
  const [form, setForm] = useState({
    name: "",
    sku: "",
    description: "",
    price: "",
    stockQuantity: "0",
    categoryId: "",
    brandId: "",
  });
  const [images, setImages] = useState<File[]>([]);
  const submit = (event: FormEvent) => {
    event.preventDefault();
    const payload = {
      ...form,
      price: Number(form.price),
      stockQuantity: Number(form.stockQuantity),
      categoryId: Number(form.categoryId),
      brandId: Number(form.brandId),
    };
    const data = new FormData();
    data.append(
      "request",
      new Blob([JSON.stringify(payload)], { type: "application/json" }),
      "request.json",
    );
    images.forEach((image) => data.append("images", image));
    runMutation(api.post("/api/admin/products", data), onCreated, onError);
  };
  return (
    <form onSubmit={submit} className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      <Input
        label="Tên sản phẩm"
        value={form.name}
        onChange={(value) => setForm({ ...form, name: value })}
        required
      />
      <Input
        label="SKU"
        value={form.sku}
        onChange={(value) => setForm({ ...form, sku: value })}
        required
      />
      <Input
        label="Giá bán"
        type="number"
        value={form.price}
        onChange={(value) => setForm({ ...form, price: value })}
        required
      />
      <Input
        label="Tồn kho ban đầu"
        type="number"
        value={form.stockQuantity}
        onChange={(value) => setForm({ ...form, stockQuantity: value })}
        required
      />
      <FieldLabel label="Danh mục">
        <select
          className="field"
          required
          value={form.categoryId}
          onChange={(event) =>
            setForm({ ...form, categoryId: event.target.value })
          }
        >
          <option value="">Chọn danh mục</option>
          {categories.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>
      </FieldLabel>
      <FieldLabel label="Thương hiệu">
        <select
          className="field"
          required
          value={form.brandId}
          onChange={(event) =>
            setForm({ ...form, brandId: event.target.value })
          }
        >
          <option value="">Chọn thương hiệu</option>
          {brands.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>
      </FieldLabel>
      <label className="md:col-span-2 xl:col-span-3">
        <span className="label">Mô tả</span>
        <textarea
          className="field min-h-24"
          value={form.description}
          onChange={(event) =>
            setForm({ ...form, description: event.target.value })
          }
        />
      </label>
      <FieldLabel label="Hình ảnh">
        <input
          className="field"
          type="file"
          multiple
          accept="image/*"
          onChange={(event) =>
            setImages(Array.from(event.target.files ?? []))
          }
        />
      </FieldLabel>
      <div className="flex items-end md:col-span-2 xl:col-span-2">
        <button className="btn-primary w-full" type="submit">
          <PackagePlus className="h-4 w-4" />
          Tạo sản phẩm
        </button>
      </div>
    </form>
  );
}

export function CatalogPanel() {
  return (
    <>
      <PanelHeader
        eyebrow="Phân loại sản phẩm"
        title="Danh mục và thương hiệu"
        description="Tạo và xóa các nhóm phân loại. Không thể xóa khi vẫn còn sản phẩm đang sử dụng."
      />
      <div className="mt-6 grid gap-6 xl:grid-cols-2">
        <TaxonomyCard kind="categories" title="Danh mục" />
        <TaxonomyCard kind="brands" title="Thương hiệu" />
      </div>
    </>
  );
}

function TaxonomyCard({
  kind,
  title,
}: {
  kind: "categories" | "brands";
  title: string;
}) {
  const singular = kind === "categories" ? "danh mục" : "thương hiệu";
  const [search, setSearch] = useState("");
  const [form, setForm] = useState({ name: "", description: "" });
  const endpoint = `/api/admin/${kind}?${buildQuery({
    search,
    page: 0,
    size: 100,
    sort: "name,asc",
  })}`;
  const result = useAdminPage<AdminTaxonomy>(endpoint);
  const submit = (event: FormEvent) => {
    event.preventDefault();
    runMutation(
      api.post(`/api/admin/${kind}`, form),
      () => {
        setForm({ name: "", description: "" });
        result.reload();
      },
      result.setError,
    );
  };
  return (
    <div className="card p-5">
      <div className="flex items-center justify-between gap-3">
        <h3 className="font-display text-xl font-extrabold">{title}</h3>
        <span className="rounded-full bg-cream px-3 py-1 text-xs font-bold">
          {result.data?.totalElements ?? 0}
        </span>
      </div>
      <form onSubmit={submit} className="mt-4 grid gap-3">
        <input
          className="field"
          placeholder={`Tên ${singular}`}
          value={form.name}
          required
          onChange={(event) => setForm({ ...form, name: event.target.value })}
        />
        <input
          className="field"
          placeholder="Mô tả"
          value={form.description}
          onChange={(event) =>
            setForm({ ...form, description: event.target.value })
          }
        />
        <button className="btn-primary" type="submit">
          <Plus className="h-4 w-4" />
          Thêm {singular}
        </button>
      </form>
      <input
        className="field mt-5"
        placeholder={`Tìm kiếm ${title.toLowerCase()}`}
        value={search}
        onChange={(event) => setSearch(event.target.value)}
      />
      <ErrorBanner error={result.error} onRetry={result.reload} />
      <div className="mt-3 max-h-[430px] divide-y divide-ink/5 overflow-auto">
        {result.data?.content.map((item) => (
          <div key={item.id} className="flex items-center gap-3 py-3">
            <div className="min-w-0 flex-1">
              <p className="truncate font-bold">{item.name}</p>
              <p className="truncate text-xs text-ink/45">{item.slug}</p>
            </div>
            <DeleteButton
              label={singular}
              onDelete={() =>
                runMutation(
                  api.delete(`/api/admin/${kind}/${item.id}`),
                  result.reload,
                  result.setError,
                )
              }
            />
          </div>
        ))}
      </div>
    </div>
  );
}

export function OrderPanel() {
  const [status, setStatus] = useState("");
  const [paymentMethod, setPaymentMethod] = useState("");
  const [page, setPage] = useState(0);
  const endpoint = useMemo(
    () =>
      `/api/admin/orders?${buildQuery({
        status,
        paymentMethod,
        page,
        size: 20,
        sort: "createdAt,desc",
      })}`,
    [page, paymentMethod, status],
  );
  const result = useAdminPage<AdminOrder>(endpoint);
  const transition = (order: AdminOrder, action: string) =>
    runMutation(
      api.put(`/api/admin/orders/${order.id}/${action}`),
      result.reload,
      result.setError,
    );
  return (
    <>
      <PanelHeader
        eyebrow="Xử lý đơn hàng"
        title="Đơn hàng"
        description="Lọc đơn hàng và cập nhật theo đúng quy trình xử lý hợp lệ."
      />
      <Toolbar>
        <FieldLabel label="Trạng thái đơn hàng">
          <select
            className="field"
            value={status}
            onChange={(event) => {
              setStatus(event.target.value);
              setPage(0);
            }}
          >
            <option value="">Tất cả trạng thái</option>
            {orderStatuses.map((value) => (
              <option key={value} value={value}>
                {orderStatusLabel[value]}
              </option>
            ))}
          </select>
        </FieldLabel>
        <FieldLabel label="Phương thức thanh toán">
          <select
            className="field"
            value={paymentMethod}
            onChange={(event) => {
              setPaymentMethod(event.target.value);
              setPage(0);
            }}
          >
            <option value="">Tất cả phương thức</option>
            <option value="MOMO">MoMo</option>
            <option value="COD">COD</option>
          </select>
        </FieldLabel>
      </Toolbar>
      <ErrorBanner error={result.error} onRetry={result.reload} />
      <TableShell
        loading={result.loading}
        empty={!result.data?.content.length}
      >
        <AdminTable
          headers={[
            "Đơn hàng",
            "Khách hàng",
            "Thanh toán",
            "Tổng cộng",
            "Trạng thái",
            "Thao tác",
          ]}
        >
          {result.data?.content.map((order) => (
            <tr key={order.id}>
              <td className="px-5 py-4">
                <p className="font-bold">{order.orderCode}</p>
                <p className="text-xs text-ink/45">
                  {order.totalQuantity} sản phẩm · {formatDate(order.createdAt)}
                </p>
              </td>
              <td className="px-5 py-4">
                <p className="font-semibold">{order.customerName}</p>
                <p className="text-xs text-ink/45">{order.customerEmail}</p>
              </td>
              <td className="px-5 py-4">
                <StatusBadge value={order.paymentMethod} tone="blue" />
              </td>
              <td className="px-5 py-4 font-bold">
                {formatCurrency(order.totalAmount)}
              </td>
              <td className="px-5 py-4">
                <StatusBadge value={order.status} />
              </td>
              <td className="px-5 py-4">
                <OrderActions order={order} onTransition={transition} />
              </td>
            </tr>
          ))}
        </AdminTable>
      </TableShell>
      {result.data && (
        <PageControls {...result.data} onChange={(value) => setPage(value)} />
      )}
    </>
  );
}

function OrderActions({
  order,
  onTransition,
}: {
  order: AdminOrder;
  onTransition: (order: AdminOrder, action: string) => void;
}) {
  const next: Partial<Record<OrderStatus, { action: string; label: string }>> = {
    PENDING: { action: "confirm", label: "Xác nhận" },
    PAID: { action: "confirm", label: "Xác nhận" },
    CONFIRMED: { action: "shipping", label: "Giao hàng" },
    SHIPPING: { action: "delivered", label: "Đã giao" },
    DELIVERED: { action: "complete", label: "Hoàn thành" },
  };
  const nextAction = next[order.status];
  const cancellable = [
    "PENDING",
    "PENDING_PAYMENT",
    "PAID",
    "CONFIRMED",
  ].includes(order.status);
  return (
    <div className="flex gap-2">
      <Link
        to={`/orders/${order.id}`}
        className="rounded-full bg-cream px-3 py-2 text-xs font-bold text-teal"
        title={`Xem hành trình đơn ${order.orderCode}`}
        aria-label={`Xem hành trình đơn ${order.orderCode}`}
      >
        <Eye className="h-4 w-4" />
      </Link>
      {nextAction && (
        <button
          className="btn-primary !px-3 !py-2 text-xs"
          onClick={() => onTransition(order, nextAction.action)}
        >
          {nextAction.label}
        </button>
      )}
      {cancellable && (
        <button
          className="rounded-full bg-red-50 px-3 py-2 text-xs font-bold text-red-700"
          onClick={() => {
            if (window.confirm(`Bạn có chắc muốn hủy đơn ${order.orderCode}?`)) {
              onTransition(order, "cancel");
            }
          }}
        >
          Hủy
        </button>
      )}
    </div>
  );
}

export function CheckoutPanel() {
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const endpoint = useMemo(
    () =>
      `/api/admin/payments?${buildQuery({
        status,
        page,
        size: 20,
        sort: "createdAt,desc",
      })}`,
    [page, status],
  );
  const result = useAdminPage<AdminPayment>(endpoint);
  return (
    <>
      <PanelHeader
        eyebrow="Quản lý thanh toán"
        title="Thanh toán"
        description="Kiểm tra mã giao dịch MoMo, trạng thái, callback và lịch sử giao dịch."
      />
      <Toolbar>
        <FieldLabel label="Trạng thái thanh toán">
          <select
            className="field"
            value={status}
            onChange={(event) => {
              setStatus(event.target.value);
              setPage(0);
            }}
          >
            <option value="">Tất cả trạng thái</option>
            {paymentStatuses.map((value) => (
              <option key={value} value={value}>
                {paymentStatusLabel[value]}
              </option>
            ))}
          </select>
        </FieldLabel>
      </Toolbar>
      <ErrorBanner error={result.error} onRetry={result.reload} />
      <TableShell
        loading={result.loading}
        empty={!result.data?.content.length}
      >
        <AdminTable
          headers={[
            "Đơn hàng",
            "Nhà cung cấp",
            "Số tiền",
            "Trạng thái",
            "Mã MoMo",
            "Lịch sử",
          ]}
        >
          {result.data?.content.map((payment) => (
            <tr key={payment.id}>
              <td className="px-5 py-4">
                <p className="font-bold">{payment.orderCode}</p>
                <p className="text-xs text-ink/45">Thanh toán #{payment.id}</p>
              </td>
              <td className="px-5 py-4 font-semibold">{payment.provider}</td>
              <td className="px-5 py-4 font-bold">
                {formatCurrency(payment.amount)}
              </td>
              <td className="px-5 py-4">
                <select
                  className="rounded-xl border border-ink/10 px-3 py-2 text-xs font-bold"
                  value={payment.status}
                  onChange={(event) =>
                    runMutation(
                      api.put(`/api/admin/payments/${payment.id}/status`, {
                        status: event.target.value,
                      }),
                      result.reload,
                      result.setError,
                    )
                  }
                >
                  {paymentStatuses.map((value) => (
                    <option key={value} value={value}>
                      {paymentStatusLabel[value]}
                    </option>
                  ))}
                </select>
              </td>
              <td className="max-w-56 px-5 py-4 text-xs text-ink/55">
                <p className="truncate">{payment.requestId || "Không có mã yêu cầu"}</p>
                <p className="truncate">
                  {payment.transactionId || payment.momoOrderId || "Không có giao dịch"}
                </p>
              </td>
              <td className="px-5 py-4 text-xs">
                {payment.transactions.length} giao dịch ·{" "}
                {payment.callbacks.length} phản hồi
              </td>
            </tr>
          ))}
        </AdminTable>
      </TableShell>
      {result.data && (
        <PageControls {...result.data} onChange={(value) => setPage(value)} />
      )}
    </>
  );
}

export function CouponsPanel() {
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
  const endpoint = useMemo(
    () =>
      `/api/admin/coupons?${buildQuery({
        search,
        page,
        size: 20,
        sort: "createdAt,desc",
      })}`,
    [page, search],
  );
  const result = useAdminPage<AdminCoupon>(endpoint);
  return (
    <>
      <PanelHeader
        eyebrow="Khuyến mãi"
        title="Mã giảm giá"
        description="Tạo ưu đãi theo phần trăm hoặc số tiền cố định, kèm thời hạn, giá trị đơn tối thiểu và giới hạn sử dụng."
        actions={
          <button className="btn-primary" onClick={() => setCreating(true)}>
            <Plus className="h-4 w-4" />
            Thêm mã giảm giá
          </button>
        }
      />
      {creating && (
        <FormCard title="Tạo mã giảm giá" onClose={() => setCreating(false)}>
          <CreateCouponForm
            onCreated={() => {
              setCreating(false);
              result.reload();
            }}
            onError={result.setError}
          />
        </FormCard>
      )}
      <Toolbar>
        <FieldLabel label="Tìm kiếm">
          <input
            className="field"
            placeholder="Mã hoặc tên khuyến mãi"
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(0);
            }}
          />
        </FieldLabel>
      </Toolbar>
      <ErrorBanner error={result.error} onRetry={result.reload} />
      <TableShell
        loading={result.loading}
        empty={!result.data?.content.length}
      >
        <AdminTable
          headers={[
            "Mã giảm giá",
            "Mức giảm",
            "Đơn tối thiểu",
            "Lượt dùng",
            "Thời hạn",
            "Thao tác",
          ]}
        >
          {result.data?.content.map((coupon) => (
            <tr key={coupon.id}>
              <td className="px-5 py-4">
                <p className="font-bold">{coupon.code}</p>
                <p className="text-xs text-ink/45">{coupon.name}</p>
              </td>
              <td className="px-5 py-4">
                <p className="font-bold">
                  {coupon.type === "PERCENTAGE"
                    ? `${coupon.value}%`
                    : formatCurrency(coupon.value)}
                </p>
                <StatusBadge
                  value={coupon.active ? "ACTIVE" : "DISABLED"}
                />
              </td>
              <td className="px-5 py-4">
                {formatCurrency(coupon.minimumOrderAmount)}
              </td>
              <td className="px-5 py-4">
                {coupon.usedCount} / {coupon.usageLimit ?? "∞"}
              </td>
              <td className="px-5 py-4 text-xs text-ink/55">
                <p>{formatDate(coupon.startsAt)}</p>
                <p>{formatDate(coupon.endsAt)}</p>
              </td>
              <td className="px-5 py-4">
                <div className="flex gap-2">
                  <button
                    className="btn-secondary !px-3 !py-2 text-xs"
                    onClick={() =>
                      runMutation(
                        api.put(`/api/admin/coupons/${coupon.id}/status`, {
                          active: !coupon.active,
                        }),
                        result.reload,
                        result.setError,
                      )
                    }
                  >
                    {coupon.active ? "Vô hiệu hóa" : "Kích hoạt"}
                  </button>
                  <DeleteButton
                    label="mã giảm giá"
                    onDelete={() =>
                      runMutation(
                        api.delete(`/api/admin/coupons/${coupon.id}`),
                        result.reload,
                        result.setError,
                      )
                    }
                  />
                </div>
              </td>
            </tr>
          ))}
        </AdminTable>
      </TableShell>
      {result.data && (
        <PageControls {...result.data} onChange={(value) => setPage(value)} />
      )}
    </>
  );
}

function CreateCouponForm({
  onCreated,
  onError,
}: {
  onCreated: () => void;
  onError: (value: string) => void;
}) {
  const tomorrow = new Date(Date.now() + 86_400_000)
    .toISOString()
    .slice(0, 16);
  const nextMonth = new Date(Date.now() + 30 * 86_400_000)
    .toISOString()
    .slice(0, 16);
  const [form, setForm] = useState({
    code: "",
    name: "",
    type: "PERCENTAGE" as CouponType,
    value: "10",
    minimumOrderAmount: "0",
    usageLimit: "100",
    startsAt: tomorrow,
    endsAt: nextMonth,
  });
  const submit = (event: FormEvent) => {
    event.preventDefault();
    runMutation(
      api.post("/api/admin/coupons", {
        ...form,
        value: Number(form.value),
        minimumOrderAmount: Number(form.minimumOrderAmount),
        usageLimit: form.usageLimit ? Number(form.usageLimit) : null,
        startsAt: new Date(form.startsAt).toISOString(),
        endsAt: new Date(form.endsAt).toISOString(),
      }),
      onCreated,
      onError,
    );
  };
  return (
    <form onSubmit={submit} className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <Input
        label="Mã"
        value={form.code}
        onChange={(value) => setForm({ ...form, code: value.toUpperCase() })}
        required
      />
      <Input
        label="Tên"
        value={form.name}
        onChange={(value) => setForm({ ...form, name: value })}
        required
      />
      <FieldLabel label="Loại giảm giá">
        <select
          className="field"
          value={form.type}
          onChange={(event) =>
            setForm({ ...form, type: event.target.value as CouponType })
          }
        >
          <option value="PERCENTAGE">Phần trăm</option>
          <option value="FIXED_AMOUNT">Số tiền cố định</option>
        </select>
      </FieldLabel>
      <Input
        label="Giá trị"
        type="number"
        value={form.value}
        onChange={(value) => setForm({ ...form, value })}
        required
      />
      <Input
        label="Giá trị đơn tối thiểu"
        type="number"
        value={form.minimumOrderAmount}
        onChange={(value) => setForm({ ...form, minimumOrderAmount: value })}
        required
      />
      <Input
        label="Giới hạn lượt dùng"
        type="number"
        value={form.usageLimit}
        onChange={(value) => setForm({ ...form, usageLimit: value })}
      />
      <Input
        label="Bắt đầu lúc"
        type="datetime-local"
        value={form.startsAt}
        onChange={(value) => setForm({ ...form, startsAt: value })}
        required
      />
      <Input
        label="Kết thúc lúc"
        type="datetime-local"
        value={form.endsAt}
        onChange={(value) => setForm({ ...form, endsAt: value })}
        required
      />
      <button className="btn-primary md:col-span-2 xl:col-span-4" type="submit">
        Tạo mã giảm giá
      </button>
    </form>
  );
}

export function ReviewsPanel() {
  const [status, setStatus] = useState("");
  const [rating, setRating] = useState("");
  const [page, setPage] = useState(0);
  const endpoint = useMemo(
    () =>
      `/api/admin/reviews?${buildQuery({
        status,
        rating,
        page,
        size: 20,
        sort: "createdAt,desc",
      })}`,
    [page, rating, status],
  );
  const result = useAdminPage<AdminReview>(endpoint);
  const action = (id: number, verb: "approve" | "hide") =>
    runMutation(
      api.put(`/api/admin/reviews/${id}/${verb}`),
      result.reload,
      result.setError,
    );
  return (
    <>
      <PanelHeader
        eyebrow="Kiểm duyệt nội dung"
        title="Đánh giá"
        description="Lọc theo số sao hoặc trạng thái kiểm duyệt, sau đó duyệt, ẩn hoặc xóa đánh giá."
      />
      <Toolbar>
        <FieldLabel label="Trạng thái">
          <select
            className="field"
            value={status}
            onChange={(event) => setStatus(event.target.value)}
          >
            <option value="">Tất cả trạng thái</option>
            {["PENDING", "APPROVED", "HIDDEN"].map((value) => (
              <option key={value} value={value}>
                {adminStatusLabel[value]}
              </option>
            ))}
          </select>
        </FieldLabel>
        <FieldLabel label="Số sao">
          <select
            className="field"
            value={rating}
            onChange={(event) => setRating(event.target.value)}
          >
            <option value="">Tất cả mức sao</option>
            {[5, 4, 3, 2, 1].map((value) => (
              <option key={value} value={value}>
                {value} sao
              </option>
            ))}
          </select>
        </FieldLabel>
      </Toolbar>
      <ErrorBanner error={result.error} onRetry={result.reload} />
      <TableShell
        loading={result.loading}
        empty={!result.data?.content.length}
      >
        <AdminTable
          headers={[
            "Đánh giá",
            "Sản phẩm",
            "Số sao",
            "Trạng thái",
            "Ngày tạo",
            "Thao tác",
          ]}
        >
          {result.data?.content.map((review) => (
            <tr key={review.id}>
              <td className="max-w-md px-5 py-4">
                <p className="font-bold">{review.userName}</p>
                <p className="mt-1 line-clamp-2 text-xs text-ink/55">
                  {review.comment}
                </p>
              </td>
              <td className="px-5 py-4 font-semibold">{review.productName}</td>
              <td className="px-5 py-4 font-bold text-amber-600">
                {"★".repeat(review.rating)}
              </td>
              <td className="px-5 py-4">
                <StatusBadge value={review.status} />
              </td>
              <td className="px-5 py-4 text-xs text-ink/45">
                {formatDate(review.createdAt)}
              </td>
              <td className="px-5 py-4">
                <div className="flex gap-2">
                  {review.status !== "APPROVED" && (
                    <IconButton
                      label="Duyệt đánh giá"
                      icon={Check}
                      onClick={() => action(review.id, "approve")}
                    />
                  )}
                  {review.status !== "HIDDEN" && (
                    <IconButton
                      label="Ẩn đánh giá"
                      icon={EyeOff}
                      onClick={() => action(review.id, "hide")}
                    />
                  )}
                  <DeleteButton
                    label="đánh giá"
                    onDelete={() =>
                      runMutation(
                        api.delete(`/api/admin/reviews/${review.id}`),
                        result.reload,
                        result.setError,
                      )
                    }
                  />
                </div>
              </td>
            </tr>
          ))}
        </AdminTable>
      </TableShell>
      {result.data && (
        <PageControls {...result.data} onChange={(value) => setPage(value)} />
      )}
    </>
  );
}

export function ConversationsPanel() {
  const [closed, setClosed] = useState("");
  const [page, setPage] = useState(0);
  const endpoint = useMemo(
    () =>
      `/api/admin/conversations?${buildQuery({
        closed,
        page,
        size: 20,
        sort: "createdAt,desc",
      })}`,
    [closed, page],
  );
  const result = useAdminPage<AdminConversation>(endpoint);
  const staff = useAdminPage<AdminUser>(
    "/api/admin/users?role=ROLE_STAFF&active=true&page=0&size=100",
  );
  return (
    <>
      <PanelHeader
        eyebrow="Hỗ trợ khách hàng"
        title="Cuộc trò chuyện"
        description="Xem cuộc trò chuyện hỗ trợ, phân công nhân viên và đóng các trường hợp đã hoàn tất."
      />
      <Toolbar>
        <FieldLabel label="Trạng thái cuộc trò chuyện">
          <select
            className="field"
            value={closed}
            onChange={(event) => setClosed(event.target.value)}
          >
            <option value="">Tất cả cuộc trò chuyện</option>
            <option value="false">Đang mở</option>
            <option value="true">Đã đóng</option>
          </select>
        </FieldLabel>
      </Toolbar>
      <ErrorBanner error={result.error} onRetry={result.reload} />
      <TableShell
        loading={result.loading}
        empty={!result.data?.content.length}
      >
        <AdminTable
          headers={[
            "Người tham gia",
            "Nhân viên phụ trách",
            "Tin nhắn cuối",
            "Trạng thái",
            "Thao tác",
          ]}
        >
          {result.data?.content.map((conversation) => (
            <tr key={conversation.id}>
              <td className="px-5 py-4">
                <p className="font-bold">
                  {conversation.participantOne.fullName}
                </p>
                <p className="text-xs text-ink/45">
                  {conversation.participantTwo.fullName}
                </p>
              </td>
              <td className="px-5 py-4">
                <select
                  className="rounded-xl border border-ink/10 px-3 py-2 text-xs"
                  value={conversation.assignedStaff?.id ?? ""}
                  disabled={conversation.closed}
                  onChange={(event) =>
                    runMutation(
                      api.put(
                        `/api/admin/conversations/${conversation.id}/assign-staff`,
                        { staffId: Number(event.target.value) },
                      ),
                      result.reload,
                      result.setError,
                    )
                  }
                >
                  <option value="">Chưa phân công</option>
                  {staff.data?.content.map((user) => (
                    <option key={user.id} value={user.id}>
                      {user.fullName}
                    </option>
                  ))}
                </select>
              </td>
              <td className="px-5 py-4 text-xs text-ink/55">
                {formatDate(conversation.lastMessageAt)}
              </td>
              <td className="px-5 py-4">
                <StatusBadge
                  value={conversation.closed ? "CLOSED" : "ACTIVE"}
                />
              </td>
              <td className="px-5 py-4">
                {!conversation.closed && (
                  <button
                    className="btn-secondary !px-3 !py-2 text-xs"
                    onClick={() =>
                      runMutation(
                        api.put(
                          `/api/admin/conversations/${conversation.id}/close`,
                        ),
                        result.reload,
                        result.setError,
                      )
                    }
                  >
                    Đóng cuộc trò chuyện
                  </button>
                )}
              </td>
            </tr>
          ))}
        </AdminTable>
      </TableShell>
      {result.data && (
        <PageControls {...result.data} onChange={(value) => setPage(value)} />
      )}
    </>
  );
}

export function NotificationsPanel() {
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
  const result = useAdminPage<AdminNotification>(
    `/api/admin/notifications?page=${page}&size=20`,
  );
  return (
    <>
      <PanelHeader
        eyebrow="Truyền thông"
        title="Thông báo"
        description="Gửi thông báo tới mọi người dùng hoặc một vai trò cụ thể và lưu lại lịch sử."
        actions={
          <button className="btn-primary" onClick={() => setCreating(true)}>
            <Send className="h-4 w-4" />
            Tạo thông báo
          </button>
        }
      />
      {creating && (
        <FormCard title="Tạo thông báo" onClose={() => setCreating(false)}>
          <CreateNotificationForm
            onCreated={() => {
              setCreating(false);
              result.reload();
            }}
            onError={result.setError}
          />
        </FormCard>
      )}
      <ErrorBanner error={result.error} onRetry={result.reload} />
      <TableShell
        loading={result.loading}
        empty={!result.data?.content.length}
      >
        <AdminTable
          headers={[
            "Thông báo",
            "Đối tượng nhận",
            "Người tạo",
            "Ngày tạo",
            "Thao tác",
          ]}
        >
          {result.data?.content.map((notification) => (
            <tr key={notification.id}>
              <td className="max-w-lg px-5 py-4">
                <p className="font-bold">{notification.title}</p>
                <p className="mt-1 line-clamp-2 text-xs text-ink/55">
                  {notification.content}
                </p>
              </td>
              <td className="px-5 py-4">
                <StatusBadge
                  value={
                    notification.targetRole
                      ? roleLabel[notification.targetRole]
                      : "TẤT CẢ NGƯỜI DÙNG"
                  }
                  tone="blue"
                />
              </td>
              <td className="px-5 py-4 font-semibold">
                {notification.createdByName}
              </td>
              <td className="px-5 py-4 text-xs text-ink/45">
                {formatDate(notification.createdAt)}
              </td>
              <td className="px-5 py-4">
                <DeleteButton
                  label="thông báo"
                  onDelete={() =>
                    runMutation(
                      api.delete(`/api/admin/notifications/${notification.id}`),
                      result.reload,
                      result.setError,
                    )
                  }
                />
              </td>
            </tr>
          ))}
        </AdminTable>
      </TableShell>
      {result.data && (
        <PageControls {...result.data} onChange={(value) => setPage(value)} />
      )}
    </>
  );
}

function CreateNotificationForm({
  onCreated,
  onError,
}: {
  onCreated: () => void;
  onError: (value: string) => void;
}) {
  const [form, setForm] = useState({
    title: "",
    content: "",
    targetRole: "",
  });
  const submit = (event: FormEvent) => {
    event.preventDefault();
    runMutation(
      api.post("/api/admin/notifications", {
        ...form,
        targetRole: form.targetRole || null,
      }),
      onCreated,
      onError,
    );
  };
  return (
    <form onSubmit={submit} className="grid gap-4 md:grid-cols-2">
      <Input
        label="Tiêu đề"
        value={form.title}
        onChange={(value) => setForm({ ...form, title: value })}
        required
      />
      <FieldLabel label="Đối tượng nhận">
        <select
          className="field"
          value={form.targetRole}
          onChange={(event) =>
            setForm({ ...form, targetRole: event.target.value })
          }
        >
          <option value="">Tất cả người dùng</option>
          {roles.map((value) => (
            <option key={value} value={value}>
              {roleLabel[value]}
            </option>
          ))}
        </select>
      </FieldLabel>
      <label className="md:col-span-2">
        <span className="label">Nội dung</span>
        <textarea
          className="field min-h-28"
          required
          value={form.content}
          onChange={(event) =>
            setForm({ ...form, content: event.target.value })
          }
        />
      </label>
      <button className="btn-primary md:col-span-2" type="submit">
        <Send className="h-4 w-4" />
        Gửi thông báo
      </button>
    </form>
  );
}

export function Input({
  label,
  value,
  onChange,
  type = "text",
  required = false,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  required?: boolean;
}) {
  return (
    <FieldLabel label={label}>
      <input
        className="field"
        type={type}
        value={value}
        required={required}
        onChange={(event) => onChange(event.target.value)}
      />
    </FieldLabel>
  );
}

function DeleteButton({
  label,
  onDelete,
}: {
  label: string;
  onDelete: () => void;
}) {
  return (
    <button
      aria-label={`Xóa ${label}`}
      className="rounded-xl bg-red-50 p-2 text-red-700 hover:bg-red-100"
      onClick={() => {
        if (window.confirm(`Bạn có chắc muốn xóa ${label} này?`)) onDelete();
      }}
    >
      <Trash2 className="h-4 w-4" />
    </button>
  );
}

function IconButton({
  label,
  icon: Icon,
  onClick,
}: {
  label: string;
  icon: typeof Check;
  onClick: () => void;
}) {
  return (
    <button
      aria-label={label}
      title={label}
      className="rounded-xl bg-cream p-2 text-teal hover:bg-lime/40"
      onClick={onClick}
    >
      <Icon className="h-4 w-4" />
    </button>
  );
}
