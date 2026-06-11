package com.techstore.payment.service;

import com.techstore.payment.dto.request.MomoResultRequest;
import com.techstore.payment.dto.response.MomoIpnResult;
import com.techstore.payment.dto.response.MomoPaymentResponse;
import com.techstore.payment.dto.response.MomoReturnResponse;

public interface PaymentService {

    MomoPaymentResponse createMomoPayment(Long userId, Long orderId);

    MomoIpnResult handleMomoIpn(MomoResultRequest request);

    MomoReturnResponse handleMomoReturn(MomoResultRequest request);

    MomoPaymentResponse getPaymentStatus(Long userId, Long orderId);
}
