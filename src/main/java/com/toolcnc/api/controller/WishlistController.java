package com.toolcnc.api.controller;

import com.toolcnc.api.dto.WishlistItemResponseDTO;
import com.toolcnc.api.model.User;
import com.toolcnc.api.repository.UserRepository;
import com.toolcnc.api.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserRepository userRepository;

    private User getAuthenticatedUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        String username = auth.getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    @GetMapping
    public ResponseEntity<List<WishlistItemResponseDTO>> getMyWishlist(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) return ResponseEntity.status(401).build();

        List<WishlistItemResponseDTO> items = wishlistService.getMyWishlist(user);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{productId}")
    public ResponseEntity<?> toggleWishlist(@PathVariable Long productId, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) return ResponseEntity.status(401).build();

        try {
            WishlistItemResponseDTO result = wishlistService.toggleWishlist(productId, user);
            if (result == null) {
                return ResponseEntity.ok(Map.of("added", false, "message", "Removed from wishlist"));
            } else {
                return ResponseEntity.ok(Map.of("added", true, "message", "Added to wishlist"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<?> removeFromWishlist(@PathVariable Long productId, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) return ResponseEntity.status(401).build();

        wishlistService.removeFromWishlist(productId, user);
        return ResponseEntity.ok(Map.of("message", "Removed from wishlist"));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearWishlist(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) return ResponseEntity.status(401).build();

        wishlistService.clearWishlist(user);
        return ResponseEntity.ok(Map.of("message", "Wishlist cleared"));
    }
}
