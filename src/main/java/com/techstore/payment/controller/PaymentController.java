package com.techstore.payment.controller;

import com.techstore.auth.dto.response.ApiResponse;
import com.techstore.auth.security.AuthUserPrincipal;
import com.techstore.payment.dto.request.CreateMomoPaymentRequest;
import com.techstore.payment.dto.request.MomoResultRequest;
import com.techstore.payment.dto.response.MomoIpnResult;
import com.techstore.payment.dto.response.MomoPaymentResponse;
import com.techstore.payment.dto.response.MomoReturnResponse;
import com.techstore.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/momo/create")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<MomoPaymentResponse> createMomoPayment(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody CreateMomoPaymentRequest request
    ) {
        return ApiResponse.success(
                "MoMo payment created",
                paymentService.createMomoPayment(principal.getId(), request.orderId())
        );
    }

    @PostMapping("/momo/ipn")
    public ResponseEntity<Void> handleMomoIpn(
            @RequestBody MomoResultRequest request
    ) {
        MomoIpnResult result = paymentService.handleMomoIpn(request);
        return result.accepted()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.badRequest().build();
    }

    @GetMapping("/momo/return")
    public ApiResponse<MomoReturnResponse> handleMomoReturn(
            @ModelAttribute MomoResultRequest request
    ) {
        return ApiResponse.success(
                "MoMo return processed",
                paymentService.handleMomoReturn(request)
        );
    }

    @GetMapping("/{orderId}/status")
    public ApiResponse<MomoPaymentResponse> getPaymentStatus(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long orderId
    ) {
        return ApiResponse.success(
                "Payment status retrieved",
                paymentService.getPaymentStatus(principal.getId(), orderId)
        );
    }
}
