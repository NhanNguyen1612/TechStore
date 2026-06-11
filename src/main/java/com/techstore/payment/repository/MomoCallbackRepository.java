package com.techstore.payment.repository;

import com.techstore.payment.entity.MomoCallback;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MomoCallbackRepository extends JpaRepository<MomoCallback, Long> {

    List<MomoCallback> findAllByPaymentIdOrderByReceivedAtDesc(Long paymentId);
}
