package com.toolcnc.api.service;

import com.toolcnc.api.dto.CartItemResponseDTO;
import com.toolcnc.api.dto.CartRequest;
import com.toolcnc.api.model.User;

import java.util.List;

public interface CartService {
    List<CartItemResponseDTO> getMyCart(User user);
    CartItemResponseDTO addToCart(CartRequest request, User user);
    CartItemResponseDTO updateCartItem(Long id, CartRequest request, User user);
    void removeCartItem(Long id, User user);
    void clearCart(User user);
}
