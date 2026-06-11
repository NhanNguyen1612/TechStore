package com.techstore.admin.repository;

import com.techstore.admin.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Override
    @EntityGraph(attributePaths = "createdBy")
    Page<Notification> findAll(Pageable pageable);
}
