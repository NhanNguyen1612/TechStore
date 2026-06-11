import {
  BarChart3,
  ChevronDown,
  Heart,
  LogOut,
  Menu,
  MessageCircle,
  Package,
  PackageSearch,
  ShieldCheck,
  ShoppingBag,
  UserRound,
  X,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import clsx from "clsx";
import { roleLabel } from "../lib/constants";
import { useAppDispatch, useAppSelector } from "../store/hooks";
import { logout, sessionExpired } from "../store/authSlice";
import { fetchCart, resetCart } from "../store/cartSlice";
import { fetchWishlist, resetWishlist } from "../store/wishlistSlice";

const navClass = ({ isActive }: { isActive: boolean }) =>
  clsx(
    "rounded-full px-4 py-2 text-sm font-semibold transition",
    isActive ? "bg-ink text-white" : "text-ink/65 hover:bg-white hover:text-ink",
  );

export function AppShell() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const user = useAppSelector((state) => state.auth.user);
  const cartCount = useAppSelector((state) => state.cart.data.totalQuantity);
  const wishlistCount = useAppSelector((state) => state.wishlist.items.length);
  const [menuOpen, setMenuOpen] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const accountRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const expire = () => {
      dispatch(sessionExpired());
      dispatch(resetCart());
      dispatch(resetWishlist());
      navigate("/login");
    };
    window.addEventListener("auth:expired", expire);
    return () => window.removeEventListener("auth:expired", expire);
  }, [dispatch, navigate]);

  useEffect(() => {
    if (user?.role === "ROLE_CUSTOMER") {
      dispatch(fetchCart());
      dispatch(fetchWishlist());
    } else {
      dispatch(resetCart());
      dispatch(resetWishlist());
    }
  }, [dispatch, user?.id, user?.role]);

  useEffect(() => {
    const closeAccount = (event: MouseEvent) => {
      if (
        accountRef.current &&
        !accountRef.current.contains(event.target as Node)
      ) {
        setAccountOpen(false);
      }
    };
    document.addEventListener("mousedown", closeAccount);
    return () => document.removeEventListener("mousedown", closeAccount);
  }, []);

  const closeMenu = () => {
    setMenuOpen(false);
    setAccountOpen(false);
  };

  const handleLogout = async () => {
    await dispatch(logout());
    dispatch(resetCart());
    dispatch(resetWishlist());
    closeMenu();
    navigate("/login", { replace: true });
  };

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-40 border-b border-black/5 bg-cream/90 backdrop-blur-xl">
        <div className="container-page flex h-20 items-center justify-between gap-4">
          <Link to="/" className="flex items-center gap-3" onClick={closeMenu}>
            <span className="grid h-11 w-11 place-items-center rounded-2xl bg-ink font-display text-lg font-black text-lime">
              TS
            </span>
            <span className="hidden font-display text-xl font-extrabold sm:block">
              TechStore
            </span>
          </Link>

          <nav className="hidden items-center gap-1 lg:flex">
            <NavLink to="/products" className={navClass}>
              Sản phẩm
            </NavLink>
            {user && (
              <NavLink to="/orders" className={navClass}>
                Đơn hàng
              </NavLink>
            )}
            {user?.role === "ROLE_CUSTOMER" && (
              <NavLink to="/order-tracking" className={navClass}>
                Tra cứu đơn hàng
              </NavLink>
            )}
            {user && (
              <NavLink to="/chat" className={navClass}>
                Trò chuyện
              </NavLink>
            )}
            {user?.role === "ROLE_ADMIN" && (
              <NavLink to="/dashboard" className={navClass}>
                Thống kê
              </NavLink>
            )}
            {user?.role === "ROLE_ADMIN" && (
              <NavLink to="/admin" className={navClass}>
                Quản trị
              </NavLink>
            )}
          </nav>

          <div className="flex items-center gap-2">
            {user?.role === "ROLE_CUSTOMER" && (
              <>
                <Link
                  to="/wishlist"
                  className="relative rounded-full p-2.5 hover:bg-white"
                  aria-label="Yêu thích"
                >
                  <Heart className="h-5 w-5" />
                  {wishlistCount > 0 && (
                    <span className="absolute -right-1 -top-1 grid h-5 min-w-5 place-items-center rounded-full bg-coral px-1 text-[10px] font-bold text-white">
                      {wishlistCount}
                    </span>
                  )}
                </Link>
                <Link
                  to="/cart"
                  className="relative rounded-full p-2.5 hover:bg-white"
                  aria-label="Giỏ hàng"
                >
                  <ShoppingBag className="h-5 w-5" />
                  {cartCount > 0 && (
                    <span className="absolute -right-1 -top-1 grid h-5 min-w-5 place-items-center rounded-full bg-ink px-1 text-[10px] font-bold text-lime">
                      {cartCount}
                    </span>
                  )}
                </Link>
              </>
            )}
            {user ? (
              <div ref={accountRef} className="relative hidden sm:block">
                <button
                  type="button"
                  data-testid="account-menu-button"
                  aria-expanded={accountOpen}
                  onClick={() => setAccountOpen((open) => !open)}
                  className="flex items-center gap-2 rounded-full bg-white py-2 pl-2 pr-3 text-sm font-semibold shadow-sm transition hover:shadow-md"
                >
                  {user.avatarUrl ? (
                    <img
                      src={user.avatarUrl}
                      alt=""
                      className="h-8 w-8 rounded-full object-cover"
                    />
                  ) : (
                    <span className="grid h-8 w-8 place-items-center rounded-full bg-lime">
                      <UserRound className="h-4 w-4" />
                    </span>
                  )}
                  <span className="max-w-28 truncate">
                    {user.fullName.split(" ")[0]}
                  </span>
                  <ChevronDown
                    className={clsx(
                      "h-4 w-4 transition",
                      accountOpen && "rotate-180",
                    )}
                  />
                </button>

                {accountOpen && (
                  <div
                    data-testid="account-menu"
                    className="absolute right-0 mt-3 w-64 overflow-hidden rounded-3xl border border-black/5 bg-white p-2 shadow-soft"
                  >
                    <div className="border-b border-ink/5 px-3 py-3">
                      <p className="truncate font-bold">{user.fullName}</p>
                      <p className="mt-0.5 truncate text-xs text-ink/45">
                        {user.email}
                      </p>
                      <span className="mt-2 inline-flex rounded-full bg-lime/50 px-2.5 py-1 text-[10px] font-extrabold uppercase tracking-wider">
                        {roleLabel[user.role]}
                      </span>
                    </div>
                    {user.role === "ROLE_ADMIN" && (
                      <Link
                        to="/admin"
                        onClick={closeMenu}
                        className="mt-2 flex items-center gap-3 rounded-2xl px-3 py-3 text-sm font-semibold hover:bg-cream"
                      >
                        <ShieldCheck className="h-5 w-5 text-teal" />
                        Trang quản trị
                      </Link>
                    )}
                    <Link
                      to="/order-tracking"
                      onClick={closeMenu}
                      className="flex items-center gap-3 rounded-2xl px-3 py-3 text-sm font-semibold hover:bg-cream"
                    >
                      <PackageSearch className="h-5 w-5 text-teal" />
                      Tra cứu đơn hàng
                    </Link>
                    <Link
                      to="/profile"
                      onClick={closeMenu}
                      className="flex items-center gap-3 rounded-2xl px-3 py-3 text-sm font-semibold hover:bg-cream"
                    >
                      <UserRound className="h-5 w-5 text-teal" />
                      Hồ sơ
                    </Link>
                    <button
                      type="button"
                      data-testid="logout-button"
                      onClick={handleLogout}
                      className="flex w-full items-center gap-3 rounded-2xl px-3 py-3 text-left text-sm font-semibold text-coral hover:bg-red-50"
                    >
                      <LogOut className="h-5 w-5" />
                      Đăng xuất
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <Link to="/login" className="btn-primary hidden sm:inline-flex">
                Đăng nhập
              </Link>
            )}
            <button
              onClick={() => setMenuOpen((open) => !open)}
              className="rounded-full p-2.5 hover:bg-white lg:hidden"
              aria-label="Mở hoặc đóng menu"
            >
              {menuOpen ? <X /> : <Menu />}
            </button>
          </div>
        </div>

        {menuOpen && (
          <div className="border-t border-black/5 bg-cream px-4 py-4 lg:hidden">
            <div className="container-page grid gap-2">
              <MobileLink to="/products" icon={Package} onClick={closeMenu}>
                Sản phẩm
              </MobileLink>
              {user && (
                <MobileLink to="/orders" icon={ShoppingBag} onClick={closeMenu}>
                  Đơn hàng
                </MobileLink>
              )}
              {user && (
                <MobileLink
                  to="/order-tracking"
                  icon={PackageSearch}
                  onClick={closeMenu}
                >
                  Tra cứu đơn hàng
                </MobileLink>
              )}
              {user && (
                <MobileLink
                  to="/chat"
                  icon={MessageCircle}
                  onClick={closeMenu}
                >
                  Trò chuyện
                </MobileLink>
              )}
              {user?.role === "ROLE_ADMIN" && (
                <MobileLink
                  to="/dashboard"
                  icon={BarChart3}
                  onClick={closeMenu}
                >
                  Thống kê
                </MobileLink>
              )}
              {user?.role === "ROLE_ADMIN" && (
                <MobileLink
                  to="/admin"
                  icon={ShieldCheck}
                  onClick={closeMenu}
                >
                  Trang quản trị
                </MobileLink>
              )}
              {user && (
                <MobileLink
                  to="/profile"
                  icon={UserRound}
                  onClick={closeMenu}
                >
                  Hồ sơ
                </MobileLink>
              )}
              {user ? (
                <button
                  className="flex items-center gap-3 rounded-2xl px-4 py-3 text-left font-semibold text-coral"
                  onClick={handleLogout}
                >
                  <LogOut className="h-5 w-5" />
                  Đăng xuất
                </button>
              ) : (
                <MobileLink to="/login" icon={UserRound} onClick={closeMenu}>
                  Đăng nhập
                </MobileLink>
              )}
            </div>
          </div>
        )}
      </header>

      <main>
        <Outlet />
      </main>

      <footer className="mt-20 border-t border-black/5 bg-ink py-12 text-white">
        <div className="container-page flex flex-col justify-between gap-8 md:flex-row">
          <div>
            <p className="font-display text-2xl font-extrabold text-lime">
              TechStore
            </p>
            <p className="mt-2 max-w-md text-sm leading-6 text-white/55">
              Công nghệ phù hợp, giá cả minh bạch và hỗ trợ tận tâm cả sau khi mua hàng.
            </p>
          </div>
          <div className="flex flex-wrap gap-x-8 gap-y-3 text-sm text-white/65">
            <Link to="/products" className="hover:text-lime">
              Sản phẩm
            </Link>
            <Link to="/chat" className="hover:text-lime">
              Hỗ trợ
            </Link>
            {user && (
              <Link to="/order-tracking" className="hover:text-lime">
                Tra cứu đơn hàng
              </Link>
            )}
            <Link to="/profile" className="hover:text-lime">
              Tài khoản
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}

function MobileLink({
  to,
  icon: Icon,
  children,
  onClick,
}: {
  to: string;
  icon: typeof Package;
  children: React.ReactNode;
  onClick: () => void;
}) {
  return (
    <Link
      to={to}
      onClick={onClick}
      className="flex items-center gap-3 rounded-2xl bg-white/60 px-4 py-3 font-semibold"
    >
      <Icon className="h-5 w-5" />
      {children}
    </Link>
  );
}
