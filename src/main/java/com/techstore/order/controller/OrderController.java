package com.techstore.order.controller;

import com.techstore.auth.dto.response.ApiResponse;
import com.techstore.auth.security.AuthUserPrincipal;
import com.techstore.order.dto.request.CancelOrderRequest;
import com.techstore.order.dto.request.CreateOrderRequest;
import com.techstore.order.dto.response.OrderDetailResponse;
import com.techstore.order.dto.response.OrderSummaryResponse;
import com.techstore.order.dto.response.OrderTimelineResponse;
import com.techstore.order.dto.response.OrderTrackingResponse;
import com.techstore.order.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> createOrder(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody(required = false) CreateOrderRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Order created",
                        orderService.createOrder(principal.getId(), request)
                ));
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<List<OrderSummaryResponse>> getMyOrders(
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return ApiResponse.success(
                "Customer orders retrieved",
                orderService.getOrders(principal.getId())
        );
    }

    @GetMapping("/code/{orderCode}")
    public ApiResponse<OrderDetailResponse> getOrderByCode(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable String orderCode
    ) {
        return ApiResponse.success(
                "Order retrieved",
                orderService.getOrderByCode(principal.getId(), orderCode)
        );
    }

    @GetMapping("/{id}/tracking")
    public ApiResponse<OrderTrackingResponse> getTracking(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                "Order tracking retrieved",
                orderService.getTracking(principal.getId(), id)
        );
    }

    @GetMapping("/{id}/timeline")
    public ApiResponse<List<OrderTimelineResponse>> getTimeline(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                "Order timeline retrieved",
                orderService.getTimeline(principal.getId(), id)
        );
    }

    @GetMapping
    public ApiResponse<List<OrderSummaryResponse>> getOrders(
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return ApiResponse.success(
                "Orders retrieved",
                orderService.getOrders(principal.getId())
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDetailResponse> getOrder(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                "Order retrieved",
                orderService.getOrder(principal.getId(), id)
        );
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<OrderDetailResponse> cancelOrder(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelOrderRequest request
    ) {
        return ApiResponse.success(
                "Order cancelled",
                orderService.cancelOrder(
                        principal.getId(),
                        id,
                        request == null ? null : request.reason()
                )
        );
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<OrderDetailResponse> confirmOrder(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                "Order confirmed",
                orderService.confirmOrder(principal.getId(), id)
        );
    }

    @PutMapping("/{id}/shipping")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<OrderDetailResponse> startShipping(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                "Order is shipping",
                orderService.startShipping(principal.getId(), id)
        );
    }

    @PutMapping("/{id}/delivered")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<OrderDetailResponse> markDelivered(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long id
    ) {
        return ApiResponse.success(
                "Order delivered",
                orderService.markDelivered(principal.getId(), id)
        );
    }
}
