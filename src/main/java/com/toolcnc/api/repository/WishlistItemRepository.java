package com.toolcnc.api.repository;

import com.toolcnc.api.model.Product;
import com.toolcnc.api.model.User;
import com.toolcnc.api.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"product"})
    List<WishlistItem> findByUserOrderByCreatedAtDesc(User user);
    Optional<WishlistItem> findByUserAndProduct(User user, Product product);
    void deleteByUser(User user);
    boolean existsByUserAndProduct(User user, Product product);
}
