import { lazy, Suspense, useEffect } from "react";
import { Route, Routes } from "react-router-dom";
import { AppShell } from "./components/AppShell";
import { LoadingScreen } from "./components/LoadingScreen";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { bootstrapAuth } from "./store/authSlice";
import { useAppDispatch } from "./store/hooks";

const HomePage = lazy(() =>
  import("./pages/HomePage").then((module) => ({ default: module.HomePage })),
);
const AuthPage = lazy(() =>
  import("./pages/AuthPage").then((module) => ({ default: module.AuthPage })),
);
const ProductListPage = lazy(() =>
  import("./pages/ProductListPage").then((module) => ({
    default: module.ProductListPage,
  })),
);
const ProductDetailPage = lazy(() =>
  import("./pages/ProductDetailPage").then((module) => ({
    default: module.ProductDetailPage,
  })),
);
const CartPage = lazy(() =>
  import("./pages/CartPage").then((module) => ({ default: module.CartPage })),
);
const WishlistPage = lazy(() =>
  import("./pages/WishlistPage").then((module) => ({
    default: module.WishlistPage,
  })),
);
const CheckoutPage = lazy(() =>
  import("./pages/CheckoutPage").then((module) => ({
    default: module.CheckoutPage,
  })),
);
const OrdersPage = lazy(() =>
  import("./pages/OrdersPage").then((module) => ({
    default: module.OrdersPage,
  })),
);
const OrderDetailPage = lazy(() =>
  import("./pages/OrderDetailPage").then((module) => ({
    default: module.OrderDetailPage,
  })),
);
const OrderTrackingPage = lazy(() =>
  import("./pages/OrderTrackingPage").then((module) => ({
    default: module.OrderTrackingPage,
  })),
);
const PaymentReturnPage = lazy(() =>
  import("./pages/PaymentReturnPage").then((module) => ({
    default: module.PaymentReturnPage,
  })),
);
const ProfilePage = lazy(() =>
  import("./pages/ProfilePage").then((module) => ({
    default: module.ProfilePage,
  })),
);
const ChatPage = lazy(() =>
  import("./pages/ChatPage").then((module) => ({ default: module.ChatPage })),
);
const DashboardPage = lazy(() =>
  import("./pages/DashboardPage").then((module) => ({
    default: module.DashboardPage,
  })),
);
const AdminPage = lazy(() =>
  import("./pages/AdminPage").then((module) => ({
    default: module.AdminPage,
  })),
);
const NotFoundPage = lazy(() =>
  import("./pages/NotFoundPage").then((module) => ({
    default: module.NotFoundPage,
  })),
);

export default function App() {
  const dispatch = useAppDispatch();

  useEffect(() => {
    dispatch(bootstrapAuth());
  }, [dispatch]);

  return (
    <Suspense fallback={<LoadingScreen label="Đang tải trang" />}>
      <Routes>
        <Route element={<AppShell />}>
          <Route index element={<HomePage />} />
          <Route path="products" element={<ProductListPage />} />
          <Route path="products/:id" element={<ProductDetailPage />} />
          <Route path="login" element={<AuthPage mode="login" />} />
          <Route path="register" element={<AuthPage mode="register" />} />
          <Route path="payment/momo/return" element={<PaymentReturnPage />} />

          <Route element={<ProtectedRoute />}>
            <Route path="profile" element={<ProfilePage />} />
            <Route path="orders" element={<OrdersPage />} />
            <Route path="orders/:id" element={<OrderDetailPage />} />
            <Route path="order-tracking" element={<OrderTrackingPage />} />
            <Route
              path="order-tracking/:orderCode"
              element={<OrderTrackingPage />}
            />
            <Route path="chat" element={<ChatPage />} />
          </Route>

          <Route element={<ProtectedRoute roles={["ROLE_CUSTOMER"]} />}>
            <Route path="cart" element={<CartPage />} />
            <Route path="wishlist" element={<WishlistPage />} />
            <Route path="checkout" element={<CheckoutPage />} />
          </Route>

          <Route element={<ProtectedRoute roles={["ROLE_ADMIN"]} />}>
            <Route path="dashboard" element={<DashboardPage />} />
            <Route path="admin" element={<AdminPage />} />
            <Route path="admin/:section" element={<AdminPage />} />
          </Route>

          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
