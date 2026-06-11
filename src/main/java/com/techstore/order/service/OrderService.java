package com.techstore.order.service;

import com.techstore.order.dto.request.CreateOrderRequest;
import com.techstore.order.dto.response.OrderDetailResponse;
import com.techstore.order.dto.response.OrderSummaryResponse;
import com.techstore.order.dto.response.OrderTimelineResponse;
import com.techstore.order.dto.response.OrderTrackingResponse;
import java.util.List;

public interface OrderService {

    OrderDetailResponse createOrder(Long userId, CreateOrderRequest request);

    List<OrderSummaryResponse> getOrders(Long userId);

    OrderDetailResponse getOrder(Long userId, Long orderId);

    OrderDetailResponse getOrderByCode(Long userId, String orderCode);

    OrderTrackingResponse getTracking(Long userId, Long orderId);

    List<OrderTimelineResponse> getTimeline(Long userId, Long orderId);

    OrderDetailResponse cancelOrder(Long userId, Long orderId, String reason);

    OrderDetailResponse confirmOrder(Long userId, Long orderId);

    OrderDetailResponse startShipping(Long userId, Long orderId);

    OrderDetailResponse markDelivered(Long userId, Long orderId);
}
