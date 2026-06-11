import type { OrderStatus, PaymentStatus, Role } from "../types/api";

export const orderStatusLabel: Record<OrderStatus, string> = {
  PENDING: "Chờ xử lý",
  PENDING_PAYMENT: "Chờ thanh toán",
  PAID: "Đã thanh toán",
  CONFIRMED: "Đã xác nhận",
  SHIPPING: "Đang giao",
  DELIVERED: "Đã giao",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy",
};

export const paymentStatusLabel: Record<PaymentStatus, string> = {
  PENDING: "Chờ xử lý",
  PAID: "Đã thanh toán",
  FAILED: "Thất bại",
  CANCELLED: "Đã hủy",
  REFUNDED: "Đã hoàn tiền",
};

export const roleLabel: Record<Role, string> = {
  ROLE_ADMIN: "Quản trị",
  ROLE_STAFF: "Nhân viên",
  ROLE_CUSTOMER: "Khách hàng",
};

export const adminStatusLabel: Record<string, string> = {
  ACTIVE: "Đang hoạt động",
  INACTIVE: "Ngừng hiển thị",
  ENABLED: "Đang bật",
  DISABLED: "Đã tắt",
  PENDING: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  HIDDEN: "Đã ẩn",
  OPEN: "Đang mở",
  CLOSED: "Đã đóng",
  PAID: "Đã thanh toán",
  FAILED: "Thất bại",
  CANCELLED: "Đã hủy",
  REFUNDED: "Đã hoàn tiền",
  COMPLETED: "Hoàn thành",
  DELIVERED: "Đã giao",
  SHIPPING: "Đang giao",
  PENDING_PAYMENT: "Chờ thanh toán",
  CONFIRMED: "Đã xác nhận",
};
