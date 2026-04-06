package com.toolcnc.api.repository;

import com.toolcnc.api.model.CartItem;
import com.toolcnc.api.model.Product;
import com.toolcnc.api.model.ProductVariant;
import com.toolcnc.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserOrderByCreatedAtDesc(User user);
    Optional<CartItem> findByUserAndProduct(User user, Product product);
    Optional<CartItem> findByUserAndProductAndVariant(User user, Product product, ProductVariant variant);
    void deleteByUser(User user);
}
