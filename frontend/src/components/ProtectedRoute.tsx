import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAppSelector } from "../store/hooks";
import type { Role } from "../types/api";
import { LoadingScreen } from "./LoadingScreen";

export function ProtectedRoute({ roles }: { roles?: Role[] }) {
  const { user, initialized } = useAppSelector((state) => state.auth);
  const location = useLocation();

  if (!initialized) {
    return <LoadingScreen label="Đang khôi phục phiên đăng nhập" />;
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  if (roles && !roles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}
