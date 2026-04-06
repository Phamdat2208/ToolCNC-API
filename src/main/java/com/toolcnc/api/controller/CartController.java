package com.toolcnc.api.controller;

import com.toolcnc.api.dto.CartRequest;
import com.toolcnc.api.model.CartItem;
import com.toolcnc.api.model.Product;
import com.toolcnc.api.model.User;
import com.toolcnc.api.repository.CartItemRepository;
import com.toolcnc.api.repository.ProductRepository;
import com.toolcnc.api.repository.ProductVariantRepository;
import com.toolcnc.api.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private UserRepository userRepository;

    private User getAuthenticatedUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        String username = auth.getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    @GetMapping
    public ResponseEntity<?> getMyCart(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) return ResponseEntity.status(401).build();

        List<CartItem> cartItems = cartItemRepository.findByUserOrderByCreatedAtDesc(user);
        return ResponseEntity.ok(cartItems);
    }

    @PostMapping
    public ResponseEntity<?> addToCart(@Valid @RequestBody CartRequest request, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) return ResponseEntity.status(401).build();

        Optional<Product> optionalProduct = productRepository.findById(request.getProductId());
        if (optionalProduct.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Product does not exist"));
        }
        Product product = optionalProduct.get();

        // Handle Variant
        com.toolcnc.api.model.ProductVariant variant = null;
        if (request.getVariantId() != null) {
            Optional<com.toolcnc.api.model.ProductVariant> vOpt = productVariantRepository.findById(request.getVariantId());
            if (vOpt.isPresent()) {
                variant = vOpt.get();
            }
        }

        // Check if item (Product + Variant) already in cart
        Optional<CartItem> existingItemObj = cartItemRepository.findByUserAndProductAndVariant(user, product, variant);
        
        CartItem savedItem;
        if (existingItemObj.isPresent()) {
            CartItem item = existingItemObj.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            savedItem = cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .user(user)
                    .product(product)
                    .variant(variant)
                    .quantity(request.getQuantity())
                    .build();
            savedItem = cartItemRepository.save(newItem);
        }

        return ResponseEntity.ok(savedItem);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<?> updateCartItem(@PathVariable Long id, @Valid @RequestBody CartRequest request, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) return ResponseEntity.status(401).build();

        Optional<CartItem> existingItemObj = cartItemRepository.findById(id);
        if (existingItemObj.isEmpty() || !existingItemObj.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        CartItem item = existingItemObj.get();
        item.setQuantity(request.getQuantity());
        return ResponseEntity.ok(cartItemRepository.save(item));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<?> removeCartItem(@PathVariable Long id, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) return ResponseEntity.status(401).build();

        Optional<CartItem> existingItemObj = cartItemRepository.findById(id);
        if (existingItemObj.isPresent() && existingItemObj.get().getUser().getId().equals(user.getId())) {
            cartItemRepository.delete(existingItemObj.get());
        }
        
        return ResponseEntity.ok(Map.of("message", "Removed item from cart"));
    }

    @DeleteMapping("/clear")
    @Transactional
    public ResponseEntity<?> clearCart(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) return ResponseEntity.status(401).build();

        cartItemRepository.deleteByUser(user);
        
        return ResponseEntity.ok(Map.of("message", "Cart cleared completely"));
    }
}
