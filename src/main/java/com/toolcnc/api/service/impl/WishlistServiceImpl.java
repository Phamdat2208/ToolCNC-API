package com.toolcnc.api.service.impl;

import com.toolcnc.api.dto.ProductSummaryDTO;
import com.toolcnc.api.dto.WishlistItemResponseDTO;
import com.toolcnc.api.model.Product;
import com.toolcnc.api.model.User;
import com.toolcnc.api.model.WishlistItem;
import com.toolcnc.api.repository.ProductRepository;
import com.toolcnc.api.repository.WishlistItemRepository;
import com.toolcnc.api.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistRepository;
    private final ProductRepository productRepository;

    @Override
    public List<WishlistItemResponseDTO> getMyWishlist(User user) {
        return wishlistRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WishlistItemResponseDTO toggleWishlist(Long productId, User user) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product does not exist"));

        Optional<WishlistItem> existing = wishlistRepository.findByUserAndProduct(user, product);

        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            // Trả về DTO rỗng hoặc null để báo hiệu là đã xóa, 
            // tuy nhiên controller yêu cầu trả về thông tin added = false.
            // Để tương thích với API cũ trả về Map, ta có thể throw exception hoặc 
            // đổi interface để Controller tự quyết định.
            // Ở đây ta có thể return null để ngụ ý "đã remove".
            return null;
        } else {
            WishlistItem newItem = WishlistItem.builder()
                    .user(user)
                    .product(product)
                    .build();
            WishlistItem saved = wishlistRepository.save(newItem);
            return convertToDTO(saved);
        }
    }

    @Override
    @Transactional
    public void removeFromWishlist(Long productId, User user) {
        Optional<Product> optionalProduct = productRepository.findById(productId);
        if (optionalProduct.isPresent()) {
            wishlistRepository.findByUserAndProduct(user, optionalProduct.get())
                    .ifPresent(wishlistRepository::delete);
        }
    }

    @Override
    @Transactional
    public void clearWishlist(User user) {
        wishlistRepository.deleteByUser(user);
    }

    private WishlistItemResponseDTO convertToDTO(WishlistItem item) {
        Product p = item.getProduct();
        ProductSummaryDTO productSummary = ProductSummaryDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .sku(p.getSku())
                .price(p.getPrice())
                .oldPrice(p.getOldPrice())
                .minPrice(p.getMinPrice())
                .maxPrice(p.getMaxPrice())
                .hasVariants(p.getHasVariants())
                .imageUrl(p.getImageUrl())
                .totalStock(p.getTotalStock())
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .brandName(p.getBrand() != null ? p.getBrand().getName() : null)
                .isActive(p.getIsActive())
                .build();

        return WishlistItemResponseDTO.builder()
                .id(item.getId())
                .product(productSummary)
                .build();
    }
}
