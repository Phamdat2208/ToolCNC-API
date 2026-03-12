package com.toolcnc.api.repository;

import com.toolcnc.api.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserId(Long userId, Pageable pageable);
    Optional<Order> findByOrderTrackingNumber(String trackingNumber);
    java.util.List<Order> findByUserIdOrderByDateCreatedDesc(Long userId);
    java.util.List<Order> findAllByOrderByDateCreatedDesc();
}
