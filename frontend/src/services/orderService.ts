import { api, apiData } from "../lib/api";
import type {
  CreateOrderInput,
  MomoPayment,
  OrderDetail,
  OrderSummary,
  OrderTimelineEntry,
  OrderTracking,
} from "../types/api";

export const orderService = {
  createOrder: (input: CreateOrderInput) =>
    apiData<OrderDetail>(api.post("/api/orders", input)),

  getMyOrder: () =>
    apiData<OrderSummary[]>(api.get("/api/orders/my-orders")),

  getOrderDetail: (orderId: number | string) =>
    apiData<OrderDetail>(api.get(`/api/orders/${orderId}`)),

  getOrderTracking: (orderId: number | string) =>
    apiData<OrderTracking>(api.get(`/api/orders/${orderId}/tracking`)),

  getOrderTimeline: (orderId: number | string) =>
    apiData<OrderTimelineEntry[]>(
      api.get(`/api/orders/${orderId}/timeline`),
    ),

  getOrderByCode: (orderCode: string) =>
    apiData<OrderDetail>(
      api.get(`/api/orders/code/${encodeURIComponent(orderCode.trim())}`),
    ),

  cancelOrder: (orderId: number, reason?: string) =>
    apiData<OrderDetail>(
      api.put(`/api/orders/${orderId}/cancel`, { reason }),
    ),

  createMomoPayment: (orderId: number) =>
    apiData<MomoPayment>(
      api.post("/api/payments/momo/create", { orderId }),
    ),

  getPaymentStatus: (orderId: number | string) =>
    apiData<MomoPayment>(
      api.get(`/api/payments/${orderId}/status`),
    ),
};
