package com.toolcnc.api.controller;

import com.toolcnc.api.model.Product;
import com.toolcnc.api.model.User;
import com.toolcnc.api.model.WishlistItem;
import com.toolcnc.api.repository.ProductRepository;
import com.toolcnc.api.repository.UserRepository;
import com.toolcnc.api.repository.WishlistItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/wishlist")
public class WishlistController {

    @Autowired
    private WishlistItemRepository wishlistRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private User getAuthenticatedUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        String username = auth.getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    @GetMapping
    public ResponseEntity<?> getMyWishlist(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) return ResponseEntity.status(401).build();

        List<WishlistItem> items = wishlistRepository.findByUserOrderByCreatedAtDesc(user);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{productId}")
    public ResponseEntity<?> toggleWishlist(@PathVariable Long productId, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) return ResponseEntity.status(401).build();

        Optional<Product> optionalProduct = productRepository.findById(productId);
        if (optionalProduct.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Product does not exist"));
        }
        Product product = optionalProduct.get();

        Optional<WishlistItem> existing = wishlistRepository.findByUserAndProduct(user, product);
        
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return ResponseEntity.ok(Map.of("added", false, "message", "Removed from wishlist"));
        } else {
            WishlistItem newItem = WishlistItem.builder()
                    .user(user)
                    .product(product)
                    .build();
            wishlistRepository.save(newItem);
            return ResponseEntity.ok(Map.of("added", true, "message", "Added to wishlist"));
        }
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<?> removeFromWishlist(@PathVariable Long productId, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) return ResponseEntity.status(401).build();

        Optional<Product> optionalProduct = productRepository.findById(productId);
        if (optionalProduct.isPresent()) {
            wishlistRepository.findByUserAndProduct(user, optionalProduct.get())
                    .ifPresent(wishlistRepository::delete);
        }
        
        return ResponseEntity.ok(Map.of("message", "Removed from wishlist"));
    }

    @DeleteMapping("/clear")
    @Transactional
    public ResponseEntity<?> clearWishlist(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) return ResponseEntity.status(401).build();

        wishlistRepository.deleteByUser(user);
        return ResponseEntity.ok(Map.of("message", "Wishlist cleared"));
    }
}
