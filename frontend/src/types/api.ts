export type Role = "ROLE_ADMIN" | "ROLE_STAFF" | "ROLE_CUSTOMER";
export type OrderStatus =
  | "PENDING"
  | "PENDING_PAYMENT"
  | "PAID"
  | "CONFIRMED"
  | "SHIPPING"
  | "DELIVERED"
  | "COMPLETED"
  | "CANCELLED";
export type PaymentStatus =
  | "PENDING"
  | "PAID"
  | "FAILED"
  | "CANCELLED"
  | "REFUNDED";
export type PaymentMethod = "COD" | "MOMO";
export type OrderPaymentStatus =
  | "UNPAID"
  | "PENDING"
  | "PAID"
  | "FAILED"
  | "CANCELLED"
  | "REFUNDED";
export type ProductSort =
  | "PRICE_ASC"
  | "PRICE_DESC"
  | "NEWEST"
  | "BEST_SELLER";
export type ReviewStatus = "PENDING" | "APPROVED" | "HIDDEN";
export type MessageType = "TEXT" | "IMAGE";

export interface ApiResponse<T> {
  success: boolean;
  code?: string;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface UserProfile {
  id: number;
  email: string;
  fullName: string;
  phone?: string | null;
  avatarUrl?: string | null;
  role: Role;
  enabled?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AuthPayload {
  tokenType: string;
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: UserProfile;
}

export interface ProductSummary {
  id: number;
  name: string;
  slug: string;
  sku: string;
  price: number;
  stockQuantity: number;
  soldCount: number;
  thumbnailUrl?: string | null;
  categoryId: number;
  categoryName: string;
  brandId: number;
  brandName: string;
  createdAt: string;
}

export interface ProductImage {
  id: number;
  url: string;
  sortOrder: number;
  primary: boolean;
}

export interface ProductDetail extends Omit<ProductSummary, "thumbnailUrl"> {
  description?: string | null;
  images: ProductImage[];
  updatedAt: string;
  createdBy?: number | null;
  updatedBy?: number | null;
}

export type ProductPage = PageResponse<ProductSummary>;

export interface Brand {
  id: number;
  name: string;
  slug: string;
  description?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CartItem {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  thumbnailUrl?: string | null;
  unitPrice: number;
  quantity: number;
  availableStock: number;
  available: boolean;
  subtotal: number;
}

export interface Cart {
  id?: number | null;
  items: CartItem[];
  totalQuantity: number;
  totalAmount: number;
}

export interface WishlistItem {
  id: number;
  productId: number;
  productName: string;
  slug: string;
  sku: string;
  price: number;
  stockQuantity: number;
  thumbnailUrl?: string | null;
  categoryId: number;
  categoryName: string;
  brandId: number;
  brandName: string;
  wishlisted: boolean;
  addedAt: string;
}

export interface Wishlist {
  items: WishlistItem[];
  totalProducts: number;
}

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  thumbnailUrl?: string | null;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

export interface OrderSummary {
  id: number;
  orderCode: string;
  customerId: number;
  customerName: string;
  status: OrderStatus;
  paymentMethod: PaymentMethod;
  paymentStatus: OrderPaymentStatus;
  totalQuantity: number;
  totalAmount: number;
  createdAt: string;
  updatedAt: string;
}

export interface OrderDetail extends OrderSummary {
  customerEmail: string;
  recipientName: string;
  phone?: string | null;
  shippingAddress?: string | null;
  note?: string | null;
  transactionId?: number | null;
  items: OrderItem[];
  confirmedAt?: string | null;
  shippedAt?: string | null;
  deliveredAt?: string | null;
  cancelledAt?: string | null;
  completedAt?: string | null;
}

export interface CreateOrderInput {
  recipientName: string;
  phone: string;
  shippingAddress: string;
  note?: string;
  paymentMethod: PaymentMethod;
}

export interface OrderTimelineEntry {
  status: OrderStatus;
  title: string;
  description: string;
  note?: string | null;
  changedBy?: number | null;
  changedByName?: string | null;
  time: string;
}

export interface OrderTracking {
  orderId: number;
  orderCode: string;
  customerName: string;
  phone?: string | null;
  shippingAddress?: string | null;
  note?: string | null;
  paymentMethod: PaymentMethod;
  paymentStatus: OrderPaymentStatus;
  transactionId?: number | null;
  orderStatus: OrderStatus;
  totalAmount: number;
  items: OrderItem[];
  timeline: OrderTimelineEntry[];
  cancellationReason?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface MomoPayment {
  paymentId: number;
  orderId: number;
  orderCode: string;
  status: PaymentStatus;
  amount: number;
  requestId: string;
  momoOrderId: string;
  momoTransactionId?: number | null;
  payUrl?: string | null;
  deeplink?: string | null;
  qrCodeUrl?: string | null;
  resultCode?: number | null;
  message?: string | null;
  paidAt?: string | null;
  expiresAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface MomoReturn {
  signatureValid: boolean;
  matched: boolean;
  orderId?: number | null;
  momoOrderId?: string | null;
  resultCode?: number | null;
  message?: string | null;
  paymentStatus?: PaymentStatus | null;
}

export interface ReviewImage {
  id: number;
  url: string;
  sortOrder: number;
}

export interface Review {
  id: number;
  productId: number;
  userId: number;
  userName: string;
  rating: number;
  comment: string;
  verifiedPurchase: boolean;
  status: ReviewStatus;
  images: ReviewImage[];
  approvedAt?: string | null;
  approvedBy?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProductReviews {
  productId: number;
  totalReviews: number;
  averageRating: number;
  reviews: Review[];
}

export interface ChatParticipant {
  id: number;
  fullName: string;
  role: Role;
  avatarUrl?: string | null;
}

export interface ChatContact extends ChatParticipant {
  email: string;
}

export interface ChatMessage {
  id: number;
  conversationId: number;
  senderId: number;
  senderName: string;
  senderAvatarUrl?: string | null;
  type: MessageType;
  content: string;
  mine: boolean;
  read: boolean;
  readAt?: string | null;
  createdAt: string;
}

export interface Conversation {
  id: number;
  participant: ChatParticipant;
  lastMessage?: ChatMessage | null;
  unreadCount: number;
  lastMessageAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ReadReceipt {
  conversationId: number;
  readBy: number;
  messagesRead: number;
  readAt: string;
}

export interface ChatEvent {
  eventType: "MESSAGE" | "READ";
  message?: ChatMessage;
  readReceipt?: ReadReceipt;
}

export interface DashboardOverview {
  totalRevenue: number;
  momoRevenue: number;
  codRevenue: number;
  totalOrder: number;
  totalUsers: number;
  totalCustomers: number;
  cancelledOrder: number;
  cancellationRate: number;
  from?: string | null;
  to?: string | null;
}

export interface DashboardRevenue {
  totalRevenue: number;
  momoRevenue: number;
  codRevenue: number;
  from?: string | null;
  to?: string | null;
}

export interface OrderStatusStatistic {
  status: OrderStatus;
  count: number;
}

export interface DashboardOrder {
  totalOrder: number;
  cancelledOrder: number;
  cancellationRate: number;
  byStatus: OrderStatusStatistic[];
  from?: string | null;
  to?: string | null;
}

export interface TopProduct {
  productId: number;
  productName: string;
  sku: string;
  thumbnailUrl?: string | null;
  quantitySold: number;
  revenue: number;
}

export interface DashboardProducts {
  totalProducts: number;
  outOfStockProducts: number;
  topSellingProducts: TopProduct[];
  from?: string | null;
  to?: string | null;
}

export interface TopCustomer {
  customerId: number;
  fullName: string;
  email: string;
  totalOrder: number;
  totalSpent: number;
}

export interface DashboardCustomers {
  totalCustomers: number;
  newCustomers: number;
  topCustomers: TopCustomer[];
  from?: string | null;
  to?: string | null;
}

export interface AdminPage<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AdminUser {
  id: number;
  email: string;
  fullName: string;
  phone?: string | null;
  role: Role;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AdminImage {
  id: number;
  url: string;
  sortOrder: number;
  primary: boolean;
}

export interface AdminProduct {
  id: number;
  name: string;
  slug: string;
  sku: string;
  description?: string | null;
  price: number;
  stockQuantity: number;
  soldCount: number;
  thumbnailUrl?: string | null;
  categoryId: number;
  categoryName: string;
  brandId: number;
  brandName: string;
  active: boolean;
  images: AdminImage[];
  createdAt: string;
  updatedAt: string;
}

export interface AdminTaxonomy {
  id: number;
  name: string;
  slug: string;
  description?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminOrderItem {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

export interface AdminOrder {
  id: number;
  orderCode: string;
  customerId: number;
  customerName: string;
  customerEmail: string;
  status: OrderStatus;
  paymentMethod: string;
  totalQuantity: number;
  totalAmount: number;
  items: AdminOrderItem[];
  createdAt: string;
  updatedAt: string;
}

export interface AdminPayment {
  id: number;
  orderId: number;
  orderCode: string;
  provider: string;
  status: PaymentStatus;
  amount: number;
  requestId?: string | null;
  momoOrderId?: string | null;
  transactionId?: number | null;
  resultCode?: number | null;
  message?: string | null;
  paidAt?: string | null;
  createdAt: string;
  transactions: Array<{
    id: number;
    type: string;
    requestId?: string | null;
    momoOrderId?: string | null;
    transactionId?: number | null;
    amount: number;
    resultCode?: number | null;
    message?: string | null;
    createdAt: string;
  }>;
  callbacks: Array<{
    id: number;
    type: string;
    requestId?: string | null;
    momoOrderId?: string | null;
    transactionId?: number | null;
    resultCode?: number | null;
    signatureValid: boolean;
    processed: boolean;
    processingMessage?: string | null;
    receivedAt: string;
  }>;
}

export type CouponType = "PERCENTAGE" | "FIXED_AMOUNT";

export interface AdminCoupon {
  id: number;
  code: string;
  name: string;
  type: CouponType;
  value: number;
  minimumOrderAmount: number;
  usageLimit?: number | null;
  usedCount: number;
  startsAt: string;
  endsAt: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AdminReview {
  id: number;
  productId: number;
  productName: string;
  userId: number;
  userName: string;
  rating: number;
  comment: string;
  status: ReviewStatus;
  images: string[];
  createdAt: string;
  updatedAt: string;
}

export interface AdminConversation {
  id: number;
  participantOne: AdminUser;
  participantTwo: AdminUser;
  assignedStaff?: AdminUser | null;
  closed: boolean;
  closedAt?: string | null;
  lastMessageAt?: string | null;
  createdAt: string;
}

export interface AdminMessage {
  id: number;
  senderId: number;
  senderName: string;
  type: MessageType;
  content: string;
  readAt?: string | null;
  createdAt: string;
}

export interface AdminNotification {
  id: number;
  title: string;
  content: string;
  targetRole?: Role | null;
  createdBy: number;
  createdByName: string;
  createdAt: string;
}

export interface AdminOverview {
  totalRevenue: number;
  todayRevenue: number;
  monthRevenue: number;
  totalOrder: number;
  totalUsers: number;
  totalProducts: number;
  paidOrder: number;
  unpaidOrder: number;
  cancelledOrder: number;
  cancellationRate: number;
  momoSuccessRate: number;
}

export interface AdminTimeSeriesPoint {
  period: string;
  revenue: number;
  orders: number;
}

export interface AdminRevenue {
  totalRevenue: number;
  momoRevenue: number;
  codRevenue: number;
  series: AdminTimeSeriesPoint[];
  fromDate: string;
  toDate: string;
  type: "DAILY" | "MONTHLY" | "YEARLY";
}

export interface AdminOrderAnalytics {
  totalOrder: number;
  newToday: number;
  shipping: number;
  completed: number;
  cancelled: number;
  byStatus: Array<{ label: string; count: number }>;
  monthly: AdminTimeSeriesPoint[];
}

export interface AdminProductMetric {
  productId: number;
  name: string;
  sku: string;
  stock: number;
  sold: number;
  revenue: number;
}

export interface AdminProductsAnalytics {
  activeProducts: number;
  inactiveProducts: number;
  bestSellers: AdminProductMetric[];
  lowStock: AdminProductMetric[];
  outOfStock: AdminProductMetric[];
  revenueByProduct: AdminProductMetric[];
}

export interface AdminCustomerMetric {
  customerId: number;
  name: string;
  email: string;
  orders: number;
  spending: number;
}

export interface AdminCustomersAnalytics {
  totalCustomers: number;
  newToday: number;
  newThisMonth: number;
  topByOrder: AdminCustomerMetric[];
  topBySpending: AdminCustomerMetric[];
}
