package com.toolcnc.api.service.impl;

import com.toolcnc.api.dto.CartItemResponseDTO;
import com.toolcnc.api.dto.CartRequest;
import com.toolcnc.api.dto.ProductSummaryDTO;
import com.toolcnc.api.dto.ProductVariantDTO;
import com.toolcnc.api.model.CartItem;
import com.toolcnc.api.model.Product;
import com.toolcnc.api.model.ProductVariant;
import com.toolcnc.api.model.User;
import com.toolcnc.api.repository.CartItemRepository;
import com.toolcnc.api.repository.ProductRepository;
import com.toolcnc.api.repository.ProductVariantRepository;
import com.toolcnc.api.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    public List<CartItemResponseDTO> getMyCart(User user) {
        return cartItemRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CartItemResponseDTO addToCart(CartRequest request, User user) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product does not exist"));

        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = productVariantRepository.findById(request.getVariantId()).orElse(null);
        }

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

        return convertToDTO(savedItem);
    }

    @Override
    @Transactional
    public CartItemResponseDTO updateCartItem(Long id, CartRequest request, User user) {
        CartItem item = cartItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        if (!item.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Not authorized");
        }

        item.setQuantity(request.getQuantity());
        CartItem savedItem = cartItemRepository.save(item);
        return convertToDTO(savedItem);
    }

    @Override
    @Transactional
    public void removeCartItem(Long id, User user) {
        Optional<CartItem> existingItemObj = cartItemRepository.findById(id);
        if (existingItemObj.isPresent() && existingItemObj.get().getUser().getId().equals(user.getId())) {
            cartItemRepository.delete(existingItemObj.get());
        }
    }

    @Override
    @Transactional
    public void clearCart(User user) {
        cartItemRepository.deleteByUser(user);
    }

    private CartItemResponseDTO convertToDTO(CartItem item) {
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

        ProductVariantDTO variantDTO = null;
        if (item.getVariant() != null) {
            ProductVariant v = item.getVariant();
            variantDTO = ProductVariantDTO.builder()
                    .id(v.getId())
                    .price(v.getPrice())
                    .stock(v.getStock())
                    .variantName(v.getVariantName())
                    .build();
        }

        return CartItemResponseDTO.builder()
                .id(item.getId())
                .quantity(item.getQuantity())
                .product(productSummary)
                .variant(variantDTO)
                .build();
    }
}
