package com.toolcnc.api.repository;

import com.toolcnc.api.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserId(Long userId, Pageable pageable);
    Optional<Order> findByOrderTrackingNumber(String trackingNumber);

    // Step 1: Fetch paginated IDs only (allows proper DB-level LIMIT/OFFSET)
    @Query("SELECT o.id FROM Order o WHERE o.user.id = :userId ORDER BY o.dateCreated DESC")
    Page<Long> findIdsByUserIdOrderByDateCreatedDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT o.id FROM Order o ORDER BY o.dateCreated DESC")
    Page<Long> findAllIdsByOrderByDateCreatedDesc(Pageable pageable);

    // Step 2: Fetch full orders with JOIN FETCH by IDs (no N+1 on orderItems)
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id IN :ids ORDER BY o.dateCreated DESC")
    List<Order> findByIdInWithItems(@Param("ids") List<Long> ids);
}
