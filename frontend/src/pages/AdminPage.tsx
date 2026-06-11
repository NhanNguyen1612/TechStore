import {
  Bell,
  Boxes,
  ClipboardList,
  CreditCard,
  FolderTree,
  MessageSquare,
  Percent,
  ShieldCheck,
  Star,
  Users,
} from "lucide-react";
import { Link, NavLink, useParams } from "react-router-dom";
import clsx from "clsx";
import {
  CatalogPanel,
  ConversationsPanel,
  CouponsPanel,
  NotificationsPanel,
  OrderPanel,
  CheckoutPanel,
  ProductsPanel,
  ReviewsPanel,
  UsersPanel,
} from "./admin/AdminPanels";

const sections = [
  { id: "users", label: "Người dùng", icon: Users },
  { id: "products", label: "Sản phẩm", icon: Boxes },
  { id: "catalog", label: "Danh mục", icon: FolderTree },
  { id: "orders", label: "Đơn hàng", icon: ClipboardList },
  { id: "payments", label: "Thanh toán", icon: CreditCard },
  { id: "coupons", label: "Mã giảm giá", icon: Percent },
  { id: "reviews", label: "Đánh giá", icon: Star },
  { id: "conversations", label: "Trò chuyện", icon: MessageSquare },
  { id: "notifications", label: "Thông báo", icon: Bell },
] as const;

export function AdminPage() {
  const { section = "users" } = useParams();

  return (
    <section className="container-page py-8 sm:py-12">
      <div className="overflow-hidden rounded-[2rem] border border-black/5 bg-ink text-white shadow-soft">
        <div className="flex flex-col justify-between gap-6 px-6 py-7 lg:flex-row lg:items-center lg:px-8">
          <div className="flex items-center gap-4">
            <span className="grid h-12 w-12 place-items-center rounded-2xl bg-lime text-ink">
              <ShieldCheck className="h-6 w-6" />
            </span>
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.2em] text-lime">
                Khu vực quản trị
              </p>
              <h1 className="mt-1 font-display text-2xl font-extrabold">
                Bảng điều khiển quản trị
              </h1>
            </div>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <p className="max-w-lg text-sm leading-6 text-white/55">
              Quản lý cửa hàng qua cùng API với Swagger và Postman.
            </p>
            <Link
              to="/dashboard"
              className="rounded-full bg-white px-5 py-3 text-sm font-bold text-ink"
            >
              Xem thống kê
            </Link>
          </div>
        </div>
        <nav className="flex gap-2 overflow-x-auto border-t border-white/10 px-4 py-3 lg:px-8">
          {sections.map(({ id, label, icon: Icon }) => (
            <NavLink
              key={id}
              to={`/admin/${id}`}
              className={() =>
                clsx(
                  "flex shrink-0 items-center gap-2 rounded-full px-4 py-2.5 text-sm font-bold transition",
                  section === id
                    ? "bg-lime text-ink"
                    : "text-white/60 hover:bg-white/10 hover:text-white",
                )
              }
            >
              <Icon className="h-4 w-4" />
              {label}
            </NavLink>
          ))}
        </nav>
      </div>

      <div className="mt-8">{renderPanel(section)}</div>
    </section>
  );
}

function renderPanel(section: string) {
  switch (section) {
    case "products":
      return <ProductsPanel />;
    case "catalog":
      return <CatalogPanel />;
    case "orders":
      return <OrderPanel />;
    case "payments":
      return <CheckoutPanel />;
    case "coupons":
      return <CouponsPanel />;
    case "reviews":
      return <ReviewsPanel />;
    case "conversations":
      return <ConversationsPanel />;
    case "notifications":
      return <NotificationsPanel />;
    default:
      return <UsersPanel />;
  }
}
