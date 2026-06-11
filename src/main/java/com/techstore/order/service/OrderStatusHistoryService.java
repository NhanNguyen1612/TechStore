package com.techstore.order.service;

import com.techstore.auth.entity.User;
import com.techstore.auth.repository.UserRepository;
import com.techstore.auth.security.AuthUserPrincipal;
import com.techstore.order.entity.Order;
import com.techstore.order.entity.OrderStatus;
import java.time.Instant;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class OrderStatusHistoryService {

    private final UserRepository userRepository;

    public OrderStatusHistoryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void record(
            Order order,
            OrderStatus oldStatus,
            String note,
            Instant time
    ) {
        OrderStatus newStatus = order.getStatus();
        if (oldStatus == newStatus) {
            return;
        }
        order.addStatusHistory(
                oldStatus,
                newStatus,
                note,
                currentUser(),
                time
        );
    }

    public void recordCreated(Order order, User customer, Instant time) {
        order.addStatusHistory(
                null,
                order.getStatus(),
                "Order created",
                customer,
                time
        );
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof AuthUserPrincipal principal)) {
            return null;
        }
        return userRepository.findById(principal.getId()).orElse(null);
    }
}
