package com.techstore.payment.repository;

import com.techstore.payment.entity.PaymentTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findAllByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    @EntityGraph(attributePaths = {"payment", "payment.order", "payment.order.user"})
    Optional<PaymentTransaction>
            findTopByMomoOrderIdAndRequestIdOrderByCreatedAtDesc(
                    String momoOrderId,
                    String requestId
            );
}
