package com.toolcnc.api.controller;

import com.toolcnc.api.dto.CartItemResponseDTO;
import com.toolcnc.api.dto.CartRequest;
import com.toolcnc.api.model.User;
import com.toolcnc.api.repository.UserRepository;
import com.toolcnc.api.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    private User getAuthenticatedUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        String username = auth.getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    @GetMapping
    public ResponseEntity<List<CartItemResponseDTO>> getMyCart(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) return ResponseEntity.status(401).build();

        List<CartItemResponseDTO> cartItems = cartService.getMyCart(user);
        return ResponseEntity.ok(cartItems);
    }

    @PostMapping
    public ResponseEntity<?> addToCart(@Valid @RequestBody CartRequest request, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) return ResponseEntity.status(401).build();

        try {
            CartItemResponseDTO savedItem = cartService.addToCart(request, user);
            return ResponseEntity.ok(savedItem);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<?> updateCartItem(@PathVariable Long id, @Valid @RequestBody CartRequest request, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) return ResponseEntity.status(401).build();

        try {
            CartItemResponseDTO updatedItem = cartService.updateCartItem(id, request, user);
            return ResponseEntity.ok(updatedItem);
        } catch (IllegalArgumentException e) {
            if ("Not authorized".equals(e.getMessage()) || "Cart item not found".equals(e.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<?> removeCartItem(@PathVariable Long id, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) return ResponseEntity.status(401).build();

        cartService.removeCartItem(id, user);
        return ResponseEntity.ok(Map.of("message", "Removed item from cart"));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart(Authentication auth) {
        User user = getAuthenticatedUser(auth);
        if (user == null) return ResponseEntity.status(401).build();

        cartService.clearCart(user);
        return ResponseEntity.ok(Map.of("message", "Cart cleared completely"));
    }
}
