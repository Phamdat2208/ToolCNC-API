package com.toolcnc.api.service;

import com.toolcnc.api.dto.WishlistItemResponseDTO;
import com.toolcnc.api.model.User;

import java.util.List;

public interface WishlistService {
    List<WishlistItemResponseDTO> getMyWishlist(User user);
    WishlistItemResponseDTO toggleWishlist(Long productId, User user);
    void removeFromWishlist(Long productId, User user);
    void clearWishlist(User user);
}
